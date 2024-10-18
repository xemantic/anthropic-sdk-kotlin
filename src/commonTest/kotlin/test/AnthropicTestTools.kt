package com.xemantic.anthropic.test

import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.serialization.Transient

@AnthropicTool("FibonacciTool")
@Description("Calculate Fibonacci number n")
data class FibonacciTool(val n: Int): UsableTool {

  tailrec fun fibonacci(
    n: Int, a: Int = 0, b: Int = 1
  ): Int = when (n) {
    0 -> a; 1 -> b; else -> fibonacci(n - 1, b, a + b)
  }

  override suspend fun use(
    toolUseId: String,
  ) = ToolResult(toolUseId, "${fibonacci(n)}")

}

@AnthropicTool("Calculator")
@Description("Calculates the arithmetic outcome of an operation when given the arguments a and b")
data class Calculator(
  val operation: Operation,
  val a: Double,
  val b: Double
): UsableTool {

  @Suppress("unused") // it is used, but by Anthropic, so we skip the warning
  enum class Operation(
    val calculate: (a: Double, b: Double) -> Double
  ) {
    ADD({ a, b -> a + b }),
    SUBTRACT({ a, b -> a - b }),
    MULTIPLY({ a, b -> a * b }),
    DIVIDE({ a, b -> a / b })
  }

  override suspend fun use(toolUseId: String) = ToolResult(
    toolUseId,
    operation.calculate(a, b).toString()
  )

}

interface Database {
  suspend fun execute(query: String): List<String>
}

class TestDatabase : Database {
  var executedQuery: String? = null
  override suspend fun execute(
    query: String
  ): List<String> {
    executedQuery = query
    return listOf("foo", "bar", "buzz")
  }
}

@AnthropicTool("DatabaseQuery")
@Description("Executes database query")
data class DatabaseQueryTool(
  val query: String
) : UsableTool {

  @Transient
  lateinit var database: Database

  override suspend fun use(
    toolUseId: String
  ) = ToolResult(
    toolUseId,
    text  = database.execute(query).joinToString()
  )

}
