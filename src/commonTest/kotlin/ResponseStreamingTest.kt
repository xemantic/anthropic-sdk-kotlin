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

package com.xemantic.ai.anthropic

import com.xemantic.ai.anthropic.event.Delta.TextDelta
import com.xemantic.ai.anthropic.event.Event
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.kotlin.test.assert
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResponseStreamingTest {

    @Test
    fun `should stream the response`() = runTest {
        // given
        val client = Anthropic()

        // when
        val chunkedResponse = client.messages.stream {
            +Message { +"Say: 'The sun slowly dipped below the horizon, painting the sky in a breathtaking array of oranges, pinks, and purples.'" }
        }
            .filterIsInstance<Event.ContentBlockDelta>()
            .map { (it.delta as TextDelta).text }
            .toList()
            .joinToString(separator = "|")

        // then
        println("chunked response: $chunkedResponse")
        val response = chunkedResponse.replace("|", "")
        assert(response == "The sun slowly dipped below the horizon, painting the sky in a breathtaking array of oranges, pinks, and purples.")
    }

}
