/*
 * Copyright 2026 Xemantic contributors
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

import com.xemantic.ai.anthropic.event.Event.ContentBlockStart
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class ContentBlockStartSerializationTest {

    @Test
    fun `should deserialize content_block_start with text`() {
        // given
        val json = """
            {
              "type": "content_block_start",
              "index": 0,
              "content_block": {
                "type": "text",
                "text": ""
              }
            }
        """

        // when
        val event = anthropicJson.decodeFromString<Event>(json)

        // then
        event should {
            be<ContentBlockStart>()
            have(index == 0)
            contentBlock should {
                be<ContentBlockStart.ContentBlock.Text>()
                have(text == "")
            }
        }
    }

    @Test
    fun `should deserialize content_block_start with tool_use`() {
        // given
        val json = """
            {
              "type": "content_block_start",
              "index": 1,
              "content_block": {
                "type": "tool_use",
                "id": "toolu_01ABC",
                "name": "get_weather",
                "input": {}
              }
            }
        """

        // when
        val event = anthropicJson.decodeFromString<Event>(json)

        // then
        event should {
            be<ContentBlockStart>()
            have(index == 1)
            contentBlock should {
                be<ContentBlockStart.ContentBlock.ToolUse>()
                have(id == "toolu_01ABC")
                have(name == "get_weather")
                have(input.isEmpty())
            }
        }
    }

    @Test
    fun `should deserialize content_block_start with thinking`() {
        // given
        val json = """
            {
              "type": "content_block_start",
              "index": 0,
              "content_block": {
                "type": "thinking",
                "thinking": ""
              }
            }
        """

        // when
        val event = anthropicJson.decodeFromString<Event>(json)

        // then
        event should {
            be<ContentBlockStart>()
            have(index == 0)
            contentBlock should {
                be<ContentBlockStart.ContentBlock.Thinking>()
                have(thinking == "")
                have(signature == null)
            }
        }
    }

    @Test
    fun `should deserialize content_block_start with thinking and signature`() {
        // given
        val json = """
            {
              "type": "content_block_start",
              "index": 0,
              "content_block": {
                "type": "thinking",
                "thinking": "Step 1...",
                "signature": "sig_abc"
              }
            }
        """

        // when
        val event = anthropicJson.decodeFromString<Event>(json)

        // then
        event should {
            be<ContentBlockStart>()
            have(index == 0)
            contentBlock should {
                be<ContentBlockStart.ContentBlock.Thinking>()
                have(thinking == "Step 1...")
                have(signature == "sig_abc")
            }
        }
    }

    @Test
    fun `should deserialize content_block_start with redacted_thinking`() {
        // given
        val json = """
            {
              "type": "content_block_start",
              "index": 0,
              "content_block": {
                "type": "redacted_thinking",
                "data": "encrypted_redacted_data"
              }
            }
        """

        // when
        val event = anthropicJson.decodeFromString<Event>(json)

        // then
        event should {
            be<ContentBlockStart>()
            have(index == 0)
            contentBlock should {
                be<ContentBlockStart.ContentBlock.RedactedThinking>()
                have(data == "encrypted_redacted_data")
            }
        }
    }

}
