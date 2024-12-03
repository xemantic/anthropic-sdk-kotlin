package com.xemantic.anthropic.content

import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

class MagicNumberTest {

  @Test
  fun `Should detect file Magic Number`() {
    File(
      "test-data/minimal.pdf"
    ).readBytes().findMagicNumber() shouldBe MagicNumber.PDF
    File(
      "test-data/minimal.jpg"
    ).readBytes().findMagicNumber() shouldBe MagicNumber.JPEG
    File(
      "test-data/minimal.png"
    ).readBytes().findMagicNumber() shouldBe MagicNumber.PNG
    File(
      "test-data/minimal.gif"
    ).readBytes().findMagicNumber() shouldBe MagicNumber.GIF
    File(
      "test-data/minimal.webp"
    ).readBytes().findMagicNumber() shouldBe MagicNumber.WEBP
  }

}