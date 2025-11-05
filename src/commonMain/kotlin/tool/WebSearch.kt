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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.location.UserLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("web_search")
class WebSearch private constructor(
    @SerialName("max_uses")
    val maxUses: Int? = null,
    @SerialName("allowed_domains")
    val allowedDomains: List<String>? = null,
    @SerialName("blocked_domains")
    val blockedDomains: List<String>? = null,
    @SerialName("user_location")
    val userLocation: UserLocation? = null
) : BuiltInTool<WebSearch.Input>(
    name = "web_search",
    type = "web_search_20250305"
) {

    /**
     * Input for the web search tool.
     * This class is typically not created directly by users, as Claude generates
     * the search query based on the conversation context.
     *
     * @param query The search query to execute
     */
    @Serializable
    data class Input(
        val query: String
    )

    class Builder {

        var maxUses: Int? = null
        var allowedDomains: List<String>? = null
        var blockedDomains: List<String>? = null
        var userLocation: UserLocation? = null

        fun build(): WebSearch = WebSearch(
            maxUses = maxUses,
            allowedDomains = allowedDomains,
            blockedDomains = blockedDomains,
            userLocation = userLocation
        )

    }

}

fun WebSearch(
    builder: WebSearch.Builder.() -> Unit = {}
): WebSearch = WebSearch.Builder().apply(builder).build()
