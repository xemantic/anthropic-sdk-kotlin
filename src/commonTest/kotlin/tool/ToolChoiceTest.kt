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

package com.xemantic.anthropic.tool

import com.xemantic.anthropic.test.testJson
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class ToolChoiceTest {

  // we are using list wrapping to trigger polymorphic deserializer
  private val ToolChoice.json get() = testJson
    .encodeToString(listOf<ToolChoice>(this))
    .removePrefix("[")
    .removeSuffix("]")

  @Test
  fun shouldSerializeToolChoiceAuto() {

    ToolChoice.Auto().json shouldEqualJson /* language=json */ """
      {
        "type": "auto"
      }
    """

    ToolChoice.Auto(
      disableParallelToolUse = true
    ).json shouldEqualJson """
      {
        "type": "auto",
        "disable_parallel_tool_use": true
      }
    """

  }

  @Test
  fun shouldSerializeToolChoiceAny() {

    ToolChoice.Any().json shouldEqualJson /* language=json */ """
      {
        "type": "any"
      }
    """

    ToolChoice.Any(
      disableParallelToolUse = true
    ).json shouldEqualJson /* language=json */ """
      {
        "type": "any",
        "disable_parallel_tool_use": true
      }
    """

  }

  @Test
  fun shouldSerializeToolChoiceTool() {

    ToolChoice.Tool(
      name = "foo"
    ).json shouldEqualJson /* language=json */ """
      {
        "type": "tool",
        "name": "foo"
      }
    """

    ToolChoice.Tool(
      name = "foo",
      disableParallelToolUse = true
    ).json shouldEqualJson /* language=json */ """
      {
        "type": "tool",
        "name": "foo",
        "disable_parallel_tool_use": true
      }
    """

  }

}
