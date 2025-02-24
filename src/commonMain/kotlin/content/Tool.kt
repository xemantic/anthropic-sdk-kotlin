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
import kotlinx.serialization.*
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

    fun input(): Any = anthropicJson.decodeFromJsonElement(
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
                val input = input()
                val result = tool.runner(input)
                if ((result != null) && (result !is Unit)) {
                    if (result is Content) {
                        +result
                    } else {
                        @OptIn(InternalSerializationApi::class)
                        val serializer = result::class.serializerOrNull() as KSerializer<Any>?
                        val value = if (serializer != null) {
                            anthropicJson.encodeToString(serializer, result)
                        } else {
                            result.toString()
                        }
                        +value
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
}

@ConsistentCopyVisibility
@Serializable
@SerialName("tool_result")
data class ToolResult private constructor(
    @SerialName("tool_use_id")
    val toolUseId: String,
    val content: List<Content>? = null,
    @SerialName("is_error")
    val isError: Boolean? = false,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    class Builder : ContentListBuilder {

        private class ToolResultList(
            private val list: MutableList<Content> = mutableListOf<Content>()
        ) : MutableList<Content> by list {

            override fun add(element: Content): Boolean {
                require(element is Image || element is Text) {
                    "Only Image and Text content element is allowed"
                }
                return list.add(element)
            }

            override fun add(index: Int, element: Content) {
                require(element is Image || element is Text) {
                    "Only Image and Text content element is allowed"
                }
                return list.add(index, element)
            }

            override fun addAll(elements: Collection<Content>): Boolean {
                require(elements.all { it is Image || it is Text }) {
                    "Only Image and Text content elements are allowed"
                }
                return list.addAll(elements)
            }

            override fun set(index: Int, element: Content): Content {
                require(element is Image || element is Text) {
                    "Only Image and Text content element is allowed"
                }
                return list.set(index, element)
            }
        }

        var toolUseId: String? = null

        override val content: MutableList<Content> = ToolResultList()

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
