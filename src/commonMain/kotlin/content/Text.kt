package com.xemantic.anthropic.content

import com.xemantic.anthropic.cache.CacheControl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("text")
data class Text(
  val text: String,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null,
) : Content()
