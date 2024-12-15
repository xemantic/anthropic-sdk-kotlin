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
import com.xemantic.ai.money.Ratio
import com.xemantic.ai.anthropic.usage.Cost

/**
 * The model used by the API.
 * E.g., Claude LLM `sonnet`, `opus`, `haiku` family.
 */
interface AnthropicModel {

  val id: String
  val contextWindow: Int
  val maxOutput: Int
  val messageBatchesApi: Boolean
  val cost: Cost

}

val ANTHROPIC_TOKEN_COST_RATIO = Money.Ratio("0.000001")

val String.dollarsPerMillion: Money get() = Money(this) * ANTHROPIC_TOKEN_COST_RATIO

/**
 * Predefined models supported by Anthropic API.
 *
 * It could include Vertex AI (Google Cloud), or Bedrock (AWS) models in the future.
 */
// TODO model should be interface AnthropicApi models should be enum
enum class Model(
  override val id: String,
  override val contextWindow: Int,
  override val maxOutput: Int,
  override val messageBatchesApi: Boolean,
  override val cost: Cost
) : AnthropicModel {

  CLAUDE_3_5_SONNET(
    id = "claude-3-5-sonnet-latest",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "3".dollarsPerMillion,
      outputTokens = "15".dollarsPerMillion
    )
  ),

  CLAUDE_3_5_SONNET_20241022(
    id = "claude-3-5-sonnet-20241022",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "3".dollarsPerMillion,
      outputTokens = "15".dollarsPerMillion
    )
  ),

  CLAUDE_3_5_HAIKU(
    id = "claude-3-5-haiku-latest",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "0.80".dollarsPerMillion,
      outputTokens = "4".dollarsPerMillion
    )
  ),

  CLAUDE_3_5_HAIKU_20241022(
    id = "claude-3-5-haiku-20241022",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "0.80".dollarsPerMillion,
      outputTokens = "4".dollarsPerMillion
    )
  ),

  CLAUDE_3_5_SONNET_20240620(
    id = "claude-3-5-sonnet-20240620",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "3".dollarsPerMillion,
      outputTokens = "15".dollarsPerMillion
    )
  ),

  CLAUDE_3_OPUS(
    id = "claude-3-opus-latest",
    contextWindow = 200000,
    maxOutput = 4096,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "15".dollarsPerMillion,
      outputTokens = "75".dollarsPerMillion
    )
  ),

  CLAUDE_3_OPUS_20240229(
    id = "claude-3-opus-20240229",
    contextWindow = 200000,
    maxOutput = 4096,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "15".dollarsPerMillion,
      outputTokens = "75".dollarsPerMillion
    )
  ),

  CLAUDE_3_SONNET_20240229(
    id = "claude-3-sonnet-20240229",
    contextWindow = 200000,
    maxOutput = 4096,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "3".dollarsPerMillion,
      outputTokens = "15".dollarsPerMillion
    )
  ),

  CLAUDE_3_HAIKU_20240307(
    id = "claude-3-haiku-20240307",
    contextWindow = 200000,
    maxOutput = 4096,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = "0.25".dollarsPerMillion,
      outputTokens = "1.25".dollarsPerMillion
    )
  );

  companion object {

    val DEFAULT: Model = CLAUDE_3_5_SONNET

  }

}
