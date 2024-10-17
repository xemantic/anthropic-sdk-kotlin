package com.xemantic.anthropic

import com.xemantic.anthropic.event.ContentBlockDeltaEvent
import com.xemantic.anthropic.event.Delta.TextDelta
import com.xemantic.anthropic.message.Image
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.MessageResponse
import com.xemantic.anthropic.message.Role
import com.xemantic.anthropic.message.StopReason
import com.xemantic.anthropic.message.Text
import com.xemantic.anthropic.message.ToolUse
import com.xemantic.anthropic.message.plusAssign
import com.xemantic.anthropic.test.Calculator
import com.xemantic.anthropic.test.DatabaseQueryTool
import com.xemantic.anthropic.test.FibonacciTool
import com.xemantic.anthropic.test.TestDatabase
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
      type shouldBe MessageResponse.Type.MESSAGE
      role shouldBe  Role.ASSISTANT
      model shouldBe "claude-3-5-sonnet-20240620"
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
  fun shouldReadTextFooFromTestImage() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.create {
      +Message {
        +Image(
          source = Image.Source(
            data = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAACXBIWXMAABOvAAATrwFj5o7DAAAAGXRFWHRTb2Z0d2FyZQB3d3cuaW5rc2NhcGUub3Jnm+48GgAAA61JREFUeJztmNtrFVcUxn/b+lYwxEuN8RKR1giiQmqxEomCghSU0krBUpqiTyraUn3oU+m/UPsoiD6JiiBeW4T64P1FKBSLGlBDjE21NV7r/SwfZp1zhmHPmRnjnPWQ/cFmcfZe69tfvuyZWTNORBjNGGMtwBrBAGsB1ggGWAuwRjDAWoA1ggHWAqwRDLAWYI1ggLUAawQDrAVYIxhgLcAawQBrAdYIBlgLsMbYZm/onGsBJhOZPyQi90bINw5oA94B/hGRu4Xq0z6LO+d2AT0FuM6IyDcpXJOArcBnQGdi+TKwD/glr3jnXCvwPfA5MDex3AfsV77bmWQi4h3Ar4AUGCdSeNYC9xO5j4Gnibn/gDVpemJ8nwJ3E7X/A08Sc/eBrzL5chjwB/BFjtHj4dgAVJTnJrAZmKJrY4CZwI/AHc15BaxvoOlr4KXmDhGdqum65oDpwA+6Jrr3lpEa8FuWiyn1i4DnyvE70NogtwP4U3OfAQs8OfP1Py3AWWBSA7524KLmvgA+tjDgtNb3AS058juAR2mXE3Bc1waAiTn42oBhrTnXVAOArti1mHqkPXU/xY5uZ2y+M3YpfVeAb1tMR5cvp6w+YIXGCnCgQN1ejQ5YFZtfrnMQPTGK8FUfc6t9CWUZsEjjZRF5kLdIRK4At/TnHA/fgIgMFeAbBK56+GrIY8BK55xkjPWJmvc0/p1XbAxVA6aWyFdDWSdgnMZHb1Bb7QzHv2W+Cb7FPK3weWBLRk5/4ve/Gt/NwZ9Ei8b4H/u2+WrIY8ADEblYcNObGjuKFDnnHDBLf14bKZ/ifQ9fDWVdAqc1fuCcm1agbh71o3ohNn9KY5tzznsz88E5NwuY4eGroSwDDhB1dAAbC9Rt0vgMOBqbPwI8TOQU4XsFHPRmlNgJ7tT6J6Q0IYn8Hup9/g7P+s/UW9vuHHwL1UgB9jS1E9T6yUSPICF62VnaIPcToru1AIN4+nygFbiuOcPAygZ8y4hunNW9pzbdAOXoov6mVwEOAb1AN7AEWEe9x6+K/bAB39yYqQIcU45uHb26R7VtHs46LaUaoDwdwImYaN+o6H4zc/C1A4cz+AQ4CczO4mv0RehbdfwvEdnuTSoA59xioo8jHwFTiO4N/UTfG3ZL1AYX4VsIfEnUJrcTvXrfIHqt3i0il3LxpBkwWjDqvwoHA6wFWCMYYC3AGsEAawHWCAZYC7BGMMBagDWCAdYCrBEMsBZgjWCAtQBrBAOsBVgjGGAtwBqvASNwHwnSLggJAAAAAElFTkSuQmCC",
            mediaType = Image.MediaType.IMAGE_PNG
          )
        )
        +"What's on this picture?"
      }
    }

    // then
    assertSoftly(response) {
      stopReason shouldBe StopReason.END_TURN
      content.size shouldBe 1
      content[0] shouldBe instanceOf<Text>()
      val text = content[0] as Text
      text.text.uppercase() shouldContain "FOO"
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
        .filterIsInstance<ContentBlockDeltaEvent>()
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
      useTools()
    }
    conversation += initialResponse

    // then
    assertSoftly(initialResponse) {
      stopReason shouldBe StopReason.TOOL_USE
      content.size shouldBe 2
      content[0] shouldBe instanceOf<Text>()
      content[1] shouldBe instanceOf<ToolUse>()
      (content[1] as ToolUse).name shouldBe "Calculator"
    }

    val toolUse = initialResponse.content[1] as ToolUse
    val result = toolUse.use() // here we execute the tool

    conversation += Message { +result }

    // when
    val resultResponse = client.messages.create {
      messages = conversation
      useTools()
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
      useTools()
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
      useTools()
    }
    conversation += fibonacciResponse

    val fibonacciToolUse = fibonacciResponse.content.filterIsInstance<ToolUse>().first()
    fibonacciToolUse.name shouldBe "FibonacciTool"
    val fibonacciResult = fibonacciToolUse.use()
    conversation += Message { +fibonacciResult }

    val calculatorResponse = client.messages.create {
      messages = conversation
      useTools()
    }
    conversation += calculatorResponse

    val calculatorToolUse = calculatorResponse.content.filterIsInstance<ToolUse>().first()
    calculatorToolUse.name shouldBe "Calculator"
    val calculatorResult = calculatorToolUse.use()
    conversation += Message { +calculatorResult }

    val finalResponse = client.messages.create {
      messages = conversation
      useTools()
    }

    finalResponse.content[0] shouldBe instanceOf<Text>()
    (finalResponse.content[0] as Text).text shouldContain "6,378,911.8"
  }

  @Test
  fun shouldUseToolWithDependencies() = runTest {
    // given
    val testDatabase = TestDatabase()
    val client = Anthropic {
      tool<DatabaseQueryTool> {
        database = testDatabase
      }
    }

    // when
    val response = client.messages.create {
      +Message { +"List data in CUSTOMER table" }
      useTool<DatabaseQueryTool>()
    }
    val toolUse = response.content.filterIsInstance<ToolUse>().first()
    toolUse.use()

    // then
    testDatabase.executedQuery shouldNotBe null
    testDatabase.executedQuery!!.uppercase() shouldStartWith "SELECT * FROM CUSTOMER"
    // depending on the response the statement might end up with semicolon, which we discard
  }

}
