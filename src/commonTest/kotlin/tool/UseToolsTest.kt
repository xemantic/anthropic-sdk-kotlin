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

import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.tool.test.Calculator
import com.xemantic.ai.anthropic.tool.test.FibonacciCalculator
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UseToolsTest {

    @Test
    fun `should only use Calculator tool - non-named`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Calculator> { calculate() }
        }
        val client = testAnthropic {
            defaultTools = toolbox.tools
        }
        val conversation = mutableListOf<Message>()
        conversation += "What's 15 multiplied by 7?"

        // when
        val initialResponse = client.messages.create {
            messages = conversation
            toolChoice = ToolChoice.Tool<Calculator>()
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

        conversation += initialResponse.useTools(toolbox)

        // when
        val resultResponse = client.messages.create {
            messages = conversation
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
    fun `should only use Calculator tool - named`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Calculator>(name = "my_calculator") {
                calculate()
            }
        }
        val client = testAnthropic {
            defaultTools = toolbox.tools
        }
        val conversation = mutableListOf<Message>()
        conversation += "What's 15 multiplied by 7?"

        // when
        val initialResponse = client.messages.create {
            messages = conversation
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

        conversation += initialResponse.useTools(toolbox)

        // when
        val resultResponse = client.messages.create {
            messages = conversation
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
    fun `should use FibonacciCalculator tool`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<FibonacciCalculator> { calculate() }
        }
        val client = testAnthropic()

        // when
        val response = client.messages.create {
            +"What's fibonacci number 42"
            tools = toolbox.tools
        }

        // then
        val toolUse = response.content.filterIsInstance<ToolUse>().first()
        response.toolUse should {
            have(name == "fibonacci_calculator")
        }

        val result = toolbox.use(toolUse)
        result should {
            have(toolUseId == toolUse.id)
            have(content != null && content.size == 1)
            content!![0] should {
                be<Text>()
                have(text == "267914296")
            }
        }
    }

    @Test
    fun `should use 2 tools in sequence`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<FibonacciCalculator> { calculate() }
            tool<Calculator> { calculate() }
        }
        val client = testAnthropic {
            defaultTools = toolbox.tools
        }
        val systemPrompt =
            "Always use tools to perform calculations. Never calculate on your own, even if you know the answer."
        val prompt = "Calculate Fibonacci number 42 and then divide it by 42"
        val conversation = mutableListOf<Message>()
        conversation += Message { +prompt }

        // when
        val fibonacciResponse = client.messages.create {
            system(systemPrompt)
            messages = conversation
            toolChoice = ToolChoice.Tool<FibonacciCalculator>()
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
        conversation += fibonacciResponse.useTools(toolbox)

        // when
        val calculatorResponse = client.messages.create {
            messages = conversation
            toolChoice = ToolChoice.Tool<Calculator>()
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
        conversation += calculatorResponse.useTools(toolbox)

        // when
        val finalResponse = client.messages.create {
            messages = conversation
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