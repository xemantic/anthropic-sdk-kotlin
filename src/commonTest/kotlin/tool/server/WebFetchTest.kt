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

package com.xemantic.ai.anthropic.tool.server

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.content.*
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.tool.WebFetch
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.Clock

class WebFetchTest {

    @Test
    @Ignore // these tests pass, but are too expensive to run
    fun `should use WebFetch tool`() = runTest {
        // given
        val webFetch = WebFetch {
            allowedDomains = listOf("xemantic.com")
        }
        val anthropic = testAnthropic {
            +Anthropic.Beta.WEB_FETCH_2025_09_10
        }

        // when
        val response = anthropic.messages.create {
            +Message {
                +"Please give me the content of https://xemantic.com/ai/ as Markdown directly, without wrapping in triple quotes?"
            }
            tools = listOf(webFetch)
        }

        // then
        response should {
            // Web fetch is handled server-side, so we get END_TURN with a text response
            have(stopReason == StopReason.END_TURN)
            content should {
                have(size == 3)
            }
            content[0] should {
                be<WebFetchServerToolUse>()
                have(name == "web_fetch")
                input should {
                    have(url == "https://xemantic.com/ai/")
                }
            }
            val firstToolUseId = (content[0] as WebFetchServerToolUse).id
            content[1] should {
                be<WebFetchToolResult>()
                have(toolUseId == firstToolUseId)
                content should {
                    be<WebFetchToolResult.Result>()
                    have(url == "https://xemantic.com/ai/")
                    have(retrievedAt < Clock.System.now())
                    content should {
                        be<Document>()
                        source should {
                            be<Source.Text>()
                            have(mediaType == "text/plain")
                            have(data.startsWith("Xemantic AI"))
                        }
                    }
                }
            }
            content[2] should {
                be<Text>()
                have(text.contains("# Xemantic AI"))
            }

            // Verify that usage tracking includes web fetch requests
            usage should {
                serverToolUse should {
                    have(webFetchRequests!! > 0)
                }
            }
        }

    }

    @Test
    fun `should serialize WebFetch tool with minimal config`() {
        anthropicJson.encodeToString(
            WebFetch {}
        ) sameAsJson """
            {
              "name": "web_fetch",
              "type": "web_fetch_20250910"
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize WebFetch tool with all parameters`() {
        anthropicJson.encodeToString(
            WebFetch {
                maxUses = 10
                allowedDomains = listOf("anthropic.com", "wikipedia.org")
                blockedDomains = listOf("spam.com")
                citations = WebFetch.Citations(enabled = true)
                maxContentTokens = 100000
            }
        ) sameAsJson """
            {
              "name": "web_fetch",
              "type": "web_fetch_20250910",
              "max_uses": 10,
              "allowed_domains": [
                "anthropic.com",
                "wikipedia.org"
              ],
              "blocked_domains": [
                "spam.com"
              ],
              "citations": {
                "enabled": true
              },
              "max_content_tokens": 100000
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize WebFetch tool with minimal config`() {
        anthropicJson.decodeFromString<Tool>(
            """
            {
              "name": "web_fetch",
              "type": "web_fetch_20250910"
            }
            """
        ) should {
            have(name == "web_fetch")
            be<WebFetch>()
            have(type == "web_fetch_20250910")
            have(maxUses == null)
            have(allowedDomains == null)
            have(blockedDomains == null)
            have(citations == null)
            have(maxContentTokens == null)
        }
    }

    @Test
    fun `should deserialize WebFetch tool with all parameters`() {
        anthropicJson.decodeFromString<Tool>(
            """
            {
              "name": "web_fetch",
              "type": "web_fetch_20250910",
              "max_uses": 5,
              "allowed_domains": ["example.com"],
              "blocked_domains": ["spam.com"],
              "citations": {
                "enabled": false
              },
              "max_content_tokens": 50000
            }
            """
        ) should {
            have(name == "web_fetch")
            be<WebFetch>()
            have(type == "web_fetch_20250910")
            have(maxUses == 5)
            have(allowedDomains == listOf("example.com"))
            have(blockedDomains == listOf("spam.com"))
            have(citations?.enabled == false)
            have(maxContentTokens == 50000)
        }
    }

    @Test
    fun `should return JSON for WebFetch tool toString`() {
        WebFetch {
            maxUses = 10
            citations = WebFetch.Citations(enabled = true)
        }.toString() sameAsJson """
            {
              "name": "web_fetch",
              "type": "web_fetch_20250910",
              "max_uses": 10,
              "citations": {
                "enabled": true
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize Citations`() {
        anthropicJson.encodeToString(
            WebFetch.Citations(enabled = false)
        ) sameAsJson """
            {
              "enabled": false
            }
        """.trimIndent()
    }

}