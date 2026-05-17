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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class ThinkingBlockSerializationTest {

    @Test
    fun `should serialize ThinkingBlock`() {
        anthropicJson.encodeToString<Content>(
            ThinkingBlock {
                thinking = "Let me analyze this step by step..."
                signature = "WaUjzkypQ2mUEVM36O2TxuC06KN8xyfbJwyem2dw3URve/op91XWHOEBLLqIOMfFG/UvLEczmEsUjavL"
            }
        ) sameAsJson """
            {
              "type": "thinking",
              "thinking": "Let me analyze this step by step...",
              "signature": "WaUjzkypQ2mUEVM36O2TxuC06KN8xyfbJwyem2dw3URve/op91XWHOEBLLqIOMfFG/UvLEczmEsUjavL"
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize ThinkingBlock with cache control`() {
        anthropicJson.encodeToString<Content>(
            ThinkingBlock {
                thinking = "Reasoning process..."
                signature = "abc123"
                cacheControl = CacheControl.Ephemeral()
            }
        ) sameAsJson """
            {
              "type": "thinking",
              "thinking": "Reasoning process...",
              "signature": "abc123",
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
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
    fun `should deserialize ThinkingBlock with cache control`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "thinking",
              "thinking": "Step-by-step reasoning...",
              "signature": "xyz789",
              "cache_control": {
                "type": "ephemeral"
              }
            }
            """
        ) should {
            be<ThinkingBlock>()
            have(thinking == "Step-by-step reasoning...")
            have(signature == "xyz789")
            have(cacheControl is CacheControl.Ephemeral)
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
