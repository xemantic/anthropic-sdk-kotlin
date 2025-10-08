/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.tool.test.Calculator
import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

class ToolChoiceSerializationTest {

    private fun ToolChoice.encodeToString() =
        anthropicJson.encodeToString<ToolChoice>(this)

    @Test
    fun `should serialize ToolChoice Auto`() {

        ToolChoice.Auto().encodeToString() shouldEqualJson """
            {
              "type": "auto"
            }
        """

        ToolChoice.Auto {
            disableParallelToolUse = true
        }.encodeToString() shouldEqualJson """
            {
              "type": "auto",
              "disable_parallel_tool_use": true
            }
        """

    }

    @Test
    fun `should serialize ToolChoice Any`() {

        ToolChoice.Any().encodeToString() shouldEqualJson """
            {
              "type": "any"
            }
        """

        ToolChoice.Any {
            disableParallelToolUse = true
        }.encodeToString() shouldEqualJson """
            {
              "type": "any",
              "disable_parallel_tool_use": true
            }
        """

    }

    @Test
    fun `should serialize ToolChoice Tool`() {

        ToolChoice.Tool("foo").encodeToString() shouldEqualJson """
            {
              "type": "tool",
              "name": "foo"
            }
        """

        ToolChoice.Tool("foo") {
            disableParallelToolUse = true
        }.encodeToString() shouldEqualJson """
            {
              "type": "tool",
              "name": "foo",
              "disable_parallel_tool_use": true
            }
        """

        ToolChoice.Tool<Calculator> {
            disableParallelToolUse = true
        }.encodeToString() shouldEqualJson """
            {
              "type": "tool",
              "name": "calculator",
              "disable_parallel_tool_use": true
            }
        """

    }

}
