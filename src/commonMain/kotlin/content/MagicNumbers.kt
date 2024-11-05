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
