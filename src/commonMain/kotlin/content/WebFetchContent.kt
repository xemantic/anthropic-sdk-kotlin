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
import com.xemantic.ai.anthropic.json.WebFetchContentSerializer
import com.xemantic.ai.anthropic.tool.WebFetch
import kotlinx.serialization.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Instant

@Serializable
class WebFetchServerToolUse private constructor(
    override val id: String,
    override val name: String = "web_fetch",
    override val input: WebFetch.Input,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : ServerToolUse<WebFetch.Input>() {

    class Builder {

        var id: String? = null
        var input: WebFetch.Input? = null
        var cacheControl: CacheControl? = null

        fun build(): WebFetchServerToolUse = WebFetchServerToolUse(
            id = requireNotNull(id) { "id cannot be null" },
            input = requireNotNull(input) { "input cannot be null" },
            cacheControl = cacheControl
        )

    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): WebFetchServerToolUse {
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
fun WebFetchServerToolUse(
    block: WebFetchServerToolUse.Builder.() -> Unit
): WebFetchServerToolUse {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return WebFetchServerToolUse.Builder().apply(block).build()
}

@Serializable
@SerialName("web_fetch_tool_result")
class WebFetchToolResult private constructor(
    @SerialName("tool_use_id")
    val toolUseId: String,
    val content: WebFetchContent,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    @Serializable(with = WebFetchContentSerializer::class)
    sealed interface WebFetchContent

    @Serializable
    @SerialName("web_fetch_result")
    data class Result(
        val url: String,
        @SerialName("retrieved_at")
        val retrievedAt: Instant,
        val content: Content
    ) : WebFetchContent

    @Serializable
    @SerialName("web_fetch_tool_result_error")
    data class Error(
        @SerialName("error_code")
        val errorCode: Code
    ) : WebFetchContent {

        @Serializable
        enum class Code {

            @SerialName("invalid_input")
            INVALID_INPUT,

            @SerialName("url_too_long")
            URL_TOO_LONG,

            @SerialName("url_not_allowed")
            URL_NOT_ALLOWED,

            @SerialName("url_not_accessible")
            URL_NOT_ACCESSIBLE,

            @SerialName("too_many_requests")
            TOO_MANY_REQUESTS,

            @SerialName("unsupported_content_type")
            UNSUPPORTED_CONTENT_TYPE,

            @SerialName("max_uses_exceeded")
            MAX_USES_EXCEEDED,

            @SerialName("unavailable")
            UNAVAILABLE

        }

    }

    class Builder {

        var toolUseId: String? = null
        var content: WebFetchContent? = null
        var cacheControl: CacheControl? = null

        fun build(): WebFetchToolResult = WebFetchToolResult(
            requireNotNull(toolUseId) { "toolUseId cannot be null" },
            requireNotNull(content) { "content cannot be null" },
            cacheControl
        )

    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): WebFetchToolResult {
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
fun WebFetchToolResult(
    block: WebFetchToolResult.Builder.() -> Unit
): WebFetchToolResult {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return WebFetchToolResult.Builder().apply(block).build()
}
