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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.test.testDataDir
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.isBrowserPlatform
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.test.Test

class DocumentCacheControlTest {

    @Test
    fun `should cache PDF document across conversation`() = runTest {
        if (isBrowserPlatform) return@runTest // we cannot access files in the browser
        // given
        val anthropic = testAnthropic()
        val conversation = mutableListOf<Message>()
        conversation += Message {
            +Document(Path(testDataDir, "test.pdf")) {
                cacheControl = CacheControl.Ephemeral()
            }
            +"What's on the first page of the document?"
        }

        // when
        val response1 = anthropic.messages.create {
            messages = conversation
        }
        conversation += response1

        // then
        response1 should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("FOO" in text.uppercase())
            }
            usage should {
                // it might have been already cached by the previous test run
                have(
                    (cacheCreationInputTokens!! > 4096
                            && cacheReadInputTokens!! == 0
                            && cacheCreation!!.ephemeral5mInputTokens == cacheCreationInputTokens)
                            // if we run the test again before 5m passed
                            || (
                            cacheCreationInputTokens == 0
                                    && cacheReadInputTokens!! > 4096
                                    && cacheCreation!!.ephemeral5mInputTokens == 0)
                )
            }
        }

        // given
        conversation += Message {
            +"What's on the second page of the document?"
        }

        // when
        val response2 = anthropic.messages.create {
            messages = conversation
        }

        // then
        response2 should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("BAR" in text.uppercase())
            }
            usage should {
                have(cacheReadInputTokens!! > 0)
                have(cacheCreationInputTokens == 0)
            }
        }

    }

}
