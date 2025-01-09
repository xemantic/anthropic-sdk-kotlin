/*
 * Copyright 2024 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.ai.anthropic

import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.MessageRequest
import com.xemantic.ai.anthropic.message.MessageResponse
import com.xemantic.ai.anthropic.message.System
import kotlinx.coroutines.runBlocking
import java.util.function.Consumer

// a very early version of Java only SDK, adapting Kotlin idioms and coroutines
// it might change a lot in the future

class JavaAnthropic private constructor(
  private val anthropic: Anthropic
) {

  companion object {

    @JvmStatic
    @JvmOverloads
    fun create(
      configurer: Consumer<Anthropic.Config> = object : Consumer<Anthropic.Config> {
        override fun accept(t: Anthropic.Config) { /* do nothing*/ }
      }
    ): JavaAnthropic = JavaAnthropic(Anthropic { configurer.accept(this) })

  }


  inner class Messages {

    fun createBlocking(
      request: MessageRequest
    ): MessageResponse = runBlocking {
      anthropic.messages.create(request)
    }

  }

  @JvmField
  val messages = Messages()

}

class MessageRequestBuilder() {

  private var system: List<System>? = null

  private var messages: List<Message>? = null

  fun system(system: List<System>): MessageRequestBuilder {
    this.system = system
    return this
  }

  fun messages(messages: List<Message>): MessageRequestBuilder {
    this.messages = messages
    return this
  }

  fun build(): MessageRequest = MessageRequest {
    this.system = system
    this.messages = messages
  }

  companion object {
    @JvmStatic
    fun builder(): MessageRequestBuilder = MessageRequestBuilder()
  }

}
