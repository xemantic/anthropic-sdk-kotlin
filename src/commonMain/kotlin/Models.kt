package com.xemantic.anthropic

enum class Model(
  val id: String,
  val contextWindow: Int,
  val maxOutput: Int,
  val messageBatchesApi: Boolean,
  val cost: Cost
) {

  CLAUDE_3_5_SONNET(
    id = "claude-3-5-sonnet-latest",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = 3.0,
      outputTokens = 15.0
    )
  ),

  CLAUDE_3_5_SONNET_20241022(
    id = "claude-3-5-sonnet-20241022",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = 3.0,
      outputTokens = 15.0
    )
  ),

  CLAUDE_3_5_SONNET_20240620(
    id = "claude-3-5-sonnet-20240620",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = 3.0,
      outputTokens = 15.0
    )
  ),

  CLAUDE_3_OPUS(
    id = "claude-3-opus-latest",
    contextWindow = 200000,
    maxOutput = 4096,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = 15.0,
      outputTokens = 75.0
    )
  ),

  CLAUDE_3_OPUS_20240229(
    id = "claude-3-opus-20240229",
    contextWindow = 200000,
    maxOutput = 4096,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = 15.0,
      outputTokens = 75.0
    )
  ),

  CLAUDE_3_SONNET_20240229(
    id = "claude-3-sonnet-20240229",
    contextWindow = 200000,
    maxOutput = 4096,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = 3.0,
      outputTokens = 15.0
    )
  ),

  CLAUDE_3_HAIKU_20240307(
    id = "claude-3-haiku-20240307",
    contextWindow = 200000,
    maxOutput = 4096,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = .25,
      outputTokens = 1.25
    )
  );

  /**
   * Cost per MTok
   */
  data class Cost(
    val inputTokens: Double,
    val outputTokens: Double
  )

  companion object {
    val DEFAULT: Model = CLAUDE_3_5_SONNET
  }

}
