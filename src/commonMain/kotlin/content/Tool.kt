package com.xemantic.anthropic.content

import com.xemantic.anthropic.anthropicJson
import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.message.Content
import com.xemantic.anthropic.message.toNullIfEmpty
import com.xemantic.anthropic.tool.Tool
import com.xemantic.anthropic.tool.ToolInput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlin.collections.plus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
@SerialName("tool_use")
data class ToolUse(
  val id: String,
  val name: String,
  val input: JsonObject,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
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
    return try {
      if (::tool.isInitialized) {
        val toolInput = decodeInput()
        toolInput.use(toolUseId = id)
      } else {
        ToolResult(toolUseId = id) {
          error("Cannot use unknown tool: $name")
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ToolResult(toolUseId = id) {
        error(e.message ?: "Unknown error occurred")
      }
    }
  }

}

@Serializable
@SerialName("tool_result")
data class ToolResult(
  @SerialName("tool_use_id")
  val toolUseId: String,
  val content: List<Content>? = null,
  @SerialName("is_error")
  val isError: Boolean? = false,
  @SerialName("cache_control")
  override val cacheControl: CacheControl? = null
) : Content() {

  class Builder {

    var content: List<Content> = emptyList()
    var isError: Boolean? = null
    var cacheControl: CacheControl? = null

    fun content(message: String) {
      content = listOf(Text(text = message))
    }

    fun error(message: String) {
      content(message)
      isError = true
    }

    operator fun List<Content>.unaryPlus() {
      content += this
    }

    operator fun String.unaryPlus() {
      content(this)
    }

  }

}

@OptIn(ExperimentalContracts::class)
fun ToolResult(
  toolUseId: String,
  block: ToolResult.Builder.() -> Unit = {}
): ToolResult {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val builder = ToolResult.Builder()
  builder.apply(block)
  return ToolResult(
    toolUseId = toolUseId,
    content = builder.content.toNullIfEmpty(),
    isError = if (builder.isError == null) false else null,
    cacheControl = builder.cacheControl
  )
}
