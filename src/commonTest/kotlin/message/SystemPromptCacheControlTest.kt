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

package com.xemantic.ai.anthropic.message

import com.xemantic.ai.anthropic.Model
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.test.fetchSkillText
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.test.uniqueSuffix
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SystemPromptCacheControlTest {

    @Test
    fun `should cache system prompt across conversation with 5m ttl`() = runTest {
        // given
        val prompt = fetchSkillText() + uniqueSuffix()
        val ttl = CacheControl.Ephemeral.TTL.FIVE_MINUTES
        val anthropic = testAnthropic {
            // system prompt caching is unavailable on Haiku 4.5
            defaultModel = Model.CLAUDE_SONNET_4_20250514
        }
        val conversation = mutableListOf<Message>()

        val systemPrompt = System(
            text = prompt,
            cacheControl = CacheControl.Ephemeral {
                this.ttl = ttl
            }
        )

        conversation += "Hi claude, I will ask a question soon."

        // when
        val response1 = anthropic.messages.create {
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
                // Should create cache on first request
                have(cacheCreationInputTokens!! > 0)
                cacheCreation should {
                    have(ephemeral5mInputTokens == cacheCreationInputTokens)
                }
                have(cacheReadInputTokens == 0)
            }
        }

        // given
        conversation += "Did you cache my system prompt?"

        // when
        val response2 = anthropic.messages.create {
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
                have(cacheReadInputTokens == response1.usage.cacheCreationInputTokens)
                have(cacheCreationInputTokens == 0)
            }
        }
    }

    @Test
    fun `should cache system prompt across conversation with 1h ttl`() = runTest {
        // given
        val prompt = fetchSkillText() + uniqueSuffix()
        val ttl = CacheControl.Ephemeral.TTL.ONE_HOUR
        val anthropic = testAnthropic {
            // system prompt caching is unavailable on Haiku 4.5
            defaultModel = Model.CLAUDE_SONNET_4_20250514
        }
        val conversation = mutableListOf<Message>()

        val systemPrompt = System(
            text = prompt,
            cacheControl = CacheControl.Ephemeral {
                this.ttl = ttl
            }
        )

        conversation += "Hi claude, I will ask a question soon."

        // when
        val response1 = anthropic.messages.create {
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
                // Should create cache on first request
                have(cacheCreationInputTokens!! > 0)
                cacheCreation should {
                    have(ephemeral1hInputTokens == cacheCreationInputTokens)
                }
                have(cacheReadInputTokens == 0)
            }
        }

        // given
        conversation += "Did you cache my system prompt?"

        // when
        val response2 = anthropic.messages.create {
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
                have(cacheReadInputTokens == response1.usage.cacheCreationInputTokens)
                have(cacheCreationInputTokens == 0)
            }
        }
    }

}
