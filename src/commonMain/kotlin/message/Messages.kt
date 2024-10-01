package com.xemantic.anthropic.message

import com.xemantic.anthropic.anthropicJson
import com.xemantic.anthropic.schema.JsonSchema
import com.xemantic.anthropic.schema.jsonSchemaOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.collections.mutableListOf

enum class Role {
  @SerialName("user")
  USER,
  @SerialName("assistant")
  ASSISTANT
}

@Serializable
data class Metadata(
  @SerialName("user_id")
  val userId: String
)

@Serializable
data class MessageRequest(
  val model: String,
  val messages: List<Message>,
  @SerialName("max_tokens")
  val maxTokens: Int,
  val metadata: Metadata?,
  @SerialName("stop_sequences")
  val stopSequences: List<String>?,
  val stream: Boolean?,
  val system: List<Text>?,
  val temperature: Double?,
  @SerialName("tool_choice")
  val toolChoice: ToolChoice?,
  val tools: List<Tool>?,
  val topK: Int?,
  val topP: Int?
) {

  class Builder(
    val defaultApiModel: String
  ) {
    var model: String? = null
    var maxTokens = 1024
    val messages = mutableListOf<Message>()
    var metadata = null
    val stopSequences = mutableListOf<String>()
    var stream: Boolean? = null
      internal set
    val systemTexts = mutableListOf<Text>()
    var temperature: Double? = null
    var toolChoice: ToolChoice? = null
    var tools: List<Tool>? = null
    val topK: Int? = null
    val topP: Int? = null

    fun messages(vararg messages: Message) {
      this.messages += messages.toList()
    }

    operator fun Message.unaryPlus() {
      messages += this
    }

    operator fun List<Message>.unaryPlus() {
      messages += this
    }

    fun stopSequences(vararg stopSequences: String) {
      this.stopSequences += stopSequences.toList()
    }

    var system: String?
      get() = if (systemTexts.isEmpty()) null else systemTexts[0].text
      set(value) {
        systemTexts.clear()
        if (value != null) {
          systemTexts.add(Text(text = value))
        }
      }

    fun build(): MessageRequest = MessageRequest(
      model = if (model != null) model!! else defaultApiModel,
      maxTokens = maxTokens,
      messages = messages,
      metadata = metadata,
      stopSequences = stopSequences.toNullIfEmpty(),
      stream = if (stream != null) stream else null,
      system = systemTexts.toNullIfEmpty(),
      temperature = temperature,
      toolChoice = toolChoice,
      tools = tools,
      topK = topK,
      topP = topP
    )
  }

}

fun MessageRequest(
  defaultModel: String,
  block: MessageRequest.Builder.() -> Unit
): MessageRequest {
  val builder = MessageRequest.Builder(defaultModel)
  block(builder)
  return builder.build()
}

@Serializable
data class MessageResponse(
  val id: String,
  val type: Type,
  val role: Role,
  val content: List<Content>, // limited to Text and ToolUse
  val model: String,
  @SerialName("stop_reason")
  val stopReason: StopReason?,
  @SerialName("stop_sequence")
  val stopSequence: String?,
  val usage: Usage
) {

  enum class Type {
    @SerialName("message")
    MESSAGE
  }
}

@Serializable
data class ErrorResponse(
  val type: String,
  val error: Error
)

@Serializable
data class Error(
  val type: String, val message: String
)

@Serializable
data class Message(
  val role: Role,
  val content: List<Content>
) {

  class Builder {
    var role = Role.USER
    val content = mutableListOf<Content>()

    operator fun Content.unaryPlus() {
      content += this
    }

    operator fun List<Content>.unaryPlus() {
      content += this
    }

    operator fun String.unaryPlus() {
      content += Text(this)
    }

    fun build() = Message(
      role = role,
      content = content
    )
  }

}

fun Message(block: Message.Builder.() -> Unit): Message {
  val builder = Message.Builder()
  block(builder)
  return builder.build()
}

@Serializable
data class Tool(
  val name: String,
  val description: String,
  @SerialName("input_schema")
  val inputSchema: JsonSchema,
  val cacheControl: CacheControl?
)

inline fun <reified T> Tool(
  description: String,
  cacheControl: CacheControl? = null
): Tool = Tool(
  name = T::class.qualifiedName!!,
  description = description,
  inputSchema = jsonSchemaOf<T>(),
  cacheControl = cacheControl
)

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Content {

  abstract val cacheControl: CacheControl?

}

@Serializable
@SerialName("text")
data class Text(
  val text: String,
  override val cacheControl: CacheControl? = null,
) : Content()

@Serializable
@SerialName("image")
data class Image(
  val source: Source,
  override val cacheControl: CacheControl? = null
) : Content() {

  enum class MediaType {
    @SerialName("image/jpeg")
    IMAGE_JPEG,
    @SerialName("image/png")
    IMAGE_PNG,
    @SerialName("image/gif")
    IMAGE_GIF,
    @SerialName("image/webp")
    IMAGE_WEBP
  }

  @Serializable
  data class Source(
    val type: Type = Type.BASE64,
    @SerialName("media_type")
    val mediaType: MediaType,
    val data: String
  ) {

    enum class Type {
      @SerialName("base64")
      BASE64
    }

  }

}

@Serializable
@SerialName("tool_use")
data class ToolUse(
  override val cacheControl: CacheControl? = null,
  val id: String,
  val name: String,
  val input: JsonObject
) : Content() {

  inline fun <reified T> input(): T =
    anthropicJson.decodeFromJsonElement<T>(input)

}

@Serializable
@SerialName("tool_result")
data class ToolResult(
  override val cacheControl: CacheControl? = null,
  @SerialName("tool_use_id")
  val toolUseId: String,
  @SerialName("is_error")
  val isError: Boolean = false,
  val content: List<Content>
) : Content()

@Serializable
data class CacheControl(
  val type: Type
) {

  @SerialName("ephemeral")
  enum class Type {
    EPHEMERAL
  }

}

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class ToolChoice {

  @Serializable
  @SerialName("auto")
  class Auto : ToolChoice()

  @Serializable
  @SerialName("any")
  class Any : ToolChoice()

  @Serializable
  @SerialName("tool")
  class Tool(
    val name: String
  ) : ToolChoice()

}

enum class StopReason {
  @SerialName("end_turn")
  END_TURN,
  @SerialName("max_tokens")
  MAX_TOKENS,
  @SerialName("stop_sequence")
  STOP_SEQUENCE,
  @SerialName("tool_use")
  TOOL_USE
}

@Serializable
data class Usage(
  @SerialName("input_tokens")
  val inputTokens: Int,
  @SerialName("cache_creation_input_tokens")
  val cacheCreationInputTokens: Int?,
  @SerialName("cache_read_input_tokens")
  val cacheReadInputTokens: Int?,
  @SerialName("output_tokens")
  val outputTokens: Int
)
