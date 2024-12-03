package com.xemantic.anthropic.tool

import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.test.assert
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test

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

    tool.assert {
      name shouldBe "TestTool"
      description shouldBe "A test tool receiving a message and outputting it back"
      inputSchema.toString() shouldEqualJson """
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
      cacheControl shouldBe null
    }
  }

  @Test
  fun `Should create a tool instance from the test tool with given cacheControl`() {
    // when
    // TODO we need a builder here?
    val tool = Tool<TestToolInput>(
      cacheControl = CacheControl(type = CacheControl.Type.EPHEMERAL)
    )

    tool.assert {
      name shouldBe "TestTool"
      description shouldBe "A test tool receiving a message and outputting it back"
      inputSchema.toString() shouldEqualJson """
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
      cacheControl shouldBe CacheControl(type = CacheControl.Type.EPHEMERAL)
    }
  }

  class NoAnnotationTool : ToolInput()

  @Test
  fun `Should fail to create a Tool without AnthropicTool annotation`() {
    shouldThrow<SerializationException> {
      Tool<NoAnnotationTool>()
    }.message shouldMatch "Cannot find serializer for class .*NoAnnotationTool, " +
        "make sure that it is annotated with @AnthropicTool and kotlin.serialization plugin is enabled for the project"
  }

  @Serializable
  class OnlySerializableAnnotationTool : ToolInput()

  @Test
  fun `Should fail to create a Tool with only Serializable annotation`() {
    shouldThrow<SerializationException> {
      Tool<OnlySerializableAnnotationTool>()
    }.message shouldMatch "The class .*OnlySerializableAnnotationTool must be annotated with @AnthropicTool"
  }

}
