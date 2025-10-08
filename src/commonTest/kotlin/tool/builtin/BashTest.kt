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
import com.xemantic.ai.anthropic.tool.Bash
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.tool.Toolbox
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class BashTest {

    @Test
    fun `should use Bash tool`() = runTest {
        // given
        var receivedInput: Bash.Input? = null
        val toolbox = Toolbox {
            tool(Bash()) {
                receivedInput = this
            }
        }
        val anthropic = testAnthropic()

        // when
        val response = anthropic.messages.create {
            +Message { +"List files in the current folder" }
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
            have(command!!.startsWith("ls"))
            have(restart == null)
        }
    }

    @Test
    fun `should serialize BashTool`() {
        anthropicJson.encodeToString(
            Bash {}
        ) shouldEqualJson """
            {
              "name": "bash",
              "type": "bash_20250124"
            }            
        """
    }

    @Test
    fun `should deserialize BashTool`() {
        anthropicJson.decodeFromString<Tool>(
            """
            {
              "name": "bash",
              "type": "bash_20250124"
            }
            """
        ) should {
            have(name == "bash")
            be<Bash>()
            have(type == "bash_20250124")
        }
    }

    @Test
    fun `should return JSON for Bash tool toString`() {
        Bash {}.toString() shouldEqualJson """
            {
              "name": "bash",
              "type": "bash_20250124"
            } 
        """
    }

    @Test
    fun `should deserialize Bash tool input`() {
        anthropicJson.decodeFromString<Bash.Input>(
            """
            {
              "command": "ls"
            }
            """
        ) should {
            have(command == "ls")
            have(restart == null)
        }
    }

    @Test
    fun `should serialize empty Bash tool input`() {
        anthropicJson.encodeToString(Bash.Input {}) shouldEqualJson """
            {}
        """
    }

    @Test
    fun `should serialize Bash tool input with command`() {
        anthropicJson.encodeToString(Bash.Input {
            command = "ls"
        }) shouldEqualJson """
            {
              "command": "ls"
            }
        """
    }

    @Test
    fun `should serialize Bash tool input with restart`() {
        anthropicJson.encodeToString(Bash.Input {
            restart = true
        }) shouldEqualJson """
            {
              "restart": true
            }
        """
    }

}