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
    val cacheCreationInputTokens: Money,
    val cacheReadInputTokens: Money,
) {

    class Builder {

        var inputTokens: Money? = null
        var outputTokens: Money? = null
        var cacheCreationInputTokens: Money? = null
        var cacheReadInputTokens: Money? = null

        fun build(): Cost = Cost(
            inputTokens = requireNotNull(inputTokens) { "inputTokens cannot be null" },
            outputTokens = requireNotNull(outputTokens) { "outputTokens cannot be null" },
            cacheCreationInputTokens = if (cacheCreationInputTokens != null) {
                cacheCreationInputTokens!!
            } else {
                inputTokens!! * Money.Ratio("1.25")
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
        cacheCreationInputTokens = cacheCreationInputTokens + cost.cacheCreationInputTokens,
        cacheReadInputTokens = cacheReadInputTokens + cost.cacheReadInputTokens
    )

    operator fun times(amount: Money): Cost = Cost(
        inputTokens = inputTokens * amount,
        outputTokens = outputTokens * amount,
        cacheCreationInputTokens = cacheCreationInputTokens * amount,
        cacheReadInputTokens = cacheReadInputTokens * amount
    )

    operator fun times(usage: Usage): Cost = Cost(
        inputTokens = inputTokens * usage.inputTokens,
        outputTokens = outputTokens * usage.outputTokens,
        cacheCreationInputTokens = cacheCreationInputTokens * (usage.cacheReadInputTokens ?: 0),
        cacheReadInputTokens = cacheReadInputTokens * (usage.cacheReadInputTokens ?: 0)
    )

    val total: Money
        get() =
            inputTokens +
                    outputTokens +
                    cacheCreationInputTokens +
                    cacheReadInputTokens

    companion object {
        val ZERO = Cost(
            inputTokens = Money.ZERO,
            outputTokens = Money.ZERO,
            cacheCreationInputTokens = Money.ZERO,
            cacheReadInputTokens = Money.ZERO
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Cost

        if (inputTokens != other.inputTokens) return false
        if (outputTokens != other.outputTokens) return false
        if (cacheCreationInputTokens != other.cacheCreationInputTokens) return false
        if (cacheReadInputTokens != other.cacheReadInputTokens) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputTokens.hashCode()
        result = 31 * result + outputTokens.hashCode()
        result = 31 * result + cacheCreationInputTokens.hashCode()
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
