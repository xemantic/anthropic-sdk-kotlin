/*
 * Copyright 2024 Kazimierz Pogoda / Xemantic
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

package com.xemantic.anthropic.tool.computer

import com.xemantic.anthropic.content.Image
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object JvmComputerService : ComputerService {

  override fun screenshot() = Image {
    bytes = takeScreenshot()
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
