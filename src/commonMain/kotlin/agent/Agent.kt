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

package com.xemantic.ai.anthropic.agent

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.collections.transformLast
import com.xemantic.ai.anthropic.content.Content
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.MessageResponse
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.tool.Tool

/**
 * This is highly experimental work in progress code. Most likely
 * it will not deliver what you are looking for.
 */
class Agent(
    private val anthropic: Anthropic = Anthropic(),
    val maxAgentLoopCount: Int = -1,
    val responseReceiver: (MessageResponse) -> Unit = {},
    val confirmToolUse: (ToolUse) -> String = { "yes" },
    private val tools: List<Tool>
) {

    class Builder {
        var anthropic: Anthropic? = null
        fun anthropic(block: Anthropic.Config.() -> Unit) {

        }
        var tools: List<Tool>? = null
        fun tools(vararg tools: Tool) {
            this.tools = tools.toList()
        }
    }

    suspend fun ask(
        input: String
    ): AgentResponse {
        val conversation = mutableListOf<Message>()
        conversation += Message {
            +Text(input) {
                cacheControl = CacheControl.Ephemeral()
            }
        }
        var response: MessageResponse
        do {
            response = anthropic.messages.create {
                tools = this@Agent.tools
                messages = conversation.transformLast {
                    copy {
                        content = content.transformLast {
                            alterCacheControl(
                                CacheControl.Ephemeral()
                            )
                        }
                    }
                }
            }
            conversation += response
            if (response.stopReason == StopReason.TOOL_USE) {
                conversation += response.useTools()
            }
        } while (
            response.stopReason == StopReason.TOOL_USE
        )
        return AgentResponse(
            response.content
        )
    }

}

class AgentResponse internal constructor(
    val content: List<Content>
)

