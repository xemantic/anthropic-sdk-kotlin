package com.xemantic.anthropic.content

import java.io.File
import kotlin.test.Test
import com.xemantic.kotlin.test.assert

class MagicNumberTest {

  @Test
  fun `Should detect file Magic Number`() {
    assert(
      File(
        "test-data/minimal.pdf"
      ).readBytes().findMagicNumber() == MagicNumber.PDF
    )
    assert(
      File(
        "test-data/minimal.jpg"
      ).readBytes().findMagicNumber() == MagicNumber.JPEG
    )
    assert(
      File(
        "test-data/minimal.png"
      ).readBytes().findMagicNumber() == MagicNumber.PNG
    )
    assert(
      File(
        "test-data/minimal.gif"
      ).readBytes().findMagicNumber() == MagicNumber.GIF
    )
    assert(
      File(
        "test-data/minimal.webp"
      ).readBytes().findMagicNumber() == MagicNumber.WEBP
    )
  }

}