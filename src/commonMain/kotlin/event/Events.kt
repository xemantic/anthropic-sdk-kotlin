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

package com.xemantic.ai.anthropic.event

import com.xemantic.ai.anthropic.message.MessageResponse
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.usage.ServerToolUsage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// reference https://docs.spring.io/spring-ai/reference/_images/anthropic-claude3-events-model.jpg

@Serializable
sealed interface Event {

    @Serializable
    @SerialName("message_start")
    data class MessageStart(
        val message: MessageResponse
    ) : Event

    @Serializable
    @SerialName("message_delta")
    data class MessageDelta(
        val delta: Delta,
        val usage: Usage
    ) : Event {

        @Serializable
        data class Delta(
            @SerialName("stop_reason")
            val stopReason: StopReason,
            @SerialName("stop_sequence")
            val stopSequence: String?
        )

        /**
         * Final usage update sent by the API on the last `message_delta` event.
         * All fields are **cumulative** for the response (replace, not add);
         * everything but `output_tokens` is optional and only included by the
         * API when relevant.
         */
        @Serializable
        data class Usage(
            @SerialName("input_tokens")
            val inputTokens: Int? = null,
            @SerialName("output_tokens")
            val outputTokens: Int,
            @SerialName("cache_creation_input_tokens")
            val cacheCreationInputTokens: Int? = null,
            @SerialName("cache_read_input_tokens")
            val cacheReadInputTokens: Int? = null,
            @SerialName("server_tool_use")
            val serverToolUse: ServerToolUsage? = null
        )

    }

    @Serializable
    @SerialName("message_stop")
    class MessageStop : Event {
        override fun toString(): String = "MessageStop"
    }

    @Serializable
    @SerialName("content_block_start")
    data class ContentBlockStart(
        val index: Int,
        @SerialName("content_block")
        val contentBlock: ContentBlock
    ) : Event {

        @Serializable
        sealed class ContentBlock {

            @Serializable
            @SerialName("text")
            data class Text(
                val text: String
            ) : ContentBlock()

            @Serializable
            @SerialName("tool_use")
            data class ToolUse(
                val id: String,
                val name: String,
                val input: JsonObject // this is not being used, because the whole JSON comes in delta
            ) : ContentBlock()

            @Serializable
            @SerialName("thinking")
            data class Thinking(
                val thinking: String
            ) : ContentBlock()

        }

    }

    @Serializable
    @SerialName("content_block_delta")
    data class ContentBlockDelta(
        val index: Int,
        val delta: Delta
    ) : Event {

        @Serializable
        sealed class Delta {

            @Serializable
            @SerialName("text_delta")
            data class TextDelta(
                val text: String
            ) : Delta()

            @Serializable
            @SerialName("input_json_delta")
            data class InputJsonDelta(
                @SerialName("partial_json")
                val partialJson: String
            ) : Delta()

            @Serializable
            @SerialName("thinking_delta")
            data class ThinkingDelta(
                val thinking: String
            ) : Delta()

            @Serializable
            @SerialName("signature_delta")
            data class SignatureDelta(
                val signature: String
            ) : Delta()

        }

    }

    @Serializable
    @SerialName("content_block_stop")
    data class ContentBlockStop(
        val index: Int
    ) : Event

    @Serializable
    @SerialName("ping")
    class Ping : Event {
        override fun toString(): String = "Ping"
    }

    @Serializable
    @SerialName("error")
    data class Error(
        val error: com.xemantic.ai.anthropic.error.Error
    ) : Event

}
