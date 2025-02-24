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
import kotlin.test.Test

class BashToolTest {

    @Test
    fun `should use BashTool`() = runTest {
        // given
        var actualInput: BashTool.Input? = null
        val tool = BashTool { actualInput = this }
        val anthropic = Anthropic()

        // when
        val response = anthropic.messages.create {
            +Message { +"List files in the current folder" }
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
            have(command!!.startsWith("ls"))
            have(restart == null)
        }
    }

    @Test
    fun `should serialize BashTool`() {
        anthropicJson.encodeToString(
            BashTool {}
        ) shouldEqualJson /* language=json */ """
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
            be<BashTool>()
            have(type == "bash_20250124")
        }
    }

    @Test
    fun `should return JSON for BashTool toString`() {
        BashTool {}.toString() shouldEqualJson /* language=json */ """
            {
              "name": "bash",
              "type": "bash_20250124"
            } 
        """
    }

    @Test
    fun `should deserialize BashTool Input`() {
        anthropicJson.decodeFromString<BashTool.Input>(
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
    fun `should serialize empty BashTool Input`() {
        anthropicJson.encodeToString(BashTool.Input {}) shouldEqualJson /* language=json */ """
            {}
        """
    }

    @Test
    fun `should serialize BashTool Input with command`() {
        anthropicJson.encodeToString(BashTool.Input {
            command = "ls"
        }) shouldEqualJson /* language=json */ """
            {
              "command": "ls"
            }
        """
    }

    @Test
    fun `should serialize BashTool Input with restart`() {
        anthropicJson.encodeToString(BashTool.Input {
            restart = true
        }) shouldEqualJson /* language=json */ """
            {
              "restart": true
            }
        """
    }

}
