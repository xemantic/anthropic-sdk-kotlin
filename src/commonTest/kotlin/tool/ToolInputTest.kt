/*
 * Copyright 2024 Kazimierz Pogoda / Xemantic
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

import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ToolInputTest {

  /**
   * Let's start with defining a test tool used later in the tests.
   */
  @AnthropicTool("TestTool")
  @Description("A test tool receiving a message and outputting it back")
  class TestToolInput(
    @Description("the message")
    val message: String
  ) : ToolInput() {
    init {
      use {
        message
      }
    }
  }

  @Test
  fun `Should create a tool instance from the test tool annotated with AnthropicTool`() {
    // when
    val tool = Tool<TestToolInput>()

    tool should {
      have(name == "TestTool")
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
  fun `Should create a tool instance from the test tool with given cacheControl`() {
    // when
    // TODO we need a builder here?
    val tool = Tool<TestToolInput>(
      cacheControl = CacheControl(type = CacheControl.Type.EPHEMERAL)
    )

    tool should  {
      have(name == "TestTool")
      have(description == "A test tool receiving a message and outputting it back")
      have(cacheControl == CacheControl(type = CacheControl.Type.EPHEMERAL))
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

  class NoAnnotationTool : ToolInput()

  @Test
  fun `Should fail to create a Tool without AnthropicTool annotation`() {
    assertFailsWith<SerializationException> {
      Tool<NoAnnotationTool>()
    } should {
      have(message!!.matches(Regex(
        "Cannot find serializer for class .*NoAnnotationTool, " +
            "make sure that it is annotated with @AnthropicTool and kotlin.serialization plugin is enabled for the project"
      )))
    }
  }

  @Serializable
  class OnlySerializableAnnotationTool : ToolInput()

  @Test
  fun `Should fail to create a Tool with only Serializable annotation`() {
    assertFailsWith<SerializationException> {
      Tool<OnlySerializableAnnotationTool>()
    } should {
      have(message!!.matches(Regex(
        "The class .*OnlySerializableAnnotationTool must be annotated with @AnthropicTool"
      )))
    }
  }

}
