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
import com.xemantic.ai.file.magic.MediaType
import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
@SerialName("document")
class Document private constructor(
    val source: Source,
    val title: String? = null,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null,
    override val citations: List<Citation>? = null
) : Content(), WithCitations {

    class Builder : BinaryContentBuilder(
        supportedMediaTypes = SUPPORTED_MEDIA_TYPES
    ) {

        var title: String? = null
        var cacheControl: CacheControl? = null

        fun build(): Document = Document(
            title = title,
            source = requireNotNull(source),
            cacheControl = cacheControl
        )

    }

    companion object {

        /**
         * The set of [MediaType]s supported by the [Document].
         */
        val SUPPORTED_MEDIA_TYPES = setOf(
            MediaType.PDF, MediaType.TEXT
        )

    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): Document {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return Builder().also {
            it.source = source
            it.cacheControl = cacheControl
            block(it)
        }.build()
    }

}

@OptIn(ExperimentalContracts::class)
fun Document(
    block: Document.Builder.() -> Unit
): Document {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Document.Builder().apply(block).build()
}

@OptIn(ExperimentalContracts::class)
fun Document(
    path: String,
    block: Document.Builder.() -> Unit = {}
): Document {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Document(Path(path), block)
}

@OptIn(ExperimentalContracts::class)
fun Document(
    path: Path,
    block: Document.Builder.() -> Unit = {}
): Document {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Document {
        this.path = path
        block(this)
    }
}

@OptIn(ExperimentalContracts::class)
fun Document(
    bytes: ByteArray,
    block: Document.Builder.() -> Unit = {}
): Document {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Document {
        this.bytes = bytes
        block(this)
    }
}
