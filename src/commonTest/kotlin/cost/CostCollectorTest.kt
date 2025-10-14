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

package com.xemantic.ai.anthropic.cost

import com.xemantic.ai.anthropic.Model
import com.xemantic.ai.anthropic.usage.CacheCreation
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ZERO
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAs
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class CostCollectorTest {

    @Test
    fun `should initialize CostCollector with zero usage`() {
        CostCollector() should {
            have(costWithUsage == CostWithUsage.ZERO)
        }
    }

    @Test
    fun `toString should return String representation of CostCollector`() {
        // given
        val collector = CostCollector()

        // when
        val asString = collector.toString()

        // then
        asString sameAs  """
            CostCollector {
              "cost": {
                "inputTokens": "0",
                "outputTokens": "0",
                "cache5mCreationInputTokens": "0",
                "cache1hCreationInputTokens": "0",
                "cacheReadInputTokens": "0"
              },
              "usage": {
                "input_tokens": 0,
                "output_tokens": 0,
                "cache_creation_input_tokens": 0,
                "cache_read_input_tokens": 0
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should update cost and usage`() {
        // given
        val collector = CostCollector()
        val usage = Usage {
            inputTokens = 1000
            outputTokens = 1000
        }
        val costWithUsage = CostWithUsage(
            cost = Model.DEFAULT.cost * usage,
            usage = usage,
        )

        // when
        collector += costWithUsage

        // then
        collector.costWithUsage should {
            have(
                usage == Usage {
                    inputTokens = 1000
                    outputTokens = 1000
                }
            )
            have(
                cost == Cost {
                    inputTokens = Money(".003")
                    outputTokens = Money(".015")
                    cache5mCreationInputTokens = Money.ZERO
                    cache1hCreationInputTokens = Money.ZERO
                    cacheReadInputTokens = Money.ZERO
                }
            )
        }
    }

    @Test
    fun `should accumulate multiple usage updates`() {
        // given
        val collector = CostCollector()
        val testUsage = Usage {
            inputTokens = 1000
            outputTokens = 1000
            cacheCreationInputTokens = 1000
            cacheReadInputTokens = 1000
            cacheCreation = CacheCreation {
                ephemeral5mInputTokens = 600
                ephemeral1hInputTokens = 400
            }
        }

        // when
        collector += CostWithUsage(
            cost = Model.CLAUDE_SONNET_4_5_20250929.cost * testUsage,
            usage = testUsage,
        )
        collector += CostWithUsage(
            cost = Model.CLAUDE_3_5_HAIKU_20241022.cost * testUsage,
            usage = testUsage
        )
        collector += CostWithUsage(
            cost = Model.CLAUDE_OPUS_4_1_20250805.cost * testUsage,
            usage = testUsage
        )

        // then
        collector.costWithUsage should {
            usage should {
                have(inputTokens == 3000)
                have(outputTokens == 3000)
                have(cacheCreationInputTokens == 3000)
                have(cacheReadInputTokens == 3000)
            }
            cost should {
                have(inputTokens == Money("0.0188")) // 0.003 + 0.0008 + 0.015
                have(outputTokens == Money("0.094")) // 0.015 + 0.004 + 0.075
                have(cache5mCreationInputTokens == Money("0.0141")) // (600 × 0.003 × 1.25) + (600 × 0.0008 × 1.25) + (600 × 0.015 × 1.25)
                have(cache1hCreationInputTokens == Money("0.01504")) // (400 × 0.003 × 2) + (400 × 0.0008 × 2) + (400 × 0.015 × 2)
                have(cacheReadInputTokens == Money("0.00188")) // 0.0003 + 0.00008 + 0.0015
            }
        }
    }

}