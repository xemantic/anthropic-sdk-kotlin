package com.xemantic.anthropic.tool

import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.schema.JsonSchema
import com.xemantic.anthropic.schema.JsonSchemaProperty
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test

class ToolInputTest {

  @AnthropicTool("TestTool")
  @Description("Test tool receiving a message and outputting it back")
  class TestToolInput(
    @Description("the message")
    val message: String
  ) : ToolInput() {
    init {
      use {
        +message
      }
    }
  }

  @Test
  fun shouldCreateToolFromUsableToolAnnotatedWithAnthropicTool() {
    // when
    val tool = Tool<TestToolInput>()

    assertSoftly(tool) {
      name shouldBe "TestTool"
      description shouldBe "Test tool receiving a message and outputting it back"
      inputSchema shouldBe JsonSchema(
        properties = mapOf("message" to JsonSchemaProperty(
          type = "string",
          description = "the message"
        )),
        required = listOf("message")
      )
      cacheControl shouldBe null
    }
  }

  // TODO maybe we need a builder here?
  @Test
  fun shouldCreateToolWithCacheControlFromUsableToolSuppliedWithCacheControl() {
    // when
    val tool = Tool<TestToolInput>(
      cacheControl = CacheControl(type = CacheControl.Type.EPHEMERAL)
    )

    assertSoftly(tool) {
      name shouldBe "TestTool"
      description shouldBe "Test tool receiving a message and outputting it back"
      inputSchema shouldBe JsonSchema(
        properties = mapOf("message" to JsonSchemaProperty(
          type = "string",
          description = "the message"
        )),
        required = listOf("message")
      )
      cacheControl shouldBe CacheControl(type = CacheControl.Type.EPHEMERAL)
    }
  }

  class NoAnnotationTool : ToolInput()

  @Test
  fun shouldFailToCreateToolWithoutAnthropicToolAnnotation() {
    shouldThrowWithMessage<SerializationException>(
      "Cannot find serializer for class com.xemantic.anthropic.tool.ToolInputTest\$NoAnnotationTool, " +
          "make sure that it is annotated with @AnthropicTool and kotlin.serialization plugin is enabled for the project"
    ) {
      Tool<NoAnnotationTool>()
    }
  }

  @Serializable
  class OnlySerializableAnnotationTool : ToolInput()

  @Test
  fun shouldFailToCreateToolWithOnlySerializableAnnotation() {
    shouldThrowWithMessage<SerializationException>(
      "The class com.xemantic.anthropic.tool.ToolInputTest\$OnlySerializableAnnotationTool must be annotated with @AnthropicTool"
    ) {
      Tool<OnlySerializableAnnotationTool>()
    }
  }

}
