/*
 * Copyright 2024 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.anthropic.content

import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

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