package com.xemantic.anthropic.usage

import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ZERO
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class CostTest {

  @Test
  fun `Should create a Cost instance with correct values`() {
    Cost(
      inputTokens = Money("0.001"),
      outputTokens = Money("0.002"),
      cacheCreationInputTokens = Money("0.00025"),
      cacheReadInputTokens = Money("0.0005")
    ) should {
      have(inputTokens == Money("0.001"))
      have(outputTokens == Money("0.002"))
      have(cacheCreationInputTokens == Money("0.00025"))
      have(cacheReadInputTokens == Money("0.0005"))
    }
  }

  /**
   * This case is used when costs per model are being defined.
   */
  @Test
  fun `Should create a Cost instance with correct values when cache costs are not specified`() {
    Cost(
      inputTokens = Money("0.001"),
      outputTokens = Money("0.002")
    ) should {
      have(inputTokens == Money("0.001"))
      have(outputTokens == Money("0.002"))
      have(cacheCreationInputTokens == Money("0.00125"))
      have(cacheReadInputTokens == Money("0.0001"))
    }
  }

  @Test
  fun `Should add two Cost instances without cache`() {
    // given
    val cost1 = Cost(
      inputTokens = Money("0.001"),
      outputTokens = Money("0.002"),
      cacheCreationInputTokens = Money.ZERO,
      cacheReadInputTokens = Money.ZERO
    )
    val cost2 = Cost(
      inputTokens = Money("0.003"),
      outputTokens = Money("0.004"),
      cacheCreationInputTokens = Money.ZERO,
      cacheReadInputTokens = Money.ZERO
    )

    // when
    val result = cost1 + cost2

    // then
    result should {
      have(inputTokens == Money("0.004"))
      have(outputTokens == Money("0.006"))
      have(cacheCreationInputTokens == Money.ZERO)
      have(cacheReadInputTokens == Money.ZERO)
    }
  }

  @Test
  fun `Should add two Cost instances with cache`() {
    // given
    val cost1 = Cost(
      inputTokens = Money("0.001"),
      outputTokens = Money("0.002"),
      cacheCreationInputTokens = Money("0.0001"),
      cacheReadInputTokens = Money("0.0002"),
    )
    val cost2 = Cost(
      inputTokens = Money("0.003"),
      outputTokens = Money("0.004"),
      cacheCreationInputTokens = Money("0.0003"),
      cacheReadInputTokens = Money("0.0004")
    )

    // when
    val result = cost1 + cost2

    // then
    result should {
      have(inputTokens == Money("0.004"))
      have(outputTokens == Money("0.006"))
      have(cacheCreationInputTokens == Money("0.0004"))
      have(cacheReadInputTokens == Money("0.0006"))
    }
  }

  @Test
  fun `Should calculate total cost`() {
    Cost(
      inputTokens = Money("0.001"),
      outputTokens = Money("0.002"),
      cacheCreationInputTokens = Money("0.0005"),
      cacheReadInputTokens = Money("0.0007")
    ) should {
      have(total == Money("0.0042"))
    }
  }

  @Test
  fun `Should create ZERO Cost instance`() {
    Cost.ZERO should {
      have(inputTokens == Money.ZERO)
      have(outputTokens == Money.ZERO)
      have(cacheCreationInputTokens == Money.ZERO)
      have(cacheReadInputTokens == Money.ZERO)
    }
  }

}
