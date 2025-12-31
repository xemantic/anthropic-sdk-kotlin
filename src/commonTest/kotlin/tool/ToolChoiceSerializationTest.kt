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
import com.xemantic.kotlin.test.sameAsJson
import kotlin.test.Test

class ToolChoiceSerializationTest {

    private fun ToolChoice.encodeToString() =
        anthropicJson.encodeToString<ToolChoice>(this)

    @Test
    fun `should serialize ToolChoice Auto`() {

        ToolChoice.Auto().encodeToString() sameAsJson """
            {
              "type": "auto"
            }
        """.trimIndent()

        ToolChoice.Auto {
            disableParallelToolUse = true
        }.encodeToString() sameAsJson """
            {
              "type": "auto",
              "disable_parallel_tool_use": true
            }
        """.trimIndent()

    }

    @Test
    fun `should serialize ToolChoice Any`() {

        ToolChoice.Any().encodeToString() sameAsJson """
            {
              "type": "any"
            }
        """.trimIndent()

        ToolChoice.Any {
            disableParallelToolUse = true
        }.encodeToString() sameAsJson """
            {
              "type": "any",
              "disable_parallel_tool_use": true
            }
        """.trimIndent()

    }

    @Test
    fun `should serialize ToolChoice Tool`() {

        ToolChoice.Tool("foo").encodeToString() sameAsJson """
            {
              "type": "tool",
              "name": "foo"
            }
        """.trimIndent()

        ToolChoice.Tool("foo") {
            disableParallelToolUse = true
        }.encodeToString() sameAsJson """
            {
              "type": "tool",
              "name": "foo",
              "disable_parallel_tool_use": true
            }
        """.trimIndent()

        ToolChoice.Tool<Calculator> {
            disableParallelToolUse = true
        }.encodeToString() sameAsJson """
            {
              "type": "tool",
              "name": "calculator",
              "disable_parallel_tool_use": true
            }
        """.trimIndent()

    }

}
