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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A thinking content block containing Claude's internal reasoning process.
 *
 * When extended thinking is enabled, Claude creates thinking blocks where it
 * outputs its step-by-step reasoning before providing the final answer.
 *
 * Note: For Claude 4 models, this represents summarized thinking. The full
 * thinking tokens are still charged but the output is summarized.
 *
 * @property thinking The thinking content (full for Claude 3.7, summarized for Claude 4)
 * @property signature Cryptographic signature verifying the thinking content
 * @property cacheControl Cache control for this content block
 */
@Serializable
@SerialName("thinking")
class ThinkingBlock private constructor(
    val thinking: String,
    val signature: String,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    class Builder {
        var thinking: String? = null
        var signature: String? = null
        var cacheControl: CacheControl? = null

        fun build(): ThinkingBlock = ThinkingBlock(
            thinking = requireNotNull(thinking) { "thinking cannot be null" },
            signature = requireNotNull(signature) { "signature cannot be null" },
            cacheControl = cacheControl
        )
    }

    fun copy(
        block: Builder.() -> Unit
    ): ThinkingBlock = Builder().apply {
        thinking = this@ThinkingBlock.thinking
        signature = this@ThinkingBlock.signature
        cacheControl = this@ThinkingBlock.cacheControl
    }.apply(block).build()

}

@OptIn(ExperimentalContracts::class)
fun ThinkingBlock(block: ThinkingBlock.Builder.() -> Unit): ThinkingBlock {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ThinkingBlock.Builder().also(block).build()
}

/**
 * A thinking content block parameter for passing previous thinking in requests.
 *
 * Used when continuing multi-turn conversations that include thinking blocks
 * from previous assistant responses.
 *
 * @property thinking The thinking content from a previous response
 * @property signature The cryptographic signature from the previous response
 * @property cacheControl Cache control for this content block
 */
@Serializable
@SerialName("thinking")
class ThinkingBlockParam private constructor(
    val thinking: String,
    val signature: String,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    class Builder {
        var thinking: String? = null
        var signature: String? = null
        var cacheControl: CacheControl? = null

        fun build(): ThinkingBlockParam = ThinkingBlockParam(
            thinking = requireNotNull(thinking) { "thinking cannot be null" },
            signature = requireNotNull(signature) { "signature cannot be null" },
            cacheControl = cacheControl
        )
    }

    fun copy(
        block: Builder.() -> Unit
    ): ThinkingBlockParam = Builder().apply {
        thinking = this@ThinkingBlockParam.thinking
        signature = this@ThinkingBlockParam.signature
        cacheControl = this@ThinkingBlockParam.cacheControl
    }.apply(block).build()

}

@OptIn(ExperimentalContracts::class)
fun ThinkingBlockParam(block: ThinkingBlockParam.Builder.() -> Unit): ThinkingBlockParam {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ThinkingBlockParam.Builder().also(block).build()
}

/**
 * A redacted thinking content block.
 *
 * In some cases, thinking content may be redacted or encrypted for privacy
 * or security reasons. This block contains the redacted/encrypted data.
 *
 * @property data The redacted or encrypted thinking data
 * @property cacheControl Cache control for this content block
 */
@Serializable
@SerialName("redacted_thinking")
class RedactedThinkingBlock private constructor(
    val data: String,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    class Builder {
        var data: String? = null
        var cacheControl: CacheControl? = null

        fun build(): RedactedThinkingBlock = RedactedThinkingBlock(
            data = requireNotNull(data) { "data cannot be null" },
            cacheControl = cacheControl
        )
    }

    fun copy(
        block: Builder.() -> Unit
    ): RedactedThinkingBlock = Builder().apply {
        data = this@RedactedThinkingBlock.data
        cacheControl = this@RedactedThinkingBlock.cacheControl
    }.apply(block).build()

}

@OptIn(ExperimentalContracts::class)
fun RedactedThinkingBlock(block: RedactedThinkingBlock.Builder.() -> Unit): RedactedThinkingBlock {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return RedactedThinkingBlock.Builder().also(block).build()
}
