package com.xemantic.anthropic

import com.xemantic.anthropic.usage.Cost

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

/**
 * Predefined models supported by Anthropic API.
 *
 * It could include Vertex AI (Google Cloud), or Bedrock (AWS) models in the future.
 */
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

  CLAUDE_3_5_HAIKU(
    id = "claude-3-5-haiku-latest",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = 1.0,
      outputTokens = 5.0
    )
  ),

  CLAUDE_3_5_HAIKU_20241022(
    id = "claude-3-5-haiku-20241022",
    contextWindow = 200000,
    maxOutput = 8182,
    messageBatchesApi = true,
    cost = Cost(
      inputTokens = 1.0,
      outputTokens = 5.0
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

  companion object {

    val DEFAULT: Model = CLAUDE_3_5_SONNET

    const val PRICE_UNIT: Double = 1000000.0

  }

}
