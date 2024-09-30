package com.xemantic.anthropic.message

import com.xemantic.anthropic.anthropicJsonConfigurer
import io.kotest.assertions.json.shouldEqualJson
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
  private val json = Json {
    anthropicJsonConfigurer()
    prettyPrint = true
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

}
