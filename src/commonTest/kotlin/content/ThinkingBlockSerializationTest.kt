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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

class ThinkingBlockSerializationTest {

    @Test
    fun `should serialize ThinkingBlock`() {
        anthropicJson.encodeToString(
            serializer = Content.serializer(),
            value = ThinkingBlock {
                thinking = "Let me analyze this step by step..."
                signature = "WaUjzkypQ2mUEVM36O2TxuC06KN8xyfbJwyem2dw3URve/op91XWHOEBLLqIOMfFG/UvLEczmEsUjavL"
            }
        ) shouldEqualJson """
            {
              "type": "thinking",
              "thinking": "Let me analyze this step by step...",
              "signature": "WaUjzkypQ2mUEVM36O2TxuC06KN8xyfbJwyem2dw3URve/op91XWHOEBLLqIOMfFG/UvLEczmEsUjavL"
            }
        """
    }

    @Test
    fun `should serialize ThinkingBlock with cache control`() {
        anthropicJson.encodeToString(
            serializer = Content.serializer(),
            value = ThinkingBlock {
                thinking = "Reasoning process..."
                signature = "abc123"
                cacheControl = CacheControl.Ephemeral()
            }
        ) shouldEqualJson """
            {
              "type": "thinking",
              "thinking": "Reasoning process...",
              "signature": "abc123",
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """
    }

    @Test
    fun `should deserialize ThinkingBlock`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "thinking",
              "thinking": "Step-by-step reasoning...",
              "signature": "xyz789"
            }
            """
        ) should {
            be<ThinkingBlock>()
            have(thinking == "Step-by-step reasoning...")
            have(signature == "xyz789")
            have(cacheControl == null)
        }
    }

    @Test
    fun `should serialize ThinkingBlockParam`() {
        anthropicJson.encodeToString(
            serializer = Content.serializer(),
            value = ThinkingBlockParam {
                thinking = "This is a nice number theory question..."
                signature = "EuYBCkQYAiJAgCs1le6..."
            }
        ) shouldEqualJson """
            {
              "type": "thinking",
              "thinking": "This is a nice number theory question...",
              "signature": "EuYBCkQYAiJAgCs1le6..."
            }
        """
    }

    @Test
    fun `should deserialize ThinkingBlockParam`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "thinking",
              "thinking": "Previous thinking content...",
              "signature": "sig123"
            }
            """
        ) should {
            be<ThinkingBlock>()  // Deserializes as ThinkingBlock since same type
            have(thinking == "Previous thinking content...")
            have(signature == "sig123")
        }
    }

    @Test
    fun `should serialize RedactedThinkingBlock`() {
        anthropicJson.encodeToString(
            serializer = Content.serializer(),
            value = RedactedThinkingBlock {
                data = "encrypted_thinking_data_here"
            }
        ) shouldEqualJson """
            {
              "type": "redacted_thinking",
              "data": "encrypted_thinking_data_here"
            }
        """
    }

    @Test
    fun `should deserialize RedactedThinkingBlock`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "redacted_thinking",
              "data": "redacted_data_xyz"
            }
            """
        ) should {
            be<RedactedThinkingBlock>()
            have(data == "redacted_data_xyz")
            have(cacheControl == null)
        }
    }

    @Test
    fun `should support copy with cache control alteration for ThinkingBlock`() {
        val original = ThinkingBlock {
            thinking = "Original thinking"
            signature = "sig"
        }

        val modified = original.copy {
            cacheControl = CacheControl.Ephemeral()
        }

        modified should {
            be<ThinkingBlock>()
            have(thinking == "Original thinking")
            have(signature == "sig")
            have(cacheControl != null)
        }
    }

}
