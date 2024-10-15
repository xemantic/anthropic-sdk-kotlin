package com.xemantic.anthropic.tool

import com.xemantic.anthropic.message.CacheControl
import com.xemantic.anthropic.message.ToolResult
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
        properties = mapOf("message" to JsonSchemaProperty.STRING),
        required = listOf("message")
      )
      cacheControl shouldBe null
    }
  }

  @Test
  fun shouldCreateToolWithCacheControlFromUsableTool() {
    // when
    val tool = toolOf<TestTool>(
      cacheControl = CacheControl(type = CacheControl.Type.EPHEMERAL)
    )

    assertSoftly(tool) {
      name shouldBe "TestTool"
      description shouldBe "Test tool receiving a message and outputting it back"
      inputSchema shouldBe JsonSchema(
        properties = mapOf("message" to JsonSchemaProperty.STRING),
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
      "The class com.xemantic.anthropic.tool.UsableToolTest.NoAnnotationTool must be annotated with @SerializableTool"
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
      "The class com.xemantic.anthropic.tool.UsableToolTest.OnlySerializableAnnotationTool must be annotated with @SerializableTool"
    ) {
      toolOf<OnlySerializableAnnotationTool>()
    }
  }

}
