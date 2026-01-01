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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.citation.Citation
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class TextWithCitationsTest {

    @Test
    fun `should serialize Text with CharLocation citations`() {
        anthropicJson.encodeToString<Content>(
            Text("the grass is green") {
                citations = listOf(
                    Citation.CharLocation {
                        citedText = "The grass is green."
                        documentIndex = 0
                        documentTitle = "My Document"
                        startCharIndex = 0
                        endCharIndex = 20
                    }
                )
            }
        ) sameAsJson """
            {
              "type": "text",
              "text": "the grass is green",
              "citations": [
                {
                  "type": "char_location",
                  "cited_text": "The grass is green.",
                  "document_index": 0,
                  "document_title": "My Document",
                  "start_char_index": 0,
                  "end_char_index": 20
                }
              ]
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize Text with CharLocation citations`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "text",
              "text": "the grass is green",
              "citations": [
                {
                  "type": "char_location",
                  "cited_text": "The grass is green.",
                  "document_index": 0,
                  "document_title": "My Document",
                  "start_char_index": 0,
                  "end_char_index": 20
                }
              ]
            }
            """
        ) should {
            be<Text>()
            have(text == "the grass is green")
            citations should {
                have(size == 1)
                get(0) should {
                    be<Citation.CharLocation>()
                    have(citedText == "The grass is green.")
                    have(documentIndex == 0)
                    have(documentTitle == "My Document")
                    have(startCharIndex == 0)
                    have(endCharIndex == 20)
                }
            }
        }
    }

    @Test
    fun `should serialize Text with PageLocation citations`() {
        anthropicJson.encodeToString<Content>(
            Text("PDF content summary") {
                citations = listOf(
                    Citation.PageLocation {
                        citedText = "Extracted from PDF"
                        documentIndex = 1
                        documentTitle = "Research Paper"
                        startPageNumber = 3
                        endPageNumber = 5
                    }
                )
            }
        ) sameAsJson """
            {
              "type": "text",
              "text": "PDF content summary",
              "citations": [
                {
                  "type": "page_location",
                  "cited_text": "Extracted from PDF",
                  "document_index": 1,
                  "document_title": "Research Paper",
                  "start_page_number": 3,
                  "end_page_number": 5
                }
              ]
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize Text with multiple citations of different types`() {
        anthropicJson.encodeToString<Content>(
            Text("Summary from multiple sources") {
                citations = listOf(
                    Citation.CharLocation {
                        citedText = "First source"
                        documentIndex = 0
                        documentTitle = "Text Document"
                        startCharIndex = 0
                        endCharIndex = 12
                    },
                    Citation.PageLocation {
                        citedText = "Second source"
                        documentIndex = 1
                        documentTitle = "PDF Document"
                        startPageNumber = 1
                        endPageNumber = 2
                    },
                    Citation.ContentBlockLocation {
                        citedText = "Third source"
                        documentIndex = 2
                        documentTitle = "Custom Document"
                        startBlockIndex = 0
                        endBlockIndex = 1
                    }
                )
            }
        ) sameAsJson """
            {
              "type": "text",
              "text": "Summary from multiple sources",
              "citations": [
                {
                  "type": "char_location",
                  "cited_text": "First source",
                  "document_index": 0,
                  "document_title": "Text Document",
                  "start_char_index": 0,
                  "end_char_index": 12
                },
                {
                  "type": "page_location",
                  "cited_text": "Second source",
                  "document_index": 1,
                  "document_title": "PDF Document",
                  "start_page_number": 1,
                  "end_page_number": 2
                },
                {
                  "type": "content_block_location",
                  "cited_text": "Third source",
                  "document_index": 2,
                  "document_title": "Custom Document",
                  "start_block_index": 0,
                  "end_block_index": 1
                }
              ]
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize Text without citations`() {
        anthropicJson.encodeToString<Content>(
            Text("Simple text without citations")
        ) sameAsJson """
            {
              "type": "text",
              "text": "Simple text without citations"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize Text without citations`() {
        anthropicJson.decodeFromString<Content>(
            """
            {
              "type": "text",
              "text": "Simple text"
            }
            """
        ) should {
            be<Text>()
            have(text == "Simple text")
            have(citations == null)
        }
    }

    @Test
    fun `should copy Text preserving citations`() {
        val original = Text("Original text") {
            citations = listOf(
                Citation.CharLocation {
                    citedText = "Original citation"
                    documentIndex = 0
                    startCharIndex = 0
                    endCharIndex = 17
                }
            )
        }

        val copied = original.copy()

        copied should {
            have(text == "Original text")
            citations should {
                have(size == 1)
            }
        }
    }

    @Test
    fun `should copy Text and modify citations`() {
        val original = Text("Original text") {
            citations = listOf(
                Citation.CharLocation {
                    citedText = "Original citation"
                    documentIndex = 0
                    startCharIndex = 0
                    endCharIndex = 17
                }
            )
        }

        val modified = original.copy {
            citations = listOf(
                Citation.PageLocation {
                    citedText = "New citation"
                    documentIndex = 1
                    startPageNumber = 1
                    endPageNumber = 2
                }
            )
        }

        modified should {
            have(text == "Original text")
            citations should {
                have(size == 1)
                get(0) should {
                    be<Citation.PageLocation>()
                }
            }
        }
    }

}
