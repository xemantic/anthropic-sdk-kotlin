package com.xemantic.anthropic.content

import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun Document(path: String): Document = Document(File(path))

// TODO in the future this can be moved to jvmAndPosixMain
// TODO in the future, if more types are supported, the magic number should be used to determine the media type.
@OptIn(ExperimentalEncodingApi::class)
fun Document(path: File): Document = Document(
  source = Document.Source(
    mediaType = Document.MediaType.APPLICATION_PDF,
    data = Base64.encode(path.readBytes())
  )
)
