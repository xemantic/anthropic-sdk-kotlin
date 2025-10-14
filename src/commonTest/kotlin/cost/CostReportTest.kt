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
import com.xemantic.ai.anthropic.usage.CacheCreation
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.kotlin.test.sameAs
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
            model = Model.CLAUDE_SONNET_4_5_20250929.id,
            stopReason = StopReason.END_TURN,
            stopSequence = null,
            usage = Usage {
                inputTokens = 1000
                outputTokens = 200
                cacheCreationInputTokens = 700
                cacheReadInputTokens = 3000
                cacheCreation = CacheCreation {
                    ephemeral5mInputTokens = 400
                    ephemeral1hInputTokens = 300
                }
            }
        ).apply {
            resolvedModel = Model.DEFAULT
        }
        val costWithUsage = response.costWithUsage
        collector += costWithUsage

        // when
        val report = costReport(
            stats = response.costWithUsage,
            totalStats = collector.costWithUsage
        )

        // then
        report sameAs $$"""
            |                | request tokens | total tokens | request cost | total cost |
            |----------------|----------------|--------------|--------------|------------|
            | input          |           1000 |         1000 |       $0.003 |     $0.003 |
            | output         |            200 |          200 |       $0.003 |     $0.003 |
            | cache 5m write |            400 |          400 |      $0.0015 |    $0.0015 |
            | cache 1h write |            300 |          300 |      $0.0018 |    $0.0018 |
            | cache read     |           3000 |         3000 |      $0.0009 |    $0.0009 |
            |                |                |              |              |            |
            |                |                |              |      $0.0102 |    $0.0102 |

        """.trimIndent()
    }

}