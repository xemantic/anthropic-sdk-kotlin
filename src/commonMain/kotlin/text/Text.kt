package com.xemantic.anthropic.text

import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.message.Content
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("text")
data class Text(
  val text: String,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null,
) : Content()
