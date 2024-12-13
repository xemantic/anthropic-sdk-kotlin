package com.xemantic.anthropic.usage

import com.xemantic.ai.money.Money
import com.xemantic.ai.money.Ratio
import com.xemantic.ai.money.ZERO
import com.xemantic.anthropic.Model
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class UsageCollectorTest {

  @Test
  fun `Should initialize UsageCollector with zero usage`() {
    UsageCollector() should {
      have(usage == Usage.ZERO)
      have(cost == Cost.ZERO)
    }
  }

  @Test
  fun `toString should return String representation of UsageCollector`() {
    assert(UsageCollector().toString() ==
        "UsageCollector(usage=" +
        "Usage(inputTokens=0, outputTokens=0, cacheCreationInputTokens=0, cacheReadInputTokens=0), cost=" +
        "Cost(inputTokens=0, outputTokens=0, cacheCreationInputTokens=0, cacheReadInputTokens=0))"
    )
  }

  @Test
  fun `Should update usage and cost`() {
    // given
    val collector = UsageCollector()

    // when
    collector.update(
      modelCost = Model.DEFAULT.cost,
      usage = Usage(
        inputTokens = 1000,
        outputTokens = 1000
      )
    )

    // then
    collector should  {
      have(usage == Usage(
        inputTokens = 1000,
        outputTokens = 1000,
        cacheCreationInputTokens = 0,
        cacheReadInputTokens = 0
      ))
      have(cost == Cost(
        inputTokens = Money(".003"),
        outputTokens = Money(".015"),
        cacheCreationInputTokens = Money.ZERO,
        cacheReadInputTokens = Money.ZERO
      ))
    }
  }

  @Test
  fun `Should update usage and cost for batch`() {
    // given
    val collector = UsageCollector()

    // when
    collector.update(
      modelCost = Model.DEFAULT.cost,
      usage = Usage(
        inputTokens = 1000,
        outputTokens = 1000
      ),
      costRatio = Money.Ratio("0.5")
    )

    // then
    collector should  {
      have(usage == Usage(
        inputTokens = 1000,
        outputTokens = 1000,
        cacheCreationInputTokens = 0,
        cacheReadInputTokens = 0
      ))
      have(cost == Cost(
        inputTokens = Money(".0015"),
        outputTokens = Money(".0075"),
        cacheCreationInputTokens = Money.ZERO,
        cacheReadInputTokens = Money.ZERO
      ))
    }
  }

  @Test
  fun `Should accumulate multiple usage updates`() {
    // given
    val collector = UsageCollector()
    val testUsage = Usage(
      inputTokens = 1000,
      outputTokens = 1000,
      cacheCreationInputTokens = 1000,
      cacheReadInputTokens = 1000
    )

    // when
    collector.update(
      modelCost = Model.CLAUDE_3_5_SONNET.cost,
      usage = testUsage
    )
    collector.update(
      modelCost = Model.CLAUDE_3_5_HAIKU.cost,
      usage = testUsage
    )
    collector.update(
      modelCost = Model.CLAUDE_3_OPUS.cost,
      usage = testUsage
    )

    // then
    collector should {
      usage should {
        have(inputTokens == 3000)
        have(outputTokens == 3000)
        have(cacheCreationInputTokens == 3000)
        have(cacheReadInputTokens == 3000)
      }
      cost should {
        have(inputTokens == Money("0.0188")) // 0.003 + 0.0008 + 0.015
        have(outputTokens == Money("0.094")) // 0.015 + 0.004 + 0.075
        have(cacheCreationInputTokens == Money("0.0235")) // 0.00375 + 0.001 + 0.01875
        have(cacheReadInputTokens == Money("0.00188")) // 0.0003 + 0.00008 + 0.0015
      }
    }
  }

}
