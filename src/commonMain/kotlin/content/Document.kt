package com.xemantic.anthropic.content

import com.xemantic.anthropic.cache.CacheControl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
@SerialName("document")
data class Document(
  val source: Source,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
) : Content() {

  enum class MediaType {
    @SerialName("application/pdf")
    APPLICATION_PDF
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

val ByteArray.isDocument get() = this.findMagicNumber()?.toDocumentMediaType() != null

fun MagicNumber.toDocumentMediaType(): Document.MediaType? = when (this) {
  MagicNumber.PDF -> Document.MediaType.APPLICATION_PDF
  else -> null
}

fun Document(block: Document.Builder.() -> Unit): Document {
  val builder = Document.Builder()
  block(builder)
  val magicNumber = builder.magicNumber()
  val mediaType = requireNotNull(magicNumber.toDocumentMediaType()) {
    "provided bytes do not contain supported Document format"
  }
  @OptIn(ExperimentalEncodingApi::class)
  return Document(
    source = Document.Source(
      mediaType = mediaType,
      data = builder.toBase64()
    )
  )
}
