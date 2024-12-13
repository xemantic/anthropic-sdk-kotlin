package com.xemantic.anthropic.tool

import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.anthropic.cache.CacheControl
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
    val exception = assertFailsWith<SerializationException> {
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
