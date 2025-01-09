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

package com.xemantic.ai.anthropic.json

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.collections.set

// probably should be moved to a common project at some point

interface WithAdditionalProperties {

  val additionalProperties: Map<String, JsonElement?>?

  abstract class Builder {

    var additionalProperties: MutableMap<String, JsonElement?> = mutableMapOf()

  }

}

operator fun MutableMap<String, JsonElement?>.set(
  key: String, value: Boolean?
) = set(key, JsonPrimitive(value))

operator fun MutableMap<String, JsonElement?>.set(
  key: String, value: Number?
) = set(key, JsonPrimitive(value))

operator fun MutableMap<String, JsonElement?>.set(
  key: String, value: String?
) = set(key, JsonPrimitive(value))

operator fun MutableMap<String, JsonElement?>.set(
  key: String, @Suppress("unused") value: Nothing?
) = set(key, JsonNull)
