package com.xemantic.anthropic

import com.xemantic.anthropic.message.MessageRequest
import kotlinx.coroutines.runBlocking
import java.util.function.Consumer

actual val envApiKey: String?
  get() = System.getenv("ANTHROPIC_API_KEY")

actual val missingApiKeyMessage: String
  get() = "apiKey is missing, it has to be provided as a parameter or as an ANTHROPIC_API_KEY environment variable."

// a very early version of Java only SDK, adapting Kotlin idioms and coroutines
// it might change a lot in the future
class JavaAnthropic {

  companion object {

    @JvmStatic
    fun create(): Anthropic = Anthropic()

    @JvmStatic
    fun create(
      configurer: Consumer<Anthropic.Config>
    ): Anthropic {
      return Anthropic { configurer.accept(this) }
    }

  }

}

fun Anthropic.createMessage(
  builder: Consumer<MessageRequest.Builder>
): MessageResponse = runBlocking {
  messages.create {
    builder.accept(this)
  }
}
