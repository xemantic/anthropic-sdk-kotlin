package com.xemantic.anthropic.tool.computer

import com.xemantic.anthropic.content.Image
import com.xemantic.anthropic.content.ToolResult
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object JvmComputerService : ComputerService {

  override suspend fun use(
    toolUseId: String,
    input: Computer.Input
  ) = when (input.action) {
    Action.SCREENSHOT -> ToolResult(
      toolUseId = toolUseId,
      content = listOf(
        Image {
          data = takeScreenshot()
          mediaType = Image.MediaType.IMAGE_JPEG
        }
      )
    )
    else -> TODO()
  }

}

fun takeScreenshot(): ByteArray {
  val robot = Robot()
  val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
  val output = ByteArrayOutputStream()
  val image = robot.createScreenCapture(screenRect)
  if (!ImageIO.write(image, "jpeg", output)) {
    throw IllegalStateException("Failed to save screenshot")
  }
  return output.toByteArray()
}

actual val computerService: ComputerService get() = JvmComputerService
