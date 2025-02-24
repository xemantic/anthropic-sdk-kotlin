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

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * This test is verifying various ways of defining tools and using them
 * with the [com.xemantic.ai.anthropic.Anthropic] client.
 */
class ToolDefinitionsTest {

    @Serializable
    class Foo(val bar: String)

    @Test
    fun `should define tool without runner`() = runTest {
        val tool = Tool<Foo>()
        tool should {
            have(name.endsWith("Foo"))
            have(description == null)
            have(cacheControl == null)
        }
        assert(tool.runner(Foo("buzz")) == "ok") // ok is the standard result
    }

    @Test
    fun `should define tool with runner`() = runTest {
        val tool = Tool<Foo> {
            "result: $bar"
        }
        tool should {
            have(name.endsWith("Foo"))
            have(description == null)
            have(cacheControl == null)
        }
        val result = tool.runner(Foo("buzz"))
        assert(result == "result: buzz")
    }

    @Test
    fun `should define 2 tools of the same type with different names and runners`() = runTest {
        val tool1 = Tool<Foo>("tool1") {
            "tool1: $bar"
        }
        val tool2 = Tool<Foo>("tool2") {
            "tool2: $bar"
        }
        val foo = Foo("buzz")
        val result1 = tool1.runner(foo)
        val result2 = tool2.runner(foo)
        assert(result1 == "tool1: buzz")
        assert(result2 == "tool2: buzz")
    }

    /**
     * Let's start with defining a test tool used later in the tests.
     */
    @SerialName("message_repeater")
    @Description("A test tool receiving a message and outputting it back")
    class MessageRepeater(
        @Description("the message")
        val message: String
    )

    @Test
    fun `should create a tool instance from the test tool annotated with description`() {
        // when
        val tool = Tool<MessageRepeater> {
            message
        }

        tool should {
            have(name == "message_repeater")
            have(description == "A test tool receiving a message and outputting it back")
            have(cacheControl == null)
            inputSchema.toString() shouldEqualJson /* language=json */ """
                {
                  "type": "object",
                  "properties": {
                    "message": {
                      "type": "string",
                      "description": "the message"
                    }
                  },
                  "required": [
                    "message"
                  ]
                }
           """
        }
    }

    @Test
    fun `should create a tool instance from the test tool with given CacheControl`() {
        // when
        val tool = Tool<MessageRepeater>(
            builder = {
                cacheControl = CacheControl.Ephemeral()
            }
        ) {
            /* no tool use */
        }

        tool should {
            have(name == "message_repeater")
            have(description == "A test tool receiving a message and outputting it back")
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
            inputSchema.toString() shouldEqualJson /* language=json */ """
            {
              "type": "object",
              "properties": {
                "message": {
                  "type": "string",
                  "description": "the message"
                }
              },
              "required": [
                "message"
              ]
            }
            """
        }
    }

    class NonSerializableTool

    @Test
    fun `should fail to create a Tool with non-Serializable tool input`() {
        assertFailsWith<SerializationException> {
            Tool<NonSerializableTool>()
        }
    }

    @Test
    fun `should create tool with kotlin Pair as input`() {
        Tool<Pair<Int, Int>>() should {
            have(name == "kotlin_Pair")
            have(description == null)
            inputSchema.toString() shouldEqualJson """
                {
                  "type": "object",
                  "properties": {
                    "first": {
                      "type": "integer"
                    },
                    "second": {
                      "type": "integer"
                    }
                  },
                  "required": [
                    "first",
                    "second"
                  ]
                }
            """
        }
    }

}
