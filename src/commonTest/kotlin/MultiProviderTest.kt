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

import com.xemantic.ai.anthropic.cost.Cost
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.message.Role
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MultiProviderTest {

    @Test
    fun `should use Anthropic model with ANTHROPIC_API_KEY`() = runTest {
        val anthropicKey = getEnvApiKey("anthropic")
        if (anthropicKey == null) {
            println("⊘ Skipping: ANTHROPIC_API_KEY not set")
            return@runTest
        }

        val client = Anthropic()

        val response = client.messages.create {
            model = Model.CLAUDE_SONNET_4_5_20250929.id
            maxTokens = 50
            +"Say hello"
        }

        response should {
            have(role == Role.ASSISTANT)
            have("claude" in model)
            have(stopReason == StopReason.END_TURN)
        }
    }

    @Test
    fun `should use Moonshot model with MOONSHOT_API_KEY`() = runTest {
        val moonshotKey = getEnvApiKey("moonshot")
        if (moonshotKey == null) {
            println("⊘ Skipping: MOONSHOT_API_KEY not set")
            return@runTest
        }

        val client = Anthropic()

        val response = client.messages.create {
            model = Model.KIMI_K2_0905_PREVIEW.id
            maxTokens = 50
            +"Say hello"
        }

        response should {
            have(role == Role.ASSISTANT)
            have("kimi" in model)
            have(stopReason == StopReason.END_TURN)
        }
    }

    @Test
    fun `should switch between Anthropic and Moonshot models`() = runTest {
        val anthropicKey = getEnvApiKey("anthropic")
        val moonshotKey = getEnvApiKey("moonshot")

        if (anthropicKey == null || moonshotKey == null) {
            println("⊘ Skipping: Requires both ANTHROPIC_API_KEY and MOONSHOT_API_KEY")
            return@runTest
        }

        val client = Anthropic()

        // Use Anthropic model
        val anthropicResponse = client.messages.create {
            model = Model.CLAUDE_SONNET_4_5_20250929.id
            maxTokens = 50
            +"Say 'Hello from Anthropic'"
        }

        anthropicResponse should {
            have("claude" in model)
        }

        // Use Moonshot model
        val moonshotResponse = client.messages.create {
            model = Model.KIMI_K2_0905_PREVIEW.id
            maxTokens = 50
            +"Say 'Hello from Moonshot'"
        }

        moonshotResponse should {
            have("kimi" in model)
        }
    }

    @Test
    fun `should use explicit apiKey and fail with auth error when key is invalid`() = runTest {
        val client = Anthropic {
            apiKey = "sk-ant-fake-key-12345"  // Fake API key
        }

        val exception = assertFailsWith<AnthropicApiException> {
            client.messages.create {
                model = Model.CLAUDE_SONNET_4_5_20250929.id
                maxTokens = 50
                +"Say hello"
            }
        }

        // Should be 401 Unauthorized, proving our fake key was used
        exception.httpStatusCode should {
            have(this == HttpStatusCode.Unauthorized)
        }
    }

    @Test
    fun `should use explicit apiBase and fail when endpoint is wrong`() = runTest {
        val apiKey = getEnvApiKey("anthropic")
        if (apiKey == null) {
            println("⊘ Skipping: ANTHROPIC_API_KEY not set")
            return@runTest
        }

        val client = Anthropic {
            this.apiKey = apiKey
            this.apiBase = "https://wrong-endpoint.example.com/"
        }

        // Should fail because wrong endpoint is used
        assertFailsWith<Exception> {
            client.messages.create {
                model = Model.CLAUDE_SONNET_4_5_20250929.id
                maxTokens = 50
                +"Say hello"
            }
        }
    }

    @Test
    fun `should fail when custom provider API key is not available`() = runTest {
        val customProvider = UnknownApiProvider(
            id = "nonexistent-provider",
            apiBase = "https://api.example.com/",
            authHeaderType = AuthHeaderType.BEARER_TOKEN
        )

        val customModel = UnknownModel(
            id = "custom-model",
            apiProvider = customProvider,
            contextWindow = 100000,
            maxOutput = 4096,
            messageBatchesApi = true,
            cost = Cost {
                inputTokens = "3".dollarsPerMillion
                outputTokens = "15".dollarsPerMillion
            },
        )

        val client = Anthropic {
            customApiProviders = listOf(customProvider)
            modelMap["custom-model"] = customModel
        }

        val exception = assertFailsWith<IllegalStateException> {
            client.messages.create {
                model = "custom-model"
                maxTokens = 50
                +"Say hello"
            }
        }

        exception.message!! should {
            have(contains("API key not found for provider 'nonexistent-provider'"))
            have(contains("NONEXISTENT_PROVIDER_API_KEY"))
        }
    }
}
