package com.xemantic.anthropic.tool

import com.xemantic.anthropic.message.CacheControl
import com.xemantic.anthropic.message.Tool
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.schema.jsonSchemaOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

/**
 * Annotation used to mark a class extending the [UsableTool].
 *
 * This annotation provides metadata for tools that can be serialized and used in the context
 * of the Anthropic API. It includes a name and description for the tool.
 *
 * @property name The name of the tool. This name is used during serialization and should be a unique identifier for the tool.
 * @property description A comprehensive description of what the tool does and how it should be used.
 */
@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class AnthropicTool(
  val name: String,
  val description: String
)

/**
 * Interface for tools that can be used in the context of the Anthropic API.
 *
 * Classes implementing this interface represent tools that can be executed
 * with a given tool use ID. The implementation of the [use] method should
 * contain the logic for executing the tool and returning the [ToolResult].
 */
interface UsableTool {

  /**
   * Executes the tool and returns the result.
   *
   * @param toolUseId A unique identifier for this particular use of the tool.
   * @return A [ToolResult] containing the outcome of executing the tool.
   */
  suspend fun use(toolUseId: String): ToolResult

}

fun Tool.cacheControl(
  cacheControl: CacheControl? = null
): Tool = if (cacheControl == null) this else Tool(
  name,
  description,
  inputSchema,
  cacheControl
)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : UsableTool> toolOf(
  cacheControl: CacheControl? = null // TODO should it be here?
): Tool {

  val serializer = try {
    serializer<T>()
  } catch (e :SerializationException) {
    throw SerializationException(
      "The class ${T::class.qualifiedName} must be annotated with @SerializableTool", e
    )
  }

  val anthropicTool = serializer
    .descriptor
    .annotations
    .filterIsInstance<AnthropicTool>()
    .firstOrNull() ?: throw SerializationException(
      "The class ${T::class.qualifiedName} must be annotated with @SerializableTool",
    )

  return Tool(
    name = anthropicTool.name,
    description = anthropicTool.description,
    inputSchema = jsonSchemaOf<T>(),
    cacheControl = cacheControl
  )
}
