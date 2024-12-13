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

    ToolChoice.Auto().json shouldEqualJson /* language=json */ """
      {
        "type": "auto"
      }
    """

    ToolChoice.Auto(
      disableParallelToolUse = true
    ).json shouldEqualJson """
      {
        "type": "auto",
        "disable_parallel_tool_use": true
      }
    """

  }

  @Test
  fun shouldSerializeToolChoiceAny() {

    ToolChoice.Any().json shouldEqualJson /* language=json */ """
      {
        "type": "any"
      }
    """

    ToolChoice.Any(
      disableParallelToolUse = true
    ).json shouldEqualJson /* language=json */ """
      {
        "type": "any",
        "disable_parallel_tool_use": true
      }
    """

  }

  @Test
  fun shouldSerializeToolChoiceTool() {

    ToolChoice.Tool(
      name = "foo"
    ).json shouldEqualJson /* language=json */ """
      {
        "type": "tool",
        "name": "foo"
      }
    """

    ToolChoice.Tool(
      name = "foo",
      disableParallelToolUse = true
    ).json shouldEqualJson /* language=json */ """
      {
        "type": "tool",
        "name": "foo",
        "disable_parallel_tool_use": true
      }
    """

  }

}
