/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.tool.schema.generator.jsonSchemaOf
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.json.ToolSerializer
import com.xemantic.ai.anthropic.json.toPrettyJson
import com.xemantic.ai.tool.schema.JsonSchema
import com.xemantic.ai.tool.schema.ObjectSchema
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.serializer

@Serializable(with = ToolSerializer::class)
abstract class Tool {

  abstract val name: String
  abstract val description: String?
  abstract val inputSchema: JsonSchema?
  abstract val cacheControl: CacheControl?

  @Transient
  @PublishedApi
  internal lateinit var inputSerializer: KSerializer<*>

  @Transient
  @PublishedApi
  internal lateinit var runner: suspend (input: Any) -> Any?

  override fun toString(): String = toPrettyJson()

}

@Serializable
class DefaultTool private constructor(
  override val name: String,
  override val description: String? = null,
  @SerialName("input_schema")
  override val inputSchema: JsonSchema? = null,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
) : Tool() {

  class Builder {

    var name: String? = null
    var description: String? = null
    var inputSchema: JsonSchema? = null
    var cacheControl: CacheControl? = null

    fun build(): DefaultTool = DefaultTool(
      requireNotNull(name) { "Tool must have a 'name'" },
      description,
      inputSchema,
      cacheControl
    )

  }

}

@Serializable
abstract class BuiltInTool(
  override val name: String,
  val type: String
) : Tool() {

  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null

  @SerialName("input_schema")
  override val inputSchema: JsonSchema? = null

  override val description: String? = null

}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> toolName(): String =
  serializer<T>().descriptor.serialName.normalizedToolName

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Tool(
  name: String? = toolName<T>(),
  description: String? = null,
  builder: DefaultTool.Builder.() -> Unit = {},
  noinline run: suspend T.() -> Any? = { "ok" }
): Tool {

  val schema = jsonSchemaOf<T>()
  require(schema is ObjectSchema) {
    "Tool input class must be an object"
  }

  return DefaultTool.Builder().apply {
    this.name = name
    this.description = schema.description ?: description
    inputSchema = schema.copy {
      this.description = null
    }
  }.also(builder).build().apply {
    inputSerializer = serializer<T>()
    runner = { input -> run(input as T) }
  }

}

@Serializable
sealed class ToolChoice {

  abstract val disableParallelToolUse: Boolean?

  @Serializable
  @SerialName("auto")
  class Auto private constructor(
    @SerialName("disable_parallel_tool_use")
    override val disableParallelToolUse: Boolean? = null
  ) : ToolChoice() {

    class Builder {

      var disableParallelToolUse: Boolean? = null

      fun build(): Auto = Auto(
        disableParallelToolUse = disableParallelToolUse
      )

    }

  }

  @Serializable
  @SerialName("any")
  class Any private constructor(
    @SerialName("disable_parallel_tool_use")
    override val disableParallelToolUse: Boolean? = null
  ) : ToolChoice() {

    class Builder {

      var disableParallelToolUse: Boolean? = null

      fun build(): Any = Any(
        disableParallelToolUse = disableParallelToolUse
      )

    }

  }

  @Serializable
  @SerialName("tool")
  class Tool private constructor(
    val name: String,
    @SerialName("disable_parallel_tool_use")
    override val disableParallelToolUse: Boolean? = null
  ) : ToolChoice() {

    class Builder {

      var name: String? = null
      var disableParallelToolUse: Boolean? = null

      fun build(): Tool = Tool(
        name = requireNotNull(name) { "name cannot be null" },
        disableParallelToolUse = disableParallelToolUse
      )

    }

  }

  companion object {

    fun Auto(
      block: Auto.Builder.() -> Unit = {}
    ): Auto = Auto.Builder().also(block).build()

    fun Any(
      block: Any.Builder.() -> Unit = {}
    ): Any = Any.Builder().also(block).build()

    fun Tool(
      name: String,
      block: Tool.Builder.() -> Unit = {}
    ): Tool = Tool.Builder().also {
      it.name = name
      block(it)
    }.build()

    inline fun <reified T> Tool(
        noinline block: Tool.Builder.() -> Unit = {}
    ): Tool = Tool(toolName<T>(), block)

  }

}

@PublishedApi
internal val String.normalizedToolName: String
  get() = trimEnd { it == '$' }
    .replace('.', '_')
    .replace('$', '_')
    .take(64)
