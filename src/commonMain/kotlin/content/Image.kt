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
