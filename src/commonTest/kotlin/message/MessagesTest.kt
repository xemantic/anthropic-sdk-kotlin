package com.xemantic.anthropic.message

import com.xemantic.anthropic.anthropicJson
import com.xemantic.anthropic.testToolsSerializersModule
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests the JSON serialization format of created Anthropic API messages.
 */
class MessagesTest {

  /**
   * A pretty JSON printing for testing.
   */
  private val json = Json(from = anthropicJson) {
    prettyPrint = true
    @OptIn(ExperimentalSerializationApi::class)
    prettyPrintIndent = "  "
    serializersModule = testToolsSerializersModule
  }

  @Test
  fun shouldCreateTheSimplestMessageRequest() {
    // given
    val request = MessageRequest(
      defaultModel = "claude-3-opus-20240229"
    ) {
      +Message {
        +"Hey Claude!?"
      }
    }

    // when
    val json = json.encodeToString(request)

    // then
    json shouldEqualJson """
      {
        "model": "claude-3-opus-20240229",
        "messages": [
          {
            "role": "user",
            "content": [
              {
                "type": "text",
                "text": "Hey Claude!?"
              }
            ]
          }
        ],
        "max_tokens": 1024
      }
    """.trimIndent()
  }

  @Test
  fun shouldDeserializeToolUseRequest() {
    val request = """
      {
        "id": "msg_01PspkNzNG3nrf5upeTsmWLF",
        "type": "message",
        "role": "assistant",
        "model": "claude-3-opus-20240229",
        "content": [
          {
            "type": "tool_use",
            "id": "toolu_01YHJK38TBKCRPn7zfjxcKHx",
            "name": "com_xemantic_anthropic_AnthropicTest_Calculator",
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

    val response = json.decodeFromString<MessageResponse>(request)
  }

}
