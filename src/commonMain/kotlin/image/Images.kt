package com.xemantic.anthropic.image

import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.message.Content
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

  class Builder {
    var data: ByteArray? = null
    var mediaType: MediaType? = null
    var cacheControl: CacheControl? = null
  }

}

// TODO move image magic here from Claudine to further simplify the API

// TODO write it functional way
fun Image(block: Image.Builder.() -> Unit): Image {
  val builder = Image.Builder()
  block(builder)
  return Image(
    source = Image.Source(
      mediaType = requireNotNull(builder.mediaType) {
        "Image 'mediaType' must be defined"
      },
      data =
        @OptIn(ExperimentalEncodingApi::class)
        Base64.encode(
          requireNotNull(builder.data) {
            "Image 'data' must be defined"
          }
        )
    )
  )
}
