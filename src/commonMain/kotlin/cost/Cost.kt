/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
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

import com.xemantic.ai.anthropic.json.toPrettyJson
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.ai.money.Money
import com.xemantic.ai.money.Ratio
import com.xemantic.ai.money.ZERO
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
class Cost private constructor(
    val inputTokens: Money,
    val outputTokens: Money,
    val cache5mCreationInputTokens: Money,
    val cache1hCreationInputTokens: Money,
    val cacheReadInputTokens: Money,
) {

    class Builder {

        var inputTokens: Money? = null
        var outputTokens: Money? = null
        var cache5mCreationInputTokens: Money? = null
        var cache1hCreationInputTokens: Money? = null
        var cacheReadInputTokens: Money? = null

        fun build(): Cost = Cost(
            inputTokens = requireNotNull(inputTokens) { "inputTokens cannot be null" },
            outputTokens = requireNotNull(outputTokens) { "outputTokens cannot be null" },
            cache5mCreationInputTokens = if (cache5mCreationInputTokens != null) {
                cache5mCreationInputTokens!!
            } else {
                inputTokens!! * Money.Ratio("1.25")
            },
            cache1hCreationInputTokens = if (cache1hCreationInputTokens != null) {
                cache1hCreationInputTokens!!
            } else {
                inputTokens!! * Money.Ratio("2.0")
            },
            cacheReadInputTokens = if (cacheReadInputTokens != null) {
                cacheReadInputTokens!!
            } else {
                inputTokens!! * Money.Ratio("0.1")
            }
        )

    }

    operator fun plus(cost: Cost): Cost = Cost(
        inputTokens = inputTokens + cost.inputTokens,
        outputTokens = outputTokens + cost.outputTokens,
        cache5mCreationInputTokens = cache5mCreationInputTokens + cost.cache5mCreationInputTokens,
        cache1hCreationInputTokens = cache1hCreationInputTokens + cost.cache1hCreationInputTokens,
        cacheReadInputTokens = cacheReadInputTokens + cost.cacheReadInputTokens
    )

    operator fun times(amount: Money): Cost = Cost(
        inputTokens = inputTokens * amount,
        outputTokens = outputTokens * amount,
        cache5mCreationInputTokens = cache5mCreationInputTokens * amount,
        cache1hCreationInputTokens = cache1hCreationInputTokens * amount,
        cacheReadInputTokens = cacheReadInputTokens * amount
    )

    operator fun times(usage: Usage): Cost {
        val cache5m = usage.cacheCreation?.ephemeral5mInputTokens ?: 0
        val cache1h = usage.cacheCreation?.ephemeral1hInputTokens ?: 0

        return Cost(
            inputTokens = inputTokens * usage.inputTokens,
            outputTokens = outputTokens * usage.outputTokens,
            cache5mCreationInputTokens = cache5mCreationInputTokens * cache5m,
            cache1hCreationInputTokens = cache1hCreationInputTokens * cache1h,
            cacheReadInputTokens = cacheReadInputTokens * (usage.cacheReadInputTokens ?: 0)
        )
    }

    val total: Money
        get() =
            inputTokens +
                    outputTokens +
                    cache5mCreationInputTokens +
                    cache1hCreationInputTokens +
                    cacheReadInputTokens

    companion object {
        val ZERO = Cost(
            inputTokens = Money.ZERO,
            outputTokens = Money.ZERO,
            cache5mCreationInputTokens = Money.ZERO,
            cache1hCreationInputTokens = Money.ZERO,
            cacheReadInputTokens = Money.ZERO
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Cost

        if (inputTokens != other.inputTokens) return false
        if (outputTokens != other.outputTokens) return false
        if (cache5mCreationInputTokens != other.cache5mCreationInputTokens) return false
        if (cache1hCreationInputTokens != other.cache1hCreationInputTokens) return false
        if (cacheReadInputTokens != other.cacheReadInputTokens) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputTokens.hashCode()
        result = 31 * result + outputTokens.hashCode()
        result = 31 * result + cache5mCreationInputTokens.hashCode()
        result = 31 * result + cache1hCreationInputTokens.hashCode()
        result = 31 * result + cacheReadInputTokens.hashCode()
        return result
    }

    override fun toString(): String = toPrettyJson()

}

@OptIn(ExperimentalContracts::class)
fun Cost(block: Cost.Builder.() -> Unit): Cost {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Cost.Builder().also(block).build()
}
