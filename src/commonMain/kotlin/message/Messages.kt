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

package com.xemantic.ai.anthropic.message

import com.xemantic.ai.anthropic.AnthropicModel
import com.xemantic.ai.anthropic.Model
import com.xemantic.ai.anthropic.Response
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.content.*
import com.xemantic.ai.anthropic.cost.CostWithUsage
import com.xemantic.ai.anthropic.json.toPrettyJson
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.tool.ToolChoice
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.kotlin.core.collections.mapLast
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
    val model: String,
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
    val topP: Double?
) {

    class Builder() {
        var model: String? = null
        var maxTokens: Int? = null
        var messages: List<Message> = emptyList()
        var metadata = null
        var stopSequences: List<String> = emptyList()
        var stream: Boolean? = null
            internal set
        var system: List<System>? = null
        var temperature: Double? = null
        var toolChoice: ToolChoice? = null
        var tools: List<Tool> = emptyList()
        var topK: Int? = null
        var topP: Double? = null

        fun messages(vararg messages: Message) {
            this.messages += messages.toList()
        }

        operator fun Message.unaryPlus() {
            messages += this
        }

        operator fun List<Message>.unaryPlus() {
            messages += this
        }

        operator fun String.unaryPlus() {
            messages += Message {
                +this@unaryPlus
            }
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
            model = requireNotNull(model) { "model must be specified" },
            maxTokens = requireNotNull(maxTokens) { "maxTokens must be specified" },
            messages = messages,
            metadata = metadata,
            stopSequences = stopSequences.ifEmpty { null },
            stream = if ((stream != null) && stream!!) true else null,
            system = system,
            temperature = temperature,
            toolChoice = toolChoice,
            tools = tools.ifEmpty { null },
            topK = topK,
            topP = topP
        )

    }

    override fun toString(): String = toPrettyJson()

}

/**
 * Used only in tests
 */
@OptIn(ExperimentalContracts::class)
internal fun MessageRequest(
    model: Model = Model.DEFAULT,
    block: MessageRequest.Builder.() -> Unit
): MessageRequest {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return MessageRequest.Builder().also {
        it.model = model.id
        it.maxTokens = model.maxOutput
        block(it)
    }.build()
}

@Serializable
class Message private constructor(
    val role: Role,
    val content: List<Content>
) {

    class Builder : ContentListBuilder() {

        var role = Role.USER

        fun build() = Message(
            role = role,
            content = content
        )
    }

    @OptIn(ExperimentalContracts::class)
    fun copy(
        block: Builder.() -> Unit = {}
    ): Message {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return Builder().also {
            it.role = role
            it.content = content
            block(it)
        }.build()
    }

    override fun toString(): String = toPrettyJson()

}

@OptIn(ExperimentalContracts::class)
fun Message(
    block: Message.Builder.() -> Unit = {}
): Message {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Message.Builder().apply(block).build()
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

operator fun MutableCollection<Message>.plusAssign(
    text: String
) {
    this += Message { +text }
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

    @Transient
    internal lateinit var resolvedModel: AnthropicModel

    fun asMessage(): Message = Message {
        role = Role.ASSISTANT
        content += this@MessageResponse.content
    }

    suspend fun useTools(): Message {
        check(stopReason == StopReason.TOOL_USE) {
            "You can only use tools if the stopReason is TOOL_USE"
        }
        val builder = ToolResultMessageBuilder()
        content.filterIsInstance<ToolUse>().map { toolUse ->
            builder.add(toolUse.use())
        }
        return builder.build()
    }

    val text: String?
        get() = content.filterIsInstance<Text>().run {
            if (isEmpty()) null else joinToString("\n") { it.text }
        }

    val toolUse: ToolUse?
        get() = toolUses.run {
            if (isEmpty()) null else first()
        }

    val toolUses: List<ToolUse> get() = content.filterIsInstance<ToolUse>()

    val requestsToolUse: Boolean get() = stopReason == StopReason.TOOL_USE

    val costWithUsage: CostWithUsage get() = CostWithUsage(
        cost = resolvedModel.cost * usage,
        usage = usage
    )

}

class ToolResultMessageBuilder {

    private val documents = mutableListOf<Document>()

    private val toolResults = mutableListOf<ToolResult>()

    fun add(toolResult: ToolResult) {
        val toolResult = toolResult.copy {
            content = content.map { contentElement ->
                if (contentElement is Document) {
                    documents += contentElement
                    Text("Document tool_result added as separate content to the message")
                } else {
                    contentElement
                }
            }
        }
        toolResults += toolResult
    }

    fun build(): Message = Message {
        content += (toolResults + documents)
    }

}

fun List<Message>.addCacheBreakpoint(): List<Message> = mapLast { message ->
    message.copy {
        content = content.mapLast { contentElement ->
            contentElement.alterCacheControl(
                CacheControl.Ephemeral()
            )
        }
    }
}
