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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.content.Document
import com.xemantic.ai.anthropic.content.Source
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.json.prettyAnthropicJson
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

class StructuredOutputTest {

    // given
    @Serializable
    class Sonnet(
        val verses: List<Verse>
    )

    @Serializable
    class Verse(
        val text: String
    )

    @Test
    fun `should obtain structured output from LLM`() = runTest {
        val poetryTools = listOf(
            Tool<Sonnet>()
        )
        val anthropic = testAnthropic()

        val response = anthropic.messages.create {
            +"Write me a sonnet"
            tools = poetryTools
            toolChoice = ToolChoice.Tool<Sonnet>()
        }

        response should {
            have(stopReason == StopReason.TOOL_USE)
            have(content.isNotEmpty())
            have(content.any { it is ToolUse })
        }

        val sonnet = response.toolUseInput<Sonnet>()
        sonnet should {
            have(verses.isNotEmpty())
        }
    }

    // given
    val documentUrl = "https://www-cdn.anthropic.com/b383cf6baddbfc72fdf8b0ed533a518e2872d531.pdf"

    @Serializable
    @SerialName("document")
    data class Document(
        val title: String,
        val authors: List<Author>,
        val year: Int
    )

    @Serializable
    data class Author(
        val name: String
    )

    @Test
    fun `should extract structured output from PDF document`() = runTest {
        val myTools = listOf(
            Tool<Document>()
        )
        val anthropic = testAnthropic()

        val response = anthropic.messages.create {
            +Message {
                +Document {
                    source = Source.Url(documentUrl)
                }
                +"Please extract data from the document."
            }
            tools = myTools
            toolChoice = ToolChoice.Tool<Document>()
        }

        response should {
            have(stopReason == StopReason.TOOL_USE)
            have(content.isNotEmpty())
            have(content.any { it is ToolUse })
        }

        val document = response.toolUseInput<Document>()
        val json = prettyAnthropicJson.encodeToString(document)
        json sameAsJson """
            {
              "title": "The AI Fluency Framework",
              "authors": [
                {
                  "name": "Rick Dakan"
                },
                {
                  "name": "Joseph Feller"
                },
                {
                  "name": "Anthropic"
                }
              ],
              "year": 2025
            }
        """.trimIndent()
    }

}
