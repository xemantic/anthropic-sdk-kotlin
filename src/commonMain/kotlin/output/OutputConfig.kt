/*
 * Copyright 2026 Xemantic contributors
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

package com.xemantic.ai.anthropic.output

import com.xemantic.ai.anthropic.json.toPrettyJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Controls how much effort Claude spends generating a response.
 *
 * Effort is a soft behavioral signal — not a hard token budget — that
 * influences all output tokens: text, tool calls, and extended thinking.
 * Higher effort favors quality and thoroughness; lower effort favors speed
 * and lower token usage. The hard ceiling on total output always remains
 * `max_tokens`. Setting effort to [HIGH] produces exactly the same behavior
 * as omitting it.
 *
 * Availability is model-dependent:
 * - [XHIGH] is supported only on Claude Opus 4.8 and Claude Opus 4.7.
 * - [MAX] is supported on Claude Opus 4.8 / 4.7 / 4.6, Claude Sonnet 4.6 and
 *   Claude Mythos Preview, but not on Claude Opus 4.5.
 *
 * Requesting an effort level the target model does not support results in an
 * API error.
 */
@Serializable
enum class Effort {

    @SerialName("low")
    LOW,

    @SerialName("medium")
    MEDIUM,

    @SerialName("high")
    HIGH,

    @SerialName("xhigh")
    XHIGH,

    @SerialName("max")
    MAX

}

/**
 * Configuration controlling how Claude produces output, serialized as the
 * `output_config` field of a message request.
 *
 * @property effort The [Effort] level Claude should apply, or `null` to use
 *   the API default (equivalent to [Effort.HIGH]).
 */
@Serializable
data class OutputConfig(
    val effort: Effort? = null
) {

    class Builder {
        var effort: Effort? = null

        fun build(): OutputConfig = OutputConfig(
            effort = effort
        )
    }

    companion object {

        @OptIn(ExperimentalContracts::class)
        operator fun invoke(block: Builder.() -> Unit): OutputConfig {
            contract {
                callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            }
            return Builder().apply(block).build()
        }

    }

    override fun toString(): String = toPrettyJson()

}
