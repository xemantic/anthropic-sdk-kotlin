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
