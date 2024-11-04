package com.xemantic.anthropic.content

import com.xemantic.anthropic.cache.CacheControl
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

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
