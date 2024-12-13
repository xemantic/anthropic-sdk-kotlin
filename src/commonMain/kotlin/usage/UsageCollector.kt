package com.xemantic.anthropic.usage

import com.xemantic.ai.money.Money
import com.xemantic.ai.money.ONE
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * Collects overall [Usage] and calculates [Cost] information
 * based on [com.xemantic.anthropic.message.MessageResponse]s returned
 * by API calls.
 */
class UsageCollector {

  // Atomic in case of several threads updating this data concurrently
  private val _usage = atomic(Usage.ZERO)

  /**
   * The current accumulated usage.
   */
  val usage: Usage get() = _usage.value

  // Atomic in case of several threads updating this data concurrently
  private val _cost = atomic(Cost.ZERO)

  /**
   * The current accumulated cost.
   */
  val cost: Cost get() = _cost.value

  /**
   * Updates the usage and cost based on the provided parameters.
   *
   * @param usage The usage to add.
   * @param modelCost The cost of the used model.
   * @param costRatio The cost ratio to apply, defaults to 1, but might be different for batch requests, etc.
   */
  fun update(
    usage: Usage,
    modelCost: Cost,
    costRatio: Money.Ratio = Money.Ratio.ONE,
  ) {
    _usage.update { it + usage }
    _cost.update { it + usage.cost(modelCost, costRatio) }
  }

  /**
   * Returns a string representation of the UsageCollector.
   *
   * @return A string containing the current usage and cost.
   */
  override fun toString(): String = "UsageCollector(usage=$usage, cost=$cost)"

}
