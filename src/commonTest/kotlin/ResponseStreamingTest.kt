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

import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.addCacheBreakpoint
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.message.toMessageResponse
import com.xemantic.ai.anthropic.test.fetchSkillText
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.test.uniqueSuffix
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ResponseStreamingTest {

    @Test
    fun `should stream the response`() = runTest {
        // given
        val client = testAnthropic()

        // when
        val chunkedResponse = client.messages.stream {
            +"Say: 'The sun slowly dipped below the horizon, painting the sky in a breathtaking array of oranges, pinks, and purples.'"
        }
            .filterIsInstance<Event.ContentBlockDelta>()
            .filter { it.delta is Event.ContentBlockDelta.Delta.TextDelta }
            .map { (it.delta as Event.ContentBlockDelta.Delta.TextDelta).text }
            .toList()
            .joinToString(separator = "|")

        // then
        println("chunked response: $chunkedResponse")
        val response = chunkedResponse.replace("|", "")
        assert(response == "The sun slowly dipped below the horizon, painting the sky in a breathtaking array of oranges, pinks, and purples.")
    }

    @Test
    fun `should analyze a text big enough to activate caching`() = runTest {
        // given
        val skillText = fetchSkillText()
        val anthropic = testAnthropic()
        val conversation = mutableListOf<Message>()
        conversation += "How many times YAML is mentioned? (Answer format: `YAML: count`): ${skillText + uniqueSuffix()}"

        // when
        val response1 = anthropic.messages.stream {
            messages = conversation.addCacheBreakpoint()
        }.toMessageResponse()
        conversation += response1

        response1 should {
            have(text!!.contains("YAML", ignoreCase = true))
            usage should {
                have(cacheReadInputTokens!! == 0)
                have(cacheCreationInputTokens!! > 4096)
                cacheCreation should {
                    have(ephemeral5mInputTokens!! == cacheCreationInputTokens)
                    have(ephemeral1hInputTokens!! == 0)
                }
            }
        }

        conversation += "How many times YAML is mentioned there?"
        val response2 = anthropic.messages.stream {
            messages = conversation.addCacheBreakpoint()
        }.onEach {
            if (it is Event.ContentBlockDelta && it.delta is Event.ContentBlockDelta.Delta.TextDelta) {
                print(it.delta.text)
            }
        }.toMessageResponse()
        response2.usage should {
            have(cacheReadInputTokens!! > 4096)
            have(cacheCreationInputTokens!! > 0)
            cacheCreation should {
                have(ephemeral5mInputTokens!! == cacheCreationInputTokens)
                have(ephemeral1hInputTokens!! == 0)
            }
        }
    }

    @Test
    fun `should return error when error is expected`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val exception = assertFailsWith<AnthropicApiException> {
            anthropic.messages.stream {
                maxTokens = 1000000000
                +"Foo"
            }.toMessageResponse()
        }

        // then
        exception.error should {
            have(type == "invalid_request_error")
            have(message == "max_tokens: 1000000000 > 64000, which is the maximum allowed number of output tokens for claude-haiku-4-5-20251001")
        }
    }

}
