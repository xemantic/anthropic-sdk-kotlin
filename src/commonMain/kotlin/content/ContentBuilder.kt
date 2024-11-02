package com.xemantic.anthropic.content

import com.xemantic.anthropic.message.Content

interface ContentBuilder {

  val content: MutableList<Content>

  operator fun Content.unaryPlus() {
    content += this
  }

  operator fun String.unaryPlus() {
    content += Text(this)
  }

  operator fun Number.unaryPlus() {
    content += Text(this.toString())
  }

  operator fun Collection<Content>.unaryPlus() {
    content += this
  }

}
