package com.xemantic.anthropic.message

import com.xemantic.anthropic.Response
import com.xemantic.anthropic.test.testJson
import com.xemantic.anthropic.tool.ToolUse
import com.xemantic.anthropic.usage.Usage
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import kotlin.test.Test

/**
 * Tests the JSON format of deserialized Anthropic API message responses.
 */
class MessageResponseTest {

  @Test
  fun shouldDeserializeToolUseMessageResponse() {
    // given
    val jsonResponse = """
      {
        "id": "msg_01PspkNzNG3nrf5upeTsmWLF",
        "type": "message",
        "role": "assistant",
        "model": "claude-3-5-sonnet-20241022",
        "content": [
          {
            "type": "tool_use",
            "id": "toolu_01YHJK38TBKCRPn7zfjxcKHx",
            "name": "Calculator",
            "input": {
              "operation": "MULTIPLY",
              "a": 15,
              "b": 7
            }
          }
        ],
        "stop_reason": "tool_use",
        "stop_sequence": null,
        "usage": {
          "input_tokens": 419,
          "output_tokens": 86
        }
      }
    """.trimIndent()

    val response = testJson.decodeFromString<Response>(jsonResponse)
    response shouldBe instanceOf<MessageResponse>()
    assertSoftly(response as MessageResponse) {
      id shouldBe "msg_01PspkNzNG3nrf5upeTsmWLF"
      role shouldBe Role.ASSISTANT
      model shouldBe "claude-3-5-sonnet-20241022"
      content.size shouldBe 1
      content[0] shouldBe instanceOf<ToolUse>()
      stopReason shouldBe StopReason.TOOL_USE
      stopSequence shouldBe null
      usage shouldBe Usage(
        inputTokens = 419,
        outputTokens = 86
      )
    }
    val toolUse = response.content[0] as ToolUse
    assertSoftly(toolUse) {
      id shouldBe "toolu_01YHJK38TBKCRPn7zfjxcKHx"
      name shouldBe "Calculator"
      // TODO generate JsonObject to assert input
    }
  }

}
