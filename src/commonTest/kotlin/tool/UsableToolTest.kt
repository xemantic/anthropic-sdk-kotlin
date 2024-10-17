package com.xemantic.anthropic.tool

import com.xemantic.anthropic.message.CacheControl
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.schema.JsonSchema
import com.xemantic.anthropic.schema.JsonSchemaProperty
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test

class UsableToolTest {

  @AnthropicTool(
    name = "TestTool",
    description = "Test tool receiving a message and outputting it back"
  )
  class TestTool(
    @Description("the message")
    val message: String
  ) : UsableTool {
    override suspend fun use(
      toolUseId: String
    ) = ToolResult(toolUseId, message)
  }

  @Test
  fun shouldCreateToolFromUsableToolAnnotatedWithAnthropicTool() {
    // when
    val tool = toolOf<TestTool>()

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
    val tool = toolOf<TestTool>(
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

  class NoAnnotationTool : UsableTool {
    override suspend fun use(
      toolUseId: String
    ) = ToolResult(toolUseId, "nothing")
  }

  @Test
  fun shouldFailToCreateToolWithoutAnthropicToolAnnotation() {
    shouldThrowWithMessage<SerializationException>(
      "Cannot find serializer for class com.xemantic.anthropic.tool.UsableToolTest.NoAnnotationTool, " +
          "make sure that it is annotated with @AnthropicTool and kotlin.serialization plugin is enabled for the project"
    ) {
      toolOf<NoAnnotationTool>()
    }
  }

  @Serializable
  class OnlySerializableAnnotationTool : UsableTool {
    override suspend fun use(
      toolUseId: String
    ) = ToolResult(toolUseId, "nothing")
  }

  @Test
  fun shouldFailToCreateToolWithOnlySerializableAnnotation() {
    shouldThrowWithMessage<SerializationException>(
      "The class com.xemantic.anthropic.tool.UsableToolTest.OnlySerializableAnnotationTool must be annotated with @AnthropicTool"
    ) {
      toolOf<OnlySerializableAnnotationTool>()
    }
  }

}
