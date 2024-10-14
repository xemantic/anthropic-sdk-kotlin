package com.xemantic.anthropic.message

import com.xemantic.anthropic.test.testJson
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlin.test.Test

/**
 * Tests the JSON serialization format of created Anthropic API message requests.
 */
class MessageRequestTest {

  @Test
  fun defaultMessageShouldHaveRoleUser() {
    // given
    val message = Message {}
    // then
    message.role shouldBe Role.USER
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
    val json = testJson.encodeToString(request)

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
