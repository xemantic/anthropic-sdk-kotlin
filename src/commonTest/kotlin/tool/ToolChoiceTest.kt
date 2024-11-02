package com.xemantic.anthropic.tool

import com.xemantic.anthropic.test.testJson
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class ToolChoiceTest {

  // we are using list wrapping to trigger polymorphic deserializer
  private val ToolChoice.json get() = testJson
    .encodeToString(listOf<ToolChoice>(this))
    .removePrefix("[")
    .removeSuffix("]")

  @Test
  fun shouldSerializeToolChoiceAuto() {

    ToolChoice.Auto().json shouldEqualJson """
      {
        "type": "auto"
      }
    """.trimIndent()

    ToolChoice.Auto(
      disableParallelToolUse = true
    ).json shouldEqualJson """
      {
        "type": "auto",
        "disable_parallel_tool_use": true
      }
    """.trimIndent()

  }

  @Test
  fun shouldSerializeToolChoiceAny() {

    ToolChoice.Any().json shouldEqualJson """
      {
        "type": "any"
      }
    """.trimIndent()

    ToolChoice.Any(
      disableParallelToolUse = true
    ).json shouldEqualJson """
      {
        "type": "any",
        "disable_parallel_tool_use": true
      }
    """.trimIndent()

  }

  @Test
  fun shouldSerializeToolChoiceTool() {

    ToolChoice.Tool(
      name = "foo"
    ).json shouldEqualJson """
      {
        "type": "tool",
        "name": "foo"
      }
    """.trimIndent()

    ToolChoice.Tool(
      name = "foo",
      disableParallelToolUse = true
    ).json shouldEqualJson """
      {
        "type": "tool",
        "name": "foo",
        "disable_parallel_tool_use": true
      }
    """.trimIndent()

  }

}
