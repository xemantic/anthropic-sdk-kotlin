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
import com.xemantic.ai.anthropic.content.RedactedThinkingBlock
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ThinkingBlock
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.event.Event.ContentBlockStart
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.message.toMessageResponse
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ThinkingStreamingTest {

    @Test
    fun `should stream message with thinking deltas`() = runTest {
        // given
        val client = testAnthropic()
        var thinkingStarted = false
        var thinkingDeltaReceived = false
        var signatureDeltaReceived = false
        var textReceived = false

        // when
        client.messages.stream {
            thinking = ThinkingConfig.Enabled {
                budgetTokens = 1024
            }
            +"What is 123 + 456? Show your work."
        }.collect { event ->
            when (event) {
                is ContentBlockStart -> {
                    when (event.contentBlock) {
                        is Thinking -> {
                            thinkingStarted = true
                        }
                        else -> {}
                    }
                }
                is ContentBlockDelta -> {
                    when (event.delta) {
                        is ThinkingDelta -> {
                            thinkingDeltaReceived = true
                        }
                        is SignatureDelta -> {
                            signatureDeltaReceived = true
                        }
                        is TextDelta -> {
                            textReceived = true
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }

        // then
        assert(thinkingStarted)
        assert(thinkingDeltaReceived)
        assert(signatureDeltaReceived)
        assert(textReceived)
    }

    @Test
    fun `should build complete ThinkingBlock from streaming deltas`() = runTest {
        // given
        val client = testAnthropic()

        // when
        val response = client.messages.stream {
            thinking = ThinkingConfig.Enabled {
                budgetTokens = 1024
            }
            +"Calculate 7 * 8"
        }.toMessageResponse()

        // then
        response.content should {
            have(any { it is ThinkingBlock })
            filterIsInstance<ThinkingBlock>().first() should {
                have(thinking.isNotBlank())
                have(signature!!.isNotBlank())
            }
            have(any { it is Text })
        }
    }

    @Test
    fun `should build complete ThinkingBlock from streaming with adaptive thinking`() = runTest {
        // given
        val client = testAnthropic()

        // when
        val response = client.messages.stream {
            model(Model.CLAUDE_SONNET_4_6)
            thinking = ThinkingConfig.Adaptive()
            +"Calculate 7 * 8"
        }.toMessageResponse()

        // then
        response.content should {
            filterIsInstance<ThinkingBlock>().first() should {
                have(signature!!.isNotBlank())
            }
            have(any { it is Text })
        }
    }

    @Test
    fun `should build ThinkingBlock with blank thinking when streaming with omitted display`() = runTest {
        // given
        val client = testAnthropic()

        // when
        val response = client.messages.stream {
            thinking = ThinkingConfig.Enabled {
                budgetTokens = 1024
                display = ThinkingConfig.Display.OMITTED
            }
            +"Calculate 7 * 8"
        }.toMessageResponse()

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
    fun `should build RedactedThinkingBlock from streaming events`() = runTest {
        // given
        // Synthetic event sequence rather than a live API call: the documented
        // magic string that used to trigger redacted_thinking server-side is no
        // longer reliable, so we exercise toMessageResponse() directly.
        val events = listOf(
            """
                {
                  "type": "message_start",
                  "message": {
                    "type": "message",
                    "id": "msg_test",
                    "model": "claude-haiku-4-5",
                    "role": "assistant",
                    "content": [],
                    "stop_reason": null,
                    "stop_sequence": null,
                    "usage": {"input_tokens": 10, "output_tokens": 1}
                  }
                }
            """,
            """
                {
                  "type": "content_block_start",
                  "index": 0,
                  "content_block": {
                    "type": "redacted_thinking",
                    "data": "encrypted_payload"
                  }
                }
            """,
            """{"type": "content_block_stop", "index": 0}""",
            """
                {
                  "type": "content_block_start",
                  "index": 1,
                  "content_block": {"type": "text", "text": ""}
                }
            """,
            """
                {
                  "type": "content_block_delta",
                  "index": 1,
                  "delta": {"type": "text_delta", "text": "Hello"}
                }
            """,
            """{"type": "content_block_stop", "index": 1}""",
            """
                {
                  "type": "message_delta",
                  "delta": {"stop_reason": "end_turn", "stop_sequence": null},
                  "usage": {"output_tokens": 5}
                }
            """,
            """{"type": "message_stop"}"""
        ).map { anthropicJson.decodeFromString<Event>(it) }

        // when
        val response = events.asFlow().toMessageResponse()

        // then
        response.content should {
            filterIsInstance<RedactedThinkingBlock>().first() should {
                have(data == "encrypted_payload")
            }
            filterIsInstance<Text>().first() should {
                have(text == "Hello")
            }
        }
    }

}
