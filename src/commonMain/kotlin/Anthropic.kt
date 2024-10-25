package com.xemantic.anthropic

import com.xemantic.anthropic.event.Event
import com.xemantic.anthropic.message.MessageRequest
import com.xemantic.anthropic.message.Tool
import com.xemantic.anthropic.message.ToolUse
import com.xemantic.anthropic.tool.UsableTool
import com.xemantic.anthropic.tool.toolOf
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * The default Anthropic API base.
 */
const val ANTHROPIC_API_BASE: String = "https://api.anthropic.com/"

/**
 * The default version to be passed to the `anthropic-version` HTTP header of each API request.
 */
const val DEFAULT_ANTHROPIC_VERSION: String = "2023-06-01"

/**
 * An exception thrown when API requests returns error.
 */
class AnthropicException(
  error: Error,
  httpStatusCode: HttpStatusCode
) : RuntimeException(error.toString())

expect val envApiKey: String?

expect val missingApiKeyMessage: String

/**
 * A JSON format suitable for communication with Anthropic API.
 */
val anthropicJson: Json = Json {
  allowSpecialFloatingPointValues = true
  explicitNulls = false
  encodeDefaults = true
}

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
    logLevel = if (config.logHttp) LogLevel.ALL else LogLevel.NONE
  ).apply {
    toolEntryMap = (config.usableTools as List<Anthropic.ToolEntry<UsableTool>>).associateBy { it.tool.name }
  }
} // TODO this can be a second constructor, then toolMap can be private

class Anthropic internal constructor(
  val apiKey: String,
  val anthropicVersion: String,
  val anthropicBeta: String?,
  val apiBase: String,
  val defaultModel: String,
  val defaultMaxTokens: Int,
  val directBrowserAccess: Boolean,
  val logLevel: LogLevel
) {

  class Config {
    var apiKey: String? = null
    var anthropicVersion: String = DEFAULT_ANTHROPIC_VERSION
    var anthropicBeta: String? = null
    var apiBase: String = ANTHROPIC_API_BASE
    var defaultModel: Model = Model.DEFAULT
    var defaultMaxTokens: Int = defaultModel.maxOutput

    var directBrowserAccess: Boolean = false
    var logHttp: Boolean = false

    @PublishedApi
    internal var usableTools: List<ToolEntry<out UsableTool>> = emptyList()

    inline fun <reified T : UsableTool> tool(
      noinline block: T.() -> Unit = {}
    ) {
      val entry = ToolEntry(typeOf<T>(), toolOf<T>(), serializer<T>(), block)
      usableTools += entry
    }

  }

  @PublishedApi
  internal class ToolEntry<T : UsableTool>(
    val type: KType,
    val tool: Tool, // TODO, no cache control
    val serializer: KSerializer<T>,
    val initialize: T.() -> Unit = {}
  )

  internal var toolEntryMap = mapOf<String, ToolEntry<UsableTool>>()

  private val client = HttpClient {

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
      retryOnServerErrors(maxRetries = 5)
      exponentialDelay()
      maxRetries = 5
      retryIf { _, response ->
        response.status == HttpStatusCode.TooManyRequests
            || response.status.value == 529 // Overloaded
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
        toolEntryMap
      ).apply(block).build()

      val apiResponse = client.post("/v1/messages") {
        contentType(ContentType.Application.Json)
        setBody(request)
      }
      val response = apiResponse.body<Response>()
      when (response) {
        is MessageResponse -> response.apply {
          content.filterIsInstance<ToolUse>()
            .forEach { toolUse ->
              val entry = toolEntryMap[toolUse.name]!!
              toolUse.toolEntry = entry
            }
        }
        is ErrorResponse -> throw AnthropicException(
          error = response.error,
          httpStatusCode = apiResponse.status
        )
        else -> throw RuntimeException("Unsupported response: $response") // should never happen
      }
      return response
    }

    fun stream(
      block: MessageRequest.Builder.() -> Unit
    ): Flow<Event> = flow {

      val request = MessageRequest.Builder(
        defaultModel,
        defaultMaxTokens,
        toolEntryMap
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
          .filter { it.data != null }
          .map { anthropicJson.decodeFromString<Event>(it.data!!) }
          .collect {
            emit(it)
          }
      }
    }

  }

  val messages = Messages()

}

