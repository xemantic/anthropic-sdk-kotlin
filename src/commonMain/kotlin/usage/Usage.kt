package com.xemantic.anthropic.usage

import com.xemantic.anthropic.AnthropicModel
import com.xemantic.anthropic.Model
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
    cacheReadInputTokens = (cacheReadInputTokens ?: 0) + (usage.cacheReadInputTokens ?: 0),
    cacheCreationInputTokens = (cacheCreationInputTokens ?: 0) + (usage.cacheCreationInputTokens ?: 0),
  )

  fun cost(
    model: AnthropicModel,
    isBatch: Boolean = false
  ): Cost = Cost(
    inputTokens = inputTokens * model.cost.inputTokens / Model.PRICE_UNIT,
    outputTokens = outputTokens * model.cost.outputTokens / Model.PRICE_UNIT,
    cacheReadInputTokens = (cacheReadInputTokens ?: 0) / Model.PRICE_UNIT,
    cacheCreationInputTokens = (cacheCreationInputTokens ?: 0) / Model.PRICE_UNIT
  ).let { if (isBatch) it * .5 else it }

}

@Serializable
data class Cost(
  val inputTokens: Double,
  val outputTokens: Double,
  val cacheCreationInputTokens: Double = inputTokens * .25,
  val cacheReadInputTokens: Double = inputTokens * .25
) {

  operator fun plus(cost: Cost): Cost = Cost(
    inputTokens = inputTokens + cost.inputTokens,
    outputTokens = outputTokens + cost.outputTokens,
    cacheCreationInputTokens = cacheCreationInputTokens + cost.cacheCreationInputTokens,
    cacheReadInputTokens = cacheReadInputTokens + cost.cacheReadInputTokens
  )

  operator fun times(value: Double): Cost = Cost(
    inputTokens = inputTokens * value,
    outputTokens = outputTokens * value,
    cacheCreationInputTokens = cacheCreationInputTokens * value,
    cacheReadInputTokens = cacheReadInputTokens * value
  )

  operator fun div(value: Double): Cost = Cost(
    inputTokens = inputTokens / value,
    outputTokens = outputTokens / value,
    cacheCreationInputTokens = cacheCreationInputTokens / value,
    cacheReadInputTokens = cacheReadInputTokens / value
  )

  val total: Double get() = inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens

  companion object {
    val ZERO = Cost(
      inputTokens = 0.0,
      outputTokens = 0.0
    )
  }

}
