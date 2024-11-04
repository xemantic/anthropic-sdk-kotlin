package com.xemantic.anthropic.content

import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Message
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
  val client = Anthropic {
    anthropicBeta = "pdfs-2024-09-25"
  }

  val response = client.messages.create {
    +Message {
      +Document("/home/morisil/Downloads/Kazik_Pogoda-cv-2025-10-28.pdf")
      +"Extract data from this PDF and return it as a markdown"
    }
  }

  println(response)
}
