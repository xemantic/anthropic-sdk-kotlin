/*
 * Copyright 2024 Kazimierz Pogoda / Xemantic
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
import com.xemantic.ai.anthropic.json.toPrettyJson
import com.xemantic.ai.file.magic.MediaType
import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("document")
class Document private constructor(
    val source: Source,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    class Builder : BinaryContentBuilder(
        supportedMediaTypes = SUPPORTED_MEDIA_TYPES
    ) {

        var cacheControl: CacheControl? = null

        fun build(): Document = Document(
            source = requireNotNull(source),
            cacheControl = cacheControl
        )

    }

    companion object {

        /**
         * The set of [MediaType]s supported by the [Document].
         */
        val SUPPORTED_MEDIA_TYPES = setOf(
            MediaType.PDF
        )

    }

    override fun toString(): String = toPrettyJson()

}

fun Document(
    block: Document.Builder.() -> Unit
): Document = Document.Builder().apply(block).build()

fun Document(
    path: String,
    block: Document.Builder.() -> Unit = {}
): Document = Document(Path(path), block)

fun Document(
    path: Path,
    block: Document.Builder.() -> Unit = {}
): Document = Document {
    this.path = path
    block(this)
}

fun Document(
    bytes: ByteArray,
    block: Document.Builder.() -> Unit = {}
): Document = Document {
    this.bytes = bytes
    block(this)
}
