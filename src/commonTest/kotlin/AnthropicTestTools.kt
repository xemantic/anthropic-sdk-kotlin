package com.xemantic.anthropic

import com.xemantic.anthropic.message.SimpleUsableTool
import com.xemantic.anthropic.message.Text
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.message.UsableTool
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("fibonacci")
@Description("Calculate Fibonacci number n")
data class FibonacciTool(val n: Int): SimpleUsableTool {

  tailrec fun fibonacci(
    n: Int, a: Int = 0, b: Int = 1
  ): Int = when (n) {
    0 -> a; 1 -> b; else -> fibonacci(n - 1, b, a + b)
  }

  override fun use(
    toolUseId: String,
  ) = ToolResult(
    toolUseId,
    content = listOf(Text(text = "${fibonacci(n)}"))
  )

}

@Serializable
@SerialName("com_xemantic_anthropic_AnthropicTest_Calculator")
@Description("Calculates the arithmetic outcome of an operation when given the arguments a and b")
data class Calculator(
  val operation: Operation,
  val a: Double,
  val b: Double
): SimpleUsableTool {

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

// TODO this can be constructed on fly
val testToolsSerializersModule = SerializersModule {
  polymorphic(UsableTool::class) {
    subclass(Calculator::class)
    subclass(FibonacciTool::class)
  }
}
