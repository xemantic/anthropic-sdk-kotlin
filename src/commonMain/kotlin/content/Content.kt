/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
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

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.citation.Citation
import com.xemantic.ai.anthropic.json.ContentSerializer
import com.xemantic.ai.anthropic.json.toPrettyJson
import com.xemantic.ai.file.magic.MediaType
import com.xemantic.ai.file.magic.detectMediaType
import com.xemantic.ai.file.magic.readBytes
import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable(with = ContentSerializer::class)
sealed class Content {

    @SerialName("cache_control")
    abstract val cacheControl: CacheControl?

    override fun toString(): String = toPrettyJson()

    fun alterCacheControl(
        cacheControl: CacheControl?
    ): Content = when (this) {
        is Text -> copy { this.cacheControl = cacheControl }
        is Image -> copy { this.cacheControl = cacheControl }
        is Document -> copy { this.cacheControl = cacheControl }
        is ToolUse -> copy { this.cacheControl = cacheControl }
        is WebSearchServerToolUse -> copy { this.cacheControl = cacheControl }
        is WebFetchServerToolUse -> copy { this.cacheControl = cacheControl }
        is WebSearchToolResult -> copy { this.cacheControl = cacheControl }
        is WebFetchToolResult -> copy { this.cacheControl = cacheControl }
        is ToolResult -> copy { this.cacheControl = cacheControl }
        is ServerToolUse<*> -> {
            throw IllegalStateException(
                "Unsupported ServerToolUse: $this"
            )
        }
    }

}

interface WithCitations {

    val citations: List<Citation>?

}

/**
 * Allows to add [Content], via [unaryPlus] operator
 * overloading, to the list of [Content] elements used during
 * the [com.xemantic.ai.anthropic.message.Message] or the [ToolResult]
 * building process.
 */
abstract class ContentListBuilder {

    var content: List<Content> = emptyList()

    operator fun Content.unaryPlus() {
        content += this
    }

    operator fun String.unaryPlus() {
        content += Text(this)
    }

    operator fun Collection<Content>.unaryPlus() {
        content += this
    }

}

/**
 * Base class for builders of binary content, like
 * [Image] or [Document].
 *
 * @param supportedMediaTypes
 */
abstract class BinaryContentBuilder(
    val supportedMediaTypes: Set<MediaType>
) {

    /**
     * The [Source] of the content.
     *
     * Note: if not set directly, it can be also automatically populated by
     * setting the [path].
     */
    var source: Source? = null

    /**
     * The path of this binary content being built.
     *
     * Note: setting up the [path] will populate the [source].
     */
    var path: Path? = null
        set(value) {
            val pathToSet = requireNotNull(value) {
                "The path of binary content cannot be null"
            }
            val bytesToSet = pathToSet.readBytes()
            try {
                bytes = bytesToSet
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Unsupported file at path \"$pathToSet\": ${e.message}"
                )
            }
            field = pathToSet
        }

    var bytes: ByteArray? = null
        set(value) {
            val bytesToSet = requireNotNull(value) {
                "The bytes of binary content cannot be null"
            }
            val type = requireNotNull(bytesToSet.detectMediaType()) {
                "Cannot detect media type"
            }
            require(type in supportedMediaTypes) {
                "Unsupported media type \"${type.mime}\", " +
                        "supported: ${supportedMediaTypes.map { "\"${it.mime}\"" }}"
            }
            source = Source.Base64 {
                mediaType = type.mime
                @OptIn(ExperimentalEncodingApi::class)
                data = Base64.encode(bytesToSet)
            }
            field = bytesToSet
        }

}
