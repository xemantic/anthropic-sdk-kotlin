package com.xemantic.anthropic.message

import com.xemantic.anthropic.schema.JsonSchema
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.serializerOrNull
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

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
  val topK: Int?,
  val topP: Int?
) {

  class Builder(
    val defaultApiModel: String
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

    fun tools(vararg classes: KClass<out UsableTool>) {
      // TODO it needs access to Anthropic, therefore either needs a constructor parameter, or needs to be inner class
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
      model = if (model != null) model!! else defaultApiModel,
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

  fun asMessage() = Message {
    role = Role.ASSISTANT
    content += this.content
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
  val input: UsableTool
) : Content() {

  fun use(): ToolResult = input.use(
    toolUseId = id
  )

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
  val cacheCreationInputTokens: Int?,
  @SerialName("cache_read_input_tokens")
  val cacheReadInputTokens: Int?,
  @SerialName("output_tokens")
  val outputTokens: Int
)


interface CacheableBuilder {

  var cacheControl: CacheControl?

  var cache: Boolean
    get() = cacheControl != null
    set(value) {
      if (value) {
        cacheControl = CacheControl(type = CacheControl.Type.EPHEMERAL)
      } else {
        cacheControl = null
      }
    }

}

//class UsableToolSerializer : JsonContentPolymorphicSerializer2<UsableTool>(UsableTool::class) {
//
////  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UsableTool") {
////    element<String>("type")
////    element<JsonElement>("data")
////  }
////
////  override fun serialize(
////    encoder: Encoder,
////    value: UsableTool
////  ) {
//////    val polymorphic: SerializationStrategy<String> = serializersModule.getPolymorphic(UsableTool::class, "foo")
////    PolymorphicSerializer(UsableTool::class)
//////    encoder.encodeString(value)
//////    encoder.encodeString(value.name)
//////    polymorphic.seri
//////    encoder.encodeSerializableValue(polymorphic)
////  }
////
////  override fun deserialize(decoder: Decoder): UsableTool {
////    require(decoder is JsonDecoder) { "This serializer can be used only with Json format" }
////    val name = decoder.decodeString()
////    val polymorphic = decoder.serializersModule.getPolymorphic(UsableTool::class, name)
////    val id = decoder.decodeString()
////    return DummyUsableTool()
////  }
//
//  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<UsableTool> {
//    println(element)
//    TODO("dupa dupa Not yet implemented")
//  }
//
//}


//class UsableToolSerializer : JsonContentPolymorphicSerializer2<UsableTool>(
//  UsableTool::class
//)

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
open class JsonContentPolymorphicSerializer2<T : Any>(private val baseClass: KClass<T>) : KSerializer<T> {
  /**
   * A descriptor for this set of content-based serializers.
   * By default, it uses the name composed of [baseClass] simple name,
   * kind is set to [PolymorphicKind.SEALED] and contains 0 elements.
   *
   * However, this descriptor can be overridden to achieve better representation of custom transformed JSON shape
   * for schema generating/introspection purposes.
   */
  override val descriptor: SerialDescriptor =
    buildSerialDescriptor("JsonContentPolymorphicSerializer<${baseClass.simpleName}>", PolymorphicKind.SEALED)

  final override fun serialize(encoder: Encoder, value: T) {
    val actualSerializer =
      encoder.serializersModule.getPolymorphic(baseClass, value)
        ?: value::class.serializerOrNull()
        ?: throw SerializationException("fiu fiu")
    @Suppress("UNCHECKED_CAST")
    (actualSerializer as KSerializer<T>).serialize(encoder, value)
  }

  final override fun deserialize(decoder: Decoder): T {
    val input = decoder.asJsonDecoder()
    input.json.serializersModule.getPolymorphic(UsableTool::class, "foo")
    val tree = input.decodeJsonElement()

    @Suppress("UNCHECKED_CAST")
    val actualSerializer = String.serializer() as KSerializer<T>
    return input.json.decodeFromJsonElement(actualSerializer, tree)
  }

}

internal fun Decoder.asJsonDecoder(): JsonDecoder = this as? JsonDecoder
  ?: throw IllegalStateException(
    "This serializer can be used only with Json format." +
        "Expected Decoder to be JsonDecoder, got ${this::class}"
  )