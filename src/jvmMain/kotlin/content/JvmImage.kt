package com.xemantic.anthropic.content

import java.io.File

fun Image.Builder.path(path: String) = file(File(path))

fun Image.Builder.file(file: File) {
  data = file.readBytes()
}
