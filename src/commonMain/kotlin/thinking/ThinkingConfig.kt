/*
 * Copyright 2025-2026 Xemantic contributors
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

package com.xemantic.ai.anthropic.thinking

import com.xemantic.ai.anthropic.json.toPrettyJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Configuration for enabling Claude's extended thinking.
 *
 * When enabled, responses include thinking content blocks showing Claude's
 * thinking process before the final answer. Requires a minimum budget of
 * 1,024 tokens and counts towards your max_tokens limit.
 */
@Serializable
sealed class ThinkingConfig {

    /**
     * Controls whether the thinking content is returned in responses.
     *
     * The full thinking tokens are always charged regardless of display mode.
     */
    enum class Display {

        /**
         * Thinking blocks contain abbreviated thinking text. Default on most
         * Claude 4 models.
         */
        @SerialName("summarized")
        SUMMARIZED,

        /**
         * Thinking blocks are returned with an empty `thinking` field, but the
         * `signature` is still provided for multi-turn continuity. Default on
         * Claude Opus 4.7 and Claude Mythos Preview.
         */
        @SerialName("omitted")
        OMITTED

    }

    /**
     * Extended thinking enabled with a specified token budget.
     *
     * Note: Deprecated on Claude Opus 4.6 and Claude Sonnet 4.6, and not
     * supported on Claude Opus 4.7 — use [Adaptive] on those models.
     *
     * @property budgetTokens Determines how many tokens Claude can use for its
     *   internal reasoning process. Must be ≥1024 and less than max_tokens.
     * @property display Whether to return summarized thinking text or only the
     *   signature. Defaults to [ThinkingConfig.Display.SUMMARIZED] on Claude 4 models
     *   and [ThinkingConfig.Display.OMITTED] on Claude Opus 4.7 / Mythos Preview.
     */
    @Serializable
    @SerialName("enabled")
    class Enabled private constructor(
        @SerialName("budget_tokens")
        val budgetTokens: Int,
        val display: Display? = null
    ) : ThinkingConfig() {

        class Builder {
            var budgetTokens: Int? = null
            var display: Display? = null

            fun build(): Enabled = Enabled(
                budgetTokens = requireNotNull(budgetTokens) { "budgetTokens cannot be null" },
                display = display
            )
        }

        companion object {

            @OptIn(ExperimentalContracts::class)
            operator fun invoke(block: Builder.() -> Unit): Enabled {
                contract {
                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
                }
                return Builder().apply(block).build()
            }

        }
    }

    /**
     * Adaptive extended thinking. Claude decides whether and how much to think
     * based on the prompt. Recommended on Claude Opus 4.6 / 4.7 and Claude
     * Sonnet 4.6, and the default on Claude Mythos Preview.
     *
     * @property display Whether to return summarized thinking text or only the
     *   signature.
     */
    @Serializable
    @SerialName("adaptive")
    class Adaptive private constructor(
        val display: Display? = null
    ) : ThinkingConfig() {

        class Builder {
            var display: Display? = null

            fun build(): Adaptive = Adaptive(
                display = display
            )
        }

        companion object {

            @OptIn(ExperimentalContracts::class)
            operator fun invoke(block: Builder.() -> Unit = {}): Adaptive {
                contract {
                    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
                }
                return Builder().apply(block).build()
            }

        }

    }

    /**
     * Extended thinking disabled.
     */
    @Serializable
    @SerialName("disabled")
    object Disabled : ThinkingConfig()

    override fun toString(): String = toPrettyJson()

}
