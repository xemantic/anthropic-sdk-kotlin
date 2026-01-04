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

package com.xemantic.ai.anthropic

import com.xemantic.ai.anthropic.json.toPrettyJson
import kotlinx.serialization.Serializable

/**
 * Base class for all API responses.
 *
 * The `type` field defaults to "error" to handle provider compatibility:
 * - Anthropic API returns: `{ "type": "error", "error": {...} }`
 * - Some compatible providers (e.g., Moonshot) omit the `type` field: `{ "error": {...} }`
 *
 * When the `type` field is missing from the JSON response, kotlinx.serialization
 * will use the default value. Since responses without an explicit type typically
 * occur in error scenarios, defaulting to "error" provides graceful handling
 * of non-compliant but compatible API providers.
 *
 * Simply put, if the response doesn't have a valid `type` it should be treated
 * like error anyway.
 */
@Serializable
abstract class Response(val type: String = "error") {

    override fun toString() = toPrettyJson()

}
