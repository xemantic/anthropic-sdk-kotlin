package com.xemantic.anthropic.tool

import com.xemantic.anthropic.schema.Description
import kotlinx.serialization.Transient

tailrec fun fibonacci(
  n: Int, a: Int = 0, b: Int = 1
): Int = when (n) {
  0 -> a; 1 -> b; else -> fibonacci(n - 1, b, a + b)
}

@AnthropicTool("FibonacciTool")
@Description("Calculate Fibonacci number n")
data class FibonacciTool(val n: Int) : ToolInput() {
  init {
    use {
      fibonacci(n)
    }
  }
}

@AnthropicTool("Calculator")
@Description("Calculates the arithmetic outcome of an operation when given the arguments a and b")
data class Calculator(
  val operation: Operation,
  val a: Double,
  val b: Double
): ToolInput() {

  @Suppress("unused") // it is used, but by Anthropic, so we skip the warning
  enum class Operation(
    val calculate: (a: Double, b: Double) -> Double
  ) {
    ADD({ a, b -> a + b }),
    SUBTRACT({ a, b -> a - b }),
    MULTIPLY({ a, b -> a * b }),
    DIVIDE({ a, b -> a / b })
  }

  init {
    use {
      operation.calculate(a, b)
    }
  }

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
data class DatabaseQuery(
  val query: String
) : ToolInput() {

  @Transient
  internal lateinit var database: Database

  init {
    use {
      database.execute(query).joinToString()
    }
  }

}
