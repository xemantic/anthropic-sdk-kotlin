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
import com.xemantic.ai.anthropic.json.prettyAnthropicJson
import com.xemantic.ai.anthropic.tool.WebSearch
import com.xemantic.kotlin.test.*
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WebSearchServerToolUseTest {

    @Test
    fun `should create WebSearchServerToolUse`() {
        WebSearchServerToolUse {
            id = "toolu_123"
            input = WebSearch.Input(query = "kotlin multiplatform")
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
        val toolUse = WebSearchServerToolUse {
            id = "toolu_123"
            input = WebSearch.Input(query = "kotlin multiplatform")
        }

        toolUse.toString() sameAs /* language=json */ """
            {
              "type": "server_tool_use",
              "id": "toolu_123",
              "name": "web_search",
              "input": {
                "query": "kotlin multiplatform"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize WebSearchServerToolUse as Content`() {
        val toolUse = WebSearchServerToolUse {
            id = "toolu_456"
            input = WebSearch.Input(query = "anthropic API")
        }

        anthropicJson.encodeToString<Content>(toolUse) sameAsJson """
            {
              "type": "server_tool_use",
              "id": "toolu_456",
              "name": "web_search",
              "input": {
                "query": "anthropic API"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize WebSearchServerToolUse as Content type`() {
        val serverToolUse = WebSearchServerToolUse {
            id = "toolu_789"
            input = WebSearch.Input(query = "Claude AI features")
        }

        prettyAnthropicJson.encodeToString<Content>(
            serverToolUse
        ) sameAs /* language=json */ """
            {
              "type": "server_tool_use",
              "id": "toolu_789",
              "name": "web_search",
              "input": {
                "query": "Claude AI features"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize WebSearchServerToolUse with cache control as Content`() {
        val toolUse = WebSearchServerToolUse {
            id = "toolu_789"
            input = WebSearch.Input(query = "kotlin serialization")
            cacheControl = CacheControl.Ephemeral()
        }

        anthropicJson.encodeToString<Content>(toolUse) sameAsJson """
            {
              "type": "server_tool_use",
              "id": "toolu_789",
              "name": "web_search",
              "input": {
                "query": "kotlin serialization"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize list of Content including WebSearchServerToolUse`() {
        val contentList  = listOf(
            WebSearchServerToolUse {
                id = "toolu_abc"
                input = WebSearch.Input(query = "Anthropic SDK")
            }
        )

        prettyAnthropicJson.encodeToString<List<Content>>(
            contentList
        ) sameAs """
            [
              {
                "type": "server_tool_use",
                "id": "toolu_abc",
                "name": "web_search",
                "input": {
                  "query": "Anthropic SDK"
                }
              }
            ]
        """.trimIndent()
    }

    @Test
    fun `should deserialize WebSearchServerToolUse as Content`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "server_tool_use",
              "id": "toolu_999",
              "name": "web_search",
              "input": {
                "query": "ktor client"
              }
            }
            """
        ) should {
            be<WebSearchServerToolUse>()
            have(id == "toolu_999")
            have(name == "web_search")
            input should {
                have(query == "ktor client")
            }
            have(cacheControl == null)
        }
    }

    @Test
    fun `should deserialize WebSearch tool Input`() {
        anthropicJson.decodeFromString<WebSearch.Input>(
            """
            {
              "query": "kotlin multiplatform tutorial"
            }
            """
        ) should {
            have(query == "kotlin multiplatform tutorial")
        }
    }

    @Test
    fun `should serialize WebSearch tool Input`() {
        prettyAnthropicJson.encodeToString(
            WebSearch.Input(
                query = "anthropic API documentation"
            )
        ) sameAs /* language=json */ """
            {
              "query": "anthropic API documentation"
            }
        """.trimIndent()
    }

    @Test
    fun `should return string representation of WebSearchServerToolUse`() {
        WebSearchServerToolUse {
            id = "toolu_101"
            input = WebSearch.Input(query = "test query")
        }.toString() sameAsJson """
            {
              "type": "server_tool_use",
              "id": "toolu_101",
              "name": "web_search",
              "input": {
                "query": "test query"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should copy WebSearchServerToolUse`() {
        WebSearchServerToolUse {
            id = "toolu_202"
            input = WebSearch.Input(query = "original query")
            cacheControl = CacheControl.Ephemeral()
        }.copy() should {
            have(id == "toolu_202")
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
            input = WebSearch.Input(query = "old query")
            cacheControl = CacheControl.Ephemeral()
        }.copy {
            id = "toolu_404"
            input = WebSearch.Input(query = "new query")
            cacheControl = null
        } should {
            have(id == "toolu_404")
            have(name == "web_search")
            input should {
                have(query == "new query")
            }
            have(cacheControl == null)
        }
    }

}
