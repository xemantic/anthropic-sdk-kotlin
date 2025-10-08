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

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.tool.Computer
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.tool.Toolbox
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ComputerTest {

    @Test
    fun `should use Computer tool`() = runTest {
        // given
        var receivedInput: Computer.Input? = null
        val toolbox = Toolbox {
            tool(
                Computer {
                    displayWidthPx = 1024
                    displayHeightPx = 768
                }
            ) {
                receivedInput = this
            }
        }

        val anthropic = testAnthropic {
            +Anthropic.Beta.COMPUTER_USE_2025_01_24
            defaultTools = toolbox.tools
        }

        // when
        val response = anthropic.messages.create {
            +Message { +"Take a screenshot" }
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
            have(action == Computer.Action.SCREENSHOT)
            have(coordinate == null)
            have(text == null)
        }
    }

    @Test
    fun `should serialize Computer tool`() {
        anthropicJson.encodeToString(
            Computer {
                displayWidthPx = 1024
                displayHeightPx = 768
                displayNumber = 0
            }
        ) shouldEqualJson """
            {
              "name": "computer",
              "type": "computer_20250124",
              "display_width_px": 1024,
              "display_height_px": 768,
              "display_number": 0
            }
        """
    }

    @Test
    fun `should deserialize Computer tool`() {
        anthropicJson.decodeFromString<Tool>(
            """
            {
              "name": "computer",
              "type": "computer_20250124",
              "display_width_px": 1024,
              "display_height_px": 768,
              "display_number": 0
            }
            """
        ) should {
            have(name == "computer")
            be<Computer>()
            have(type == "computer_20250124")
            have(displayWidthPx == 1024)
            have(displayHeightPx == 768)
            have(displayNumber == 0)
        }
    }

    @Test
    fun `should return JSON for Computer tool toString`() {
        Computer {
            displayWidthPx = 1024
            displayHeightPx = 768
            displayNumber = 0
        }.toString() shouldEqualJson """
            {
              "name": "computer",
              "type": "computer_20250124",
              "display_width_px": 1024,
              "display_height_px": 768,
              "display_number": 0
            }
        """
    }

    @Test
    fun `should deserialize Computer tool Input`() {
        anthropicJson.decodeFromString<Computer.Input>(
            """
            {
              "action": "screenshot"
            }
            """
        ) should {
            have(action == Computer.Action.SCREENSHOT)
            have(coordinate == null)
            have(text == null)
        }
    }

    @Test
    fun `should serialize Computer tool Input with action`() {
        anthropicJson.encodeToString(Computer.Companion.Input {
            action = Computer.Action.SCREENSHOT
        }) shouldEqualJson """
            {
              "action": "screenshot"
            }
        """
    }

}