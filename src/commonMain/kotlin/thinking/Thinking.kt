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

import com.xemantic.ai.anthropic.json.WithAdditionalProperties
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Thinking: WithAdditionalProperties {

    @Serializable
    class Enabled private constructor(
        @SerialName("budget_tokens")
        val budgetTokens: Int,
        override val additionalProperties: Map<String, JsonElement?>?
    ) : Thinking() {

        init {
            require(budgetTokens > 1024) {
                "budgetTokens must be greater than 1024"
            }
        }

        class Builder {

            var budgetTokens: Int? = null

            fun build(): Enabled {
                return Enabled(
                    requireNotNull(budgetTokens) {
                        "budgetTokens cannot be null"
                    }
                )
            }

        }

    }

    class Disabled(
        override val additionalProperties: Map<String, JsonElement?>?
    ) : Thinking() {

    }

}
