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

package com.xemantic.ai.anthropic.citation

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class CitationTest {

    @Test
    fun `should serialize CharLocation citation`() {
        anthropicJson.encodeToString<Citation>(
            Citation.CharLocation {
                citedText = "The grass is green."
                documentIndex = 0
                documentTitle = "My Document"
                startCharIndex = 0
                endCharIndex = 20
            }
        ) sameAsJson """
            {
              "type": "char_location",
              "cited_text": "The grass is green.",
              "document_index": 0,
              "document_title": "My Document",
              "start_char_index": 0,
              "end_char_index": 20
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize CharLocation citation`() {
        anthropicJson.decodeFromString<Citation>(
            """
            {
              "type": "char_location",
              "cited_text": "The sky is blue.",
              "document_index": 1,
              "document_title": "Another Document",
              "start_char_index": 21,
              "end_char_index": 37
            }
            """.trimIndent()
        ) should {
            be<Citation.CharLocation>()
            have(citedText == "The sky is blue.")
            have(documentIndex == 1)
            have(documentTitle == "Another Document")
            have(startCharIndex == 21)
            have(endCharIndex == 37)
        }
    }

    @Test
    fun `should serialize CharLocation citation without document title`() {
        anthropicJson.encodeToString<Citation>(
            Citation.CharLocation {
                citedText = "Sample text"
                documentIndex = 0
                documentTitle = null
                startCharIndex = 0
                endCharIndex = 11
            }
        ) sameAsJson """
            {
              "type": "char_location",
              "cited_text": "Sample text",
              "document_index": 0,
              "start_char_index": 0,
              "end_char_index": 11
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize PageLocation citation`() {
        anthropicJson.encodeToString<Citation>(
            Citation.PageLocation {
                citedText = "Text from PDF page"
                documentIndex = 0
                documentTitle = "Research Paper"
                startPageNumber = 1
                endPageNumber = 2
            }
        ) sameAsJson """
            {
              "type": "page_location",
              "cited_text": "Text from PDF page",
              "document_index": 0,
              "document_title": "Research Paper",
              "start_page_number": 1,
              "end_page_number": 2
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize PageLocation citation`() {
        anthropicJson.decodeFromString<Citation>(
            """
            {
              "type": "page_location",
              "cited_text": "Multi-page content",
              "document_index": 2,
              "document_title": "Technical Manual",
              "start_page_number": 5,
              "end_page_number": 8
            }
            """.trimIndent()
        ) should {
            be<Citation.PageLocation>()
            have(citedText == "Multi-page content")
            have(documentIndex == 2)
            have(documentTitle == "Technical Manual")
            have(startPageNumber == 5)
            have(endPageNumber == 8)
        }
    }

    @Test
    fun `should serialize ContentBlockLocation citation`() {
        anthropicJson.encodeToString<Citation>(
            Citation.ContentBlockLocation {
                citedText = "Custom content block"
                documentIndex = 0
                documentTitle = "Structured Document"
                startBlockIndex = 0
                endBlockIndex = 1
            }
        ) sameAsJson """
            {
              "type": "content_block_location",
              "cited_text": "Custom content block",
              "document_index": 0,
              "document_title": "Structured Document",
              "start_block_index": 0,
              "end_block_index": 1
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize ContentBlockLocation citation`() {
        anthropicJson.decodeFromString<Citation>(
            """
            {
              "type": "content_block_location",
              "cited_text": "Multiple blocks",
              "document_index": 1,
              "document_title": "Custom Document",
              "start_block_index": 3,
              "end_block_index": 7
            }
            """.trimIndent()
        ) should {
            be<Citation.ContentBlockLocation>()
            have(citedText == "Multiple blocks")
            have(documentIndex == 1)
            have(documentTitle == "Custom Document")
            have(startBlockIndex == 3)
            have(endBlockIndex == 7)
        }
    }

    @Test
    fun `should serialize WebSearchResultLocation citation`() {
        anthropicJson.encodeToString<Citation>(
            Citation.WebSearchResultLocation {
                citedText = "Claude Shannon was born on April 30, 1916..."
                url = "https://en.wikipedia.org/wiki/Claude_Shannon"
                title = "Claude Shannon - Wikipedia"
                encryptedIndex = "Eo8BCioIAhgBIiQyYjQ0OWJmZi1lNm"
            }
        ) sameAsJson """
            {
              "type": "web_search_result_location",
              "cited_text": "Claude Shannon was born on April 30, 1916...",
              "url": "https://en.wikipedia.org/wiki/Claude_Shannon",
              "title": "Claude Shannon - Wikipedia",
              "encrypted_index": "Eo8BCioIAhgBIiQyYjQ0OWJmZi1lNm"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize WebSearchResultLocation citation`() {
        anthropicJson.decodeFromString<Citation>(
            """
            {
              "type": "web_search_result_location",
              "cited_text": "Claude Shannon was an American mathematician...",
              "url": "https://example.com/claude-shannon",
              "title": "Claude Shannon Biography",
              "encrypted_index": "xyz123abc"
            }
            """.trimIndent()
        ) should {
            be<Citation.WebSearchResultLocation>()
            have(citedText == "Claude Shannon was an American mathematician...")
            have(url == "https://example.com/claude-shannon")
            have(title == "Claude Shannon Biography")
            have(encryptedIndex == "xyz123abc")
        }
    }

    @Test
    fun `should deserialize list of mixed citation types`() {
        val citations = anthropicJson.decodeFromString<List<Citation>>(
            """
            [
              {
                "type": "char_location",
                "cited_text": "Plain text",
                "document_index": 0,
                "start_char_index": 0,
                "end_char_index": 10
              },
              {
                "type": "page_location",
                "cited_text": "PDF text",
                "document_index": 1,
                "start_page_number": 1,
                "end_page_number": 2
              },
              {
                "type": "content_block_location",
                "cited_text": "Block text",
                "document_index": 2,
                "start_block_index": 0,
                "end_block_index": 1
              },
              {
                "type": "web_search_result_location",
                "cited_text": "Web search result",
                "url": "https://example.com",
                "title": "Example",
                "encrypted_index": "abc123"
              }
            ]
            """.trimIndent()
        )

        citations should {
            have(size == 4)
            have(get(0) is Citation.CharLocation)
            have(get(1) is Citation.PageLocation)
            have(get(2) is Citation.ContentBlockLocation)
            have(get(3) is Citation.WebSearchResultLocation)
        }
    }

}
