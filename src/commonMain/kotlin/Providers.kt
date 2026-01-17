/*
 * Copyright 2026 Xemantic contributors
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

package com.xemantic.ai.anthropic

enum class AuthHeaderType {
    /** Uses `x-api-key: {key}` header (Anthropic). */
    X_API_KEY,

    /** Uses `Authorization: Bearer {key}` header (Moonshot, OpenRouter, etc.). */
    BEARER_TOKEN;

    fun createAuthHeader(apiKey: String): Pair<String, String> = when (this) {
        X_API_KEY -> "x-api-key" to apiKey
        BEARER_TOKEN -> "Authorization" to "Bearer $apiKey"
    }
}

interface ApiProvider {
    val id: String
    val apiBase: String
    val authHeaderType: AuthHeaderType
}

enum class StandardApiProvider(
    override val id: String,
    override val apiBase: String,
    override val authHeaderType: AuthHeaderType
) : ApiProvider {

    ANTHROPIC(
        id = "anthropic",
        apiBase = "https://api.anthropic.com/",
        authHeaderType = AuthHeaderType.X_API_KEY
    ),

    MOONSHOT(
        id = "moonshot",
        apiBase = "https://api.moonshot.ai/anthropic/",
        authHeaderType = AuthHeaderType.BEARER_TOKEN
    )

}

data class UnknownApiProvider(
    override val id: String,
    override val apiBase: String,
    override val authHeaderType: AuthHeaderType
) : ApiProvider