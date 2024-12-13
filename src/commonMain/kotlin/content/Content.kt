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

package com.xemantic.anthropic.content

import com.xemantic.anthropic.cache.CacheControl
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
abstract class Content {

  @SerialName("cache_control")
  abstract val cacheControl: CacheControl?

}

interface ContentBuilder {

  val content: MutableList<Content>

  operator fun Content.unaryPlus() {
    content += this
  }

  operator fun String.unaryPlus() {
    content += Text(this)
  }

  operator fun Collection<Content>.unaryPlus() {
    content += this
  }

}

interface DataBuilder {

  var bytes: ByteArray?

  fun magicNumber(): MagicNumber {
    val bytes = requireNotNull(bytes) {
      "bytes must be provided"
    }
    return requireNotNull(bytes.findMagicNumber()) {
      "provided bytes do not contain any supported format"
    }
  }

  @OptIn(ExperimentalEncodingApi::class)
  fun toBase64(): String = Base64.encode(bytes!!)

}
