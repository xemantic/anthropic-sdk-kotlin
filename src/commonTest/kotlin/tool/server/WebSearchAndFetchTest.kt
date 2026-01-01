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
import com.xemantic.ai.anthropic.content.WebFetchServerToolUse
import com.xemantic.ai.anthropic.content.WebFetchToolResult
import com.xemantic.ai.anthropic.content.WebSearchServerToolUse
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.tool.WebFetch
import com.xemantic.ai.anthropic.tool.WebSearch
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test

class WebSearchAndFetchTest {

    @Test
    @Ignore // these tests pass, but are too expensive to run
    fun `should use WebSearch and WebFetch together in single turn`() = runTest {
        // given
        val webSearch = WebSearch {
            maxUses = 3
        }
        val webFetch = WebFetch {
            maxUses = 5
        }
        val anthropic = testAnthropic {
            +Anthropic.Beta.WEB_FETCH_2025_09_10
        }

        // when
        val response = anthropic.messages.create {
            +Message {
                +"""
                    Search for information about the Anthropic Claude API documentation,
                    then fetch the full content from the most relevant page you find
                    to give me detailed information about tool use capabilities.
                """.trimIndent()
            }
            tools = listOf(webSearch, webFetch)
        }

        // then
        response should {
            // Both tools are handled server-side, so we get END_TURN with results
            have(stopReason == StopReason.END_TURN)

            // Verify WebSearch was used
            val searchToolUses = content.filterIsInstance<WebSearchServerToolUse>()
            searchToolUses should {
                have(isNotEmpty())
                get(0).input should {
                    have(query.contains("Anthropic", ignoreCase = true) ||
                         query.contains("Claude", ignoreCase = true) ||
                         query.contains("API", ignoreCase = true))
                }
            }

            // Verify WebFetch was used
            val fetchToolUses = content.filterIsInstance<WebFetchServerToolUse>()
            fetchToolUses should {
                have(isNotEmpty())
                get(0).input should {
                    have(url.startsWith("http"))
                }
            }

            // Verify we got a text response combining both results
            have(text != null)
            have(text!!.isNotEmpty())

            // Verify usage tracking includes both web search and fetch requests
            usage should {
                serverToolUse should {
                    have(webSearchRequests!! > 0)
                    have(webFetchRequests!! > 0)
                }
            }
        }
    }

    @Test
    @Ignore // these tests pass, but are too expensive to run
    fun `should use WebSearch first then WebFetch in follow-up with filtered messages`() = runTest {
        // given
        val webSearch = WebSearch {
            maxUses = 2
            allowedDomains = listOf("docs.anthropic.com", "docs.claude.com")
        }
        val webFetch = WebFetch {
            maxUses = 3
            allowedDomains = listOf("docs.anthropic.com", "docs.claude.com")
        }
        val anthropic = testAnthropic {
            +Anthropic.Beta.WEB_FETCH_2025_09_10
        }
        val conversation = mutableListOf<Message>()
        conversation += "Search for the official Anthropic documentation about tool use from docs.anthropic.com or docs.claude.com"

        // when - Step 1: Search for information
        val searchResponse = anthropic.messages.create {
            messages = conversation
            tools = listOf(webSearch)
        }

        // then - Verify search response
        searchResponse should {
            have(stopReason == StopReason.END_TURN)

            val searchToolUses = content.filterIsInstance<WebSearchServerToolUse>()
            searchToolUses should {
                have(isNotEmpty())
            }

            usage should {
                serverToolUse should {
                    have(webSearchRequests!! > 0)
                }
            }
        }

        conversation += searchResponse

        // Add a follow-up request to fetch specific content
        conversation += """
            Based on the search results, fetch the full content from
            the most relevant documentation page about tool use from docs.anthropic.com or docs.claude.com.
        """.trimIndent()

        // when - Step 2: Use search results to fetch specific content
        val fetchResponse = anthropic.messages.create {
            messages = conversation
            tools = listOf(webFetch)
        }

        // then - Verify fetch response
        fetchResponse should {
            have(stopReason == StopReason.END_TURN)

            val fetchToolUses = content.filterIsInstance<WebFetchServerToolUse>()
            fetchToolUses should {
                have(isNotEmpty())
                get(0).input should {
                    have(url.startsWith("http"))
                }
            }

            // Verify we got WebFetchToolResult (could be Result or Error)
            val fetchResults = content.filterIsInstance<WebFetchToolResult>()
            fetchResults should {
                have(isNotEmpty())
            }

            // Verify we got a text response
            have(text != null)
            have(text!!.isNotEmpty())

            usage should {
                serverToolUse should {
                    have(webFetchRequests!! > 0)
                }
            }
        }
    }

}
