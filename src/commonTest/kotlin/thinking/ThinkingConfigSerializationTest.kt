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

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

class ThinkingConfigSerializationTest {

    @Test
    fun `should serialize ThinkingConfig Enabled`() {
        anthropicJson.encodeToString(
            serializer = ThinkingConfig.serializer(),
            value = ThinkingConfigEnabled {
                budgetTokens = 10000
            }
        ) shouldEqualJson """
            {
              "type": "enabled",
              "budget_tokens": 10000
            }
        """
    }

    @Test
    fun `should serialize ThinkingConfig Disabled`() {
        anthropicJson.encodeToString(
            serializer = ThinkingConfig.serializer(),
            value = ThinkingConfig.Disabled
        ) shouldEqualJson """
            {
              "type": "disabled"
            }
        """
    }

    @Test
    fun `should deserialize ThinkingConfig Enabled`() {
        anthropicJson.decodeFromString<ThinkingConfig>(
            """
            {
              "type": "enabled",
              "budget_tokens": 16000
            }
            """
        ) should {
            be<ThinkingConfig.Enabled>()
            have(budgetTokens == 16000)
        }
    }

    @Test
    fun `should deserialize ThinkingConfig Disabled`() {
        anthropicJson.decodeFromString<ThinkingConfig>(
            """
            {
              "type": "disabled"
            }
            """
        ) should {
            be<ThinkingConfig.Disabled>()
        }
    }

    @Test
    fun `should enforce minimum budget tokens`() {
        val result = runCatching {
            ThinkingConfigEnabled {
                budgetTokens = 512  // Less than minimum of 1024
            }
        }
        result should {
            have(isFailure)
        }
    }

    @Test
    fun `should allow minimum budget tokens of 1024`() {
        val result = runCatching {
            ThinkingConfigEnabled {
                budgetTokens = 1024
            }
        }
        result should {
            have(isSuccess)
        }
    }

}
