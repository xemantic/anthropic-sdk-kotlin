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
import com.xemantic.ai.anthropic.content.Document
import com.xemantic.ai.anthropic.content.Source
import com.xemantic.ai.anthropic.content.WebFetchToolResult
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test
import kotlin.time.Instant

class WebFetchToolResultTest {

    @Test
    fun `should deserialize WebFetchToolResult with web_fetch_result as content`() {
        anthropicJson.decodeFromString<Content>("""
            {
              "type": "web_fetch_tool_result",
              "tool_use_id": "srvtoolu_017DYtKUHkZAaTZsxxwFtF7s",
              "content": {
                "type": "web_fetch_result",
                "url": "https://xemantic.com/ai/",
                "retrieved_at": "2025-10-17T15:22:49.286000+00:00",
                "content": {
                  "type": "document",
                  "source": {
                    "type": "text",
                    "media_type": "text/plain",
                    "data": "Xemantic AI\nScience Hackathon\n ..."
                  },
                  "title": "Xemantic AI"
                }
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """
        ) should {
            be<WebFetchToolResult>()
            have(toolUseId == "srvtoolu_017DYtKUHkZAaTZsxxwFtF7s")
            content should {
                be<WebFetchToolResult.Result>()
                have(url == "https://xemantic.com/ai/")
                have(retrievedAt == Instant.parse("2025-10-17T15:22:49.286000+00:00"))
                content should {
                    be<Document>()
                    have(title == "Xemantic AI")
                    source should {
                        be<Source.Text>()
                        have(mediaType == "text/plain")
                        have(data == "Xemantic AI\nScience Hackathon\n ...")
                    }
                }
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should deserialize WebFetchToolResult with web_fetch_result as content and serialize the exact JSON`() {
        // given
        val json = """
            {
              "type": "web_fetch_tool_result",
              "tool_use_id": "srvtoolu_017DYtKUHkZAaTZsxxwFtF7s",
              "content": {
                "type": "web_fetch_result",
                "url": "https://xemantic.com/ai/",
                "retrieved_at": "2025-10-17T15:22:49.286Z",
                "content": {
                  "type": "document",
                  "source": {
                    "type": "text",
                    "media_type": "text/plain",
                    "data": "Xemantic AI\nScience Hackathon\n ..."
                  },
                  "title": "Xemantic AI"
                }
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
    fun `should deserialize WebFetchToolResult with web_fetch_tool_result_error as content`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "web_fetch_tool_result",
              "tool_use_id": "toolu_error_1",
              "content": {
                "type": "web_fetch_tool_result_error",
                "error_code": "url_not_allowed"
              }
            }
            """
        ) should {
            be<WebFetchToolResult>()
            have(toolUseId == "toolu_error_1")
            content should {
                be<WebFetchToolResult.Error>()
                have(errorCode == WebFetchToolResult.Error.Code.URL_NOT_ALLOWED)
            }
        }
    }

    @Test
    fun `should deserialize WebFetchToolResult with web_fetch_tool_result_error as content and serialize the exact JSON`() {
        // given
        val json = """
            {
              "type": "web_fetch_tool_result",
              "tool_use_id": "toolu_error_1",
              "content": {
                "type": "web_fetch_tool_result_error",
                "error_code": "url_not_allowed"
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
    fun `should deserialize WebFetchToolResult with all error codes`() {
        // Test each error code to ensure they all deserialize correctly
        val errorCodes = listOf(
            "invalid_input" to WebFetchToolResult.Error.Code.INVALID_INPUT,
            "url_too_long" to WebFetchToolResult.Error.Code.URL_TOO_LONG,
            "url_not_allowed" to WebFetchToolResult.Error.Code.URL_NOT_ALLOWED,
            "url_not_accessible" to WebFetchToolResult.Error.Code.URL_NOT_ACCESSIBLE,
            "too_many_requests" to WebFetchToolResult.Error.Code.TOO_MANY_REQUESTS,
            "unsupported_content_type" to WebFetchToolResult.Error.Code.UNSUPPORTED_CONTENT_TYPE,
            "max_uses_exceeded" to WebFetchToolResult.Error.Code.MAX_USES_EXCEEDED,
            "unavailable" to WebFetchToolResult.Error.Code.UNAVAILABLE
        )

        errorCodes.forEach { (jsonCode, enumValue) ->
            anthropicJson.decodeFromString<Content>(
                """
                {
                  "type": "web_fetch_tool_result",
                  "tool_use_id": "toolu_test",
                  "content": {
                    "type": "web_fetch_tool_result_error",
                    "error_code": "$jsonCode"
                  }
                }
                """
            ) should {
                be<WebFetchToolResult>()
                content should {
                    be<WebFetchToolResult.Error>()
                    have(errorCode == enumValue)
                }
            }
        }
    }

    @Test
    fun `should return string representation of WebFetchToolResult with Result`() {
        val result = WebFetchToolResult {
            toolUseId = "srvtoolu_017DYtKUHkZAaTZsxxwFtF7s"
            content = WebFetchToolResult.Result(
                url = "https://xemantic.com/ai/",
                retrievedAt = Instant.parse("2025-10-17T15:22:49.286Z"),
                content = Document {
                    source = Source.Text {
                        data = "Xemantic AI\nScience Hackathon\n ..."
                    }
                    title = "Xemantic AI"
                }
            )
            cacheControl = CacheControl.Ephemeral()
        }

        result.toString() sameAsJson """
            {
              "type": "web_fetch_tool_result",
              "tool_use_id": "srvtoolu_017DYtKUHkZAaTZsxxwFtF7s",
              "content": {
                "type": "web_fetch_result",
                "url": "https://xemantic.com/ai/",
                "retrieved_at": "2025-10-17T15:22:49.286Z",
                "content": {
                  "type": "document",
                  "source": {
                    "type": "text",
                    "media_type": "text/plain",
                    "data": "Xemantic AI\nScience Hackathon\n ..."
                  },
                  "title": "Xemantic AI"
                }
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should copy WebFetchToolResult`() {
        // given
        val original = WebFetchToolResult {
            toolUseId = "srvtoolu_017DYtKUHkZAaTZsxxwFtF7s"
            content = WebFetchToolResult.Result(
                url = "https://xemantic.com/ai/",
                retrievedAt = Instant.parse("2025-10-17T15:22:49.286Z"),
                content = Document {
                    source = Source.Text {
                        data = "Xemantic AI\nScience Hackathon\n ..."
                    }
                    title = "Xemantic AI"
                }
            )
            cacheControl = CacheControl.Ephemeral()
        }

        // when
        val copy = original.copy()

        // then
        copy should {
            have(toolUseId == "srvtoolu_017DYtKUHkZAaTZsxxwFtF7s")
            content should {
                be<WebFetchToolResult.Result>()
                have(url == "https://xemantic.com/ai/")
                have(retrievedAt == Instant.parse("2025-10-17T15:22:49.286Z"))
                content should {
                    be<Document>()
                    source should {
                        be<Source.Text>()
                        have(data == "Xemantic AI\nScience Hackathon\n ...")
                        have(title == "Xemantic AI")
                    }
                }
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy WebFetchToolResult while altering properties`() {
        // given
        val original = WebFetchToolResult {
            toolUseId = "srvtoolu_017DYtKUHkZAaTZsxxwFtF7s"
            content = WebFetchToolResult.Result(
                url = "https://xemantic.com/ai/",
                retrievedAt = Instant.parse("2025-10-17T15:22:49.286Z"),
                content = Document {
                    source = Source.Text {
                        data = "Xemantic AI\nScience Hackathon\n ..."
                    }
                    title = "Xemantic AI"
                }
            )
        }

        // when
        val copy = original.copy {
            toolUseId = "toolu_new"
            content = WebFetchToolResult.Error(
                errorCode = WebFetchToolResult.Error.Code.URL_NOT_ALLOWED
            )
            cacheControl = CacheControl.Ephemeral()
        }

        // then
        copy should {
            have(toolUseId == "toolu_new")
            content should {
                be<WebFetchToolResult.Error>()
                have(errorCode == WebFetchToolResult.Error.Code.URL_NOT_ALLOWED)
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

}
