package com.xemantic.anthropic.usage

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
)

fun Usage.add(usage: Usage): Usage = Usage(
  inputTokens = inputTokens + usage.inputTokens,
  outputTokens = outputTokens + usage.outputTokens,
  cacheReadInputTokens = (cacheReadInputTokens ?: 0) + (usage.cacheReadInputTokens ?: 0),
  cacheCreationInputTokens = (cacheCreationInputTokens ?: 0) + (usage.cacheCreationInputTokens ?: 0),
)

fun Usage.cost(
  model: Model,
  isBatch: Boolean = false
): Cost = Cost(
  inputTokens = inputTokens * model.cost.inputTokens / 1000000.0 * (if (isBatch) .5 else 1.0),
  outputTokens = outputTokens * model.cost.outputTokens / 1000000.0 * (if (isBatch) .5 else 1.0),
  cacheReadInputTokens = (cacheReadInputTokens ?: 0) * model.cost.inputTokens * .1 / 1000000.0 * (if (isBatch) .5 else 1.0),
  cacheCreationInputTokens = (cacheCreationInputTokens ?: 0) * model.cost.inputTokens * .25 / 1000000.0 * (if (isBatch) .5 else 1.0)
)

data class Cost(
  val inputTokens: Double,
  val outputTokens: Double,
  val cacheCreationInputTokens: Double,
  val cacheReadInputTokens: Double
) {

  fun add(cost: Cost): Cost = Cost(
    inputTokens = inputTokens + cost.inputTokens,
    outputTokens = outputTokens + cost.outputTokens,
    cacheCreationInputTokens = cacheCreationInputTokens + cost.cacheCreationInputTokens,
    cacheReadInputTokens = cacheReadInputTokens + cost.cacheReadInputTokens
  )

}
