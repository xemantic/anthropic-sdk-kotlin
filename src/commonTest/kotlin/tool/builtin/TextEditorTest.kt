/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic.tool.builtin

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.tool.TextEditor
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.tool.Toolbox
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TextEditorTest {

    @Test
    fun `should use TextEditor tool`() = runTest {
        // given
        var receivedInput: TextEditor.Input? = null
        val toolbox = Toolbox {
            tool(TextEditor()) {
                receivedInput = this
            }
        }
        val anthropic = testAnthropic {
            defaultTools = toolbox.tools
        }

        // when
        val response = anthropic.messages.create {
            +Message { +"Look at the /tmp/foo.txt file" }
            tools = toolbox.tools
        }

        // then
        response should {
            have(stopReason == StopReason.TOOL_USE)
        }

        // when
        response.useTools(toolbox)

        // then
        receivedInput should {
            have(command == TextEditor.Command.VIEW)
            have(path == "/tmp/foo.txt")
        }
    }

    @Test
    fun `should serialize TextEditorTool`() {
        anthropicJson.encodeToString(
            TextEditor {}
        ) sameAsJson """
            {
              "name": "str_replace_based_edit_tool",
              "type": "text_editor_20250728"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize TextEditorTool`() {
        anthropicJson.decodeFromString<Tool>(
            """
           {
             "name": "str_replace_based_edit_tool",
             "type": "text_editor_20250728"
           }
           """
        ) should {
            have(name == "str_replace_based_edit_tool")
            be<TextEditor>()
            have(type == "text_editor_20250728")
        }
    }

    @Test
    fun `should return JSON for TextEditorTool toString`() {
        TextEditor {}.toString() sameAsJson """
            {
              "name": "str_replace_based_edit_tool",
              "type": "text_editor_20250728"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize TextEditorTool Input`() {
        anthropicJson.decodeFromString<TextEditor.Input>("""
            {
              "command": "view",
              "path": "/tmp/foo.txt"
            }
        """.trimIndent()) should {
            have(command == TextEditor.Command.VIEW)
            have(path == "/tmp/foo.txt")
            have(fileText == null)
            have(insertLine == null)
            have(newStr == null)
            have(oldStr == null)
            have(viewRange == null)
        }
    }

    @Test
    fun `should serialize TextEditorTool Input with command`() {
        anthropicJson.encodeToString(TextEditor.Input {
            command = TextEditor.Command.VIEW
            path = "/tmp/foo.txt"
        }) sameAsJson """
            {
              "command": "view",
              "path": "/tmp/foo.txt"
            }
        """.trimIndent()
    }

}
