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
import com.xemantic.ai.anthropic.tool.Tool
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
@SerialName("tool_use")
data class ToolUse(
    val id: String,
    val name: String,
    val input: JsonObject,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    @Transient
    @PublishedApi
    internal lateinit var tool: Tool

    fun decodeInput(): Any = anthropicJson.decodeFromJsonElement(
        deserializer = tool.inputSerializer,
        element = input
    )!!

    /**
     * Executes the tool and returns the result.
     *
     * @return A [ToolResult] containing the outcome of executing the tool.
     */
    suspend fun use(): ToolResult = ToolResult {
        toolUseId = id
        try {
            if (::tool.isInitialized) {
                val input = decodeInput()
                val result = tool.runner(input)
                if ((result != null) && (result !is Unit)) {
                    // TODO maybe we should also support ByteArray?
                    if (result is Content) {
                        content += result
                    } else if (result is List<*>) {
                        content += result.map { it!!.toContent() }
                    } else {
                        content += result.toContent()
                    }
                }
            } else {
                error("Cannot use unknown tool: $name")
            }
        } catch (e: Exception) {
            // TODO a better way to log this exception
            e.printStackTrace()
            error(e.message ?: "Unknown error occurred")
        }
    }

    private fun Any.toContent(): Content = this as? Content ?: Text(this.toString())

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
            content = buildList { addAll(content) },
            isError = isError,
            cacheControl = cacheControl
        )

    }

    fun copy(block: Builder.() -> Unit = {}): ToolResult {
        val builder = Builder()
        builder.toolUseId = toolUseId
        builder.content = content!!.toMutableList()
        builder.isError = isError
        builder.cacheControl = cacheControl
        block(builder)
        return builder.build()
    }

}

@OptIn(ExperimentalContracts::class)
inline fun ToolResult(
    block: ToolResult.Builder.() -> Unit = {}
): ToolResult {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ToolResult.Builder().also(block).build()
}
