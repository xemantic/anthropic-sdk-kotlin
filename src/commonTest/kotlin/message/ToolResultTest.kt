package com.xemantic.anthropic.message

import com.xemantic.anthropic.text.Text
import com.xemantic.anthropic.tool.ToolResult
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlin.test.Test

class ToolResultTest {

  @Test
  fun shouldCreateToolResultForSingleString() {
    ToolResult(
      toolUseId = "42",
      "foo"
    ) shouldBe ToolResult(
      toolUseId = "42",
      content = listOf(Text(text = "foo"))
    )
  }

  @Serializable
  data class Foo(val bar: String)

  @Test
  fun shouldCreateToolResultForSerializableInstance() {
    ToolResult(
      toolUseId = "42",
      Foo("buzz")
    ) shouldBe ToolResult(
      toolUseId = "42",
      content = listOf(Text(text = "{\"bar\":\"buzz\"}"))
    )
  }

}
