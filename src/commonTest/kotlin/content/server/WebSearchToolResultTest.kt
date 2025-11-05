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
import com.xemantic.ai.anthropic.content.WebSearchToolResult
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class WebSearchToolResultTest {

    @Test
    fun `should deserialize WebSearchToolResult with results content as Content`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "web_search_tool_result",
              "tool_use_id": "toolu_result_1",
              "content": [
                {
                  "type": "web_search_result",
                  "title": "Kotlin Multiplatform",
                  "url": "https://kotlinlang.org/docs/multiplatform.html",
                  "encrypted_content": "encrypted123",
                  "page_age": "2024-01-15"
                },
                {
                  "type": "web_search_result",
                  "title": "Kotlin Docs",
                  "url": "https://kotlinlang.org/docs/home.html",
                  "encrypted_content": "encrypted456"
                }
              ],
              "cache_control": {
                "type": "ephemeral"
              }
            }
            """
        ) should {
            be<WebSearchToolResult>()
            have(toolUseId == "toolu_result_1")
            content should {
                be<WebSearchToolResult.Results>()
                have(results.size == 2)
                results[0] should {
                    have(title == "Kotlin Multiplatform")
                    have(url == "https://kotlinlang.org/docs/multiplatform.html")
                    have(encryptedContent == "encrypted123")
                    have(pageAge == "2024-01-15")
                }
                results[1] should {
                    have(title == "Kotlin Docs")
                    have(url == "https://kotlinlang.org/docs/home.html")
                    have(encryptedContent == "encrypted456")
                    have(pageAge == null)
                }
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should deserialize WebSearchToolResult with results content and serialize the exact JSON`() {
        // given
        val json = """
            {
              "type": "web_search_tool_result",
              "tool_use_id": "toolu_result_1",
              "content": [
                {
                  "title": "Kotlin Multiplatform",
                  "url": "https://kotlinlang.org/docs/multiplatform.html",
                  "encrypted_content": "encrypted123",
                  "page_age": "2024-01-15"
                }
              ]
            }
        """.trimIndent()
        val result = anthropicJson.decodeFromString<Content>(json)

        // when
        val encoded = anthropicJson.encodeToString(result)

        // then
        encoded sameAsJson json
    }

    @Test
    fun `should deserialize WebSearchToolResult with error content as Content`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "web_search_tool_result",
              "tool_use_id": "toolu_error_1",
              "content": {
                "type": "web_search_tool_result_error",
                "error_code": "too_many_requests"
              }
            }
            """
        ) should {
            be<WebSearchToolResult>()
            have(toolUseId == "toolu_error_1")
            content should {
                be<WebSearchToolResult.Error>()
                have(errorCode == WebSearchToolResult.Error.Code.TOO_MANY_REQUESTS)
            }
        }
    }

    @Test
    fun `should deserialize WebSearchToolResult with error content and serialize the exact JSON`() {
        // given
        val json = """
            {
              "type": "web_search_tool_result",
              "tool_use_id": "toolu_error_1",
              "content": {
                "error_code": "too_many_requests"
              }
            }
        """.trimIndent()
        val result = anthropicJson.decodeFromString<Content>(json)

        // when
        val encoded = anthropicJson.encodeToString(result)

        // then
        encoded sameAsJson json
    }

    @Test
    fun `should deserialize WebSearchToolResult with all error codes`() {
        // Test each error code to ensure they all deserialize correctly
        val errorCodes = listOf(
            "invalid_input" to WebSearchToolResult.Error.Code.INVALID_INPUT,
            "query_too_long" to WebSearchToolResult.Error.Code.QUERY_TOO_LONG,
            "too_many_requests" to WebSearchToolResult.Error.Code.TOO_MANY_REQUESTS,
            "max_uses_exceeded" to WebSearchToolResult.Error.Code.MAX_USES_EXCEEDED,
            "unavailable" to WebSearchToolResult.Error.Code.UNAVAILABLE
        )

        errorCodes.forEach { (jsonCode, enumValue) ->
            anthropicJson.decodeFromString<Content>(
                """
                {
                  "type": "web_search_tool_result",
                  "tool_use_id": "toolu_test",
                  "content": {
                    "type": "web_search_tool_result_error",
                    "error_code": "$jsonCode"
                  }
                }
                """
            ) should {
                be<WebSearchToolResult>()
                content should {
                    be<WebSearchToolResult.Error>()
                    have(errorCode == enumValue)
                }
            }
        }
    }


    @Test
    fun `should return string representation of WebSearchToolResult with Results`() {
        val result = anthropicJson.decodeFromString<WebSearchToolResult>(
            """
            {
              "type": "web_search_tool_result",
              "tool_use_id": "toolu_string",
              "content": [
                {
                  "title": "ToString Test",
                  "url": "https://tostring.com",
                  "encrypted_content": "str123"
                }
              ],
              "cache_control": {
                "type": "ephemeral"
              }
            }
            """
        )

        result.toString() sameAsJson """
            {
              "type": "web_search_tool_result",
              "tool_use_id": "toolu_string",
              "content": [
                {
                  "title": "ToString Test",
                  "url": "https://tostring.com",
                  "encrypted_content": "str123"
                }
              ],
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should copy WebSearchToolResult`() {
        // given
        val original = anthropicJson.decodeFromString<WebSearchToolResult>(
            """
            {
              "type": "web_search_tool_result",
              "tool_use_id": "toolu_copy",
              "content": [
                {
                  "title": "Copy Test",
                  "url": "https://copy.com",
                  "encrypted_content": "copy123"
                }
              ],
              "cache_control": {
                "type": "ephemeral"
              }
            }
            """
        )

        // when
        val copy = original.copy()

        // then
        copy should {
            have(toolUseId == "toolu_copy")
            content should {
                be<WebSearchToolResult.Results>()
                have(results.size == 1)
                results[0] should {
                    have(title == "Copy Test")
                    have(url == "https://copy.com")
                    have(encryptedContent == "copy123")
                }
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy WebSearchToolResult while altering properties`() {
        // given
        val original = anthropicJson.decodeFromString<WebSearchToolResult>(
            """
            {
              "type": "web_search_tool_result",
              "tool_use_id": "toolu_old",
              "content": [
                {
                  "title": "Old",
                  "url": "https://old.com",
                  "encrypted_content": "old123"
                }
              ]
            }
            """
        )

        // when
        val copy = original.copy {
            toolUseId = "toolu_new"
            content = WebSearchToolResult.Error(
                errorCode = WebSearchToolResult.Error.Code.QUERY_TOO_LONG
            )
            cacheControl = CacheControl.Ephemeral()
        }

        // then
        copy should {
            have(toolUseId == "toolu_new")
            content should {
                be<WebSearchToolResult.Error>()
                have(errorCode == WebSearchToolResult.Error.Code.QUERY_TOO_LONG)
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

}
