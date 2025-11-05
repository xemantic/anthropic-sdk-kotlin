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

import com.xemantic.ai.anthropic.content.WebSearchServerToolUse
import com.xemantic.ai.anthropic.content.WebSearchToolResult
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.location.UserLocation
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.tool.WebSearch
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class WebSearchTest {

    @Test
    fun `should use WebSearch tool`() = runTest {
        // given
        val webSearch = WebSearch {
            maxUses = 3
            blockedDomains = listOf("example.com")
            userLocation = UserLocation.Approximate {
                city = "San Francisco"
                region = "California"
                country = "US"
                timezone = "America/Los_Angeles"
            }
        }
        val anthropic = testAnthropic()

        // when
        val response = anthropic.messages.create {
            +Message { +"Who won the Nobel Prize in Physics in 2025?" }
            tools = listOf(webSearch)
        }

        // then
        response should {
            // Web search is handled server-side, so we get END_TURN with a text response
            have(stopReason == StopReason.END_TURN)

            // Verify that WebSearchServerToolUse content was present in the response
            val serverToolUses = content.filterIsInstance<WebSearchServerToolUse>()
            serverToolUses should {
                have(size == 1)
                get(0) should {
                    input should {
                        have(query.contains("nobel", ignoreCase = true))
                    }
                }
            }

            have("John Clarke," in text!!)
            have("Michel H. Devoret" in text!!)
            have("John M. Martinis" in text!!)

            // Verify that usage tracking includes web search requests
            usage should {
                serverToolUse should {
                    have(webSearchRequests!! > 0)
                }
            }
        }
    }

    @Test
    fun `should handle WebSearch error when max_uses exceeded`() = runTest {
        // given
        val webSearch = WebSearch {
            maxUses = 1  // Very low limit to trigger error
        }
        val anthropic = testAnthropic()

        // when - make multiple requests that should exceed the limit
        val response = anthropic.messages.create {
            +Message {
                +"""
                    Search for: kotlin multiplatform,
                    then search for: anthropic API,
                    then search for: kotlin serialization,
                    then search for: ktor client
                """.trimIndent()
            }
            tools = listOf(webSearch)
        }

        // then - should receive error in WebSearchToolResult
        response should {
            // Web search is handled server-side, so we get END_TURN with a text response
            have(stopReason == StopReason.END_TURN)

            // Verify that WebSearchServerToolUse content was present in the response
            have(content.any { it is WebSearchServerToolUse })

            // Also verify we have WebSearchToolResult blocks with errors
            have(content.any { it is WebSearchToolResult })

            usage should {
                serverToolUse should {
                    have(webSearchRequests == 1)
                }
            }

        }
    }

    @Test
    fun `should serialize WebSearch tool with minimal config`() {
        anthropicJson.encodeToString(
            WebSearch {}
        ) sameAsJson """
            {
              "name": "web_search",
              "type": "web_search_20250305"
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize WebSearch tool with all parameters`() {
        anthropicJson.encodeToString(
            WebSearch {
                maxUses = 5
                allowedDomains = listOf("example.com", "wikipedia.org")
                blockedDomains = listOf("spam.com")
                userLocation = UserLocation.Approximate {
                    city = "San Francisco"
                    region = "CA"
                    country = "US"
                    timezone = "America/Los_Angeles"
                }
            }
        ) sameAsJson """
            {
              "name": "web_search",
              "type": "web_search_20250305",
              "max_uses": 5,
              "allowed_domains": [
                "example.com",
                "wikipedia.org"
              ],
              "blocked_domains": [
                "spam.com"
              ],
              "user_location": {
                "type": "approximate",
                "city": "San Francisco",
                "region": "CA",
                "country": "US",
                "timezone": "America/Los_Angeles"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize WebSearch tool with minimal config`() {
        anthropicJson.decodeFromString<Tool>(
            """
            {
              "name": "web_search",
              "type": "web_search_20250305"
            }
            """
        ) should {
            have(name == "web_search")
            be<WebSearch>()
            have(type == "web_search_20250305")
            have(maxUses == null)
            have(allowedDomains == null)
            have(blockedDomains == null)
            have(userLocation == null)
        }
    }

    @Test
    fun `should deserialize WebSearch tool with all parameters`() {
        anthropicJson.decodeFromString<Tool>(
            """
            {
              "name": "web_search",
              "type": "web_search_20250305",
              "max_uses": 3,
              "allowed_domains": ["example.com"],
              "blocked_domains": ["spam.com"],
              "user_location": {
                "type": "approximate",
                "city": "New York",
                "country": "US"
              }
            }
            """
        ).also { tool ->
            tool should {
                have(name == "web_search")
                be<WebSearch>()
                have(type == "web_search_20250305")
                have(maxUses == 3)
                have(allowedDomains == listOf("example.com"))
                have(blockedDomains == listOf("spam.com"))
                have(userLocation != null)
            }
            (tool as WebSearch).userLocation!! should {
                be<UserLocation.Approximate>()
                have(city == "New York")
                have(country == "US")
            }
        }
    }

    @Test
    fun `should return JSON for WebSearch tool toString`() {
        WebSearch {
            maxUses = 5
        }.toString() sameAsJson """
            {
              "name": "web_search",
              "type": "web_search_20250305",
              "max_uses": 5
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize UserLocation`() {
        anthropicJson.encodeToString(
            serializer = UserLocation.serializer(),
            value = UserLocation.Approximate {
                city = "London"
                country = "GB"
                timezone = "Europe/London"
            }
        ) sameAsJson """
            {
              "type": "approximate",
              "city": "London",
              "country": "GB",
              "timezone": "Europe/London"
            }
        """.trimIndent()
    }

}