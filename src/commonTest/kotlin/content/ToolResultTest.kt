package com.xemantic.anthropic.content

import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import org.junit.Test

class ToolResultTest {

  @Test
  fun `Should create ToolResult for a single String representing Text content`() {
    ToolResult {
      toolUseId = "42"
      +"foo"
    } should {
      be<ToolResult>()
      have(toolUseId == "42")
      have(content!!.size == 1)
      content[0] should {
        be<Text>()
        have(text == "foo")
      }
      have(isError == null)
      have(cacheControl == null)
    }
  }

  @Test
  fun `Should create ToolResult for Text element representing content`() {
    ToolResult {
      toolUseId = "42"
      +Text(text = "foo")
    } should {
      be<ToolResult>()
      have(toolUseId == "42")
      have(content!!.size == 1)
      content[0] should {
        be<Text>()
        have(text == "foo")
      }
      have(isError == null)
      have(cacheControl == null)
    }
  }

  @Test
  fun `Should create error ToolResult`() {
    ToolResult {
      toolUseId = "42"
      error("Error message")
    } should {
      be<ToolResult>()
      have(toolUseId == "42")
      have(content!!.size == 1)
      content[0] should {
        be<Text>()
        have(text == "Error message")
      }
      have(isError == true)
      have(cacheControl == null)
    }
  }

}