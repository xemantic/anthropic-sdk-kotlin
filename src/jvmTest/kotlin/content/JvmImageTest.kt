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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import org.junit.Test

class JvmImageTest {

  @Test
  fun `Should read test image file specified by String`() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.create {
      +Message {
        +Image("test-data/foo.png")
        +"What's on this picture?"
      }
    }

    // then
    response should {
      have(stopReason == StopReason.END_TURN)
      have(content.size == 1)
      content[0] should {
        be<Text>()
        have("FOO" in text.uppercase())
      }
    }
  }

}