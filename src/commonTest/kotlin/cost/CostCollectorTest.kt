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
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ZERO
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class CostCollectorTest {

    @Test
    fun `Should initialize CostCollector with zero usage`() {
        CostCollector() should {
            have(costWithUsage == CostWithUsage.ZERO)
        }
    }

    @Test
    fun `toString should return String representation of UsageCollector`() {
        assert(
            CostCollector().toString() ==
                    "UsageCollector(usage=" +
                    "Usage(inputTokens=0, outputTokens=0, cacheCreationInputTokens=0, cacheReadInputTokens=0), cost=" +
                    "Cost(inputTokens=0, outputTokens=0, cacheCreationInputTokens=0, cacheReadInputTokens=0))"
        )
    }

    @Test
    fun `Should update cost and usage`() {
        // given
        val collector = CostCollector()
        val costWithUsage = CostWithUsage(
            usage = Usage {
                inputTokens = 1000
                outputTokens = 1000
            },
            cost = Model.Companion.DEFAULT.cost
        )

        // when
        collector += costWithUsage

        // then
        collector.costWithUsage should {
            have(
                usage == Usage {
                    inputTokens = 1000
                    outputTokens = 1000
                    cacheCreationInputTokens = 0
                    cacheReadInputTokens = 0
                }
            )
            have(
                cost == Cost {
                    inputTokens = Money(".003")
                    outputTokens = Money(".015")
                    cacheCreationInputTokens = Money.Companion.ZERO
                    cacheReadInputTokens = Money.Companion.ZERO
                }
            )
        }
    }

    @Test
    fun `Should accumulate multiple usage updates`() {
        // given
        val collector = CostCollector()
        val testUsage = Usage {
            inputTokens = 1000
            outputTokens = 1000
            cacheCreationInputTokens = 1000
            cacheReadInputTokens = 1000
        }

        // when
        collector += CostWithUsage(
            usage = testUsage,
            cost = Model.CLAUDE_3_5_SONNET.cost
        )
        collector += CostWithUsage(
            usage = testUsage,
            cost = Model.CLAUDE_3_5_HAIKU.cost
        )
        collector += CostWithUsage(
            usage = testUsage,
            cost = Model.CLAUDE_3_OPUS.cost
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
                have(cacheCreationInputTokens == Money("0.0235")) // 0.00375 + 0.001 + 0.01875
                have(cacheReadInputTokens == Money("0.00188")) // 0.0003 + 0.00008 + 0.0015
            }
        }
    }

}