package com.xemantic.anthropic.cache

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CacheControl(
  val type: Type
) {

  enum class Type {
    @SerialName("ephemeral")
    EPHEMERAL
  }

}
