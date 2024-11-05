package com.xemantic.anthropic.content

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

class MagicNumberTest {

  @Test
  fun shouldDetectImageMediaType() {
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