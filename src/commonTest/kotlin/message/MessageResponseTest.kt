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

package com.xemantic.ai.anthropic.message

import com.xemantic.ai.anthropic.Response
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.test.testJson
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test

/**
 * Tests the JSON format of deserialized Anthropic API message responses.
 */
class MessageResponseTest {

  @Test
  fun `Should deserialize ToolUse message response`() {
    // given
    /* language=json */
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
    """

    // when
    val response = testJson.decodeFromString<Response>(jsonResponse)

    // then
    response should {
      be<MessageResponse>()
      have(id == "msg_01PspkNzNG3nrf5upeTsmWLF")
      have(role == Role.ASSISTANT)
      have(model == "claude-3-5-sonnet-20241022")
      have(content.size == 1)
      have(stopReason == StopReason.TOOL_USE)
      have(stopSequence == null)
      have(usage == Usage(
        inputTokens = 419,
        outputTokens = 86
      ))
      content[0] should {
        be<ToolUse>()
        have(id == "toolu_01YHJK38TBKCRPn7zfjxcKHx")
        have(name == "Calculator")
        have(input == buildJsonObject {
          put("operation", JsonPrimitive("MULTIPLY"))
          put("a", JsonPrimitive(15))
          put("b", JsonPrimitive(7))
        })
      }
    }
  }

}
