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

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test

class SystemPromptCacheControlTest {

    @Test
    @Ignore
    fun `should cache system prompt across conversation`() = runTest {
        // given
        val client = Anthropic()
        val conversation = mutableListOf<Message>()
        val systemPrompt = System(
            text = "This system prompt should be cached.",
            cacheControl = CacheControl.Ephemeral()
        )

        conversation += Message {
            +"Hi claude, I will ask a question soon."
        }

        // when
        val response1 = client.messages.create {
            system = listOf(systemPrompt)
            messages = conversation
        }
        conversation += response1

        // then
        response1 should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
            }
            usage should {
                // it might have been already cached by the previous test run
                have(cacheCreationInputTokens!! > 0 || cacheReadInputTokens!! > 0)
            }
        }

        // given
        conversation += Message {
            +"Did you cache my system prompt?"
        }

        // when
        val response2 = client.messages.create {
            system = listOf(systemPrompt)
            messages = conversation
        }

        // then
        response2 should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
            }
            usage should {
                have(cacheReadInputTokens!! > 0)
                have(cacheCreationInputTokens == 0)
            }
        }

    }

}