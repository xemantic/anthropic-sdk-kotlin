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

package com.xemantic.ai.anthropic.json

import com.xemantic.ai.anthropic.Response
import com.xemantic.ai.anthropic.batch.MessageBatchResponse
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.citation.Citation
import com.xemantic.ai.anthropic.content.*
import com.xemantic.ai.anthropic.error.ErrorResponse
import com.xemantic.ai.anthropic.location.UserLocation
import com.xemantic.ai.anthropic.message.MessageResponse
import com.xemantic.ai.anthropic.tool.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// Note: surprisingly the order is important. This definition needs to go first.
@Suppress("UNUSED_ANONYMOUS_PARAMETER")
private val anthropicSerializersModule = SerializersModule {
    polymorphicDefaultDeserializer(Response::class) { ResponseSerializer }

    // Use ContentSerializer as the ONLY serializer/deserializer for Content
    // We handle all Content subclasses manually in ContentSerializer
    polymorphicDefaultSerializer(Content::class) { ContentSerializer }
    polymorphicDefaultDeserializer(Content::class) { ContentSerializer }

    polymorphic(CacheControl::class) {
        subclass(
            CacheControl.Ephemeral::class,
            AdditionalPropertiesSerializer(
                CacheControl.Ephemeral.serializer()
            )
        )
        subclass(
            CacheControl.Unknown::class,
            AdditionalPropertiesSerializer(
                CacheControl.Unknown.serializer(),
                removeTypeFromDescriptor = true
            )
        )
        defaultDeserializer {
            AdditionalPropertiesSerializer(
                CacheControl.Unknown.serializer()
            )
        }
    }

    polymorphic(Source::class) {
        subclass(Source.Base64::class, AdditionalPropertiesSerializer(Source.Base64.serializer()))
        subclass(Source.Url::class, AdditionalPropertiesSerializer(Source.Url.serializer()))
        subclass(Source.Text::class, AdditionalPropertiesSerializer(Source.Text.serializer()))
        subclass(
            Source.Unknown::class,
            AdditionalPropertiesSerializer(Source.Unknown.serializer(), removeTypeFromDescriptor = true)
        )
        defaultDeserializer { AdditionalPropertiesSerializer(Source.Unknown.serializer()) }
    }

    polymorphic(Citation::class) {
        subclass(Citation.CharLocation::class)
        subclass(Citation.PageLocation::class)
        subclass(Citation.ContentBlockLocation::class)
        subclass(Citation.WebSearchResultLocation::class)
    }

    polymorphic(UserLocation::class) {
        subclass(UserLocation.Approximate::class, AdditionalPropertiesSerializer(UserLocation.Approximate.serializer()))
        subclass(
            UserLocation.Unknown::class,
            AdditionalPropertiesSerializer(UserLocation.Unknown.serializer(), removeTypeFromDescriptor = true)
        )
        defaultDeserializer { AdditionalPropertiesSerializer(UserLocation.Unknown.serializer()) }
    }
}

/**
 * A JSON format suitable for communication with Anthropic API.
 */
val anthropicJson: Json = Json {
    serializersModule = anthropicSerializersModule
    allowSpecialFloatingPointValues = true
    explicitNulls = false
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalSerializationApi::class)
val prettyAnthropicJson: Json = Json(from = anthropicJson) {
    prettyPrint = true
    prettyPrintIndent = "  "
}

inline fun <reified T> T.toPrettyJson(): String = prettyAnthropicJson.encodeToString<T>(this)

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

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
private class ServerToolUseWrapperSerializer<T : ServerToolUse<*>>(
    private val actualSerializer: KSerializer<T>
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = actualSerializer.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        // Serialize using a JsonTransformingSerializer approach
        require(encoder is JsonEncoder) {
            "ServerToolUse can only be serialized with Json format"
        }

        // Create a new encoder without the polymorphic context
        val json = encoder.json

        // Serialize the object using the actual serializer (without polymorphic wrapper)
        val element = json.encodeToJsonElement(actualSerializer, value)

        // Transform: ensure type is "server_tool_use"
        val transformed = buildJsonObject {
            put("type", JsonPrimitive("server_tool_use"))
            element.jsonObject.forEach { (key, jsonValue) ->
                if (key != "type") {
                    put(key, jsonValue)
                }
            }
        }

        // Write the transformed element directly using JsonEncoder method
        // This should bypass the polymorphic wrapper
        encoder.encodeJsonElement(transformed)
    }

    override fun deserialize(decoder: Decoder): T {
        return actualSerializer.deserialize(decoder)
    }

}

private fun selectServerToolUseDeserializer(
    element: JsonElement
) = when (
    val name = element.jsonObject["name"]?.jsonPrimitive?.content
) {
    "web_search" -> WebSearchServerToolUse.serializer()
    "web_fetch" -> WebFetchServerToolUse.serializer()
    else -> throw SerializationException(
        "Unsupported ServerToolUse name: $name, element: $element"
    )
}

object ContentSerializer : KSerializer<Content> {

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        serialName = "com.xemantic.ai.anthropic.content.Content",
        kind = SerialKind.CONTEXTUAL
    )

    override fun serialize(encoder: Encoder, value: Content) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException(
            "Content can only be serialized with Json format"
        )

        when (value) {
            // Special handling for ServerToolUse subclasses
            is WebSearchServerToolUse -> {
                ServerToolUseWrapperSerializer(WebSearchServerToolUse.serializer())
                    .serialize(encoder, value)
            }
            is WebFetchServerToolUse -> {
                ServerToolUseWrapperSerializer(WebFetchServerToolUse.serializer())
                    .serialize(encoder, value)
            }
            // For all other Content types, serialize normally with proper type field
            is Text -> serializeWithType(jsonEncoder, Text.serializer(), value, "text")
            is Image -> serializeWithType(jsonEncoder, Image.serializer(), value, "image")
            is ToolUse -> serializeWithType(jsonEncoder, ToolUse.serializer(), value, "tool_use")
            is WebSearchToolResult -> serializeWithType(jsonEncoder, WebSearchToolResult.serializer(), value, "web_search_tool_result")
            is WebFetchToolResult -> serializeWithType(jsonEncoder, WebFetchToolResult.serializer(), value, "web_fetch_tool_result")
            is ToolResult -> serializeWithType(jsonEncoder, ToolResult.serializer(), value, "tool_result")
            is Document -> serializeWithType(jsonEncoder, Document.serializer(), value, "document")
            else -> throw SerializationException("Unsupported Content type: ${value::class}")
        }
    }

    private fun <T : Content> serializeWithType(
        encoder: JsonEncoder,
        serializer: KSerializer<T>,
        value: T,
        typeName: String
    ) {
        val element = encoder.json.encodeToJsonElement(serializer, value)
        val withType = buildJsonObject {
            put("type", JsonPrimitive(typeName))
            element.jsonObject.forEach { (key, jsonValue) ->
                if (key != "type") {
                    put(key, jsonValue)
                }
            }
        }
        encoder.encodeJsonElement(withType)
    }

    override fun deserialize(decoder: Decoder): Content {
        val input = decoder as JsonDecoder
        val tree = input.decodeJsonElement()

        val type = tree.stringProperty("type")
        @Suppress("UNCHECKED_CAST")
        val serializer = when (type) {
            "text" -> Text.serializer()
            "image" -> Image.serializer()
            "tool_use" -> ToolUse.serializer()
            "server_tool_use" -> selectServerToolUseDeserializer(tree)
            "web_search_tool_result" -> WebSearchToolResult.serializer()
            "web_fetch_tool_result" -> WebFetchToolResult.serializer()
            "tool_result" -> ToolResult.serializer()
            "document" -> Document.serializer()
            else -> throw SerializationException(
                "Unsupported Content type: $type, element: $tree"
            )
        } as KSerializer<Content>
        return input.json.decodeFromJsonElement(serializer, tree)
    }

}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object WebSearchContentSerializer : KSerializer<WebSearchToolResult.WebSearchContent> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        serialName = "WebSearchContent",
        kind = StructureKind.LIST
    )

    override fun serialize(
        encoder: Encoder,
        value: WebSearchToolResult.WebSearchContent
    ) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException(
            "WebSearchContent can only be serialized with Json format"
        )

        when (value) {
            is WebSearchToolResult.Results -> {
                // Serialize each result with a "type" field
                val resultsWithType = value.results.map { result ->
                    val element = jsonEncoder.json.encodeToJsonElement(WebSearchResult.serializer(), result)
                    buildJsonObject {
                        put("type", JsonPrimitive("web_search_result"))
                        element.jsonObject.forEach { (key, jsonValue) ->
                            put(key, jsonValue)
                        }
                    }
                }
                jsonEncoder.encodeJsonElement(JsonArray(resultsWithType))
            }
            is WebSearchToolResult.Error -> {
                jsonEncoder.encodeSerializableValue(
                    WebSearchToolResult.Error.serializer(),
                    value
                )
            }
        }
    }

    override fun deserialize(
        decoder: Decoder
    ): WebSearchToolResult.WebSearchContent {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException(
            "WebSearchContent can only be deserialized with Json format"
        )

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> {
                // Success case: array of results
                val results = jsonDecoder.json.decodeFromJsonElement(
                    ListSerializer(WebSearchResult.serializer()),
                    element
                )
                WebSearchToolResult.Results(results)
            }
            is JsonObject -> {
                // Error case: single error object
                jsonDecoder.json.decodeFromJsonElement(
                    WebSearchToolResult.Error.serializer(),
                    element
                )
            }
            else -> throw SerializationException(
                "Unexpected content type: $element"
            )
        }
    }

}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object WebFetchContentSerializer : KSerializer<WebFetchToolResult.WebFetchContent> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        serialName = "WebFetchContent",
        kind = StructureKind.OBJECT
    )

    override fun serialize(
        encoder: Encoder,
        value: WebFetchToolResult.WebFetchContent
    ) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException(
            "WebFetchContent can only be serialized with Json format"
        )

        when (value) {
            is WebFetchToolResult.Result -> {
                serializeWithType(
                    jsonEncoder,
                    WebFetchToolResult.Result.serializer(),
                    value,
                    "web_fetch_result"
                )
            }
            is WebFetchToolResult.Error -> {
                serializeWithType(
                    jsonEncoder,
                    WebFetchToolResult.Error.serializer(),
                    value,
                    "web_fetch_tool_result_error"
                )
            }
        }
    }

    private fun <T> serializeWithType(
        encoder: JsonEncoder,
        serializer: KSerializer<T>,
        value: T,
        typeName: String
    ) {
        val element = encoder.json.encodeToJsonElement(serializer, value)
        val withType = buildJsonObject {
            put("type", JsonPrimitive(typeName))
            element.jsonObject.forEach { (key, jsonValue) ->
                if (key != "type") {
                    put(key, jsonValue)
                }
            }
        }
        encoder.encodeJsonElement(withType)
    }

    override fun deserialize(
        decoder: Decoder
    ): WebFetchToolResult.WebFetchContent {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException(
            "WebFetchContent can only be deserialized with Json format"
        )

        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonObject -> {
                if (element.containsKey("error_code")) {
                    // Error case: object with error_code
                    jsonDecoder.json.decodeFromJsonElement(
                        WebFetchToolResult.Error.serializer(),
                        element
                    )
                } else {
                    // Success case: result object
                    jsonDecoder.json.decodeFromJsonElement(
                        WebFetchToolResult.Result.serializer(),
                        element
                    )
                }
            }
            else -> throw SerializationException(
                "Unexpected content type: $element"
            )
        }
    }

}

object ToolSerializer : KSerializer<Tool> {

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        serialName = "com.xemantic.ai.anthropic.tool.Tool",
        kind = SerialKind.CONTEXTUAL
    )

    override fun serialize(encoder: Encoder, value: Tool) {
        val serializer = when (value) {
            is DefaultTool -> DefaultTool.serializer()
            is BuiltInTool<*> -> when (value) {
                is Computer -> Computer.serializer()
                is TextEditor -> TextEditor.serializer()
                is Bash -> Bash.serializer()
                is WebSearch -> WebSearch.serializer()
                is WebFetch -> WebFetch.serializer()
                else -> throw SerializationException("Unsupported BuiltInTool type: $value")
            }
            else -> throw SerializationException("Unsupported Tool type: $value")
        }
        @Suppress("UNCHECKED_CAST")
        (serializer as KSerializer<Tool>).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Tool {
        val input = decoder as JsonDecoder
        val tree = input.decodeJsonElement()
        val type = tree.jsonObject["type"]
        @Suppress("UNCHECKED_CAST")
        val serializer = if (type == null) {
            DefaultTool.serializer()
        } else {
            when (val name = tree.stringProperty("name")) {
                "computer" -> Computer.serializer()
                "str_replace_based_edit_tool" -> TextEditor.serializer()
                "bash" -> Bash.serializer()
                "web_search" -> WebSearch.serializer()
                "web_fetch" -> WebFetch.serializer()
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

private class AdditionalPropertiesSerializer<T : WithAdditionalProperties>(
    private val serializer: KSerializer<T>,
    removeTypeFromDescriptor: Boolean = false
) : KSerializer<T> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = if (removeTypeFromDescriptor) {
        buildClassSerialDescriptor(
            serialName = serializer.descriptor.serialName
        ) {
            val count = serializer.descriptor.elementsCount
            repeat(count) {
                serializer.descriptor.apply {
                    val name = getElementName(it)
                    if (name != "type") {
                        element(
                            elementName = name,
                            descriptor = getElementDescriptor(it),
                            annotations = getElementAnnotations(it),
                            isOptional = isElementOptional(it)
                        )
                    }
                }
            }
        }
    } else {
        serializer.descriptor
    }

    private val transformingSerializer = object : JsonTransformingSerializer<T>(serializer) {

        override fun transformSerialize(element: JsonElement): JsonElement {
            return buildJsonObject {
                element.jsonObject.filter { (name, _) ->
                    name != "additionalProperties"
                }.forEach { (key, value) ->
                    put(key, value)
                }
                val props = element.jsonObject["additionalProperties"]
                if (props != null && props.jsonObject.isNotEmpty()) {
                    props.jsonObject.forEach { (key, value) ->
                        put(key, value)
                    }
                }
            }
        }

        override fun transformDeserialize(element: JsonElement): JsonElement {
            @OptIn(ExperimentalSerializationApi::class)
            val knownProperties =
                descriptor.elementNames.toMutableSet() - "additionalProperties" + "type"
            val additionalProperties = JsonObject(
                element.jsonObject.filter { (name, _) ->
                    !knownProperties.contains(name)
                }.toMap()
            )
            return buildJsonObject {
                element.jsonObject.forEach { (key, value) ->
                    put(key, value)
                }
                if (additionalProperties.isNotEmpty()) {
                    put("additionalProperties", additionalProperties)
                }
            }
        }

    }

    override fun serialize(encoder: Encoder, value: T) {
        transformingSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): T {
        return transformingSerializer.deserialize(decoder)
    }

}
