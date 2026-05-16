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

package com.xemantic.ai.anthropic.thinking

import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ThinkingBlock
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.Role
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ThinkingIntegrationTest {

    @Test
    fun `should create message with thinking enabled`() = runTest {
        val client = testAnthropic()

        val response = client.messages.create {
            model = "claude-sonnet-4-5-20250929"
            maxTokens = 4096
            thinking = ThinkingConfigEnabled {
                budgetTokens = 2048
            }
            messages = listOf(
                Message {
                    +"What is 27 * 453? Show your reasoning."
                }
            )
        }

        response should {
            have(model == "claude-sonnet-4-5-20250929")
            have(content.isNotEmpty())
        }

        // Should have at least one thinking block
        val thinkingBlocks = response.content.filterIsInstance<ThinkingBlock>()
        thinkingBlocks should {
            have(isNotEmpty())
        }

        // Thinking block should have content
        thinkingBlocks.first() should {
            have(thinking.isNotBlank())
            have(signature.isNotBlank())
        }

        // Should also have text response
        val textBlocks = response.content.filterIsInstance<Text>()
        textBlocks should {
            have(isNotEmpty())
        }

        println("✓ Thinking block received: ${thinkingBlocks.first().thinking.take(100)}...")
        println("✓ Text response: ${textBlocks.first().text.take(100)}...")
    }

    @Test
    fun `should create message with minimum thinking budget`() = runTest {
        val client = testAnthropic()

        val response = client.messages.create {
            model = "claude-sonnet-4-5-20250929"
            maxTokens = 4096
            thinking = ThinkingConfigEnabled {
                budgetTokens = 1024  // Minimum allowed
            }
            messages = listOf(
                Message {
                    +"What is the capital of France?"
                }
            )
        }

        response should {
            have(model == "claude-sonnet-4-5-20250929")
            have(content.isNotEmpty())
        }

        println("✓ Message created with minimum thinking budget (1024 tokens)")
    }

    @Test
    fun `should handle multi-turn conversation with thinking`() = runTest {
        val client = testAnthropic()

        // First request
        val response1 = client.messages.create {
            model = "claude-sonnet-4-5-20250929"
            maxTokens = 4096
            thinking = ThinkingConfigEnabled {
                budgetTokens = 2048
            }
            messages = listOf(
                Message {
                    +"What is 15 + 28?"
                }
            )
        }

        response1.content should {
            have(isNotEmpty())
        }

        val thinkingBlock1 = response1.content.filterIsInstance<ThinkingBlock>().firstOrNull()
        checkNotNull(thinkingBlock1)

        println("✓ First turn - thinking block: ${thinkingBlock1.thinking.take(50)}...")

        // Second request with previous thinking
        val response2 = client.messages.create {
            model = "claude-sonnet-4-5-20250929"
            maxTokens = 4096
            thinking = ThinkingConfigEnabled {
                budgetTokens = 2048
            }
            messages = listOf(
                Message {
                    +"What is 15 + 28?"
                },
                Message {
                    role = Role.ASSISTANT
                    content = response1.content
                },
                Message {
                    +"Now multiply that result by 3"
                }
            )
        }

        response2.content should {
            have(isNotEmpty())
        }

        val thinkingBlock2 = response2.content.filterIsInstance<ThinkingBlock>().firstOrNull()
        checkNotNull(thinkingBlock2)

        println("✓ Second turn - thinking block: ${thinkingBlock2.thinking.take(50)}...")
        println("✓ Multi-turn conversation with thinking successful")
    }

    @Test
    fun `should track thinking tokens in usage`() = runTest {
        val client = testAnthropic()

        val response = client.messages.create {
            model = "claude-sonnet-4-5"
            maxTokens = 4096
            thinking = ThinkingConfigEnabled {
                budgetTokens = 2048
            }
            messages = listOf(
                Message {
                    +"Explain the Pythagorean theorem step by step."
                }
            )
        }

        response.usage should {
            have(inputTokens > 0)
            have(outputTokens > 0)
        }

        println("✓ Input tokens: ${response.usage.inputTokens}")
        println("✓ Output tokens: ${response.usage.outputTokens}")
        println("✓ (Note: Thinking tokens are included in output_tokens)")
    }

    @Test
    fun `should work with different models supporting thinking`() = runTest {
        val client = testAnthropic()

        // Test with Claude Haiku 4.5
        val response = client.messages.create {
            model = "claude-haiku-4-5"
            maxTokens = 4096
            thinking = ThinkingConfigEnabled {
                budgetTokens = 1024
            }
            messages = listOf(
                Message {
                    +"What is 5 + 3?"
                }
            )
        }

        response should {
            have(model.startsWith("claude-haiku-4-5"))
            have(content.isNotEmpty())
        }

        val thinkingBlocks = response.content.filterIsInstance<ThinkingBlock>()
        thinkingBlocks should {
            have(isNotEmpty())
        }

        println("✓ Claude Haiku 4.5 with thinking works correctly")
    }

}
