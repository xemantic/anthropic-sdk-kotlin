package com.xemantic.anthropic

import com.xemantic.anthropic.event.Delta.TextDelta
import com.xemantic.anthropic.event.Event
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.Role
import com.xemantic.anthropic.message.StopReason
import com.xemantic.anthropic.message.plusAssign
import com.xemantic.anthropic.tool.Calculator
import com.xemantic.anthropic.tool.DatabaseQuery
import com.xemantic.anthropic.tool.FibonacciTool
import com.xemantic.anthropic.tool.TestDatabase
import com.xemantic.anthropic.content.Text
import com.xemantic.anthropic.content.ToolUse
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.instanceOf
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AnthropicTest {

  @Test
  fun shouldReceiveAnIntroductionFromClaude() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.create {
      +Message {
        +"Hello World! What's your name?"
      }
      maxTokens = 1024
    }

    // then
    assertSoftly(response) {
      role shouldBe Role.ASSISTANT
      model shouldBe "claude-3-5-sonnet-20241022"
      stopReason shouldBe StopReason.END_TURN
      content.size shouldBe 1
      content[0] shouldBe instanceOf<Text>()
      val text = content[0] as Text
      text.text shouldContain "Claude"
      stopSequence shouldBe null
      usage.inputTokens shouldBe 15
      usage.outputTokens shouldBeGreaterThan 0
    }
  }

  @Test
  fun shouldStreamTheResponse() = runTest {
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
    response shouldBe "The sun slowly dipped below the horizon, painting the sky in a breathtaking array of oranges, pinks, and purples."
  }

  @Test
  fun shouldUseCalculatorTool() = runTest {
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
    assertSoftly(initialResponse) {
      stopReason shouldBe StopReason.TOOL_USE
      content.size shouldBe 1 // and therefore there is only ToolUse without commentary
      content[0] shouldBe instanceOf<ToolUse>()
      (content[0] as ToolUse).name shouldBe "Calculator"
    }

    val toolUse = initialResponse.content[0] as ToolUse
    val result = toolUse.use() // here we execute the tool

    conversation += Message { +result }

    // when
    val resultResponse = client.messages.create {
      messages = conversation
      tool<Calculator>() // we are not forcing the use, but tool definition needs to be present
    }

    // then
    assertSoftly(resultResponse) {
      stopReason shouldBe StopReason.END_TURN
      content.size shouldBe 1
      content[0] shouldBe instanceOf<Text>()
      (content[0] as Text).text shouldContain "105"
    }
  }

  @Test
  fun shouldUseFibonacciTool() = runTest {
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
    toolUse.name shouldBe "FibonacciTool"

    val result = toolUse.use()
    assertSoftly(result) {
      toolUseId shouldBe toolUse.id
      isError shouldBe false
      content shouldBe listOf(Text(text = "267914296"))
    }
  }

  @Test
  fun shouldUse2ToolsInSequence() = runTest {
    // given
    val client = Anthropic {
      tool<FibonacciTool>()
      tool<Calculator>()
    }

    // when
    val conversation = mutableListOf<Message>()
    conversation += Message { +"Calculate Fibonacci number 42 and then divide it by 42" }

    val fibonacciResponse = client.messages.create {
      messages = conversation
      allTools()
    }
    conversation += fibonacciResponse

    val fibonacciToolUse = fibonacciResponse.content.filterIsInstance<ToolUse>().first()
    fibonacciToolUse.name shouldBe "FibonacciTool"
    val fibonacciResult = fibonacciToolUse.use()
    conversation += Message { +fibonacciResult }

    val calculatorResponse = client.messages.create {
      messages = conversation
      allTools()
    }
    conversation += calculatorResponse

    val calculatorToolUse = calculatorResponse.content.filterIsInstance<ToolUse>().first()
    calculatorToolUse.name shouldBe "Calculator"
    val calculatorResult = calculatorToolUse.use()
    conversation += Message { +calculatorResult }

    val finalResponse = client.messages.create {
      messages = conversation
      allTools()
    }

    finalResponse.content[0] shouldBe instanceOf<Text>()
    // the result might be in the format: 6,378,911.8....
    (finalResponse.content[0] as Text).text.replace(",", "") shouldContain "6378911.8"
  }

  @Test
  fun shouldUseToolWithDependencies() = runTest {
    // given
    val testDatabase = TestDatabase()
    val client = Anthropic {
      tool<DatabaseQuery> {
        database = testDatabase
      }
    }

    // when
    val response = client.messages.create {
      +Message { +"List data in CUSTOMER table" }
      singleTool<DatabaseQuery>() // we are forcing the use of this tool
      // could be also just tool<DatabaseQuery>() if we are confident that LLM will use this one
    }
    val toolUse = response.content.filterIsInstance<ToolUse>().first()
    toolUse.use()

    // then
    testDatabase.executedQuery shouldNotBe null
    testDatabase.executedQuery!!.uppercase() shouldStartWith "SELECT * FROM CUSTOMER"
    // depending on the response the statement might end up with semicolon, which we discard
  }

}
