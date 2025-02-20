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

package com.xemantic.ai.anthropic

import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ZERO
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.Role
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.usage.Cost
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AnthropicTest {

  @Test
  fun `Should create Anthropic instance with 0 Usage and Cost`() {
    Anthropic() should {
      have(usage == Usage.ZERO)
      have(cost == Cost.ZERO)
    }
  }

  @Test
  fun `Should receive an introduction from Claude`() = runTest {
    // given
    val anthropic = Anthropic()

    // when
    val response = anthropic.messages.create {
      +Message {
        +"Hello World! What's your name?"
      }
    }

    // then
    response should {
      have(role == Role.ASSISTANT)
      have("claude" in model)
      have(stopReason == StopReason.END_TURN)
      have(content.size == 1)
      content[0] should {
        be<Text>()
        have("Claude" in text)
      }
      have(stopSequence == null)
      usage should {
        have(inputTokens == 15)
        have(outputTokens > 0)
      }
    }
  }

  @Test
  fun `Should receive Usage and update Cost calculation`() = runTest {
    // given
    val anthropic = Anthropic()

    // when
    val response = anthropic.messages.create {
      +Message {
        +"Hello Claude! I am testing the amount of input and output tokens."
      }
    }

    // then
    response should {
      have(role == Role.ASSISTANT)
      have("claude" in model)
      have(stopReason == StopReason.END_TURN)
      have(content.size == 1)
      have(stopSequence == null)
      usage should {
        have(inputTokens == 21)
        have(outputTokens > 0)
        have(cacheCreationInputTokens == 0)
        have(cacheReadInputTokens == 0)
      }
    }

    anthropic should {
      usage should {
        have(inputTokens == 21)
        have(inputTokens > 0)
        have(cacheCreationInputTokens == 0)
        have(cacheReadInputTokens == 0)
      }
      cost should {
        have(inputTokens >= Money.ZERO && inputTokens == Money("0.000063"))
        have(outputTokens >= Money.ZERO && inputTokens <= Money("0.0005"))
        have(cacheCreationInputTokens == Money.ZERO)
        have(cacheReadInputTokens == Money.ZERO)
      }
    }

  }

  @Test
  fun `Should use system prompt`() = runTest {
    // given
    val anthropic = Anthropic()

    // when
    val response = anthropic.messages.create {
      system("Whatever the human says, answer \"HAHAHA\"")
      +Message {
        +"Hello World! What's your name?"
      }
      maxTokens = 1024
    }

    // then
    response should {
      have(content.size == 1)
      content[0] should {
        be<Text>()
        have(text == "HAHAHA")
      }
    }
  }

}
