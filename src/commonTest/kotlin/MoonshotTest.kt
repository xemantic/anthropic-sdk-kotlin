/*
 * Copyright 2024-2026 Xemantic contributors
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

package com.xemantic.ai.anthropic

import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ThinkingBlock
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.message.toMessageResponse
import com.xemantic.ai.anthropic.test.env
import com.xemantic.ai.anthropic.thinking.ThinkingConfig
import com.xemantic.ai.anthropic.tool.Toolbox
import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.isBrowserPlatform
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlin.test.Test

class MoonshotTest {

    private fun moonshotAnthropic(
        block: Anthropic.Config.() -> Unit = {}
    ) = Anthropic {
        baseUrl = env["MOONSHOT_API_BASE_URL"]
        defaultModel = env["MOONSHOT_DEFAULT_MODEL"]
        apiKey = env["MOONSHOT_API_KEY"]
        useXApiKeyHeader = false
        useAuthorizationBearerHeader = true
        block()
    }

    @SerialName("get_weather")
    @Description("Get the current weather in a given location")
    class GetWeather(
        @Description("The city and state, e.g. San Francisco, CA")
        val location: String
    )

    @Test
    fun `should receive an introduction from Kimi`() = runTest {
        if (isBrowserPlatform) return@runTest // our test Moonshot API server depends on CORS
        // given
        val anthropic = moonshotAnthropic()

        // when
        val response = anthropic.messages.create {
            +"Hello World! What's your name?"
            thinking = ThinkingConfig.Disabled // Our Kimi model deployment is returning thinking tokens by default
        }

        // then
        response should {
            have(role == ASSISTANT)
            have("Kimi" in model)
            have(stopReason == END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("Kimi" in text)
            }
            have(stopSequence == null)
            usage should {
                have(inputTokens > 0)
                have(outputTokens > 0)
            }
        }
    }

    @Test
    fun `should stream an introduction from Kimi with adaptive thinking`() = runTest {
        if (isBrowserPlatform) return@runTest // our test Moonshot API server depends on CORS
        // given
        val anthropic = moonshotAnthropic()

        // when
        val response = anthropic.messages.stream {
            +"Hello World! What's your name?"
            thinking = ThinkingConfig.Adaptive()
        }.toMessageResponse()

        // then
        response should {
            have(role == ASSISTANT)
            have("Kimi" in model)
            have(stopReason == END_TURN)
            content[0] should {
                be<ThinkingBlock>()
            }
            content[1] should {
                be<Text>()
                have("Kimi" in text)
            }
        }
    }

    @Test
    fun `should stream tool use from Kimi`() = runTest {
        if (isBrowserPlatform) return@runTest // our test Moonshot API server depends on CORS
        // given
        val toolbox = Toolbox {
            tool<GetWeather> {
                "15 degrees in $location"
            }
        }
        val anthropic = moonshotAnthropic {
            defaultTools = toolbox.tools
        }
        val conversation = mutableListOf<Message>()
        conversation += "What is the weather like in San Francisco?"

        // when
        val events = anthropic.messages.stream {
            messages = conversation
            thinking = ThinkingConfig.Disabled
        }.toList()
        val response = events.asFlow().toMessageResponse()

        // DEBUG (remove once #148 is green): surface the full Kimi event sequence
        // so we can verify the index/order assumptions made by toMessageResponse.
        val eventDump = events.joinToString("\n  ", prefix = "  ")
        println("[kimi-stream-events]\n$eventDump")
        if (!response.content.any { it is ToolUse }) {
            kotlin.test.fail(
                "No ToolUse block in response.content.\n" +
                    "stopReason=${response.stopReason}\n" +
                    "content=${response.content}\n" +
                    "events:\n$eventDump"
            )
        }

        // then
        response should {
            have(stopReason == TOOL_USE)
            have(content.any { it is ToolUse })
            content.filterIsInstance<ToolUse>().first() should {
                have(name == "get_weather")
                have("San Francisco" in (input["location"]?.toString() ?: ""))
            }
        }
    }

}
