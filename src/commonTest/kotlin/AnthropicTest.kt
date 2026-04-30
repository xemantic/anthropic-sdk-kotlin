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
import com.xemantic.ai.anthropic.cost.Cost
import com.xemantic.ai.anthropic.cost.times
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.message.Role
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ZERO
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AnthropicTest {

    private object HttpClientConfigTestAbort : RuntimeException()

    @Test
    fun `should receive an introduction from Claude`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val response = anthropic.messages.create {
            +"Hello World! What's your name?"
        }

        // then
        response should {
            have(role == Role.ASSISTANT)
            have("claude" in model)
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("Claude" in text)
            }
            have(stopSequence == null)
            usage should {
                have(inputTokens == 15)
                have(outputTokens > 0)
            }
        }
    }

    @Test
    fun `should use topK topP and temperature`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val response = anthropic.messages.create {
            +"Hello World! What's your name?"
            model = Model.CLAUDE_SONNET_4_20250514.id // the latest sonnet does not allow to set up both
            topK = 40
            topP = 0.7
            temperature = 0.3
        }

        // then
        response should {
            have(role == Role.ASSISTANT)
            have("claude" in model)
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("Claude" in text)
            }
            have(stopSequence == null)
            usage should {
                have(inputTokens == 15)
                have(outputTokens > 0)
            }
        }
    }

    @Test
    fun `should receive Usage and allow Cost calculation from chosen model`() = runTest {
        // given
        val anthropic = testAnthropic()
        val haiku = Model.CLAUDE_HAIKU_4_5_20251001

        // when
        val response = anthropic.messages.create {
            +"Hello Claude! I am testing the amount of input and output tokens."
            model(haiku)
        }
        val cost = response.usage * haiku.cost

        // then
        response should {
            have(role == Role.ASSISTANT)
            have(model == haiku.id)
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            have(stopSequence == null)
            usage should {
                have(inputTokens == 21)
                have(outputTokens > 0)
                have(cacheCreationInputTokens == 0)
                have(cacheReadInputTokens == 0)
            }
        }
        cost should {
            have(inputTokens == Money("0.000021"))
            have(outputTokens >= Money.ZERO && outputTokens <= Money("0.0005"))
            have(cache5mCreationInputTokens == Money.ZERO)
            have(cache1hCreationInputTokens == Money.ZERO)
            have(cacheReadInputTokens == Money.ZERO)
        }
    }

    @Test
    fun `should use system prompt`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val response = anthropic.messages.create {
            system("This is an integration test of Anthropic API response. Whatever the person (caller) says, answer \"HAHAHA\"")
            +"Hello World! What's your name?"
            maxTokens = 1024
        }

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have(text == "HAHAHA")
            }
        }
    }

    @Test
    fun `should return error when error is expected`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val exception = assertFailsWith<AnthropicApiException> {
            anthropic.messages.create {
                maxTokens = 1000000000
                +"Foo"
            }
        }

        // then
        exception should {
            have(httpStatusCode == HttpStatusCode.BadRequest)
            error should {
                have(type == "invalid_request_error")
                have(message == "max_tokens: 1000000000 > 64000, which is the maximum allowed number of output tokens for claude-haiku-4-5-20251001")
            }
        }
    }

    @Test
    fun `should use external model definition`() = runTest {
        // given
        val anthropic = testAnthropic()
        val otherModel = Model(
            id = "claude-haiku-4-5-20251001",
            contextWindow = 200000,
            maxOutput = 64000,
            messageBatchesApi = true,
            cost = Cost {
                inputTokens = "1".dollarsPerMillion
                outputTokens = "5".dollarsPerMillion
            },
            cacheMinTokens = 4096
        )

        // when
        val response = anthropic.messages.create {
            model(otherModel)
            +"Hello World! What's your name?"
        }

        // then
        response should {
            have(role == Role.ASSISTANT)
            have(model == "claude-haiku-4-5-20251001")
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("Claude" in text)
            }
            have(stopSequence == null)
            usage should {
                have(inputTokens == 15)
                have(outputTokens > 0)
            }
        }
    }

    @Test
    fun `should invoke httpClientConfig when constructing Anthropic`() {
        // given
        var configInvoked = false

        // when
        Anthropic {
            apiKey = "test-key"
            httpClientConfig = {
                configInvoked = true
            }
        }

        // then
        assert(configInvoked)
    }

    @Test
    fun `should allow customizing HTTP request headers via httpClientConfig`() = runTest {
        // given
        val capturedHeaders = mutableMapOf<String, List<String>>()
        val captureAndAbort = createClientPlugin("CaptureAndAbort") {
            onRequest { request, _ ->
                request.headers.entries().forEach { (key, values) ->
                    capturedHeaders[key] = values
                }
                throw HttpClientConfigTestAbort
            }
        }
        val anthropic = Anthropic {
            apiKey = "test-api-key"
            httpClientConfig = {
                install(captureAndAbort)
                defaultRequest {
                    header("Authorization", "Bearer custom-token")
                    header("X-Custom-Header", "custom-value")
                }
            }
        }

        // when
        assertFailsWith<HttpClientConfigTestAbort> {
            anthropic.messages.create { +"Hi" }
        }

        // then
        capturedHeaders should {
            have(get("x-api-key") == listOf("test-api-key"))
            have(get("Authorization") == listOf("Bearer custom-token"))
            have(get("X-Custom-Header") == listOf("custom-value"))
        }
    }

}
