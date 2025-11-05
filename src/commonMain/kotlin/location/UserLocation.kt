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

package com.xemantic.ai.anthropic.location

import com.xemantic.ai.anthropic.json.WithAdditionalProperties
import com.xemantic.ai.anthropic.json.toPrettyJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * User location information for search localization.
 */
@Serializable
abstract class UserLocation : WithAdditionalProperties {

    /**
     * Approximate location based on city, region, country, and timezone.
     *
     * @param city The user's city (optional)
     * @param region The user's region/state (optional)
     * @param country The user's country code (optional)
     * @param timezone The user's timezone (optional)
     */
    @Serializable
    @SerialName("approximate")
    class Approximate private constructor(
        val city: String? = null,
        val region: String? = null,
        val country: String? = null,
        val timezone: String? = null,
        override val additionalProperties: Map<String, JsonElement?>? = null
    ) : UserLocation() {

        class Builder : WithAdditionalProperties.Builder() {

            var city: String? = null
            var region: String? = null
            var country: String? = null
            var timezone: String? = null

            fun build(): Approximate = Approximate(
                city = city,
                region = region,
                country = country,
                timezone = timezone,
                additionalProperties = additionalProperties
            )

        }

    }

    /**
     * Unknown user location type for future extension.
     *
     * @param type The type of the location
     */
    @Serializable
    class Unknown private constructor(
        val type: String,
        override val additionalProperties: Map<String, JsonElement?>? = null
    ) : UserLocation() {

        class Builder : WithAdditionalProperties.Builder() {

            var type: String? = null

            fun build(): Unknown = Unknown(
                type = requireNotNull(type),
                additionalProperties = additionalProperties
            )

        }

    }

    companion object {

        fun Approximate(
            block: Approximate.Builder.() -> Unit = {}
        ): Approximate = Approximate.Builder().apply(block).build()

        fun Unknown(
            block: Unknown.Builder.() -> Unit = {}
        ): Unknown = Unknown.Builder().apply(block).build()

    }

    override fun toString(): String = toPrettyJson()

}
