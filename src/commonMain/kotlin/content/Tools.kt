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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.json.anthropicJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
@SerialName("tool_use")
class ToolUse private constructor(
    val id: String,
    val name: String,
    val input: JsonObject,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    class Builder {

        var id: String? = null
        var name: String? = null
        var input: JsonObject? = null
        var cacheControl: CacheControl? = null

        fun build(): ToolUse = ToolUse(
            requireNotNull(id) { "id cannot be null" },
            requireNotNull(name) { "name cannot be null" },
            requireNotNull(input) { "input cannot be null" },
            cacheControl
        )

    }

    inline fun <reified T> decodeInput(
        json: Json = anthropicJson
    ): T = json.decodeFromJsonElement(
        deserializer = json.serializersModule.serializer<T>(),
        element = input
    )

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): ToolUse {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return Builder().also {
            it.id = id
            it.name = name
            it.input = input
            it.cacheControl = cacheControl
            block(it)
        }.build()
    }

}

@OptIn(ExperimentalContracts::class)
fun ToolUse(
    block: ToolUse.Builder.() -> Unit
): ToolUse {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ToolUse.Builder().apply(block).build()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("server_tool_use")
abstract class ServerToolUse<Input> : Content() {

    abstract val id: String
    abstract val name: String
    abstract val input: Input

}

@Serializable
@SerialName("tool_result")
class ToolResult private constructor(
    @SerialName("tool_use_id")
    val toolUseId: String,
    val content: List<Content>? = null,
    @SerialName("is_error")
    val isError: Boolean? = false,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    class Builder : ContentListBuilder() {

        var toolUseId: String? = null

        var isError: Boolean? = null
        var cacheControl: CacheControl? = null

        fun error(message: String) {
            +message
            isError = true
        }

        operator fun plus(text: Text) {
            content += text
        }

        operator fun plus(image: Image) {
            content += image
        }

        fun build(): ToolResult = ToolResult(
            toolUseId = requireNotNull(toolUseId) {
                "toolUseId cannot be null"
            },
            content = if (content.isEmpty()) null else content,
            isError = isError,
            cacheControl = cacheControl
        )

    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): ToolResult {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return Builder().also {
            it.toolUseId = toolUseId
            it.content = content ?: emptyList()
            it.isError = isError
            it.cacheControl = cacheControl
            block(it)
        }.build()
    }

}

@OptIn(ExperimentalContracts::class)
inline fun ToolResult(
    block: ToolResult.Builder.() -> Unit = {}
): ToolResult {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ToolResult.Builder().apply(block).build()
}
