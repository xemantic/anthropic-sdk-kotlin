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

import com.xemantic.ai.anthropic.json.toPrettyJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Represents a citation that links generated text to source documents.
 * Citations provide traceable references to the exact sentences and passages
 * used to generate responses.
 *
 * There are four types of citations:
 * - [CharLocation]: For plain text documents (character-based indexing)
 * - [PageLocation]: For PDF documents (page-based indexing)
 * - [ContentBlockLocation]: For custom content documents (block-based indexing)
 * - [WebSearchResultLocation]: For web search results
 */
@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Citation {

    /**
     * The exact text being cited from the source document.
     * This field is provided for convenience and does not count towards output tokens.
     */
    abstract val citedText: String

    /**
     * Citation for plain text documents using character-based indexing.
     *
     * @property citedText The exact text being cited
     * @property documentIndex 0-indexed document reference
     * @property documentTitle Optional document name
     * @property startCharIndex 0-indexed start position (inclusive)
     * @property endCharIndex 0-indexed end position (exclusive)
     */
    @Serializable
    @SerialName("char_location")
    class CharLocation private constructor(
        @SerialName("cited_text")
        override val citedText: String,
        @SerialName("document_index")
        val documentIndex: Int,
        @SerialName("document_title")
        val documentTitle: String? = null,
        @SerialName("start_char_index")
        val startCharIndex: Int,
        @SerialName("end_char_index")
        val endCharIndex: Int
    ) : Citation() {

        class Builder {

            var citedText: String? = null
            var documentIndex: Int? = null
            var documentTitle: String? = null
            var startCharIndex: Int? = null
            var endCharIndex: Int? = null

            fun build(): CharLocation = CharLocation(
                citedText = requireNotNull(citedText) { "citedText cannot be null" },
                documentIndex = requireNotNull(documentIndex) { "documentIndex cannot be null" },
                documentTitle = documentTitle,
                startCharIndex = requireNotNull(startCharIndex) { "startCharIndex cannot be null" },
                endCharIndex = requireNotNull(endCharIndex) { "endCharIndex cannot be null" }
            )

        }

    }

    /**
     * Citation for PDF documents using page-based indexing.
     *
     * @property citedText The exact text being cited
     * @property documentIndex 0-indexed document reference
     * @property documentTitle Optional document name
     * @property startPageNumber 1-indexed start page (inclusive)
     * @property endPageNumber 1-indexed end page (exclusive)
     */
    @Serializable
    @SerialName("page_location")
    class PageLocation private constructor(
        @SerialName("cited_text")
        override val citedText: String,
        @SerialName("document_index")
        val documentIndex: Int,
        @SerialName("document_title")
        val documentTitle: String? = null,
        @SerialName("start_page_number")
        val startPageNumber: Int,
        @SerialName("end_page_number")
        val endPageNumber: Int
    ) : Citation() {

        class Builder {

            var citedText: String? = null
            var documentIndex: Int? = null
            var documentTitle: String? = null
            var startPageNumber: Int? = null
            var endPageNumber: Int? = null

            fun build(): PageLocation = PageLocation(
                citedText = requireNotNull(citedText) { "citedText cannot be null" },
                documentIndex = requireNotNull(documentIndex) { "documentIndex cannot be null" },
                documentTitle = documentTitle,
                startPageNumber = requireNotNull(startPageNumber) { "startPageNumber cannot be null" },
                endPageNumber = requireNotNull(endPageNumber) { "endPageNumber cannot be null" }
            )

        }

    }

    /**
     * Citation for custom content documents using content block-based indexing.
     *
     * @property citedText The exact text being cited
     * @property documentIndex 0-indexed document reference
     * @property documentTitle Optional document name
     * @property startBlockIndex 0-indexed start block (inclusive)
     * @property endBlockIndex 0-indexed end block (exclusive)
     */
    @Serializable
    @SerialName("content_block_location")
    class ContentBlockLocation private constructor(
        @SerialName("cited_text")
        override val citedText: String,
        @SerialName("document_index")
        val documentIndex: Int,
        @SerialName("document_title")
        val documentTitle: String? = null,
        @SerialName("start_block_index")
        val startBlockIndex: Int,
        @SerialName("end_block_index")
        val endBlockIndex: Int
    ) : Citation() {

        class Builder {

            var citedText: String? = null
            var documentIndex: Int? = null
            var documentTitle: String? = null
            var startBlockIndex: Int? = null
            var endBlockIndex: Int? = null

            fun build(): ContentBlockLocation = ContentBlockLocation(
                citedText = requireNotNull(citedText) { "citedText cannot be null" },
                documentIndex = requireNotNull(documentIndex) { "documentIndex cannot be null" },
                documentTitle = documentTitle,
                startBlockIndex = requireNotNull(startBlockIndex) { "startBlockIndex cannot be null" },
                endBlockIndex = requireNotNull(endBlockIndex) { "endBlockIndex cannot be null" }
            )

        }

    }

    /**
     * Citation for web search results.
     *
     * @property citedText The exact text being cited (up to 150 characters)
     * @property url The URL of the cited source
     * @property title The title of the cited source
     * @property encryptedIndex A reference that must be passed back for multi-turn conversations
     */
    @Serializable
    @SerialName("web_search_result_location")
    class WebSearchResultLocation private constructor(
        @SerialName("cited_text")
        override val citedText: String,
        val url: String,
        val title: String,
        @SerialName("encrypted_index")
        val encryptedIndex: String
    ) : Citation() {

        class Builder {

            var citedText: String? = null
            var url: String? = null
            var title: String? = null
            var encryptedIndex: String? = null

            fun build(): WebSearchResultLocation = WebSearchResultLocation(
                citedText = requireNotNull(citedText) { "citedText cannot be null" },
                url = requireNotNull(url) { "url cannot be null" },
                title = requireNotNull(title) { "title cannot be null" },
                encryptedIndex = requireNotNull(encryptedIndex) { "encryptedIndex cannot be null" }
            )

        }

    }

    companion object {

        fun CharLocation(
            block: CharLocation.Builder.() -> Unit
        ): CharLocation = CharLocation.Builder().apply(block).build()

        fun PageLocation(
            block: PageLocation.Builder.() -> Unit
        ): PageLocation = PageLocation.Builder().apply(block).build()

        fun ContentBlockLocation(
            block: ContentBlockLocation.Builder.() -> Unit
        ): ContentBlockLocation = ContentBlockLocation.Builder().apply(block).build()

        fun WebSearchResultLocation(
            block: WebSearchResultLocation.Builder.() -> Unit
        ): WebSearchResultLocation = WebSearchResultLocation.Builder().apply(block).build()

    }

    override fun toString(): String = toPrettyJson()

}
