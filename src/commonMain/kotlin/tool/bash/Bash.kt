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

package com.xemantic.ai.anthropic.tool.bash

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.tool.BuiltInTool
import com.xemantic.ai.anthropic.tool.ToolInput
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
