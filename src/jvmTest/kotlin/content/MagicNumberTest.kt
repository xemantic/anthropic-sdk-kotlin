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