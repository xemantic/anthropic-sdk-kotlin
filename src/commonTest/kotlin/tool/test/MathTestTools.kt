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

package com.xemantic.ai.anthropic.tool.test

import com.xemantic.ai.tool.schema.meta.Description

@Description("Calculates the arithmetic outcome of an operation when given the arguments a and b")
data class Calculator(
    val operation: Operation,
    val a: Double,
    val b: Double
) {

    @Suppress("unused") // it is used, but by Anthropic, so we skip the warning
    enum class Operation(
        val calculate: (a: Double, b: Double) -> Double
    ) {
        ADD({ a, b -> a + b }),
        SUBTRACT({ a, b -> a - b }),
        MULTIPLY({ a, b -> a * b }),
        DIVIDE({ a, b -> a / b })
    }

    fun calculate(): Double = operation.calculate(a, b)

}

tailrec fun fibonacci(
    n: Int, a: Int = 0, b: Int = 1
): Int = when (n) {
    0 -> a; 1 -> b; else -> fibonacci(n - 1, b, a + b)
}

@Description("Calculates the n-th fibonacci number")
data class FibonacciCalculator(
    val n: Int
) {

    fun calculate(): Int = fibonacci(n)

}
