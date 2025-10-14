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

import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ZERO
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class CostTest {

    @Test
    fun `should create a Cost instance with correct values`() {
        Cost {
            inputTokens = Money("0.001")
            outputTokens = Money("0.002")
            cache5mCreationInputTokens = Money("0.00025")
            cache1hCreationInputTokens = Money("0.0003")
            cacheReadInputTokens = Money("0.0005")
        } should {
            have(inputTokens == Money("0.001"))
            have(outputTokens == Money("0.002"))
            have(cache5mCreationInputTokens == Money("0.00025"))
            have(cache1hCreationInputTokens == Money("0.0003"))
            have(cacheReadInputTokens == Money("0.0005"))
        }
    }

    /**
     * This case is used when costs per model are being defined.
     */
    @Test
    fun `should create a Cost instance with correct values when cache costs are not specified`() {
        Cost {
            inputTokens = Money("0.001")
            outputTokens = Money("0.002")
        } should {
            have(inputTokens == Money("0.001"))
            have(outputTokens == Money("0.002"))
            have(cache5mCreationInputTokens == Money("0.00125"))
            have(cache1hCreationInputTokens == Money("0.002"))
            have(cacheReadInputTokens == Money("0.0001"))
        }
    }

    @Test
    fun `should add two Cost instances without cache`() {
        // given
        val cost1 = Cost {
            inputTokens = Money("0.001")
            outputTokens = Money("0.002")
            cache5mCreationInputTokens = Money.ZERO
            cache1hCreationInputTokens = Money.ZERO
            cacheReadInputTokens = Money.ZERO
        }
        val cost2 = Cost {
            inputTokens = Money("0.003")
            outputTokens = Money("0.004")
            cache5mCreationInputTokens = Money.ZERO
            cache1hCreationInputTokens = Money.ZERO
            cacheReadInputTokens = Money.ZERO
        }

        // when
        val result = cost1 + cost2

        // then
        result should {
            have(inputTokens == Money("0.004"))
            have(outputTokens == Money("0.006"))
            have(cache5mCreationInputTokens == Money.ZERO)
            have(cache1hCreationInputTokens == Money.ZERO)
            have(cacheReadInputTokens == Money.ZERO)
        }
    }

    @Test
    fun `should add two Cost instances with cache`() {
        // given
        val cost1 = Cost {
            inputTokens = Money("0.001")
            outputTokens = Money("0.002")
            cache5mCreationInputTokens = Money("0.0001")
            cache1hCreationInputTokens = Money("0.00015")
            cacheReadInputTokens = Money("0.0002")
        }
        val cost2 = Cost {
            inputTokens = Money("0.003")
            outputTokens = Money("0.004")
            cache5mCreationInputTokens = Money("0.0003")
            cache1hCreationInputTokens = Money("0.00035")
            cacheReadInputTokens = Money("0.0004")
        }

        // when
        val result = cost1 + cost2

        // then
        result should {
            have(inputTokens == Money("0.004"))
            have(outputTokens == Money("0.006"))
            have(cache5mCreationInputTokens == Money("0.0004"))
            have(cache1hCreationInputTokens == Money("0.0005"))
            have(cacheReadInputTokens == Money("0.0006"))
        }
    }

    @Test
    fun `should calculate total cost`() {
        Cost {
            inputTokens = Money("0.001")
            outputTokens = Money("0.002")
            cache5mCreationInputTokens = Money("0.0005")
            cache1hCreationInputTokens = Money("0.0006")
            cacheReadInputTokens = Money("0.0007")
        } should {
            have(total == Money("0.0048"))
        }
    }

    @Test
    fun `should create ZERO Cost instance`() {
        Cost.ZERO should {
            have(inputTokens == Money.ZERO)
            have(outputTokens == Money.ZERO)
            have(cache5mCreationInputTokens == Money.ZERO)
            have(cache1hCreationInputTokens == Money.ZERO)
            have(cacheReadInputTokens == Money.ZERO)
        }
    }

}