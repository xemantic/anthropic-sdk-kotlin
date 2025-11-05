/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
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
import com.xemantic.ai.anthropic.json.WebSearchContentSerializer
import com.xemantic.ai.anthropic.tool.WebSearch
import kotlinx.serialization.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
class WebSearchServerToolUse private constructor(
    override val id: String,
    override val name: String = "web_search",
    override val input: WebSearch.Input,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : ServerToolUse<WebSearch.Input>() {

    class Builder {

        var id: String? = null
        var input: WebSearch.Input? = null
        var cacheControl: CacheControl? = null

        fun build(): WebSearchServerToolUse = WebSearchServerToolUse(
            id = requireNotNull(id) { "id cannot be null" },
            input = requireNotNull(input) { "input cannot be null" },
            cacheControl = cacheControl
        )

    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): WebSearchServerToolUse {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return Builder().also {
            it.id = id
            it.input = input
            it.cacheControl = cacheControl
            block(it)
        }.build()
    }

}

@OptIn(ExperimentalContracts::class)
fun WebSearchServerToolUse(
    block: WebSearchServerToolUse.Builder.() -> Unit
): WebSearchServerToolUse {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return WebSearchServerToolUse.Builder().apply(block).build()
}

@Serializable
@SerialName("web_search_tool_result")
class WebSearchToolResult private constructor(
    @SerialName("tool_use_id")
    val toolUseId: String,
    val content: WebSearchContent,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    @Serializable(with = WebSearchContentSerializer::class)
    sealed interface WebSearchContent

    data class Results(
        val results: List<WebSearchResult>
    ) : WebSearchContent

    @Serializable
    @SerialName("web_search_tool_result_error")
    data class Error(
        @SerialName("error_code")
        val errorCode: Code
    ) : WebSearchContent {

        @Serializable
        enum class Code {

            @SerialName("too_many_requests")
            TOO_MANY_REQUESTS,

            @SerialName("invalid_input")
            INVALID_INPUT,

            @SerialName("max_uses_exceeded")
            MAX_USES_EXCEEDED,

            @SerialName("query_too_long")
            QUERY_TOO_LONG,

            @SerialName("unavailable")
            UNAVAILABLE

        }

    }

    class Builder {

        var toolUseId: String? = null
        var content: WebSearchContent? = null
        var cacheControl: CacheControl? = null

        fun build(): WebSearchToolResult = WebSearchToolResult(
            requireNotNull(toolUseId) { "toolUseId cannot be null" },
            requireNotNull(content) { "content cannot be null" },
            cacheControl
        )

    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): WebSearchToolResult {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return Builder().also {
            it.toolUseId = toolUseId
            it.content = content
            it.cacheControl = cacheControl
            block(it)
        }.build()
    }

}

@OptIn(ExperimentalContracts::class)
fun WebSearchToolResult(
    block: WebSearchToolResult.Builder.() -> Unit
): WebSearchToolResult {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return WebSearchToolResult.Builder().apply(block).build()
}

@Serializable
@SerialName("web_search_result")
data class WebSearchResult(
    val title: String,
    val url: String,
    @SerialName("encrypted_content")
    val encryptedContent: String,
    @SerialName("page_age")
    val pageAge: String? = null
)
