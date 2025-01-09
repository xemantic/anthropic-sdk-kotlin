/*
 * Copyright 2024 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic

import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ZERO
import com.xemantic.ai.anthropic.event.Delta.TextDelta
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.Role
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.tool.Calculator
import com.xemantic.ai.anthropic.tool.DatabaseQuery
import com.xemantic.ai.anthropic.tool.FibonacciTool
import com.xemantic.ai.anthropic.tool.TestDatabase
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.usage.Cost
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.collections.any
import kotlin.test.Test

class AnthropicTest {

  @Test
  fun `Should create Anthropic instance with 0 Usage and Cost`() {
    Anthropic() should {
      have(usage == Usage.ZERO)
      have(cost == Cost.ZERO)
    }
  }

  @Test
  fun `Should receive an introduction from Claude`() = runTest {
    // given
    val anthropic = Anthropic()

    // when
    val response = anthropic.messages.create {
      +Message {
        +"Hello World! What's your name?"
      }
    }

    // then
    response should {
      have(role == Role.ASSISTANT)
      have("claude" in model)
      have(stopReason == StopReason.END_TURN)
      have(content.size == 1)
      content[0] should {
        be<Text>()
        have("Claude" in text)
      }
      have(stopSequence == null)
      usage should {
        have(inputTokens == 15)
        have(outputTokens > 0)
      }
    }
  }

  @Test
  fun `Should receive Usage and update Cost calculation`() = runTest {
    // given
    val anthropic = Anthropic()

    // when
    val response = anthropic.messages.create {
      +Message {
        +"Hello Claude! I am testing the amount of input and output tokens."
      }
    }

    // then
    response should {
      have(role == Role.ASSISTANT)
      have("claude" in model)
      have(stopReason == StopReason.END_TURN)
      have(content.size == 1)
      have(stopSequence == null)
      usage should {
        have(inputTokens == 21)
        have(outputTokens > 0)
        have(cacheCreationInputTokens == 0)
        have(cacheReadInputTokens == 0)
      }
    }

    anthropic should {
      usage should {
        have(inputTokens == 21)
        have(inputTokens > 0)
        have(cacheCreationInputTokens == 0)
        have(cacheReadInputTokens == 0)
      }
      cost should {
        have(inputTokens >= Money.ZERO && inputTokens == Money("0.000063"))
        have(outputTokens >= Money.ZERO && inputTokens <= Money("0.0005"))
        have(cacheCreationInputTokens == Money.ZERO)
        have(cacheReadInputTokens == Money.ZERO)
      }
    }

  }

  @Test
  fun `Should use system prompt`() = runTest {
    // given
    val anthropic = Anthropic()

    // when
    val response = anthropic.messages.create {
      system("Whatever the human says, answer \"HAHAHA\"")
      +Message {
        +"Hello World! What's your name?"
      }
      maxTokens = 1024
    }

    // then
    response should {
      have(content.size == 1)
      content[0] should {
        be<Text>()
        have(text == "HAHAHA")
      }
    }
  }

  @Test
  fun `Should stream the response`() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.stream {
      +Message { +"Say: 'The sun slowly dipped below the horizon, painting the sky in a breathtaking array of oranges, pinks, and purples.'" }
    }
      .filterIsInstance<Event.ContentBlockDelta>()
      .map { (it.delta as TextDelta).text }
      .toList()
      .joinToString(separator = "")

    // then
    assert(response == "The sun slowly dipped below the horizon, painting the sky in a breathtaking array of oranges, pinks, and purples.")
  }

  @Test
  fun `Should use Calculator tool`() = runTest {
    // given
    val client = Anthropic {
      tool<Calculator>()
    }
    val conversation = mutableListOf<Message>()
    conversation += Message { +"What's 15 multiplied by 7?" }

    // when
    val initialResponse = client.messages.create {
      messages = conversation
      singleTool<Calculator>() // we are forcing the use of this tool
    }
    conversation += initialResponse

    // then
    initialResponse should {
      have(stopReason == StopReason.TOOL_USE)
      have(content.size == 1) // and therefore there is only ToolUse without commentary
      content[0] should {
        be<ToolUse>()
        have(name == "Calculator")
      }
    }

    conversation += initialResponse.useTools()

    // when
    val resultResponse = client.messages.create {
      messages = conversation
      tool<Calculator>() // we are not forcing the use, but tool definition needs to be present
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
  fun `Should use FibonacciTool`() = runTest {
    // given
    val client = Anthropic {
      tool<FibonacciTool>()
    }

    // when
    val response = client.messages.create {
      +Message { +"What's fibonacci number 42" }
      allTools()
    }

    // then
    val toolUse = response.content.filterIsInstance<ToolUse>().first()
    toolUse should {
      have(name == "FibonacciTool")
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
    val client = Anthropic {
      tool<FibonacciTool>()
      tool<Calculator>()
    }
    val systemPrompt = "Always use tools to perform calculations. Never calculate on your own, even if you know the answer."
    val prompt = "Calculate Fibonacci number 42 and then divide it by 42"
    val conversation = mutableListOf<Message>()
    conversation += Message { +prompt }

    // when
    val fibonacciResponse = client.messages.create {
      system(systemPrompt)
      messages = conversation
      singleTool<FibonacciTool>()
    }
    conversation += fibonacciResponse

    // then
    fibonacciResponse should {
      have(stopReason == StopReason.TOOL_USE)
      have(content.any { it is ToolUse && it.name == "FibonacciTool" })
    }
    conversation += fibonacciResponse.useTools()

    // when
    val calculatorResponse = client.messages.create {
      messages = conversation
      singleTool<Calculator>()
    }
    conversation += calculatorResponse

    // then
    calculatorResponse should {
      have(stopReason == StopReason.TOOL_USE)
      have(content.any { it is ToolUse && it.name == "Calculator" })
    }
    conversation += calculatorResponse.useTools()

    // when
    val finalResponse = client.messages.create {
      messages = conversation
      allTools()
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

  @Test
  fun `Should use tool with dependencies`() = runTest {
    // given
    val testDatabase = TestDatabase()
    val anthropic = Anthropic {
      tool<DatabaseQuery> {
        database = testDatabase
      }
    }

    // when
    val response = anthropic.messages.create {
      +Message { +"List data in CUSTOMER table" }
      singleTool<DatabaseQuery>() // we are forcing the use of this tool
      // could be also just tool<DatabaseQuery>() if we are confident that LLM will use this one
    }

    // then
    response should {
      have(stopReason == StopReason.TOOL_USE)
      have(content.any { it is ToolUse && it.name == "DatabaseQuery" })
    }

    // when
    response.useTools()

    // then
    testDatabase should {
      have(executedQuery != null)
      have(executedQuery!!.uppercase().startsWith("SELECT * FROM CUSTOMER"))
    }

    // depending on the response the statement might end up with semicolon, which we discard
  }

}
