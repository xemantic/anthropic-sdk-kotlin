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

package com.xemantic.ai.anthropic.usage

import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ONE
import com.xemantic.ai.money.Ratio
import com.xemantic.ai.money.times
import com.xemantic.ai.money.ZERO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usage(
  @SerialName("input_tokens")
  val inputTokens: Int,
  @SerialName("output_tokens")
  val outputTokens: Int,
  @SerialName("cache_creation_input_tokens")
  val cacheCreationInputTokens: Int? = null,
  @SerialName("cache_read_input_tokens")
  val cacheReadInputTokens: Int? = null,
) {

  companion object {

    val ZERO = Usage(
      inputTokens = 0,
      outputTokens = 0,
      cacheCreationInputTokens = 0,
      cacheReadInputTokens = 0
    )

  }

  operator fun plus(usage: Usage): Usage = Usage(
    inputTokens = inputTokens + usage.inputTokens,
    outputTokens = outputTokens + usage.outputTokens,
    cacheCreationInputTokens = (cacheCreationInputTokens ?: 0) + (usage.cacheCreationInputTokens ?: 0),
    cacheReadInputTokens = (cacheReadInputTokens ?: 0) + (usage.cacheReadInputTokens ?: 0)
  )

  fun cost(
    modelCost: Cost,
    costRatio: Money.Ratio = Money.Ratio.ONE
  ): Cost = Cost(
    inputTokens = inputTokens * modelCost.inputTokens * costRatio,
    outputTokens = outputTokens * modelCost.outputTokens * costRatio,
    // how cacheCreation and batch are playing together?
    cacheCreationInputTokens = (cacheCreationInputTokens ?: 0) * modelCost.cacheCreationInputTokens * costRatio,
    cacheReadInputTokens = (cacheReadInputTokens ?: 0) * modelCost.cacheReadInputTokens * costRatio
  )

}

@Serializable
data class Cost(
  val inputTokens: Money,
  val outputTokens: Money,
  val cacheCreationInputTokens: Money = inputTokens * Money.Ratio("1.25"),
  val cacheReadInputTokens: Money = inputTokens * Money.Ratio("0.1"),
) {

  operator fun plus(cost: Cost): Cost = Cost(
    inputTokens = inputTokens + cost.inputTokens,
    outputTokens = outputTokens + cost.outputTokens,
    cacheCreationInputTokens = cacheCreationInputTokens + cost.cacheCreationInputTokens,
    cacheReadInputTokens = cacheReadInputTokens + cost.cacheReadInputTokens
  )

  operator fun times(amount: Money): Cost = Cost(
    inputTokens = inputTokens * amount,
    outputTokens = outputTokens * amount,
    cacheCreationInputTokens = cacheCreationInputTokens * amount,
    cacheReadInputTokens = cacheReadInputTokens * amount
  )

  val total: Money get() =
    inputTokens +
        outputTokens +
        cacheCreationInputTokens +
        cacheReadInputTokens

  companion object {
    val ZERO = Cost(
      inputTokens = Money.ZERO,
      outputTokens = Money.ZERO
    )
  }

}
