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
import com.xemantic.ai.anthropic.json.toPrettyJson
import com.xemantic.ai.file.magic.MediaType
import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
@SerialName("image")
class Image private constructor(
    val source: Source,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Content() {

    class Builder : BinaryContentBuilder(
        supportedMediaTypes = SUPPORTED_MEDIA_TYPES
    ) {

        var cacheControl: CacheControl? = null

        fun build(): Image = Image(
            source = requireNotNull(source) { "source cannot be null" },
            cacheControl = cacheControl
        )

    }

    companion object {

        /**
         * The set of [MediaType]s supported by the [Image].
         */
        val SUPPORTED_MEDIA_TYPES = setOf(
            MediaType.JPEG,
            MediaType.PNG,
            MediaType.GIF,
            MediaType.WEBP
        )

    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): Image {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return Builder().also {
            it.source = source
            it.cacheControl = cacheControl
            block(it)
        }.build()
    }

    override fun toString(): String = toPrettyJson()

}

@OptIn(ExperimentalContracts::class)
fun Image(
    block: Image.Builder.() -> Unit
): Image {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Image.Builder().apply(block).build()
}

@OptIn(ExperimentalContracts::class)
fun Image(
    path: String,
    block: Image.Builder.() -> Unit = {}
): Image {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Image(Path(path), block)
}

@OptIn(ExperimentalContracts::class)
fun Image(
    path: Path,
    block: Image.Builder.() -> Unit = {}
): Image {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Image {
        this.path = path
        block(this)
    }
}

@OptIn(ExperimentalContracts::class)
fun Image(
    bytes: ByteArray,
    block: Image.Builder.() -> Unit = {}
): Image {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Image {
        this.bytes = bytes
        block(this)
    }
}
