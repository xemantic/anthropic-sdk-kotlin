package com.xemantic.anthropic.content

import java.io.File
import kotlin.io.encoding.ExperimentalEncodingApi

fun Document(path: String): Document = Document {
  path(path)
}

fun Document.Builder.path(path: String) = file(File(path))

@OptIn(ExperimentalEncodingApi::class)
fun Document.Builder.file(file: File) {
  bytes = file.readBytes()
}
