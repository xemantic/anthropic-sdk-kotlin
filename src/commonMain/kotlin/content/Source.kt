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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.json.WithAdditionalProperties
import com.xemantic.ai.anthropic.json.toPrettyJson
import com.xemantic.ai.file.magic.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
abstract class Source : WithAdditionalProperties { // TODO cross check with official API, spring AI

  @Serializable
  @SerialName("base64")
  class Base64 private constructor(
    @SerialName("media_type")
    val mediaType: String,
    val data: String,
    override val additionalProperties: Map<String, JsonElement?>? = null
  ) : Source() {

    class Builder : WithAdditionalProperties.Builder() {

      var data: String? = null

      var mediaType: String? = null

      fun mediaType(type: MediaType) {
        mediaType = type.mime
      }

      fun build(): Base64 = Base64(
        mediaType = requireNotNull(mediaType) { "mediaType cannot be null" },
        data = requireNotNull(data) { "data cannot be null" },
        additionalProperties = additionalProperties
      )

    }

  }

  @Serializable
  class Unknown private constructor(
    val type: String,
    override val additionalProperties: Map<String, JsonElement?>? = null
  ) : Source() {

    class Builder : WithAdditionalProperties.Builder() {

      var type: String? = null

      fun build(): Unknown = Unknown(
        type = requireNotNull(type),
        additionalProperties = additionalProperties
      )

    }

  }

  companion object {

    fun Base64(
      block: Base64.Builder.() -> Unit
    ): Base64 = Base64.Builder().apply(block).build()

    fun Unknown(
      block: Unknown.Builder.() -> Unit
    ): Unknown = Unknown.Builder().apply(block).build()

  }

  override fun toString(): String = toPrettyJson()

}
