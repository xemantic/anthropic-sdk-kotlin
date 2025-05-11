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

fun costReport(
    stats: CostWithUsage,
    totalStats: CostWithUsage,
): String = ReportTable().apply {
    row("", "request tokens", "total tokens", "request cost", "total cost")
    row(
        "input",
        stats.usage.inputTokens,
        totalStats.usage.inputTokens,
        "$${stats.cost.inputTokens}",
        "$${totalStats.cost.inputTokens}"
    )
    row(
        "output",
        stats.usage.outputTokens,
        totalStats.usage.outputTokens,
        "$${stats.cost.outputTokens}",
        "$${totalStats.cost.outputTokens}",
    )
    row(
        "cache write",
        stats.usage.cacheCreationInputTokens ?: 0,
        totalStats.usage.cacheCreationInputTokens ?: 0,
        "$${stats.cost.cacheCreationInputTokens}",
        "$${totalStats.cost.cacheCreationInputTokens}"
    )
    row(
        "cache read",
        stats.usage.cacheReadInputTokens ?: 0,
        totalStats.usage.cacheReadInputTokens ?: 0,
        "$${stats.cost.cacheReadInputTokens}",
        "$${totalStats.cost.cacheReadInputTokens}"
    )
    row("", "", "", "", "")
    row("", "", "", "$${stats.cost.total}", "$${totalStats.cost.total}")
}.toString()

internal class ReportTable {

    private val rows = mutableListOf<List<String>>()

    fun row(vararg any: Any) {
        rows += any.map { it.toString() }
    }

    override fun toString() = buildString {
        val maxLengths: List<Int> = rows[0].mapIndexed { index, item ->
            rows.maxOf {
                it[index].length
            }
        }
        rows.forEachIndexed { rowIndex, row ->
            append("| ")
            row.forEachIndexed { index, column ->
                val max = maxLengths[index]
                append(
                    if (index == 0 || rowIndex == 0) {
                        column.padEnd(max)
                    } else {
                        column.padStart(max)
                    }
                )
                append(" |")
                if (index < row.lastIndex) append(" ")
            }
            if (rowIndex == 0) {
                append("\n")
                append("|")
                maxLengths.forEach {
                    append("-".repeat(it + 2))
                    append("|")
                }
            }
            append("\n")
        }
    }

}
