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
import com.xemantic.ai.anthropic.message.Role
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.env
import com.xemantic.ai.anthropic.thinking.ThinkingConfig
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.isBrowserPlatform
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MoonshotTest {

    @Test
    fun `should receive an introduction from Kimi`() = runTest {
        if (isBrowserPlatform) return@runTest // our test Moonshot API server depends on CORS
        // given
        val anthropic = Anthropic {
            baseUrl = env["MOONSHOT_API_BASE_URL"]
            defaultModel = env["MOONSHOT_DEFAULT_MODEL"]
            apiKey = env["MOONSHOT_API_KEY"]
            useXApiKeyHeader = false
            useAuthorizationBearerHeader = true
        }

        // when
        val response = anthropic.messages.create {
            +"Hello World! What's your name?"
            thinking = ThinkingConfig.Disabled // Our Kimi model deployment is returning thinking tokens by default
        }

        // then
        response should {
            have(role == Role.ASSISTANT)
            have("Kimi" in model)
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("Kimi" in text)
            }
            have(stopSequence == null)
            usage should {
                have(inputTokens > 0)
                have(outputTokens > 0)
            }
        }
    }

}
