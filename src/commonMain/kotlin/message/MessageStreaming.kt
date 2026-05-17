/*
 * Copyright 2025-2026 Xemantic contributors
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
import com.xemantic.ai.anthropic.content.RedactedThinkingBlock
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ThinkingBlock
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.event.Event.*
import com.xemantic.ai.anthropic.usage.Usage
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

suspend fun Flow<Event>.toMessageResponse(): MessageResponse {
    var response: MessageResponse? = null
    val builder = StringBuilder()
    val signatureBuilder = StringBuilder()
    val content = mutableListOf<Content>()
    var currentBlock: ContentBlockStart.ContentBlock? = null
    var messageStopped = false
    collect { event ->
        when (event) {
            is MessageStart -> {
                response = event.message
            }
            is ContentBlockStart -> {
                currentBlock = event.contentBlock
                when (val block = event.contentBlock) {
                    is ContentBlockStart.ContentBlock.Text -> {
                        // actually, the first event seems to always have an empty text
                        builder.append(block.text)
                    }
                    is ContentBlockStart.ContentBlock.Thinking -> {
                        builder.append(block.thinking)
                        block.signature?.let(signatureBuilder::append)
                    }
                    is ContentBlockStart.ContentBlock.ToolUse,
                    is ContentBlockStart.ContentBlock.RedactedThinking -> Unit
                }
            }
            is ContentBlockDelta -> {
                when (event.delta) {
                    is TextDelta -> builder.append(event.delta.text)
                    is InputJsonDelta -> builder.append(event.delta.partialJson)
                    is ThinkingDelta -> builder.append(event.delta.thinking)
                    is SignatureDelta -> signatureBuilder.append(event.delta.signature)
                }
            }
            is ContentBlockStop -> {
                content += when (val block = currentBlock) {
                    is ContentBlockStart.ContentBlock.Text -> Text(builder.toString())
                    is ContentBlockStart.ContentBlock.ToolUse -> ToolUse {
                        id = block.id
                        name = block.name
                        input = Json.decodeFromString<JsonObject>(builder.toString())
                    }
                    is ContentBlockStart.ContentBlock.Thinking -> ThinkingBlock {
                        thinking = builder.toString()
                        signature = signatureBuilder.toString().ifEmpty { null }
                    }
                    is ContentBlockStart.ContentBlock.RedactedThinking -> RedactedThinkingBlock {
                        data = block.data
                    }
                    null -> error("content_block_stop received without a preceding content_block_start")
                }
                builder.clear()
                signatureBuilder.clear()
                currentBlock = null
            }
            is MessageDelta -> {
                val startUsage = response!!.usage
                response = response!!.copy(
                    // Each field in message_delta.usage is the cumulative total
                    // for the response, so we replace (not add). Optional fields
                    // are only sent when they differ from message_start, hence
                    // the fallback to startUsage.
                    usage = Usage {
                        inputTokens = event.usage.inputTokens ?: startUsage.inputTokens
                        outputTokens = event.usage.outputTokens
                        cacheCreationInputTokens = event.usage.cacheCreationInputTokens
                            ?: startUsage.cacheCreationInputTokens
                        cacheReadInputTokens = event.usage.cacheReadInputTokens
                            ?: startUsage.cacheReadInputTokens
                        cacheCreation = startUsage.cacheCreation
                        serverToolUse = event.usage.serverToolUse ?: startUsage.serverToolUse
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
