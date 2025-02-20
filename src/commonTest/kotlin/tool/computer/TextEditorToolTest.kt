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

package com.xemantic.ai.anthropic.tool.computer

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Test

class TextEditorToolTest {

    @Test
    fun `should use TextEditorTool`() = runTest {
        // given
        var actualInput: TextEditorTool.Input? = null
        val tool = TextEditorTool { actualInput = this }
        val anthropic = Anthropic()

        // when
        val response = anthropic.messages.create {
            +Message { +"Look at the /tmp/foo.txt file" }
            tools = listOf(tool)
        }

        // then
        response should {
            have(stopReason == StopReason.TOOL_USE)
        }

        // when
        response.useTools()

        // then
        actualInput should {
            have(command == TextEditorTool.Command.VIEW)
            have(path == "/tmp/foo.txt")
        }
    }

    @Test
    fun `should serialize TextEditorTool`() {
        anthropicJson.encodeToString(
            TextEditorTool {}
        ) shouldEqualJson /* language=json */ """
            {
              "name": "str_replace_editor",
              "type": "text_editor_20250124"
            }
        """
    }

    @Test
    fun `should deserialize TextEditorTool`() {
        anthropicJson.decodeFromString<Tool>(
           """
           {
             "name": "str_replace_editor",
             "type": "text_editor_20250124"
           }
           """
        ) should {
            have(name == "str_replace_editor")
            be<TextEditorTool>()
            have(type == "text_editor_20250124")
        }
    }

    @Test
    fun `should return JSON for TextEditorTool toString`() {
        TextEditorTool {}.toString() shouldEqualJson /* language=json */ """
            {
              "name": "str_replace_editor",
              "type": "text_editor_20250124"
            }
        """
    }

    @Test
    fun `should deserialize TextEditorTool Input`() {
        anthropicJson.decodeFromString<TextEditorTool.Input>(
            """
            {
              "command": "view",
              "path": "/tmp/foo.txt"
            }
            """
        ) should {
            have(command == TextEditorTool.Command.VIEW)
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
        anthropicJson.encodeToString(TextEditorTool.Input {
            command = TextEditorTool.Command.VIEW
            path = "/tmp/foo.txt"
        }) shouldEqualJson /* language=json */ """
            {
              "command": "view",
              "path": "/tmp/foo.txt"
            }
        """
    }


}