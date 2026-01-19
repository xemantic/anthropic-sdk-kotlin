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

package com.xemantic.ai.anthropic.message

import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlin.test.Test

class MessageCountTokensTest {

    @Description("Get the current weather in a given location")
    data class GetWeather(
        @Description("The city and state, e.g. San Francisco, CA")
        val location: String,
        val unit: TemperatureUnit? = null
    )

    @Description("The unit of temperature, either 'celsius' or 'fahrenheit'")
    @Suppress("unused")
    enum class TemperatureUnit {
        @SerialName("celsius")
        CELSIUS,

        @SerialName("fahrenheit")
        FAHRENHEIT
    }

    @Test
    fun `should count tokens in basic message`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val tokenCount = anthropic.messages.countTokens {
            +"Hello, Claude"
        }

        // then
        tokenCount should {
            have(inputTokens > 0)
            have(inputTokens < 20) // Should be a small number for such a simple message
        }
    }

    @Test
    fun `should count tokens in message with system prompt`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val tokenCount = anthropic.messages.countTokens {
            system("You are a scientist")
            +"Hello, Claude"
        }

        // then
        tokenCount should {
            have(inputTokens > 0)
            // Should be more tokens than just the message alone
            have(inputTokens > 10)
        }
    }

    @Test
    fun `should count tokens in message with tools`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val tokenCount = anthropic.messages.countTokens {
            tools = listOf(
                Tool<GetWeather>("get_weather")
            )
            +"What's the weather like in San Francisco?"
        }

        // then
        tokenCount should {
            have(inputTokens > 0)
            // Should include tokens for the tool definition
            have(inputTokens > 50)
        }
    }

    @Test
    fun `should count tokens in message with multiple messages`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val tokenCount = anthropic.messages.countTokens {
            +Message {
                role = Role.USER
                +"Hello there."
            }
            +Message {
                role = Role.ASSISTANT
                +"Hi, I'm Claude. How can I help you?"
            }
            +Message {
                role = Role.USER
                +"Can you explain LLMs in plain English?"
            }
        }

        // then
        tokenCount should {
            have(inputTokens > 0)
            // Multi-turn conversation should have more tokens
            have(inputTokens > 20)
        }
    }

    @Test
    fun `should count tokens consistently for same input`() = runTest {
        // given
        val anthropic = testAnthropic()
        val message = "Hello, Claude"

        // when
        val tokenCount1 = anthropic.messages.countTokens {
            +message
        }

        val tokenCount2 = anthropic.messages.countTokens {
            +message
        }

        // then
        have(tokenCount1.inputTokens == tokenCount2.inputTokens)
    }

    @Test
    fun `should count more tokens for longer messages`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val shortTokenCount = anthropic.messages.countTokens {
            +"Hi"
        }

        val longTokenCount = anthropic.messages.countTokens {
            +"Hello, I would like to ask you a very detailed question about the nature of reality and consciousness."
        }

        // then
        have(longTokenCount.inputTokens > shortTokenCount.inputTokens)
    }

}
