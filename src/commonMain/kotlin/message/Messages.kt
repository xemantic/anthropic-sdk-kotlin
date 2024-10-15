package com.xemantic.anthropic.message

import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.anthropicJson
import com.xemantic.anthropic.schema.JsonSchema
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject
import kotlin.collections.mutableListOf
import kotlin.reflect.typeOf

/**
 * The roles that can be taken by entities in a conversation.
 */
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
  val system: List<System>?,
  val temperature: Double?,
  @SerialName("tool_choice")
  val toolChoice: ToolChoice?,
  val tools: List<Tool>?,
  @SerialName("top_k")
  val topK: Int?,
  @SerialName("top_p")
  val topP: Int?
) {

  class Builder internal constructor(
    private val defaultModel: String,
    @PublishedApi
    internal val toolEntryMap: Map<String, Anthropic.ToolEntry<out UsableTool>>
  ) {
    var model: String? = null
    var maxTokens = 1024
    var messages: List<Message> = mutableListOf<Message>()
    var metadata = null
    val stopSequences = mutableListOf<String>()
    var stream: Boolean? = null
      internal set
    var system: List<System>? = null
    var temperature: Double? = null
    var toolChoice: ToolChoice? = null
    var tools: List<Tool>? = null
    val topK: Int? = null
    val topP: Int? = null

    fun useTools() {
      tools = toolEntryMap.values.map { it.tool }
    }

    /**
     * Sets both, the [tools] list and the [toolChoice] with
     * just one tool to use, forcing the API to respond with the [ToolUse].
     */
    inline fun <reified T : UsableTool> useTool() {
      val type = typeOf<T>()
      val toolEntry = toolEntryMap.values.find { it.type == type }
      requireNotNull(toolEntry) {
        "No such tool defined in Anthropic client: ${T::class.qualifiedName}"
      }
      tools = listOf(toolEntry.tool)
      toolChoice = ToolChoice.Tool(name = toolEntry.tool.name)
    }

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

    fun system(
      text: String
    ) {
      system = listOf(System(text = text))
    }

    fun build(): MessageRequest = MessageRequest(
      model = if (model != null) model!! else defaultModel,
      maxTokens = maxTokens,
      messages = messages,
      metadata = metadata,
      stopSequences = stopSequences.toNullIfEmpty(),
      stream = if (stream != null) stream else null,
      system = system,
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
  val builder = MessageRequest.Builder(
    defaultModel, emptyMap()
  )
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

  fun asMessage(): Message = Message {
    role = Role.ASSISTANT
    content += this@MessageResponse.content
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
data class System(
  @SerialName("cache_control")
  val cacheControl: CacheControl? = null,
  val type: Type = Type.TEXT,
  val text: String? = null,
) {

  enum class Type {
    @SerialName("text")
    TEXT
  }

}

@Serializable
data class Tool(
  val name: String,
  val description: String,
  @SerialName("input_schema")
  val inputSchema: JsonSchema,
  @SerialName("cache_control")
  val cacheControl: CacheControl?
)

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Content {

  @SerialName("cache_control")
  abstract val cacheControl: CacheControl?

}

@Serializable
@SerialName("text")
data class Text(
  val text: String,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null,
) : Content()

@Serializable
@SerialName("image")
data class Image(
  val source: Source,
  @SerialName("cache_control")
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

@SerialName("tool_use")
@Serializable
data class ToolUse(
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null,
  val id: String,
  val name: String,
  val input: JsonObject
) : Content() {

  @Transient
  @PublishedApi
  internal lateinit var toolEntry: Anthropic.ToolEntry<UsableTool>

  inline fun <reified T> input(): T = anthropicJson.decodeFromJsonElement(
    deserializer = toolEntry.serializer as KSerializer<T>,
    element = input
  )

  suspend fun use(): ToolResult {
    val tool = anthropicJson.decodeFromJsonElement(
      deserializer = toolEntry.serializer,
      element = input
    )
    val result = try {
      toolEntry.initialize(tool)
      tool.use(toolUseId = id)
    } catch (e: Exception) {
      ToolResult(
        toolUseId = id,
        isError = true,
        content = listOf(
          Text(
            text = e.message ?: "Unknown error occurred"
          )
        )
      )
    }
    return result
  }

}

@Serializable
@SerialName("tool_result")
data class ToolResult(
  @SerialName("tool_use_id")
  val toolUseId: String,
  val content: List<Content>, // TODO only Text, Image allowed here, should be accessible in gthe builder
  @SerialName("is_error")
  val isError: Boolean = false,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
) : Content()

fun ToolResult(
  toolUseId: String,
  text: String
): ToolResult = ToolResult(
  toolUseId,
  content = listOf(Text(text))
)

@Serializable
data class CacheControl(
  val type: Type
) {

  enum class Type {
    @SerialName("ephemeral")
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
  val cacheCreationInputTokens: Int? = null,
  @SerialName("cache_read_input_tokens")
  val cacheReadInputTokens: Int? = null,
  @SerialName("output_tokens")
  val outputTokens: Int
)
