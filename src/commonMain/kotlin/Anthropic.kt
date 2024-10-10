package com.xemantic.anthropic

import com.xemantic.anthropic.event.Event
import com.xemantic.anthropic.message.Error
import com.xemantic.anthropic.message.ErrorResponse
import com.xemantic.anthropic.message.MessageRequest
import com.xemantic.anthropic.message.MessageResponse
import com.xemantic.anthropic.message.Tool
import com.xemantic.anthropic.message.ToolUse
import com.xemantic.anthropic.tool.UsableTool
import com.xemantic.anthropic.tool.toolOf
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
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
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

const val ANTHROPIC_API_BASE: String = "https://api.anthropic.com/"

const val DEFAULT_ANTHROPIC_VERSION: String = "2023-06-01"

class AnthropicException(
  error: Error,
  httpStatusCode: HttpStatusCode
) : RuntimeException(error.toString())

expect val envApiKey: String?

expect val missingApiKeyMessage: String

val anthropicJson: Json = Json {
  allowSpecialFloatingPointValues = true
  explicitNulls = false
  encodeDefaults = true
//  serializersModule = SerializersModule {
//    //contextual(UsableTool::class, UsableToolSerializer::class)
////    polymorphic(UsableTool::class) {
////    }
//  }
}

fun Anthropic(
  block: Anthropic.Config.() -> Unit = {}
): Anthropic {
  val config = Anthropic.Config().apply(block)
  val apiKey = if (config.apiKey != null) config.apiKey else envApiKey
  requireNotNull(apiKey) { missingApiKeyMessage }
  val defaultModel = if (config.defaultModel != null) config.defaultModel!! else "claude-3-opus-20240229"
  return Anthropic(
    apiKey = apiKey,
    anthropicVersion = config.anthropicVersion,
    anthropicBeta = config.anthropicBeta,
    apiBase = config.apiBase,
    defaultModel = defaultModel,
    directBrowserAccess = config.directBrowserAccess
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
  val directBrowserAccess: Boolean
) {

  class Config {
    var apiKey: String? = null
    var anthropicVersion: String = DEFAULT_ANTHROPIC_VERSION
    var anthropicBeta: String? = null
    var apiBase: String = ANTHROPIC_API_BASE
    var defaultModel: String? = null
    var directBrowserAccess: Boolean = false
    @PublishedApi
    internal var usableTools: List<ToolEntry<out UsableTool>> = emptyList()

    inline fun <reified T : UsableTool> tool(
      noinline block: T.() -> Unit = {}
    ) {
      val entry = ToolEntry(toolOf<T>(), serializer<T>(), block)
      usableTools += entry
    }

  }

  @PublishedApi
  internal class ToolEntry<T : UsableTool>(
    val tool: Tool, // TODO, no cache control
    val serializer: KSerializer<T>,
    val initializer: T.() -> Unit = {}
  )

  internal var toolEntryMap = mapOf<String, ToolEntry<UsableTool>>()

//  var usableTools: List<KClass<out Tool>> = emptyList()
//    set(value) {
//      toolMap += mapOf(value)
//      field = value
//    }

  inline fun <reified T : UsableTool> tool() {
    //usableTools += T::class
  }

  private val client = HttpClient {
    install(ContentNegotiation) {
      json(anthropicJson)
    }
    install(SSE)
    install(Logging) {
      level = LogLevel.BODY
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
        toolEntryMap = toolEntryMap
      ).apply(block).build()

      val response = client.post("/v1/messages") {
        contentType(ContentType.Application.Json)
        setBody(request)
      }
      if (response.status.isSuccess()) {
        return response.body<MessageResponse>().apply {
          content.filterIsInstance<ToolUse>()
            .forEach { toolUse ->
              val entry = toolEntryMap[toolUse.name]!!
              toolUse.toolEntry = entry
            }
        }
      } else {
        throw AnthropicException(
          error = response.body<ErrorResponse>().error,
          httpStatusCode = response.status
        )
      }
    }

    fun stream(
      block: MessageRequest.Builder.() -> Unit
    ): Flow<Event> = flow {

      val request = MessageRequest.Builder(
        defaultModel,
        toolEntryMap = toolEntryMap
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

