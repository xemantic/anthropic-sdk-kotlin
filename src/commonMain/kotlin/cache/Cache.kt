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

package com.xemantic.ai.anthropic.cache

import com.xemantic.ai.anthropic.json.WithAdditionalProperties
import com.xemantic.ai.anthropic.json.toPrettyJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
abstract class CacheControl : WithAdditionalProperties {

    @Serializable
    @SerialName("ephemeral")
    class Ephemeral private constructor(
        override val additionalProperties: Map<String, JsonElement?>? = null
    ) : CacheControl() {

        class Builder : WithAdditionalProperties.Builder() {

            fun build(): Ephemeral = Ephemeral(
                additionalProperties = additionalProperties
            )

        }

    }

    @Serializable
    class Unknown private constructor(
        val type: String,
        override val additionalProperties: Map<String, JsonElement?>? = null
    ) : CacheControl() {

        class Builder : WithAdditionalProperties.Builder() {

            var type: String? = null

            fun build(): Unknown = Unknown(
                type = requireNotNull(type),
                additionalProperties = additionalProperties
            )

        }

    }

    companion object {

        fun Ephemeral(
            block: Ephemeral.Builder.() -> Unit = {}
        ): Ephemeral = Ephemeral.Builder().apply(block).build()

        fun Unknown(
            block: Unknown.Builder.() -> Unit = {}
        ): Unknown = Unknown.Builder().apply(block).build()

    }

    override fun toString(): String = toPrettyJson()

}
