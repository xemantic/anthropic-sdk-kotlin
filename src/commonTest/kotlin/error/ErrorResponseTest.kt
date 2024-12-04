package com.xemantic.anthropic.error

import com.xemantic.anthropic.Response
import com.xemantic.anthropic.test.assert
import com.xemantic.anthropic.test.testJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import kotlin.test.Test

/**
 * Tests the JSON format of deserialized Anthropic API error responses.
 */
class ErrorResponseTest {

  @Test
  fun shouldDeserializeToolUseMessageResponse() {
    // given
    val jsonResponse = """
      {
        "type": "error",
        "error": {
          "type": "not_found_error",
          "message": "The requested resource could not be found."
        }
      }
    """.trimIndent()

    val response = testJson.decodeFromString<Response>(jsonResponse)
    response shouldBe instanceOf<ErrorResponse>()
    (response as ErrorResponse).assert {
      error shouldBe Error(
        type = "not_found_error",
        message = "The requested resource could not be found."
      )
    }
  }

}