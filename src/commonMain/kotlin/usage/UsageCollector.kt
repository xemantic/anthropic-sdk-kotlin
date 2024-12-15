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
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * Collects overall [Usage] and calculates [Cost] information
 * based on [com.xemantic.ai.anthropic.message.MessageResponse]s returned
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
