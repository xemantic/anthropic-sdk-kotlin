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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.plusAssign
import com.xemantic.ai.anthropic.tool.test.Calculator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ToolCacheControlTest {

    @Test
    fun `Should cache Calculator tool definition`() = runTest {
        // given
        val mathTools = listOf(
            Tool<Calculator>(
                builder = {
                    cacheControl = CacheControl.Ephemeral()
                }
            ) { calculate() }
        )
        val client = Anthropic {
            logHttp = true
        }
        val conversation = mutableListOf<Message>()
        conversation += "What's 15 multiplied by 7?"

        // when
        val initialResponse = client.messages.create {
            messages = conversation
            tools = mathTools
            toolChoice = ToolChoice.Tool<Calculator>()
        }
        conversation += initialResponse

        // then
        // no indication of cache tokens in use at the moment, probably only big content would be cached
    }

}
