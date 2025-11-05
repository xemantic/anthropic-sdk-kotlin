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

import com.xemantic.kotlin.test.sameAsJson
import kotlin.test.Test

class CitationToStringTest {

    @Test
    fun `should return pretty JSON for CharLocation toString`() {
        val citation = Citation.CharLocation {
            citedText = "The grass is green."
            documentIndex = 0
            documentTitle = "My Document"
            startCharIndex = 0
            endCharIndex = 20
        }

        citation.toString() sameAsJson """
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
    fun `should return pretty JSON for PageLocation toString`() {
        val citation = Citation.PageLocation {
            citedText = "Text from PDF page"
            documentIndex = 1
            documentTitle = "Research Paper"
            startPageNumber = 3
            endPageNumber = 5
        }

        citation.toString() sameAsJson """
            {
              "type": "page_location",
              "cited_text": "Text from PDF page",
              "document_index": 1,
              "document_title": "Research Paper",
              "start_page_number": 3,
              "end_page_number": 5
            }
        """.trimIndent()
    }

    @Test
    fun `should return pretty JSON for ContentBlockLocation toString`() {
        val citation = Citation.ContentBlockLocation {
            citedText = "Custom content block"
            documentIndex = 2
            documentTitle = "Structured Document"
            startBlockIndex = 0
            endBlockIndex = 1
        }

        citation.toString() sameAsJson """
            {
              "type": "content_block_location",
              "cited_text": "Custom content block",
              "document_index": 2,
              "document_title": "Structured Document",
              "start_block_index": 0,
              "end_block_index": 1
            }
        """.trimIndent()
    }

    @Test
    fun `should return pretty JSON for WebSearchResultLocation toString`() {
        val citation = Citation.WebSearchResultLocation {
            citedText = "Claude Shannon was born on April 30, 1916..."
            url = "https://en.wikipedia.org/wiki/Claude_Shannon"
            title = "Claude Shannon - Wikipedia"
            encryptedIndex = "Eo8BCioIAhgBIiQyYjQ0OWJmZi1lNm"
        }

        citation.toString() sameAsJson """
            {
              "type": "web_search_result_location",
              "cited_text": "Claude Shannon was born on April 30, 1916...",
              "url": "https://en.wikipedia.org/wiki/Claude_Shannon",
              "title": "Claude Shannon - Wikipedia",
              "encrypted_index": "Eo8BCioIAhgBIiQyYjQ0OWJmZi1lNm"
            }
        """.trimIndent()
    }

}
