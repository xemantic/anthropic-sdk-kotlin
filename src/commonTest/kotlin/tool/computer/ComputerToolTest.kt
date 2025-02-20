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

class ComputerToolTest {

    @Test
    fun `should use ComputerTool`() = runTest {
        // given
        var actualInput: ComputerTool.Input? = null
        val tool = ComputerTool(
            builder = {
                displayWidthPx = 1024
                displayHeightPx = 768
            }
        ) { actualInput = this }
        val anthropic = Anthropic {
            anthropicBeta += Anthropic.Beta.COMPUTER_USE_2024_10_22.id
        }

        // when
        val response = anthropic.messages.create {
            +Message { +"Take a screenshot" }
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
            have(action == ComputerTool.Action.SCREENSHOT)
            have(coordinate == null)
            have(text == null)
        }
    }

    @Test
    fun `should serialize ComputerTool`() {
        anthropicJson.encodeToString(
            ComputerTool(
                builder = {
                    displayWidthPx = 1024
                    displayHeightPx = 768
                    displayNumber = 0
                }
            ) {}
        ) shouldEqualJson /* language=json */ """
            {
              "name": "computer",
              "type": "computer_20241022",
              "display_width_px": 1024,
              "display_height_px": 768,
              "display_number": 0
            }
        """
    }

    @Test
    fun `should deserialize ComputerTool`() {
        anthropicJson.decodeFromString<Tool>(
            """
            {
              "name": "computer",
              "type": "computer_20241022",
              "display_width_px": 1024,
              "display_height_px": 768,
              "display_number": 0
            }
            """
        ) should {
            have(name == "computer")
            be<ComputerTool>()
            have(type == "computer_20241022")
            have(displayWidthPx == 1024)
            have(displayHeightPx == 768)
            have(displayNumber == 0)
        }
    }

    @Test
    fun `should return JSON for ComputerTool toString`() {
        ComputerTool(
            builder = {
                displayWidthPx = 1024
                displayHeightPx = 768
                displayNumber = 0
            }
        ) {}.toString() shouldEqualJson /* language=json */ """
            {
              "name": "computer",
              "type": "computer_20241022",
              "display_width_px": 1024,
              "display_height_px": 768,
              "display_number": 0
            }
        """
    }

    @Test
    fun `should deserialize ComputerTool Input`() {
        anthropicJson.decodeFromString<ComputerTool.Input>(
            """
            {
              "action": "screenshot"
            }
            """
        ) should {
            have(action == ComputerTool.Action.SCREENSHOT)
            have(coordinate == null)
            have(text == null)
        }
    }

    @Test
    fun `should serialize ComputerTool Input with action`() {
        anthropicJson.encodeToString(ComputerTool.Input {
            action = ComputerTool.Action.SCREENSHOT
        }) shouldEqualJson /* language=json */ """
            {
              "action": "screenshot"
            }
        """
    }

}
