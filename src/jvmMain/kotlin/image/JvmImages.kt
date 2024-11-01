package com.xemantic.anthropic.image

import java.io.File

fun Image.Builder.path(path: String) = file(File(path))

fun Image.Builder.file(file: File) {
  data = file.readBytes()
}
