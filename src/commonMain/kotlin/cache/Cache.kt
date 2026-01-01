/*
 * Copyright 2024-2026 Xemantic contributors
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Serializable
abstract class CacheControl : WithAdditionalProperties {

    @Serializable
    @SerialName("ephemeral")
    class Ephemeral private constructor(
        val ttl: TTL? = null,
        override val additionalProperties: Map<String, JsonElement?>? = null
    ) : CacheControl() {

        @Serializable
        enum class TTL(val duration: Duration) {
            @SerialName("5m")
            FIVE_MINUTES(5.minutes),
            @SerialName("1h")
            ONE_HOUR(1.hours)
        }

        class Builder : WithAdditionalProperties.Builder() {

            var ttl: TTL? = null

            fun build(): Ephemeral = Ephemeral(
                ttl = ttl,
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
