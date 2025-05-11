/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic.cost

import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.ai.anthropic.util.update
import kotlinx.serialization.Serializable
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Serializable
data class CostWithUsage(
    val cost: Cost,
    val usage: Usage
) {

    operator fun plus(
        other: CostWithUsage
    ): CostWithUsage = CostWithUsage(
        cost = cost + other.cost,
        usage = usage + other.usage
    )

    companion object {

        val ZERO = CostWithUsage(Cost.ZERO,  Usage.ZERO)

    }

}

/**
 * Collects overall [Usage] and calculates [Cost] information
 * based on [com.xemantic.ai.anthropic.message.MessageResponse]s returned
 * by API calls.
 */
@OptIn(ExperimentalAtomicApi::class)
class CostCollector {

    // Atomic in the case of several threads updating this data concurrently
    private val _costWithUsage = AtomicReference(CostWithUsage.ZERO)

    /**
     * The current accumulated usage.
     */
    val costWithUsage: CostWithUsage get() = _costWithUsage.load()

    operator fun plusAssign(
        other: CostWithUsage
    ) {
        _costWithUsage.update { it + other }
    }

    /**
     * Returns a string representation of the UsageCollector.
     *
     * @return A string containing the current usage and cost.
     */
    override fun toString(): String = "UsageAndCostCollector(${_costWithUsage.load()})"

}
