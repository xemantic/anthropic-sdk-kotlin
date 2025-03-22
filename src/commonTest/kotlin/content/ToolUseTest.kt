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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test
import kotlin.test.assertFailsWith

class ToolUseTest {

    @Test
    fun `should create ToolUse`() {
        ToolUse {
            id = "42"
            name = "foo"
            input = buildJsonObject {}
        } should {
            be<ToolUse>()
            have(id == "42")
            have(name == "foo")
            have(input == buildJsonObject {})
            have(cacheControl == null)
        }
    }

    @Test
    fun `should fail to create empty ToolUse`() {
        assertFailsWith<IllegalArgumentException> {
            ToolUse {}
        } should {
            have(message == "id cannot be null")
        }
    }

    @Test
    fun `should return string representation of ToolUse`() {
        ToolUse {
            id = "42"
            name = "foo"
            input = buildJsonObject {}
        }.toString() shouldEqualJson /* language=json */ """
            {
              "type": "tool_use",
              "id": "42",
              "name": "foo",
              "input": {}
            }
        """.trimIndent()
    }

    @Test
    fun `should copy ToolUse`() {
        ToolUse {
            id = "42"
            name = "foo"
            input = buildJsonObject {}
        }.copy() should {
            have(id == "42")
            have(name == "foo")
            have(input == buildJsonObject {})
            have(cacheControl == null)
        }
    }

    @Test
    fun `should copy ToolUse while altering properties`() {
        ToolUse {
            id = "42"
            name = "foo"
            input = buildJsonObject {}
        }.copy {
            id = "43"
            name = "bar"
            input = buildJsonObject {
                put("foo", JsonPrimitive("bar"))
            }
            cacheControl = CacheControl.Ephemeral()
        } should {
            have(id == "43")
            have(name == "bar")
            have(input == buildJsonObject {
                put("foo", JsonPrimitive("bar"))
            })
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

}