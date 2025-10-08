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
 * The default Anthropic API base.
 */
const val ANTHROPIC_API_BASE: String = "https://api.anthropic.com/"

/**
 * The default version to be passed to the `anthropic-version` HTTP header of each API request.
 */
const val DEFAULT_ANTHROPIC_VERSION: String = "2023-06-01"

expect val envApiKey: String?

expect val missingApiKeyMessage: String


/**
 * The public constructor function which for the Anthropic API client.
 *
 * @param block the config block to set up the API access.
 */
fun Anthropic(
    block: Anthropic.Config.() -> Unit = {}
): Anthropic {
    val config = Anthropic.Config().apply(block)
    val apiKey = if (config.apiKey != null) config.apiKey else envApiKey
    if (apiKey == null) {
        throw AnthropicConfigException(missingApiKeyMessage)
    }
    return Anthropic(
        apiKey = apiKey,
        anthropicVersion = config.anthropicVersion,
        anthropicBeta = if (config.anthropicBeta.isEmpty()) null else config.anthropicBeta.joinToString(","),
        apiBase = config.apiBase,
        defaultModel = config.defaultModel.id,
        defaultMaxTokens = config.defaultMaxTokens,
        defaultTools = config.defaultTools,
        directBrowserAccess = config.directBrowserAccess,
        logLevel = if (config.logHttp) LogLevel.ALL else LogLevel.NONE,
        modelMap = config.modelMap
    )
} // TODO this can be a second constructor, then toolMap can be private

class Anthropic internal constructor(
    val apiKey: String,
    val anthropicVersion: String,
    val anthropicBeta: String?,
    val apiBase: String,
    val defaultModel: String,
    val defaultMaxTokens: Int,
    val defaultTools: List<Tool>?,
    val directBrowserAccess: Boolean,
    val logLevel: LogLevel,
    private val modelMap: Map<String, AnthropicModel>
) {

    private val costCollector = CostCollector()

    val costWithUsage: CostWithUsage get() = costCollector.costWithUsage

    class Config {
        var apiKey: String? = null
        var anthropicVersion: String = DEFAULT_ANTHROPIC_VERSION
        var anthropicBeta: List<String> = emptyList()
        var apiBase: String = ANTHROPIC_API_BASE
        var defaultModel: AnthropicModel = Model.DEFAULT
        var defaultMaxTokens: Int = defaultModel.maxOutput

        /**
         * The list of tools used by default for every message request.
         * Can be overridden on per-request basis.
         */
        var defaultTools: List<Tool>? = null

        var directBrowserAccess: Boolean = false
        var logHttp: Boolean = false

        var modelMap: Map<String, AnthropicModel> = Model.entries.associateBy { it.id }

        operator fun Beta.unaryPlus() {
            anthropicBeta += this.id
        }

    }

    enum class Beta(val id: String) {
        OUTPUT_128K_2025_02_19("output-128k-2025-02-19"),
        COMPUTER_USE_2025_01_24("computer-use-2025-01-24"),
        COMPUTER_USE_2024_10_22("computer-use-2024-10-22")
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
            url(apiBase)
            header("x-api-key", apiKey)
            header("anthropic-version", anthropicVersion)
            if (anthropicBeta != null) {
                header("anthropic-beta", anthropicBeta)
            }
            if (directBrowserAccess) {
                header("anthropic-dangerous-direct-browser-access", true)
            }
        }

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
            val apiResponse = client.post("/v1/messages") {
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

            try {
                client.sse(
                    urlString = "/v1/messages",
                    request = {
                        method = HttpMethod.Post
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

    private val MessageResponse.anthropicModel: AnthropicModel
        get() = requireNotNull(
            modelMap[model]
        ) {
            "The model returned in the response is not known to Anthropic API client: $id"
        }

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
