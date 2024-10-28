package com.xemantic.anthropic.cost

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
