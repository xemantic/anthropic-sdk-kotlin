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
            requireNotNull(text) { "text must be provided" },
            cacheControl
        )

    }

}

fun Text(
    block: Text.Builder.() -> Unit
): Text = Text.Builder().apply(block).build()

fun Text(
    text: String,
    block: Text.Builder.() -> Unit = {}
): Text = Text {
    this.text = text
    block(this)
}
