package com.xemantic.anthropic.message

import com.xemantic.anthropic.content.Text
import com.xemantic.anthropic.content.ToolResult
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ToolResultTest {

  @Test
  fun shouldCreateToolResultForSingleContentString() {
    ToolResult(toolUseId = "42") {
      +"foo"
    } shouldBe ToolResult(
      toolUseId = "42",
      content = listOf(Text(text = "foo"))
    )
  }

}
