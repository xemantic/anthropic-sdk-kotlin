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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.tool.test.Calculator
import com.xemantic.ai.anthropic.tool.test.FibonacciCalculator
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.collections.plusAssign

class ToolUseTest {

    @Test
    fun `Should only use Calculator tool (non-named)`() = runTest {
        // given
        val mathTools = listOf(
            Tool<Calculator> { calculate() }
        )
        val client = Anthropic()
        val conversation = mutableListOf<Message>()
        conversation += Message { +"What's 15 multiplied by 7?" }

        // when
        val initialResponse = client.messages.create {
            messages = conversation
            tools = mathTools
            toolChoice = ToolChoice.Companion.Tool<Calculator>()
        }
        conversation += initialResponse

        // then
        initialResponse should {
            have(stopReason == StopReason.TOOL_USE)
            have(content.size == 1) // and therefore there is only ToolUse without commentary
            content[0] should {
                be<ToolUse>()
                have(name == "calculator")
            }
        }

        conversation += initialResponse.useTools()

        // when
        val resultResponse = client.messages.create {
            messages = conversation
            tools = mathTools // tool definitions need to be present
        }

        // then
        resultResponse should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("105" in text)
            }
        }
    }

    @Test
    fun `Should only use Calculator tool (named)`() = runTest {
        // given
        val mathTools = listOf(
            Tool<Calculator>("my_calculator") { calculate() }
        )
        val client = Anthropic()
        val conversation = mutableListOf<Message>()
        conversation += Message { +"What's 15 multiplied by 7?" }

        // when
        val initialResponse = client.messages.create {
            messages = conversation
            tools = mathTools
            toolChoice = ToolChoice.Tool("my_calculator")
        }
        conversation += initialResponse

        // then
        initialResponse should {
            have(stopReason == StopReason.TOOL_USE)
            have(content.size == 1) // and therefore there is only ToolUse without commentary
            content[0] should {
                be<ToolUse>()
                have(name == "my_calculator")
            }
        }

        conversation += initialResponse.useTools()

        // when
        val resultResponse = client.messages.create {
            messages = conversation
            tools = mathTools // tool definitions need to be present
        }

        // then
        resultResponse should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("105" in text)
            }
        }
    }

    @Test
    fun `Should use FibonacciCalculator tool`() = runTest {
        // given
        val mathTools = listOf(
            Tool<FibonacciCalculator> { calculate() }
        )
        val client = Anthropic()

        // when
        val response = client.messages.create {
            +Message { +"What's fibonacci number 42" }
            tools = mathTools
        }

        // then
        val toolUse = response.content.filterIsInstance<ToolUse>().first()
        response.toolUse should {
            have(name == "fibonacci_calculator")
        }

        val result = toolUse.use()
        result should {
            have(toolUseId == toolUse.id)
            have(content == listOf(Text(text = "267914296")))
        }
    }

    @Test
    fun `Should use 2 tools in sequence`() = runTest {
        // given
        val mathTools = listOf(
            Tool<FibonacciCalculator> { calculate() },
            Tool<Calculator> { calculate() }
        )
        val client = Anthropic()
        val systemPrompt =
            "Always use tools to perform calculations. Never calculate on your own, even if you know the answer."
        val prompt = "Calculate Fibonacci number 42 and then divide it by 42"
        val conversation = mutableListOf<Message>()
        conversation += Message { +prompt }

        // when
        val fibonacciResponse = client.messages.create {
            system(systemPrompt)
            messages = conversation
            tools = mathTools
            toolChoice = ToolChoice.Companion.Tool<FibonacciCalculator>()
        }
        conversation += fibonacciResponse

        // then
        fibonacciResponse should {
            have(stopReason == StopReason.TOOL_USE)
            have(toolUses.size == 1)
            toolUse should {
                have(name == "fibonacci_calculator")
            }
        }
        conversation += fibonacciResponse.useTools()

        // when
        val calculatorResponse = client.messages.create {
            messages = conversation
            tools = mathTools
            toolChoice = ToolChoice.Companion.Tool<Calculator>()
        }
        conversation += calculatorResponse

        // then
        calculatorResponse should {
            have(stopReason == StopReason.TOOL_USE)
            have(toolUses.size == 1)
            toolUse should {
                have(name == "calculator")
            }
        }
        conversation += calculatorResponse.useTools()

        // when
        val finalResponse = client.messages.create {
            messages = conversation
            tools = mathTools
        }
        finalResponse should {
            have(content.isNotEmpty())
            content[0] should {
                be<Text>()
                // the result might be in the format: 6,378,911.8....
                have(text.replace(",", "").contains("6378911.8"))
            }
        }
    }

}