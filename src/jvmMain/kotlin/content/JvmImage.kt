package com.xemantic.anthropic.content

import java.io.File

fun Image(path: String): Image = Image {
  path(path)
}

fun Image.Builder.path(path: String) = file(File(path))

fun Image.Builder.file(file: File) {
  bytes = file.readBytes()
}
