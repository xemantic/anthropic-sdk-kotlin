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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("web_fetch") // TODO is it needed in the current version?
class WebFetch private constructor(
    @SerialName("max_uses")
    val maxUses: Int? = null,
    @SerialName("allowed_domains")
    val allowedDomains: List<String>? = null,
    @SerialName("blocked_domains")
    val blockedDomains: List<String>? = null,
    val citations: Citations? = null,
    @SerialName("max_content_tokens")
    val maxContentTokens: Int? = null
) : BuiltInTool<WebFetch.Input>(
    name = "web_fetch",
    type = "web_fetch_20250910"
) {

    /**
     * Citations configuration for web fetch results.
     *
     * @param enabled Whether to include source citations in responses
     */
    @Serializable
    data class Citations(
        val enabled: Boolean = true
    )

    /**
     * Input for the web fetch tool.
     * This class is typically not created directly by users, as Claude generates
     * the fetch request based on the conversation context.
     *
     * @param url The URL to fetch content from
     */
    @Serializable
    data class Input(
        val url: String
    )

    class Builder {

        var maxUses: Int? = null
        var allowedDomains: List<String>? = null
        var blockedDomains: List<String>? = null
        var citations: Citations? = null
        var maxContentTokens: Int? = null

        fun build(): WebFetch = WebFetch(
            maxUses = maxUses,
            allowedDomains = allowedDomains,
            blockedDomains = blockedDomains,
            citations = citations,
            maxContentTokens = maxContentTokens
        )

    }

}

fun WebFetch(
    builder: WebFetch.Builder.() -> Unit = {}
): WebFetch = WebFetch.Builder().apply(builder).build()
