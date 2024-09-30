package com.xemantic.anthropic.message

import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun Image(
  path: String,
  mediaType: Image.MediaType
): Image = Image(
  file = File(path),
  mediaType
)

@OptIn(ExperimentalEncodingApi::class)
fun Image(
  file: File,
  mediaType: Image.MediaType
): Image = Image(
  source = Image.Source(
    data =  Base64.encode(file.readBytes()),
    mediaType = mediaType
  )
)
