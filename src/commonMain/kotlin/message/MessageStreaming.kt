/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
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

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.content.Content
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ThinkingBlock
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.event.Event.*
import com.xemantic.ai.anthropic.thinking.ThinkingConfig
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.tool.ToolChoice
import com.xemantic.ai.anthropic.usage.Usage
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

suspend fun Anthropic.Messages.stream(
    model: String,
    messages: List<Message>,
    maxTokens: Int,
    thinking: ThinkingConfig? = null,
    system: List<System>? = null,
    metadata: Metadata? = null,
    stopSequences: List<String>? = null,
    temperature: Double? = null,
    toolChoice: ToolChoice? = null,
    tools: List<Tool>? = null,
    topK: Int? = null,
    topP: Double? = null
): Flow<Event> = stream {
    this.model = model
    this.messages = messages
    this.maxTokens = maxTokens
    this.thinking = thinking
    this.system = system
    this.metadata = metadata
    this.stopSequences = stopSequences ?: emptyList()
    this.temperature = temperature
    this.toolChoice = toolChoice
    this.tools = tools ?: emptyList()
    this.topK = topK
    this.topP = topP
}

suspend fun Flow<Event>.toMessageResponse(): MessageResponse {
    var response: MessageResponse? = null
    val contentBuilder = StringBuilder()
    val content = mutableListOf<Content>()
    var toolUse: ToolUse? = null
    var thinkingBuilder: StringBuilder? = null
    val signatureBuilder = StringBuilder()
    var messageStopped = false
    collect { event ->
        when (event) {
            is MessageStart -> {
                response = event.message
            }
            is ContentBlockStart -> {
                when (event.contentBlock) {
                    is ContentBlockStart.ContentBlock.Text -> {
                        // actually, the first event seems to always have an empty text
                        contentBuilder.append(event.contentBlock.text)
                    }
                    is ContentBlockStart.ContentBlock.ToolUse -> {
                        toolUse = ToolUse {
                            id = event.contentBlock.id
                            name = event.contentBlock.name
                            input = emtpyJson
                        }
                    }
                    is ContentBlockStart.ContentBlock.Thinking -> {
                        thinkingBuilder = StringBuilder()
                        thinkingBuilder!!.append(event.contentBlock.thinking)
                    }
                }
            }
            is ContentBlockDelta -> {
                when (event.delta) {
                    is ContentBlockDelta.Delta.TextDelta -> {
                        contentBuilder.append(event.delta.text)
                    }
                    is ContentBlockDelta.Delta.InputJsonDelta -> {
                        contentBuilder.append(event.delta.partialJson)
                    }
                    is ContentBlockDelta.Delta.ThinkingDelta -> {
                        thinkingBuilder?.append(event.delta.thinking)
                    }
                    is ContentBlockDelta.Delta.SignatureDelta -> {
                        signatureBuilder.append(event.delta.signature)
                    }
                }
            }
            is ContentBlockStop -> {
                val data = contentBuilder.toString()
                content += when {
                    toolUse != null -> toolUse!!.copy {
                        input = Json.decodeFromString<JsonObject>(data)
                    }
                    thinkingBuilder != null -> ThinkingBlock {
                        thinking = thinkingBuilder!!.toString()
                        signature = signatureBuilder.toString()
                    }
                    else -> Text(data)
                }
                contentBuilder.clear()
                signatureBuilder.clear()
                toolUse = null
                thinkingBuilder = null
            }
            is MessageDelta -> {
                response = response!!.copy(
                    usage = response!!.usage + Usage {
                        inputTokens = 0
                        outputTokens = event.usage.outputTokens
                    },
                    stopReason = event.delta.stopReason,
                    stopSequence = event.delta.stopSequence
                )
            }
            is MessageStop -> {
                response = response!!.copy(
                    content = content
                )
                messageStopped = true
            }
            is Ping -> { /* nothing to do */ }
            is Error -> {
                throw AnthropicApiException(
                    error = event.error,
                    httpStatusCode = HttpStatusCode.InternalServerError // Is it a correct error code? It should only fail in runtime.
                )
            }
        }
    }
    check(messageStopped) {
        "No final message_stop event received"
    }
    return response!!
}

private val emtpyJson = buildJsonObject {}
