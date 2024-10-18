package com.xemantic.anthropic.tool

import com.xemantic.anthropic.message.CacheControl
import com.xemantic.anthropic.message.Tool
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.schema.Description
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
 */
@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class AnthropicTool(
  val name: String
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

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : UsableTool> toolOf(
  cacheControl: CacheControl? = null // TODO should it be here?
): Tool {

  val serializer = try {
    serializer<T>()
  } catch (e: SerializationException) {
    throw SerializationException(
      "Cannot find serializer for class ${T::class.qualifiedName}, " +
          "make sure that it is annotated with @AnthropicTool and " +
          "kotlin.serialization plugin is enabled for the project",
      e
    )
  }

  val anthropicTool = serializer
    .descriptor
    .annotations
    .filterIsInstance<AnthropicTool>()
    .firstOrNull() ?: throw SerializationException(
      "The class ${T::class.qualifiedName} must be annotated with @AnthropicTool"
    )

  val description = serializer
    .descriptor
    .annotations
    .filterIsInstance<Description>()
    .firstOrNull()
    ?.value

  return Tool(
    name = anthropicTool.name,
    // annotation description cannot be null, so we allow empty and detect it here
    description = description,
    inputSchema = jsonSchemaOf<T>(),
    cacheControl = cacheControl
  )
}
