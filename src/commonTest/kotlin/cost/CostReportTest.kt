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
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.message.MessageResponse
import com.xemantic.ai.anthropic.message.Role
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.kotlin.test.assert
import kotlin.test.Test

class CostReportTest {

    @Test
    fun `should render cost report table`() {
        // given
        val collector = CostCollector()
        val response = MessageResponse(
            id = "foo",
            role = Role.ASSISTANT,
            content = listOf(
                Text("bar")
            ),
            model = "claude-3-7-sonnet-20250219",
            stopReason = StopReason.END_TURN,
            stopSequence = null,
            usage = Usage {
                inputTokens = 419
                outputTokens = 86
            }
        ).apply {
            resolvedModel = Model.DEFAULT
        }
        val usageWithCost = response.costWithUsage
        collector += usageWithCost

        // when
        val report = costReport(
            stats = response.costWithUsage,
            totalStats = collector.costWithUsage
        )

        // then
        assert(
            report == $$"""
            |             | request tokens | total tokens | request cost | total cost |
            |-------------|----------------|--------------|--------------|------------|
            | input       |            419 |          419 |    $0.001257 |  $0.001257 |
            | output      |             86 |           86 |     $0.00129 |   $0.00129 |
            | cache write |              0 |            0 |           $0 |         $0 |
            | cache read  |              0 |            0 |           $0 |         $0 |
            |             |                |              |              |            |
            |             |                |              |    $0.002547 |  $0.002547 |

        """.trimIndent()
        )

    }

}