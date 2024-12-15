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

package com.xemantic.ai.anthropic.error

import com.xemantic.ai.anthropic.Response
import com.xemantic.ai.anthropic.test.testJson
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
