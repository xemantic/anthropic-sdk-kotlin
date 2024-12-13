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

@OptIn(ExperimentalUnsignedTypes::class)
enum class MagicNumber(
  vararg magic: UByte,
  private val test: (
    data: ByteArray,
    magic: ByteArray
  ) -> Boolean = { data, magic ->
    data.startsWith(magic)
  }
) {

  PDF(*"%PDF-".toUByteArray()),
  JPEG(0xFFu, 0xD8u, 0xFFu),
  PNG(0x89u, 0x50u, 0x4Eu, 0x47u, 0x0Du, 0x0Au, 0x1Au, 0x0Au),
  GIF(*"GIF8".toUByteArray()),
  WEBP(*"WEBP".toUByteArray(), test = { data, magic ->
    (data.size >= 12) && data.slice(8..11).toByteArray().contentEquals(magic)
  });

  private val magic = magic.toUByteArray()

  companion object {
    fun find(data: ByteArray): MagicNumber? =
      entries.find { it.test(data, it.magic.toByteArray()) }
  }

}

fun ByteArray.findMagicNumber(): MagicNumber? = MagicNumber.find(this)

@OptIn(ExperimentalUnsignedTypes::class)
private fun String.toUByteArray() = toCharArray().map {
  it.code.toUByte()
}.toUByteArray()

fun ByteArray.startsWith(
  prefix: ByteArray
): Boolean =
  (size >= prefix.size)
      && slice(prefix.indices)
    .toByteArray()
    .contentEquals(prefix)
