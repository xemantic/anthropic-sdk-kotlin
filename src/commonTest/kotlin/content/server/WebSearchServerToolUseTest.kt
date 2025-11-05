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
import com.xemantic.ai.anthropic.content.WebSearchServerToolUse
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.tool.WebSearch
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WebSearchServerToolUseTest {

    @Test
    fun `should create WebSearchServerToolUse`() {
        WebSearchServerToolUse {
            id = "toolu_123"
            input = WebSearch.Input(
                query = "kotlin multiplatform"
            )
        } should {
            be<WebSearchServerToolUse>()
            have(id == "toolu_123")
            have(name == "web_search")
            input should {
                have(query == "kotlin multiplatform")
            }
            have(cacheControl == null)
        }
    }

    @Test
    fun `should fail to create empty WebSearchServerToolUse`() {
        assertFailsWith<IllegalArgumentException> {
            WebSearchServerToolUse {}
        } should {
            have(message == "id cannot be null")
        }
    }

    @Test
    fun `should return JSON for WebSearchServerToolUse toString`() {
        WebSearchServerToolUse {
            id = "toolu_456"
            input = WebSearch.Input(
                query = "kotlin multiplatform"
            )
            cacheControl = CacheControl.Ephemeral()
        }.toString() sameAsJson """
            {
              "type": "server_tool_use",
              "id": "toolu_456",
              "name": "web_search",
              "input": {
                "query": "kotlin multiplatform"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize WebSearchServerToolUse as Content`() {
        // given
        val toolUse = WebSearchServerToolUse {
            id = "toolu_456"
            input = WebSearch.Input(
                query = "anthropic API"
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
              "id": "toolu_456",
              "name": "web_search",
              "input": {
                "query": "anthropic API"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize WebSearchServerToolUse as Content`() {
        // given
        val json = """
            {
              "type": "server_tool_use",
              "id": "toolu_abc",
              "name": "web_search",
              "input": {
                "query": "kotlin multiplatform"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()

        // when
        val toolUse = anthropicJson.decodeFromString<Content>(json)

        // then
        toolUse should {
            be<WebSearchServerToolUse>()
            have(id == "toolu_abc")
            have(name == "web_search")
            input should {
                have(query == "kotlin multiplatform")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy WebSearchServerToolUse`() {
        WebSearchServerToolUse {
            id = "toolu_abc"
            input = WebSearch.Input(
                query = "original query"
            )
            cacheControl = CacheControl.Ephemeral()
        }.copy() should {
            have(id == "toolu_abc")
            have(name == "web_search")
            input should {
                have(query == "original query")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy WebSearchServerToolUse while altering properties`() {
        WebSearchServerToolUse {
            id = "toolu_303"
            input = WebSearch.Input(
                query = "old query"
            )
            cacheControl = null
        }.copy {
            id = "toolu_404"
            input = WebSearch.Input(
                query = "new query"
            )
            cacheControl = CacheControl.Ephemeral()
        } should {
            have(id == "toolu_404")
            have(name == "web_search")
            input should {
                have(query == "new query")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

}
