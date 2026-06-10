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

import com.xemantic.ai.anthropic.content.*
import com.xemantic.ai.anthropic.error.AnthropicApiException
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.event.Event.*
import com.xemantic.ai.anthropic.usage.Usage
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private class BlockBuilder(
    val block: ContentBlockStart.ContentBlock
) {
    val text = StringBuilder()
    val signature = StringBuilder()

    fun toContent(): Content = when (val block = block) {
        is ContentBlockStart.ContentBlock.Text -> Text(text.toString())
        is ContentBlockStart.ContentBlock.ToolUse -> ToolUse {
            id = block.id
            name = block.name
            input = Json.decodeFromString<JsonObject>(text.toString())
        }
        is ContentBlockStart.ContentBlock.Thinking -> ThinkingBlock {
            thinking = text.toString()
            signature = this@BlockBuilder.signature.toString().ifEmpty { null }
        }
        is ContentBlockStart.ContentBlock.RedactedThinking -> RedactedThinkingBlock {
            data = block.data
        }
    }
}

suspend fun Flow<Event>.toMessageResponse(): MessageResponse {
    var response: MessageResponse? = null
    // Built blocks in arrival (start) order — emitted as-is on message_stop.
    // We don't key emission by event.index because some Anthropic-compatible
    // providers (e.g. Kimi via Moonshot) reuse the same index for sequential
    // blocks (e.g. tool_use then text), so the index is not a stable
    // identifier for a content block.
    val builders = mutableListOf<BlockBuilder>()
    // Currently-open builder per index — used only to route deltas/stops
    // back to the correct builder while a block is in-flight. A new
    // content_block_start at an already-open index implicitly closes the
    // previous one (Kimi behavior); we don't remove it from `builders`,
    // we only replace it in `openByIndex`.
    val openByIndex = mutableMapOf<Int, BlockBuilder>()
    var messageStopped = false

    collect { event ->
        when (event) {
            is MessageStart -> {
                response = event.message
            }
            is ContentBlockStart -> {
                val builder = BlockBuilder(event.contentBlock)
                when (val block = event.contentBlock) {
                    is ContentBlockStart.ContentBlock.Text -> {
                        // actually, the first event seems to always have an empty text
                        builder.text.append(block.text)
                    }
                    is ContentBlockStart.ContentBlock.Thinking -> {
                        builder.text.append(block.thinking)
                        block.signature?.let(builder.signature::append)
                    }
                    is ContentBlockStart.ContentBlock.ToolUse,
                    is ContentBlockStart.ContentBlock.RedactedThinking -> Unit
                }
                builders += builder
                openByIndex[event.index] = builder
            }
            is ContentBlockDelta -> {
                val builder = checkNotNull(openByIndex[event.index]) {
                    "content_block_delta for unopened index ${event.index}"
                }
                when (val delta = event.delta) {
                    is TextDelta -> builder.text.append(delta.text)
                    is InputJsonDelta -> builder.text.append(delta.partialJson)
                    is ThinkingDelta -> builder.text.append(delta.thinking)
                    is SignatureDelta -> builder.signature.append(delta.signature)
                }
            }
            is ContentBlockStop -> {
                // Deliberately lenient: an unknown or duplicate index is a
                // no-op. The block is already in `builders` from its start,
                // and stop is only used here to free routing — a missing or
                // out-of-order stop must not drop content (see #147).
                openByIndex.remove(event.index)
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
                    content = builders.map { it.toContent() }
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
