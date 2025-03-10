/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
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

import com.xemantic.ai.anthropic.message.MessageRequest
import com.xemantic.ai.anthropic.message.MessageResponse
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
                override fun accept(t: Anthropic.Config) {
                    /* do nothing*/
                }
            }
        ): JavaAnthropic = JavaAnthropic(Anthropic { configurer.accept(this) })

    }


    inner class Messages {

        fun createBlocking(
            builder: Consumer<MessageRequest.Builder>
        ): MessageResponse = runBlocking {
            val kotlinBuilder = MessageRequest.Builder()
            builder.accept(kotlinBuilder)
            anthropic.messages.create(kotlinBuilder.build())
        }

    }

    @JvmField
    val messages = Messages()

}

inline fun <reified T> noOpConsumer(): Consumer<T> = object : Consumer<T> {
    override fun accept(any: T) {
        /* do nothing*/
    }
}
