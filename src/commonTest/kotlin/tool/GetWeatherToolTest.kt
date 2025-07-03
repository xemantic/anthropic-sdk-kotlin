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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ToolResult
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.Role
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlin.test.Test

/**
 * The `get_weather` is a common example in OpenAI and Anthropic
 * documentation when showing how to use tools / function calling.
 */
class GetWeatherToolTest {

    @SerialName("get_weather")
    @Description("Get the current weather in a given location")
    class GetWeather(
        @Description("The city and state, e.g. San Francisco, CA")
        val location: String
    )

    @Test
    fun `should get weather for the location`() = runTest {
        val weatherTools = listOf(
            Tool<GetWeather> {
                "15 degrees in $location" // We are returning static value. In real-life it should be another API call
            }
        )
        val anthropic = testAnthropic()
        val conversation = mutableListOf<Message>()

        conversation += "What is the weather like in San Francisco?"
        val response1 = anthropic.messages.create {
            messages = conversation
            tools = weatherTools
        }
        conversation += response1

        response1 should {
            have(stopReason == StopReason.TOOL_USE)
            have(content.isNotEmpty())
            have(content.any { it is ToolUse })
        }

        val tooResults = response1.useTools()
        tooResults should {
            have(role == Role.USER)
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(isError != false)
                content should {
                    have(size == 1)
                    get(0) should {
                        be<Text>()
                        have("15 degrees in San Francisco" in text)
                    }
                }
            }
        }

        conversation += tooResults
        val response2 = anthropic.messages.create {
            messages = conversation
            tools = weatherTools
        }
        response2 should {
            have("15" in text!!)
            have("San Francisco" in text!!)
        }
    }

}
