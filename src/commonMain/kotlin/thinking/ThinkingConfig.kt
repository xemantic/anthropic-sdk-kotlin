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
     * Extended thinking enabled with a specified token budget.
     *
     * @property budgetTokens Determines how many tokens Claude can use for its
     *   internal reasoning process. Must be ≥1024 and less than max_tokens.
     */
    @Serializable
    @SerialName("enabled")
    data class Enabled(
        @SerialName("budget_tokens")
        val budgetTokens: Int
    ) : ThinkingConfig() {

        class Builder {
            var budgetTokens: Int? = null

            fun build(): Enabled = Enabled(
                budgetTokens = requireNotNull(budgetTokens) { "budgetTokens cannot be null" }
            )
        }

        init {
            require(budgetTokens >= 1024) {
                "budgetTokens must be at least 1024, got $budgetTokens"
            }
        }

    }

    /**
     * Extended thinking disabled.
     */
    @Serializable
    @SerialName("disabled")
    data object Disabled : ThinkingConfig()

    companion object {

        /**
         * Creates an enabled thinking configuration with the specified budget.
         */
        fun Enabled(
            block: Enabled.Builder.() -> Unit
        ): Enabled = Enabled.Builder().apply(block).build()

    }

    override fun toString(): String = toPrettyJson()

}

@OptIn(ExperimentalContracts::class)
fun ThinkingConfigEnabled(block: ThinkingConfig.Enabled.Builder.() -> Unit): ThinkingConfig.Enabled {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ThinkingConfig.Enabled.Builder().also(block).build()
}
