package com.xemantic.anthropic.tool

import com.xemantic.anthropic.anthropicJson
import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.message.Content
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.schema.JsonSchema
import com.xemantic.anthropic.schema.jsonSchemaOf
import com.xemantic.anthropic.text.Text
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MetaSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject
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
@SerialName("tool_use")
data class ToolUse(
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null,
  val id: String,
  val name: String,
  val input: JsonObject
) : Content() {

  @Transient
  @PublishedApi
  internal lateinit var tool: Tool

  @PublishedApi
  internal fun decodeInput() = anthropicJson.decodeFromJsonElement(
    deserializer = tool.inputSerializer,
    element = input
  ).apply(tool.inputInitializer)

  inline fun <reified T : ToolInput> input(): T = (decodeInput() as T)

  suspend fun use(): ToolResult {
    val toolInput = decodeInput()
    return try {
      toolInput.use(toolUseId = id)
    } catch (e: Exception) {
      e.printStackTrace()
      ToolResult(
        toolUseId = id,
        isError = true,
        content = listOf(
          Text(
            text = e.message ?: "Unknown error occurred"
          )
        )
      )
    }
  }

}


@Serializable
@SerialName("tool_result")
data class ToolResult(
  @SerialName("tool_use_id")
  val toolUseId: String,
  val content: List<Content>, // TODO only Text, Image allowed here, should be accessible in gthe builder
  @SerialName("is_error")
  val isError: Boolean = false,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
) : Content()

fun ToolResult(
  toolUseId: String,
  text: String
): ToolResult = ToolResult(
  toolUseId,
  content = listOf(Text(text))
)

inline fun <reified T> ToolResult(
  toolUseId: String,
  value: T
): ToolResult = ToolResult(
  toolUseId,
  content = listOf(
    Text(
      anthropicJson.encodeToString(
        serializer = serializer<T>(),
        value = value
      )
    )
  )
)

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class ToolChoice(
  @SerialName("disable_parallel_tool_use")
  val disableParallelToolUse: Boolean? = false
) {

  @Serializable
  @SerialName("auto")
  class Auto : ToolChoice()

  @Serializable
  @SerialName("any")
  class Any : ToolChoice()

  @Serializable
  @SerialName("tool")
  class Tool(
    val name: String
  ) : ToolChoice()

}
