package com.xemantic.anthropic

import com.xemantic.anthropic.error.AnthropicException
import com.xemantic.anthropic.error.ErrorResponse
import com.xemantic.anthropic.event.Event
import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.content.ToolUse
import com.xemantic.anthropic.message.MessageRequest
import com.xemantic.anthropic.message.MessageResponse
import com.xemantic.anthropic.tool.BuiltInTool
import com.xemantic.anthropic.tool.Tool
import com.xemantic.anthropic.tool.ToolInput
import com.xemantic.anthropic.usage.Cost
import com.xemantic.anthropic.usage.Usage
import com.xemantic.anthropic.usage.UsageCollector
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
  requireNotNull(apiKey) { missingApiKeyMessage }
  return Anthropic(
    apiKey = apiKey,
    anthropicVersion = config.anthropicVersion,
    anthropicBeta = config.anthropicBeta,
    apiBase = config.apiBase,
    defaultModel = config.defaultModel.id,
    defaultMaxTokens = config.defaultMaxTokens,
    directBrowserAccess = config.directBrowserAccess,
    logLevel = if (config.logHttp) LogLevel.ALL else LogLevel.NONE,
    modelMap = config.modelMap,
    toolMap = config.tools.associateBy { it.name },
  )
} // TODO this can be a second constructor, then toolMap can be private

class Anthropic internal constructor(
  val apiKey: String,
  val anthropicVersion: String,
  val anthropicBeta: String?,
  val apiBase: String,
  val defaultModel: String,
  val defaultMaxTokens: Int,
  val directBrowserAccess: Boolean,
  val logLevel: LogLevel,
  private val modelMap: Map<String, AnthropicModel>,
  private val toolMap: Map<String, Tool>
) {

  class Config {
    var apiKey: String? = null
    var anthropicVersion: String = DEFAULT_ANTHROPIC_VERSION
    var anthropicBeta: String? = null
    var apiBase: String = ANTHROPIC_API_BASE
    var defaultModel: AnthropicModel = Model.DEFAULT
    var defaultMaxTokens: Int = defaultModel.maxOutput

    var directBrowserAccess: Boolean = false
    var logHttp: Boolean = false

    var tools: List<Tool> = emptyList()

    var modelMap: Map<String, AnthropicModel> = Model.entries.associateBy { it.id }

    // TODO in the future this should be rather Tool builder
    inline fun <reified T : ToolInput> tool(
      cacheControl: CacheControl? = null,
      noinline inputInitializer: T.() -> Unit = {}
    ) {
      tools += Tool<T>(cacheControl, initializer = inputInitializer)
    }

    inline fun <reified T : BuiltInTool> builtInTool(
      tool: T,
      noinline inputInitializer: T.() -> Unit = {}
    ) {
      @Suppress("UNCHECKED_CAST")
      tool.inputInitializer = inputInitializer as ToolInput.() -> Unit
      tools += tool
    }

  }

  private val client = HttpClient {

    val retriableResponses = setOf<HttpStatusCode>(
      HttpStatusCode.RequestTimeout,
      HttpStatusCode.Conflict,
      HttpStatusCode.TooManyRequests,
      HttpStatusCode.InternalServerError
    )

    install(ContentNegotiation) {
      json(anthropicJson)
    }

    install(SSE)

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
    ): MessageResponse {

      val request = MessageRequest.Builder(
        defaultModel,
        defaultMaxTokens,
        toolMap
      ).apply(block).build()

      val apiResponse = client.post("/v1/messages") {
        contentType(ContentType.Application.Json)
        setBody(request)
      }
      val response = apiResponse.body<Response>()
      when (response) {
        is MessageResponse -> response.apply {
          updateUsage(response)
          content.filterIsInstance<ToolUse>()
            .forEach { toolUse ->
              val tool = toolMap[toolUse.name]
              if (tool != null) {
                toolUse.tool = tool
              } else {
                // Sometimes it happens that Claude is sending non-defined tool name in tool use
                // TODO in the future it should go to the stderr
                println("Error!!! Unexpected tool use: ${toolUse.name}")
              }
            }
        }
        is ErrorResponse -> throw AnthropicException(
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

      val request = MessageRequest.Builder(
        defaultModel,
        defaultMaxTokens,
        toolMap
      ).apply {
        block(this)
        stream = true
      }.build()

      client.sse(
        urlString = "/v1/messages",
        request = {
          method = HttpMethod.Post
          contentType(ContentType.Application.Json)
          setBody(request)
        }
      ) {
        incoming
          .map { it.data }
          .filterNotNull()
          .map { anthropicJson.decodeFromString<Event>(it) }
          .collect { event ->
            // TODO we need better way of handling subsequent deltas with usage
            if (event is Event.MessageStart) {
              // TODO more rules are needed here
              updateUsage(event.message)
            }
            emit(event)
          }
      }
    }

  }

  val messages = Messages()

  private val usageCollector = UsageCollector()

  val usage: Usage get() = usageCollector.usage

  val cost: Cost get() = usageCollector.cost

  override fun toString(): String = "Anthropic($usage, $cost)"

  private val MessageResponse.anthropicModel: AnthropicModel get() = requireNotNull(
    modelMap[model]
  ) {
    "The model returned in the response is not known to Anthropic API client: $id"
  }

  private fun updateUsage(response: MessageResponse) {
    usageCollector.update(
      modelCost = response.anthropicModel.cost,
      usage = response.usage
    )
  }

}
