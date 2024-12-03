package com.xemantic.anthropic

import com.xemantic.anthropic.batch.MessageBatchResponse
import com.xemantic.anthropic.content.Content
import com.xemantic.anthropic.content.Document
import com.xemantic.anthropic.error.ErrorResponse
import com.xemantic.anthropic.content.Image
import com.xemantic.anthropic.content.Text
import com.xemantic.anthropic.message.MessageResponse
import com.xemantic.anthropic.content.ToolResult
import com.xemantic.anthropic.content.ToolUse
import com.xemantic.anthropic.tool.BuiltInTool
import com.xemantic.anthropic.tool.DefaultTool
import com.xemantic.anthropic.tool.Tool
import com.xemantic.anthropic.tool.bash.Bash
import com.xemantic.anthropic.tool.computer.Computer
import com.xemantic.anthropic.tool.editor.TextEditor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// Note: surprisingly the order is important. This definition needs to go first.
@Suppress("UNUSED_ANONYMOUS_PARAMETER")
private val anthropicSerializersModule = SerializersModule {
  polymorphicDefaultDeserializer(Response::class) { ResponseSerializer }
  polymorphicDefaultDeserializer(Content::class) { ContentSerializer }
  polymorphic(Content::class) {
    subclass(Text::class)
    subclass(Image::class)
    subclass(ToolUse::class)
    subclass(ToolResult::class)
    subclass(Document::class)
  }
  polymorphicDefaultDeserializer(Tool::class) { ToolSerializer }
  polymorphicDefaultSerializer(Tool::class) { ToolSerializer }
  polymorphicDefaultDeserializer(BuiltInTool::class) {
    @Suppress("UNCHECKED_CAST")
    ToolSerializer as KSerializer<BuiltInTool>
  }
  polymorphicDefaultSerializer(BuiltInTool::class) { ToolSerializer }
}

/**
 * A JSON format suitable for communication with Anthropic API.
 */
val anthropicJson: Json = Json {
  serializersModule = anthropicSerializersModule
  allowSpecialFloatingPointValues = true
  explicitNulls = false
  encodeDefaults = true
}

private object ResponseSerializer : JsonContentPolymorphicSerializer<Response>(
  baseClass = Response::class
) {

  override fun selectDeserializer(
    element: JsonElement
  ) = when (
    val type = element.stringProperty("type")
  ) {
    "error" -> ErrorResponse.serializer()
    "message" -> MessageResponse.serializer()
    "message_batch" -> MessageBatchResponse.serializer()
    else -> throw SerializationException(
      "Unsupported Response type: $type, full response: $element"
    )
  }

}

private object ContentSerializer : JsonContentPolymorphicSerializer<Content>(
  baseClass = Content::class
) {

  override fun selectDeserializer(
    element: JsonElement
  ) = when (
    val type = element.stringProperty("type")
  ) {
    "text" -> Text.serializer()
    "image" -> Image.serializer()
    "tool_use" -> ToolUse.serializer()
    "tool_result" -> ToolResult.serializer()
    else -> throw SerializationException(
      "Unsupported Content type: $type, element: $element"
    )
  }

}

private object ToolSerializer : KSerializer<Tool> {

  @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
  override val descriptor: SerialDescriptor = buildSerialDescriptor(
    serialName = "com.xemantic.anthropic.Tool",
    kind = SerialKind.CONTEXTUAL
  )

  override fun serialize(encoder: Encoder, value: Tool) {
    val serializer = when (value) {
      is DefaultTool -> DefaultTool.serializer()
      is BuiltInTool -> when (value) {
        is Computer -> Computer.serializer()
        is TextEditor -> TextEditor.serializer()
        is Bash -> Bash.serializer()
        else -> throw SerializationException("Unsupported BuiltInTool type: $value")
      }
      else -> throw SerializationException("Unsupported Tool type: $value")
    }
    (serializer as KSerializer<Tool>).serialize(encoder, value)
  }

  override fun deserialize(decoder: Decoder): Tool {
    val input = decoder as JsonDecoder
    val tree = input.decodeJsonElement()
    val type = tree.jsonObject["type"]
    val serializer = if (type == null) {
      DefaultTool.serializer()
    } else {
      when (val name = tree.stringProperty("name")) {
        "computer" -> Computer.serializer()
        "str_replace_editor" -> TextEditor.serializer()
        "bash" -> Bash.serializer()
        else -> throw SerializationException("Unsupported Tool name: $name")
      }
    } as KSerializer<Tool>
    return input.json.decodeFromJsonElement(serializer, tree)
  }

}

private fun JsonElement.stringProperty(
  name: String
) = (jsonObject[name] ?: throw SerializationException(
      "Missing '$name' attribute in element: $this"
)).jsonPrimitive.content
