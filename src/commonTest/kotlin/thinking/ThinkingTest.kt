/*
 * Copyright 2025-2026 Xemantic contributors
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

package com.xemantic.ai.anthropic.thinking

import com.xemantic.ai.anthropic.Model
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ThinkingBlock
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThinkingTest {

    @Test
    fun `should create message with thinking enabled`() = runTest {
        // given
        val client = testAnthropic()

        // when
        val response = client.messages.create {
            thinking = ThinkingConfig.Enabled {
                budgetTokens = 1024 // minimal budget
            }
            +"What is 27 * 453? Show your reasoning."
        }

        // then
        response.content should {
            filterIsInstance<ThinkingBlock>().first() should {
                have(thinking.isNotBlank())
                have(signature!!.isNotBlank())
            }
            have(any { it is Text })
        }
    }

    @Test
    fun `should create message with thinking enabled and omitted display`() = runTest {
        // given
        val client = testAnthropic()

        // when
        val response = client.messages.create {
            thinking = ThinkingConfig.Enabled {
                budgetTokens = 1024
                display = ThinkingConfig.Display.OMITTED
            }
            +"What is 27 * 453? Show your reasoning."
        }

        // then
        response.content should {
            filterIsInstance<ThinkingBlock>().first() should {
                have(thinking.isBlank())
                have(signature!!.isNotBlank())
            }
            have(any { it is Text })
        }
    }

    @Test
    fun `should fail to create message if budgetTokens is below 1024`() = runTest {
        // given
        val client = testAnthropic()

        val exception = assertFailsWith<AnthropicApiException> {
            // when
            client.messages.create {
                thinking = ThinkingConfig.Enabled {
                    budgetTokens = 1023
                }
                +"What is the capital of France?"
            }
        }

        // then
        assert(exception.message == "Error(type=invalid_request_error, message=thinking.enabled.budget_tokens: Input should be greater than or equal to 1024)")
    }

    @Test
    fun `should handle multi-turn conversation with thinking`() = runTest {
        // given
        val client = testAnthropic()
        val conversation = mutableListOf<Message>()

        // when
        conversation += "What is 15 + 28?"
        val response1 = client.messages.create {
            thinking = ThinkingConfig.Enabled {
                budgetTokens = 1024
            }
            messages = conversation
        }
        conversation += response1
        conversation += "Now multiply that result by 3"
        val response2 = client.messages.create {
            thinking = ThinkingConfig.Enabled {
                budgetTokens = 1024
            }
            messages = conversation
        }

        // then
        response1.content should {
            have(any { it is ThinkingBlock })
        }
        response2.content should {
            have(any { it is ThinkingBlock })
        }
    }

    @Test
    fun `should create message with adaptive thinking`() = runTest {
        // given
        val client = testAnthropic()

        // when
        val response = client.messages.create {
            model(Model.CLAUDE_SONNET_4_6)
            thinking = ThinkingConfig.Adaptive()
            +"What is 27 * 453? Show your reasoning."
        }

        // then
        response.content should {
            filterIsInstance<ThinkingBlock>().first() should {
                have(thinking.isNotBlank())
                have(signature!!.isNotBlank())
            }
            have(any { it is Text })
        }
    }

    @Test
    fun `should create message with adaptive thinking and omitted display`() = runTest {
        // given
        val client = testAnthropic()

        // when
        val response = client.messages.create {
            model(Model.CLAUDE_SONNET_4_6)
            thinking = ThinkingConfig.Adaptive {
                display = ThinkingConfig.Display.OMITTED
            }
            +"What is 27 * 453? Show your reasoning."
        }

        // then
        response.content should {
            filterIsInstance<ThinkingBlock>().first() should {
                have(thinking.isBlank())
                have(signature!!.isNotBlank())
            }
            have(any { it is Text })
        }
    }

    @Test
    fun `should handle multi-turn conversation with adaptive thinking`() = runTest {
        // given
        val client = testAnthropic()
        val conversation = mutableListOf<Message>()

        // when
        conversation += "What is 15 + 28?"
        val response1 = client.messages.create {
            model(Model.CLAUDE_SONNET_4_6)
            thinking = ThinkingConfig.Adaptive()
            messages = conversation
        }
        conversation += response1
        conversation += "Now multiply that result by 3"
        val response2 = client.messages.create {
            model(Model.CLAUDE_SONNET_4_6)
            thinking = ThinkingConfig.Adaptive()
            messages = conversation
        }

        // then
        response1.content should {
            have(any { it is ThinkingBlock })
        }
        response2.content should {
            have(any { it is ThinkingBlock })
        }
    }

}
