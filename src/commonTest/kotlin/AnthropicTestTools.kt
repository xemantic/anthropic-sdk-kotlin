package com.xemantic.anthropic

import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.tool.Description
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("fibonacci")
@Description("Calculate Fibonacci number n")
data class FibonacciTool(val n: Int): UsableTool {

  tailrec fun fibonacci(
    n: Int, a: Int = 0, b: Int = 1
  ): Int = when (n) {
    0 -> a; 1 -> b; else -> fibonacci(n - 1, b, a + b)
  }

  override fun use(
    toolUseId: String,
  ) = ToolResult(toolUseId, "${fibonacci(n)}")

}

@Serializable
@SerialName("calculator")
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

  override fun use(toolUseId: String) = ToolResult(
    toolUseId,
    operation.calculate(a, b).toString()
  )

}
