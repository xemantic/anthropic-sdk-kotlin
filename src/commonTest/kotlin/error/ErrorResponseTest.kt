package com.xemantic.anthropic.error

import com.xemantic.anthropic.Response
import com.xemantic.anthropic.test.testJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

/**
 * Tests the JSON format of deserialized Anthropic API error responses.
 */
class ErrorResponseTest {

  @Test
  fun `Should deserialize ErrorResponse`() {
    testJson.decodeFromString<Response>(/* language=json */ """
      {
        "type": "error",
        "error": {
          "type": "not_found_error",
          "message": "The requested resource could not be found."
        }
      }
    """) should {
      be<ErrorResponse>()
      have(error == Error(
        type = "not_found_error",
        message = "The requested resource could not be found."
      ))
    }
  }

}
