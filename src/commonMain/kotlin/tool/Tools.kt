package com.xemantic.anthropic.tool

import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.schema.JsonSchema
import com.xemantic.anthropic.schema.jsonSchemaOf
import com.xemantic.anthropic.content.ToolResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MetaSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.serializer

@Serializable
@JsonClassDiscriminator("name")
@OptIn(ExperimentalSerializationApi::class)
abstract class Tool {

  abstract val name: String
  abstract val description: String?
  abstract val inputSchema: JsonSchema?
  abstract val cacheControl: CacheControl?

  @Transient
  @PublishedApi
  internal lateinit var inputSerializer: KSerializer<out ToolInput>

  @Transient
  @PublishedApi
  internal lateinit var inputInitializer: ToolInput.() -> Unit

  inline fun <reified T: ToolInput> initialize(
    noinline block: T.() -> Unit
  ) {
    @Suppress("UNCHECKED_CAST")
    inputInitializer = block as ToolInput.() -> Unit
  }

}

@Serializable
@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal data class DefaultTool(
  override val name: String,
  override val description: String? = null,
  @SerialName("input_schema")
  override val inputSchema: JsonSchema? = null,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
) : Tool()

@Serializable
@OptIn(ExperimentalSerializationApi::class)
abstract class BuiltInTool(
  override val name: String,
  val type: String,
  override val description: String? = null,
  @SerialName("input_schema")
  override val inputSchema: JsonSchema? = null,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
) : Tool()

/**
 * Interface for tools that can be used in the context of the Anthropic API.
 *
 * Classes implementing this interface represent tools that can be executed
 * with a given tool use ID. The implementation of the [use] method should
 * contain the logic for executing the tool and returning the [ToolResult].
 */
interface ToolInput {

  /**
   * Executes the tool and returns the result.
   *
   * @param toolUseId A unique identifier for this particular use of the tool.
   * @return A [ToolResult] containing the outcome of executing the tool.
   */
  suspend fun use(toolUseId: String): ToolResult

}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : ToolInput> toolName(): String = serializer<T>().name()

@OptIn(ExperimentalSerializationApi::class)
@PublishedApi
internal inline fun <reified T : ToolInput> KSerializer<T>.name() = (
    descriptor
      .annotations
      .filterIsInstance<AnthropicTool>()
      .firstOrNull() ?: throw SerializationException(
        "The ${T::class} must be annotated with @AnthropicTool"
      )
).name

/**
 * Annotation used to mark a class extending the [ToolInput].
 *
 * This annotation provides metadata for tools that can be serialized and used in the context
 * of the Anthropic API. It includes a name and description for the tool.
 *
 * @property name The name of the tool. This name is used during serialization and should be a unique identifier for the tool.
 */
@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class AnthropicTool(
  val name: String
)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : ToolInput> Tool(
  cacheControl: CacheControl? = null,
  noinline initializer: T.() -> Unit = {}
): Tool {

  val serializer = try {
    serializer<T>()
  } catch (e: SerializationException) {
    throw SerializationException(
      "Cannot find serializer for ${T::class}, " +
          "make sure that it is annotated with @AnthropicTool and " +
          "kotlin.serialization plugin is enabled for the project",
      e
    )
  }

  val toolName = toolName<T>()
  val description = serializer
    .descriptor
    .annotations
    .filterIsInstance<Description>()
    .firstOrNull()
    ?.value

  return DefaultTool(
    name = toolName,
    description = description,
    inputSchema = jsonSchemaOf<T>(),
    cacheControl = cacheControl
  ).apply {
    @Suppress("UNCHECKED_CAST")
    inputSerializer = serializer as KSerializer<ToolInput>
    @Suppress("UNCHECKED_CAST")
    inputInitializer = initializer as ToolInput.() -> Unit
  }

}

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class ToolChoice {

  abstract val disableParallelToolUse: Boolean?

  @Serializable
  @SerialName("auto")
  class Auto(
    @SerialName("disable_parallel_tool_use")
    override val disableParallelToolUse: Boolean? = null
  ) : ToolChoice()

  @Serializable
  @SerialName("any")
  class Any(
    @SerialName("disable_parallel_tool_use")
    override val disableParallelToolUse: Boolean? = null
  ) : ToolChoice()

  @Serializable
  @SerialName("tool")
  class Tool(
    val name: String,
    @SerialName("disable_parallel_tool_use")
    override val disableParallelToolUse: Boolean? = null
  ) : ToolChoice()

}
