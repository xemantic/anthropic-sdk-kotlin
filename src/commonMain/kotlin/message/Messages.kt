package com.xemantic.anthropic.message

import com.xemantic.anthropic.Model
import com.xemantic.anthropic.Response
import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.content.Content
import com.xemantic.anthropic.content.ContentBuilder
import com.xemantic.anthropic.tool.Tool
import com.xemantic.anthropic.tool.ToolChoice
import com.xemantic.anthropic.tool.ToolInput
import com.xemantic.anthropic.tool.toolName
import com.xemantic.anthropic.usage.Usage
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.collections.mutableListOf

/**
 * The roles that can be taken by entities in a conversation.
 */
enum class Role {

  @SerialName("user")
  USER,

  @SerialName("assistant")
  ASSISTANT

}

@Serializable
data class Metadata(
  @SerialName("user_id")
  val userId: String
)

@Serializable
data class MessageRequest(
  val model: String? = null, // model has to be off on Vertex AI
  @SerialName("anthropic_version")
  val anthropicVersion: String? = null, // needed by Vertex AI
  val messages: List<Message>,
  @SerialName("max_tokens")
  val maxTokens: Int,
  val metadata: Metadata?,
  @SerialName("stop_sequences")
  val stopSequences: List<String>?,
  val stream: Boolean?,
  val system: List<System>?,
  val temperature: Double?,
  @SerialName("tool_choice")
  val toolChoice: ToolChoice?,
  val tools: List<Tool>?,
  @SerialName("top_k")
  val topK: Int?,
  @SerialName("top_p")
  val topP: Int?
) {

  class Builder internal constructor(
    defaultModel: String,
    defaultMaxTokens: Int,
    private val vertexAi: Boolean,
    @PublishedApi
    internal val toolMap: Map<String, Tool>
  ) {
    var model: String? = if (vertexAi) null else defaultModel
    var maxTokens: Int = defaultMaxTokens
    var messages: List<Message> = emptyList()
    var metadata = null
    var stopSequences: List<String> = emptyList()
    var stream: Boolean? = null
      internal set
    var system: List<System>? = null
    var temperature: Double? = null
    var toolChoice: ToolChoice? = null
    var tools: List<Tool> = emptyList()
    val topK: Int? = null
    val topP: Int? = null

    /**
     * Will fill [tools] with all the tools defined
     * when creating this [com.xemantic.anthropic.Anthropic] client.
     */
    fun allTools() {
      tools = toolMap.values.toList()
    }

    inline fun <reified T : ToolInput> tool() {
      val name = toolName<T>()
      tools += listOf(toolMap[name]!!)
    }

    /**
     * Sets both, the [tools] list and the [toolChoice] with
     * just one tool to use, forcing the API to respond with the [com.xemantic.anthropic.content.ToolUse].
     */
    inline fun <reified T : ToolInput> singleTool() {
      val name = toolName<T>()
      tools = listOf(toolMap[name]!!)
      toolChoice = ToolChoice.Tool(name)
    }

//    inline fun <reified T : BuiltInTool<*>> useBuiltInTool() {
//      this.name
//    }

    /**
     * Sets both, the [tools] list and the [toolChoice] with
     * just one tool to use, forcing the API to respond with the
     * [com.xemantic.anthropic.content.ToolUse] instance.
     */
    fun chooseTool(name: String) {
      val tool = requireNotNull(toolMap[name]) {
        "No tool with such name defined in Anthropic client: $name"
      }
      tools = listOf(tool)
      toolChoice = ToolChoice.Tool(name = tool.name, disableParallelToolUse = true)
    }

    fun messages(vararg messages: Message) {
      this.messages += messages.toList()
    }

    operator fun Message.unaryPlus() {
      messages += this
    }

    operator fun List<Message>.unaryPlus() {
      messages += this
    }

    fun stopSequences(vararg stopSequences: String) {
      this.stopSequences += stopSequences.toList()
    }

    fun system(
      text: String
    ) {
      system = listOf(System(text = text))
    }

    fun build(): MessageRequest = MessageRequest(
      model = model,
      anthropicVersion = if (vertexAi) "vertex-2023-10-16" else model,
      maxTokens = maxTokens,
      messages = messages,
      metadata = metadata,
      stopSequences = stopSequences.toNullIfEmpty(),
      stream = if ((stream != null) && stream!!) true else null,
      system = system,
      temperature = temperature,
      toolChoice = toolChoice,
      tools = tools.toNullIfEmpty(),
      topK = topK,
      topP = topP
    )
  }

}

/**
 * Used only in tests
 */
internal fun MessageRequest(
  model: Model = Model.DEFAULT,
  toolMap: Map<String, Tool> = emptyMap(),
  block: MessageRequest.Builder.() -> Unit
): MessageRequest {
  val builder = MessageRequest.Builder(
    defaultModel = model.id,
    defaultMaxTokens = model.maxOutput,
    vertexAi = true,
    toolMap = toolMap
  )
  block(builder)
  return builder.build()
}

@Serializable
data class Message(
  val role: Role,
  val content: List<Content>
) {

  class Builder : ContentBuilder {

    override val content = mutableListOf<Content>()

    var role = Role.USER

    fun build() = Message(
      role = role,
      content = content
    )
  }

}

fun Message(block: Message.Builder.() -> Unit): Message {
  val builder = Message.Builder()
  block(builder)
  return builder.build()
}

@Serializable
data class System(
  @SerialName("cache_control")
  val cacheControl: CacheControl? = null,
  val type: Type = Type.TEXT,
  val text: String? = null,
) {

  enum class Type {
    @SerialName("text")
    TEXT
  }

}

enum class StopReason {
  @SerialName("end_turn")
  END_TURN,
  @SerialName("max_tokens")
  MAX_TOKENS,
  @SerialName("stop_sequence")
  STOP_SEQUENCE,
  @SerialName("tool_use")
  TOOL_USE
}

operator fun MutableCollection<in Message>.plusAssign(
  response: MessageResponse
) {
  this += response.asMessage()
}

@Serializable
@SerialName("message")
data class MessageResponse(
  val id: String,
  val role: Role,
  val content: List<Content>, // limited to Text and ToolUse
  val model: String,
  @SerialName("stop_reason")
  val stopReason: StopReason?,
  @SerialName("stop_sequence")
  val stopSequence: String?,
  val usage: Usage
) : Response(type = "message") {

  fun asMessage(): Message = Message {
    role = Role.ASSISTANT
    content += this@MessageResponse.content
  }

}
