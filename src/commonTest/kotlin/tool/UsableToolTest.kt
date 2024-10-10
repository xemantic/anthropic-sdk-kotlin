package com.xemantic.anthropic.tool

import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.schema.JsonSchema
import com.xemantic.anthropic.schema.JsonSchemaProperty
import com.xemantic.anthropic.test.then
import com.xemantic.anthropic.test.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class UsableToolTest {

  @SerializableTool(
    name = "TestTool",
    description = "Test tool receiving a message and outputting it back"
  )
  class TestTool(
    val message: String
  ) : UsableTool {
    override fun use(toolUseId: String) = ToolResult(toolUseId, message)
  }

  @Test
  fun shouldCreateToolFromUsableTool() {
    // when
    val tool = toolOf<TestTool>()

    then(tool) {
      name shouldBe "TestTool"
      description shouldBe "Test tool receiving a message and outputting it back"
      inputSchema shouldBe JsonSchema(
        properties = mapOf("message" to JsonSchemaProperty.STRING),
        required = listOf("message")
      )
      cacheControl shouldBe null
    }
  }

  class NoAnnotationTool : UsableTool {
    override fun use(toolUseId: String) = ToolResult(toolUseId, "nothing")
  }

  @Test
  fun shouldFailToCreateToolWithoutSerializableToolAnnotation() {
    assertFailsWith<SerializationException> {
      toolOf<NoAnnotationTool>()
    }
    try {
      toolOf<NoAnnotationTool>()
    } catch (e: SerializationException) {
      e.message shouldBe "The class com.xemantic.anthropic.tool.UsableToolTest.NoAnnotationTool must be annotated with @SerializableTool"
    }
  }

  @Serializable
  class OnlySerializableAnnotationTool : UsableTool {
    override fun use(toolUseId: String) = ToolResult(toolUseId, "nothing")
  }

  @Test
  fun shouldFailToCreateToolWithOnlySerializableToolAnnotation() {
    assertFailsWith<SerializationException> {
      toolOf<OnlySerializableAnnotationTool>()
    }
    try {
      toolOf<OnlySerializableAnnotationTool>()
    } catch (e: SerializationException) {
      e.message shouldBe "The class com.xemantic.anthropic.tool.UsableToolTest.NoAnnotationTool must be annotated with @SerializableTool"
    }
  }

}
