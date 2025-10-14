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

package com.xemantic.ai.anthropic.usage

import com.xemantic.ai.anthropic.json.toPrettyJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Detailed cache creation token counts broken down by cache duration.
 */
@Serializable
class CacheCreation private constructor(
    @SerialName("ephemeral_5m_input_tokens")
    val ephemeral5mInputTokens: Int? = null,
    @SerialName("ephemeral_1h_input_tokens")
    val ephemeral1hInputTokens: Int? = null
) {

    class Builder {

        var ephemeral5mInputTokens: Int? = null
        var ephemeral1hInputTokens: Int? = null

        fun build(): CacheCreation = CacheCreation(
            requireNotNull(ephemeral5mInputTokens) { "ephemeral5mInputTokens cannot be null" },
            requireNotNull(ephemeral1hInputTokens) { "ephemeral1hInputTokens cannot be null"}
        )

    }

    operator fun plus(cacheCreation: CacheCreation): CacheCreation {
        return CacheCreation(
            ephemeral5mInputTokens = (ephemeral5mInputTokens  ?: 0) + (cacheCreation.ephemeral5mInputTokens ?: 0),
            ephemeral1hInputTokens = (ephemeral1hInputTokens  ?: 0) + (cacheCreation.ephemeral1hInputTokens ?: 0),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CacheCreation) return false

        if (ephemeral5mInputTokens != other.ephemeral5mInputTokens) return false
        if (ephemeral1hInputTokens != other.ephemeral1hInputTokens) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ephemeral5mInputTokens ?: 0
        result = 31 * result + (ephemeral1hInputTokens ?: 0)
        return result
    }

    companion object {

        val ZERO = CacheCreation(
            ephemeral5mInputTokens = 0,
            ephemeral1hInputTokens = 0
        )

    }

}

@OptIn(ExperimentalContracts::class)
fun CacheCreation(block: CacheCreation.Builder.() -> Unit): CacheCreation {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return CacheCreation.Builder().also(block).build()
}

/**
 * Represents API usage object.
 */
@Serializable
class Usage private constructor(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("output_tokens")
    val outputTokens: Int,
    @SerialName("cache_creation_input_tokens")
    val cacheCreationInputTokens: Int? = null,
    @SerialName("cache_read_input_tokens")
    val cacheReadInputTokens: Int? = null,
    @SerialName("cache_creation")
    val cacheCreation: CacheCreation? = null
) {

    class Builder {

        var inputTokens: Int? = null
        var outputTokens: Int? = null
        var cacheCreationInputTokens: Int? = null
        var cacheReadInputTokens: Int? = null
        var cacheCreation: CacheCreation? = null

        fun build(): Usage = Usage(
            requireNotNull(inputTokens) { "inputTokens cannot be null" },
            requireNotNull(outputTokens) { "outputTokens cannot be null"},
            cacheCreationInputTokens,
            cacheReadInputTokens,
            cacheCreation
        )

    }

    companion object {

        val ZERO = Usage(
            inputTokens = 0,
            outputTokens = 0,
            cacheCreationInputTokens = 0,
            cacheReadInputTokens = 0
        )

    }

    operator fun plus(usage: Usage): Usage = Usage(
        inputTokens = inputTokens + usage.inputTokens,
        outputTokens = outputTokens + usage.outputTokens,
        cacheCreationInputTokens = (cacheCreationInputTokens ?: 0) + (usage.cacheCreationInputTokens ?: 0),
        cacheReadInputTokens = (cacheReadInputTokens ?: 0) + (usage.cacheReadInputTokens ?: 0),
        cacheCreation = (cacheCreation ?: CacheCreation.ZERO) + (usage.cacheCreation ?: CacheCreation.ZERO)
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Usage) return false

        if (inputTokens != other.inputTokens) return false
        if (outputTokens != other.outputTokens) return false
        if (cacheCreationInputTokens != other.cacheCreationInputTokens) return false
        if (cacheReadInputTokens != other.cacheReadInputTokens) return false
        if (cacheCreation != other.cacheCreation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputTokens
        result = 31 * result + outputTokens
        result = 31 * result + (cacheCreationInputTokens ?: 0)
        result = 31 * result + (cacheReadInputTokens ?: 0)
        result = 31 * result + (cacheCreation?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = toPrettyJson()

}

@OptIn(ExperimentalContracts::class)
fun Usage(block: Usage.Builder.() -> Unit): Usage {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Usage.Builder().also(block).build()
}
