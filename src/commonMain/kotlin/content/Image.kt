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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("image")
data class Image(
  val source: Source,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
) : Content() {

  enum class MediaType {
    @SerialName("image/jpeg")
    IMAGE_JPEG,
    @SerialName("image/png")
    IMAGE_PNG,
    @SerialName("image/gif")
    IMAGE_GIF,
    @SerialName("image/webp")
    IMAGE_WEBP
  }

  @Serializable
  data class Source(
    val type: Type = Type.BASE64,
    @SerialName("media_type")
    val mediaType: MediaType,
    val data: String
  ) {

    enum class Type {
      @SerialName("base64")
      BASE64
    }

  }

  class Builder : DataBuilder {
    override var bytes: ByteArray? = null
    var cacheControl: CacheControl? = null
  }

}

val ByteArray.isImage get() = this.findMagicNumber()?.toImageMediaType() != null

fun MagicNumber.toImageMediaType(): Image.MediaType? = when (this) {
  MagicNumber.JPEG -> Image.MediaType.IMAGE_JPEG
  MagicNumber.PNG -> Image.MediaType.IMAGE_PNG
  MagicNumber.GIF -> Image.MediaType.IMAGE_GIF
  MagicNumber.WEBP -> Image.MediaType.IMAGE_WEBP
  else -> null
}

fun Image(block: Image.Builder.() -> Unit): Image {
  val builder = Image.Builder()
  block(builder)
  val magicNumber = builder.magicNumber()
  val mediaType = requireNotNull(magicNumber.toImageMediaType()) {
    "provided bytes do not contain any supported Image format"
  }
  return Image(
    source = Image.Source(
      mediaType = mediaType,
      data = builder.toBase64()
    )
  )
}
