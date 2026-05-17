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

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class ThinkingConfigSerializationTest {

    private fun ThinkingConfig.encodeToString() =
        anthropicJson.encodeToString<ThinkingConfig>(this)

    @Test
    fun `should serialize ThinkingConfig Enabled`() {
        ThinkingConfig.Enabled {
            budgetTokens = 10000
        }.encodeToString() sameAsJson  """
            {
              "type": "enabled",
              "budget_tokens": 10000
            }
        """
    }

    @Test
    fun `should serialize ThinkingConfig Disabled`() {
        ThinkingConfig.Disabled.encodeToString() sameAsJson  """
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
            ThinkingConfig.Enabled {
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
            ThinkingConfig.Enabled {
                budgetTokens = 1024
            }
        }
        result should {
            have(isSuccess)
        }
    }

    @Test
    fun `should serialize ThinkingConfig Enabled with display summarized`() {
        ThinkingConfig.Enabled {
            budgetTokens = 10000
            display = ThinkingConfig.Display.SUMMARIZED
        }.encodeToString() sameAsJson  """
            {
              "type": "enabled",
              "budget_tokens": 10000,
              "display": "summarized"
            }
        """
    }

    @Test
    fun `should serialize ThinkingConfig Enabled with display omitted`() {
        ThinkingConfig.Enabled {
            budgetTokens = 10000
            display = ThinkingConfig.Display.OMITTED
        }.encodeToString() sameAsJson  """
            {
              "type": "enabled",
              "budget_tokens": 10000,
              "display": "omitted"
            }
        """
    }

    @Test
    fun `should deserialize ThinkingConfig Enabled with display`() {
        anthropicJson.decodeFromString<ThinkingConfig>(
            """
            {
              "type": "enabled",
              "budget_tokens": 16000,
              "display": "omitted"
            }
            """
        ) should {
            be<ThinkingConfig.Enabled>()
            have(budgetTokens == 16000)
            have(display == ThinkingConfig.Display.OMITTED)
        }
    }

    @Test
    fun `should serialize ThinkingConfig Adaptive`() {
        ThinkingConfig.Adaptive().encodeToString() sameAsJson  """
            {
              "type": "adaptive"
            }
        """
    }

    @Test
    fun `should serialize ThinkingConfig Adaptive with display summarized`() {
        ThinkingConfig.Adaptive {
            display = ThinkingConfig.Display.SUMMARIZED
        }.encodeToString() sameAsJson  """
            {
              "type": "adaptive",
              "display": "summarized"
            }
        """
    }

    @Test
    fun `should serialize ThinkingConfig Adaptive with display omitted`() {
        ThinkingConfig.Adaptive {
            display = ThinkingConfig.Display.OMITTED
        }.encodeToString() sameAsJson  """
            {
              "type": "adaptive",
              "display": "omitted"
            }
        """
    }

    @Test
    fun `should deserialize ThinkingConfig Adaptive`() {
        anthropicJson.decodeFromString<ThinkingConfig>(
            """
            {
              "type": "adaptive",
              "display": "omitted"
            }
            """
        ) should {
            be<ThinkingConfig.Adaptive>()
            have(display == ThinkingConfig.Display.OMITTED)
        }
    }

    @Test
    fun `should deserialize ThinkingConfig Adaptive without display`() {
        anthropicJson.decodeFromString<ThinkingConfig>(
            """
            {
              "type": "adaptive"
            }
            """
        ) should {
            be<ThinkingConfig.Adaptive>()
            have(display == null)
        }
    }

}
