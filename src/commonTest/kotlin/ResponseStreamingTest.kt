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

package com.xemantic.ai.anthropic

import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.addCacheBreakpoint
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.message.toMessageResponse
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

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
        val text = fetchText("https://raw.githubusercontent.com/xemantic/anthropic-sdk-kotlin/refs/heads/main/README.md")
        val anthropic = testAnthropic()
        val conversation = mutableListOf<Message>()

        conversation += Message {
            +Text(text)
            +"Analyze this text"
        }
        val response1 = anthropic.messages.stream {
            messages = conversation.addCacheBreakpoint()
        }.onEach {
            if (it is Event.ContentBlockDelta && it.delta is Event.ContentBlockDelta.Delta.TextDelta) {
                print(it.delta.text)
            }
        }.toMessageResponse()
        conversation += response1

        response1 should {
            have(text.contains("anthropic", ignoreCase = true))
            if (usage.cacheReadInputTokens != null && usage.cacheCreationInputTokens != null) {
                have(usage.cacheReadInputTokens >= 0)
                have( usage.cacheCreationInputTokens >= 0)
            } else {
                fail("Usage: cacheReadInputTokens and cacheCreationInputTokens must be provided")
            }
        }

        conversation += "How many times anthropic is mentioned in the README?"
        val response2 = anthropic.messages.stream {
            messages = conversation.addCacheBreakpoint()
        }.onEach {
            if (it is Event.ContentBlockDelta && it.delta is Event.ContentBlockDelta.Delta.TextDelta) {
                print(it.delta.text)
            }
        }.toMessageResponse()
        response2 should {
            if (usage.cacheReadInputTokens != null && usage.cacheCreationInputTokens != null) {
                have(usage.cacheReadInputTokens > 0)
                have( usage.cacheCreationInputTokens > 0)
            } else {
                fail("Usage: cacheReadInputTokens and cacheCreationInputTokens must be provided")
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
            have(message == "max_tokens: 1000000000 > 64000, which is the maximum allowed number of output tokens for claude-sonnet-4-5-20250929")
        }
    }

}

suspend fun fetchText(
    url: String
): String = HttpClient().run {
    get(url).bodyAsText()
}
