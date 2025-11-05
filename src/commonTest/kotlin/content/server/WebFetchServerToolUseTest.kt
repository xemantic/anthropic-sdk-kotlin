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

package com.xemantic.ai.anthropic.content.server

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.content.Content
import com.xemantic.ai.anthropic.content.WebFetchServerToolUse
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.tool.WebFetch
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WebFetchServerToolUseTest {

    @Test
    fun `should create WebFetchServerToolUse`() {
        WebFetchServerToolUse {
            id = "tool_123"
            input = WebFetch.Input(url = "https://example.com")
            cacheControl = CacheControl.Ephemeral()
        } should {
            be<WebFetchServerToolUse>()
            have(id == "tool_123")
            have(name == "web_fetch")
            input should {
                have(url == "https://example.com")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should fail to create empty WebFetchServerToolUse`() {
        assertFailsWith<IllegalArgumentException> {
            WebFetchServerToolUse {}
        } should {
            have(message == "id cannot be null")
        }
    }

    @Test
    fun `should return JSON for WebFetchServerToolUse toString`() {
        WebFetchServerToolUse {
            id = "tool_456"
            input = WebFetch.Input(
                url = "https://www.anthropic.com"
            )
            cacheControl = CacheControl.Ephemeral()
        }.toString() sameAsJson """
            {
              "type": "server_tool_use",
              "id": "tool_456",
              "name": "web_fetch",
              "input": {
                "url": "https://www.anthropic.com"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize WebFetchServerToolUse as Content`() {
        // given
        val toolUse = WebFetchServerToolUse {
            id = "tool_456"
            input = WebFetch.Input(
                url = "https://www.anthropic.com"
            )
            cacheControl = CacheControl.Ephemeral()
        }

        // when
        val json = anthropicJson.encodeToString<Content>(
            toolUse
        )

        // then
        json sameAsJson """
            {
              "type": "server_tool_use",
              "id": "tool_456",
              "name": "web_fetch",
              "input": {
                "url": "https://www.anthropic.com"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize WebFetchServerToolUse as Content`() {
        // given
        val json = """
            {
              "type": "server_tool_use",
              "id": "tool_abc",
              "name": "web_fetch",
              "input": {
                "url": "https://www.anthropic.com"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()

        // when
        val tooUse = anthropicJson.decodeFromString<Content>(json)

        // then
        tooUse should {
            be<WebFetchServerToolUse>()
            have(id == "tool_abc")
            have(name == "web_fetch")
            input should {
                have(url == "https://www.anthropic.com")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy WebFetchServerToolUse`() {
        WebFetchServerToolUse {
            id = "tool_abc"
            input = WebFetch.Input(url = "https://original.com")
            cacheControl = CacheControl.Ephemeral()
        }.copy() should {
            have(id == "tool_abc")
            have(name == "web_fetch")
            input should {
                have(url == "https://original.com")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy WebFetchServerToolUse while altering properties`() {
        WebFetchServerToolUse {
            id = "tool_303"
            input = WebFetch.Input(url = "https://old.com")
            cacheControl = null
        }.copy {
            id = "tool_404"
            input = WebFetch.Input(url = "https://new.com")
            cacheControl = CacheControl.Ephemeral()
        } should {
            have(id == "tool_404")
            have(name == "web_fetch")
            input should {
                have(url == "https://new.com")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

}
