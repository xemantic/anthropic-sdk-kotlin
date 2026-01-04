/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.ai.anthropic

import com.xemantic.ai.anthropic.cost.CostCollector
import com.xemantic.ai.anthropic.cost.CostWithUsage
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.error.ErrorResponse
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.message.MessageRequest
import com.xemantic.ai.anthropic.message.MessageResponse
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.usage.Usage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * The default version to be passed to the `anthropic-version` HTTP header of each API request.
 */
const val DEFAULT_ANTHROPIC_VERSION: String = "2023-06-01"

/**
 * Gets the API key for a specific provider from environment variables.
 * Converts provider name to env var: "anthropic" -> "ANTHROPIC_API_KEY"
 */
internal expect fun getEnvApiKey(provider: String): String?

/**
 * Gets the API provider to use for testing from the API_PROVIDER_TO_TEST environment variable.
 * Used by test utilities to select which provider to test against.
 */
internal expect val envApiProviderToTest: String?

/**
 * The public constructor function which for the Anthropic API client.
 *
 * @param block the config block to set up the API access.
 */
fun Anthropic(
    block: Anthropic.Config.() -> Unit = {}
): Anthropic {
    val config = Anthropic.Config().apply(block)
    val allApiProviders: List<ApiProvider> =
        StandardApiProvider.entries + config.customApiProviders
    val availableApiKeys: Map<String, String> = allApiProviders.mapNotNull { info ->
        val apiKey = getEnvApiKey(info.id)
        apiKey?.let { info.id to it }
    }.toMap()
    if (config.apiKey == null && availableApiKeys.isEmpty()) {
        throw AnthropicConfigException(
            "No API key available. Either provide an explicit apiKey in the config block, " +
            "or set environment variables for supported providers (e.g., ANTHROPIC_API_KEY, MOONSHOT_API_KEY)."
        )
    }
    return Anthropic(
        apiKey = config.apiKey,
        anthropicVersion = config.anthropicVersion,
        anthropicBeta = if (config.anthropicBeta.isEmpty()) null else config.anthropicBeta.joinToString(","),
        apiBase = config.apiBase,
        defaultModel = config.defaultModel.id,
        defaultMaxTokens = config.defaultMaxTokens,
        defaultTools = config.defaultTools,
        directBrowserAccess = config.directBrowserAccess,
        logLevel = if (config.logHttp) LogLevel.ALL else LogLevel.NONE,
        modelMap = config.modelMap,
        availableApiKeys = availableApiKeys
    )
} // TODO this can be a second constructor, then toolMap can be private

class Anthropic internal constructor(
    val apiKey: String?,
    val anthropicVersion: String,
    val anthropicBeta: String?,
    val apiBase: String?,
    val defaultModel: String,
    val defaultMaxTokens: Int,
    val defaultTools: List<Tool>?,
    val directBrowserAccess: Boolean,
    val logLevel: LogLevel,
    private val modelMap: Map<String, AnthropicModel>,
    val availableApiKeys: Map<String, String>
) {

    private val costCollector = CostCollector()

    val costWithUsage: CostWithUsage get() = costCollector.costWithUsage

    /**
     * Holds the effective API configuration for a request.
     */
    private data class EffectiveConfig(
        val apiBase: String,
        val authHeader: Pair<String, String>
    )

    /**
     * Resolves the effective configuration for a model, supporting both
     * apiKey and apiBase from config and providers (API keys from environment).
     */
    private fun getEffectiveConfig(model: AnthropicModel): EffectiveConfig {
        val effectiveApiBase = apiBase ?: model.apiProvider.apiBase
        val effectiveApiKey = apiKey ?: availableApiKeys[model.apiProvider.id]
            ?: error("API key not found for provider '${model.apiProvider.id}'. Please set ${model.apiProvider.id.uppercase().replace("-", "_")}_API_KEY environment variable.")

        val authHeader = model.apiProvider.authHeaderType.createAuthHeader(effectiveApiKey)
        return EffectiveConfig(effectiveApiBase, authHeader)
    }

    class Config {
        var apiKey: String? = null
        var anthropicVersion: String = DEFAULT_ANTHROPIC_VERSION
        var anthropicBeta: List<String> = emptyList()
        var apiBase: String? = null
        var defaultModel: AnthropicModel = Model.DEFAULT
        var defaultMaxTokens: Int = defaultModel.maxOutput

        /**
         * The list of tools used by default for every message request.
         * Can be overridden on per-request basis.
         */
        var defaultTools: List<Tool>? = null

        var directBrowserAccess: Boolean = false
        var logHttp: Boolean = false

        var modelMap: MutableMap<String, AnthropicModel> =
            Model.entries.associateBy { it.id }.toMutableMap()

        operator fun Beta.unaryPlus() {
            anthropicBeta += this.id
        }

        var customApiProviders: List<UnknownApiProvider> = emptyList()

    }

    enum class Beta(val id: String) {
        OUTPUT_128K_2025_02_19("output-128k-2025-02-19"),
        COMPUTER_USE_2025_01_24("computer-use-2025-01-24"),
        COMPUTER_USE_2024_10_22("computer-use-2024-10-22"),
        WEB_SEARCH_2025_03_05("web-search-2025-03-05"),
        WEB_FETCH_2025_09_10("web-fetch-2025-09-10")
    }

    private val client = HttpClient {

        val retriableResponses = setOf(
            HttpStatusCode.RequestTimeout,
            HttpStatusCode.Conflict,
            HttpStatusCode.TooManyRequests,
            HttpStatusCode.InternalServerError
        )

        // declaration order matters :(
        install(SSE)

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status != HttpStatusCode.OK
                    && !(response.status in retriableResponses || response.status.value >= 500)) {
                    val bytes = response.readRawBytes()
                    val errorString = bytes.decodeToString()
                    val errorResponse = anthropicJson.decodeFromString<ErrorResponse>(errorString)
                    throw AnthropicApiException(
                        error = errorResponse.error,
                        httpStatusCode = response.status
                    )
                }
            }
        }

        install(ContentNegotiation) {
            json(anthropicJson)
        }

        if (logLevel != LogLevel.NONE) {
            install(Logging) {
                level = logLevel
            }
        }

        install(HttpRequestRetry) {
            exponentialDelay()
            maxRetries = 5
            retryIf { _, response ->
                response.status in retriableResponses || response.status.value >= 500
            }
        }

        defaultRequest {
            header("anthropic-version", anthropicVersion)
            if (anthropicBeta != null) {
                header("anthropic-beta", anthropicBeta)
            }
            if (directBrowserAccess) {
                header("anthropic-dangerous-direct-browser-access", true)
            }
        }

    }

    /**
     * Retrieves a model from the modelMap or throws a helpful error.
     * This validation happens before making API requests to fail fast.
     */
    private fun getModelOrThrow(modelId: String): AnthropicModel {
        return modelMap[modelId]
            ?: error("Unknown model '$modelId', consider adding modelMap[\"$modelId\"] = UnknownModel(...) when creating Anthropic client instance.")
    }

    inner class Messages {

        suspend fun create(
            block: MessageRequest.Builder.() -> Unit
        ): MessageResponse = create(
            request = MessageRequest.Builder().apply {
                applyDefaults()
                block(this)
            }.build()
        )

        private suspend fun create(
            request: MessageRequest
        ): MessageResponse {
            val model = getModelOrThrow(request.model)
            val config = getEffectiveConfig(model)

            val apiResponse = client.post {
                url("${config.apiBase}v1/messages")
                header(config.authHeader.first, config.authHeader.second)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val response = apiResponse.body<Response>()
            when (response) {
                is MessageResponse -> {
                    response.resolvedModel = response.anthropicModel
                    costCollector += response.costWithUsage
                }
                is ErrorResponse -> throw AnthropicApiException( // technically, this should be handled by the validator
                    error = response.error,
                    httpStatusCode = apiResponse.status
                )
                else -> throw RuntimeException(
                    "Unsupported response: $response"
                ) // should never happen
            }
            return response
        }

        fun stream(
            block: MessageRequest.Builder.() -> Unit
        ): Flow<Event> = flow {

            val request = MessageRequest.Builder().apply {
                applyDefaults()
                block()
                stream = true
            }.build()

            val model = getModelOrThrow(request.model)
            val config = getEffectiveConfig(model)

            try {
                client.sse(
                    urlString = "${config.apiBase}v1/messages",
                    request = {
                        method = HttpMethod.Post
                        header(config.authHeader.first, config.authHeader.second)
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                ) {
                    var usage = Usage.ZERO
                    lateinit var resolvedModel: AnthropicModel
                    incoming
                        .map { it.data }
                        .filterNotNull()
                        .map { anthropicJson.decodeFromString<Event>(it) }
                        .collect { event ->
                            when (event) {
                                is Event.MessageDelta -> {
                                    usage += Usage {
                                        inputTokens = 0
                                        outputTokens = event.usage.outputTokens
                                    }
                                }
                                is Event.MessageStart -> {
                                    resolvedModel = event.message.anthropicModel
                                    usage += event.message.usage
                                }
                                is Event.MessageStop -> {
                                    val costWithUsage = CostWithUsage(
                                        cost = resolvedModel.cost * usage,
                                        usage = usage
                                    )
                                    costCollector += costWithUsage
                                }
                                else -> { /* do nothing */ }
                            }
                            emit(event)
                        }
                }
            } catch (e: SSEClientException) {
                if (e.cause is AnthropicApiException) throw e.cause!!
                throw e
            }
        }

        private fun MessageRequest.Builder.applyDefaults() {
            model = defaultModel
            maxTokens = defaultMaxTokens
            if (defaultTools != null) {
                tools = defaultTools
            }
        }

    }

    val messages = Messages()

    /**
     * Gets the model definition from the response.
     * Note: This should always succeed since we validate the model exists before making requests.
     */
    private val MessageResponse.anthropicModel: AnthropicModel
        get() = getModelOrThrow(model)

    override fun toString(): String = "Anthropic($costWithUsage)"
}

open class AnthropicException(
    msg: String,
    cause: Throwable? = null
) : RuntimeException(msg, cause)

class AnthropicConfigException(
    msg: String,
    cause: Throwable? = null
) : AnthropicException(
    msg, cause
)
