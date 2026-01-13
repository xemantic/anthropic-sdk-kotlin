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

import com.xemantic.ai.anthropic.content.Content
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.event.Event.*
import com.xemantic.ai.anthropic.usage.Usage
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

suspend fun Flow<Event>.toMessageResponse(): MessageResponse {
    var response: MessageResponse? = null
    val contentBuilder = StringBuilder()
    val content = mutableListOf<Content>()
    var toolUse: ToolUse? = null
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
                            input = emptyJson
                        }
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
                }
            }
            is ContentBlockStop -> {
                val data = contentBuilder.toString()
                content += toolUse?.copy {
                    input = Json.decodeFromString<JsonObject>(data)
                } ?: Text(data)
                contentBuilder.clear()
                toolUse = null
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

private val emptyJson = buildJsonObject {}
