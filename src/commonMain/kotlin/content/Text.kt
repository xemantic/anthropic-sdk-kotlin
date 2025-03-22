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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
@SerialName("text")
class Text private constructor(
    val text: String,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null,
) : Content() {

    class Builder {

        var text: String? = null
        var cacheControl: CacheControl? = null

        fun build(): Text = Text(
            requireNotNull(text) { "text cannot be null" },
            cacheControl
        )

    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): Text {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return Builder().also {
            it.text = text
            it.cacheControl = cacheControl
            block(it)

        }.build()
    }

}

@OptIn(ExperimentalContracts::class)
fun Text(
    block: Text.Builder.() -> Unit
): Text {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Text.Builder().apply(block).build()
}

@OptIn(ExperimentalContracts::class)
fun Text(
    text: String,
    block: Text.Builder.() -> Unit = {}
): Text {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Text {
        this.text = text
        block(this)
    }
}
