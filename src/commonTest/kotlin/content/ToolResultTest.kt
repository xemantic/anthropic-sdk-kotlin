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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ToolResultTest {

    @Test
    fun `should create ToolResult for a single String representing Text content`() {
        ToolResult {
            toolUseId = "42"
            +"foo"
        } should {
            be<ToolResult>()
            have(toolUseId == "42")
            have(content!!.size == 1)
            content[0] should {
                be<Text>()
                have(text == "foo")
            }
            have(isError == null)
            have(cacheControl == null)
        }
    }

    @Test
    fun `should create ToolResult for Text element representing content`() {
        ToolResult {
            toolUseId = "42"
            +Text(text = "foo")
        } should {
            be<ToolResult>()
            have(toolUseId == "42")
            have(content!!.size == 1)
            content[0] should {
                be<Text>()
                have(text == "foo")
            }
            have(isError == null)
            have(cacheControl == null)
        }
    }

    @Test
    fun `should create error ToolResult`() {
        ToolResult {
            toolUseId = "42"
            error("Error message")
        } should {
            be<ToolResult>()
            have(toolUseId == "42")
            have(content!!.size == 1)
            content[0] should {
                be<Text>()
                have(text == "Error message")
            }
            have(isError == true)
            have(cacheControl == null)
        }
    }

    @Test
    fun `should fail to create empty ToolResult`() {
        assertFailsWith<IllegalArgumentException> {
            ToolResult {}
        } should {
            have(message == "toolUseId cannot be null")
        }
    }

    @Test
    fun `should return string representation of ToolResult`() {
        ToolResult {
            toolUseId = "42"
            content = listOf(Text("foo"))
            isError = true
            cacheControl = CacheControl.Ephemeral()
        }.toString() shouldEqualJson /* language=json */ """
            {
              "type": "tool_result",
              "tool_use_id": "42",
              "content": [
                {
                  "type": "text",
                  "text": "foo"
                }
              ],
              "is_error": true,
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """.trimIndent()
    }

    @Test
    fun `should copy ToolResult`() {
        ToolResult {
            toolUseId = "42"
            content = listOf(Text("foo"))
            isError = true
            cacheControl = CacheControl.Ephemeral()
        }.copy() should {
            have(toolUseId == "42")
            content!![0] should {
                be<Text>()
                have(text == "foo")
            }
            have(isError == true)
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy ToolResult while altering properties`() {
        ToolResult {
            toolUseId = "42"
            content = listOf(Text("foo"))
            isError = true
            cacheControl = CacheControl.Ephemeral()
        }.copy {
            toolUseId = "43"
            content = emptyList()
            isError = null
            cacheControl = null
        } should {
            have(toolUseId == "43")
            have(content == null)
            have(isError == null)
            have(cacheControl == null)
        }
    }

}