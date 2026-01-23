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

import com.xemantic.ai.anthropic.content.ThinkingBlock
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.stream
import com.xemantic.ai.anthropic.message.toMessageResponse
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ThinkingStreamingIntegrationTest {

    @Test
    fun `should stream message with thinking deltas`() = runTest {
        val client = testAnthropic()

        var thinkingStarted = false
        var thinkingDeltaReceived = false
        var signatureDeltaReceived = false
        var textReceived = false

        client.messages.stream(
            model = "claude-sonnet-4-5",
            maxTokens = 4096,
            thinking = ThinkingConfigEnabled {
                budgetTokens = 2048
            },
            messages = listOf(
                Message {
                    +"What is 123 + 456? Show your work."
                }
            )
        ).collect { event ->
            when (event) {
                is Event.ContentBlockStart -> {
                    when (event.contentBlock) {
                        is Event.ContentBlockStart.ContentBlock.Thinking -> {
                            thinkingStarted = true
                            println("✓ Thinking block started")
                        }
                        is Event.ContentBlockStart.ContentBlock.Text -> {
                            println("✓ Text block started")
                        }
                        else -> {}
                    }
                }
                is Event.ContentBlockDelta -> {
                    when (event.delta) {
                        is Event.ContentBlockDelta.Delta.ThinkingDelta -> {
                            thinkingDeltaReceived = true
                            print(event.delta.thinking)
                        }
                        is Event.ContentBlockDelta.Delta.SignatureDelta -> {
                            signatureDeltaReceived = true
                            println("\n✓ Signature delta received")
                        }
                        is Event.ContentBlockDelta.Delta.TextDelta -> {
                            textReceived = true
                            print(event.delta.text)
                        }
                        else -> {}
                    }
                }
                is Event.MessageStop -> {
                    println("\n✓ Message streaming complete")
                }
                else -> {}
            }
        }

        thinkingStarted should { have(this == true) }
        thinkingDeltaReceived should { have(this == true) }
        signatureDeltaReceived should { have(this == true) }
        textReceived should { have(this == true) }

        println("\n✓ All streaming events received correctly")
    }

    @Test
    fun `should build complete ThinkingBlock from streaming deltas`() = runTest {
        val client = testAnthropic()

        // Convert streaming events to message response
        val response = client.messages.stream(
            model = "claude-sonnet-4-5",
            maxTokens = 4096,
            thinking = ThinkingConfigEnabled {
                budgetTokens = 2048
            },
            messages = listOf(
                Message {
                    +"Calculate 7 * 8"
                }
            )
        ).toMessageResponse()

        response.content should {
            have(isNotEmpty())
        }

        val thinkingBlocks = response.content.filterIsInstance<ThinkingBlock>()
        thinkingBlocks should {
            have(isNotEmpty())
        }

        thinkingBlocks.first() should {
            have(thinking.isNotBlank())
            have(signature.isNotBlank())
        }

        println("✓ Complete ThinkingBlock built from streaming:")
        println("  - Thinking length: ${thinkingBlocks.first().thinking.length} chars")
        println("  - Signature length: ${thinkingBlocks.first().signature.length} chars")
    }

    @Test
    fun `should handle multiple thinking blocks in stream`() = runTest {
        val client = testAnthropic()

        var thinkingBlockCount = 0
        var textBlockCount = 0

        client.messages.stream(
            model = "claude-sonnet-4-5",
            maxTokens = 8192,
            thinking = ThinkingConfigEnabled {
                budgetTokens = 4096
            },
            messages = listOf(
                Message {
                    +"Solve this: What is (15 + 28) * 3? Explain each step."
                }
            )
        ).collect { event ->
            when (event) {
                is Event.ContentBlockStart -> {
                    when (event.contentBlock) {
                        is Event.ContentBlockStart.ContentBlock.Thinking -> {
                            thinkingBlockCount++
                        }
                        is Event.ContentBlockStart.ContentBlock.Text -> {
                            textBlockCount++
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }

        println("✓ Thinking blocks received: $thinkingBlockCount")
        println("✓ Text blocks received: $textBlockCount")

        thinkingBlockCount should {
            have(this > 0)
        }
    }

    @Test
    fun `should stream thinking with large budget`() = runTest {
        val client = testAnthropic()

        var totalThinkingChars = 0
        var deltaCount = 0

        client.messages.stream(
            model = "claude-sonnet-4-5",
            maxTokens = 16000,
            thinking = ThinkingConfigEnabled {
                budgetTokens = 10000
            },
            messages = listOf(
                Message {
                    +"Explain the concept of recursion in programming with detailed examples."
                }
            )
        ).collect { event ->
            when (event) {
                is Event.ContentBlockDelta -> {
                    when (event.delta) {
                        is Event.ContentBlockDelta.Delta.ThinkingDelta -> {
                            totalThinkingChars += event.delta.thinking.length
                            deltaCount++
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }

        println("✓ Large thinking budget test:")
        println("  - Total thinking chars: $totalThinkingChars")
        println("  - Delta events: $deltaCount")

        totalThinkingChars should {
            have(this > 0)
        }
    }

}
