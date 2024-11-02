package com.xemantic.anthropic.tool.bash

import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.tool.BuiltInTool
import com.xemantic.anthropic.tool.ToolInput
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("bash")
@OptIn(ExperimentalSerializationApi::class)
data class Bash(
  override val cacheControl: CacheControl? = null
) : BuiltInTool(
  name = "bash",
  type = "bash_20241022"
) {

  @Serializable
  data class Input(
    val command: String,
    val restart: Boolean? = false,
  ) : ToolInput() {

    init {
      use {
        TODO("Not yet implemented")
      }
    }

  }

}
