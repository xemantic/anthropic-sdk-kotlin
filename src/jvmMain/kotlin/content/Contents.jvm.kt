/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.noOpConsumer
import java.util.function.Consumer

class Contents private constructor() {

    companion object {

        @JvmStatic
        fun text(
            builder: Consumer<Text.Builder>
        ): Text = Text.Builder().also {
            builder.accept(it)
        }.build()

        @JvmStatic
        @JvmOverloads
        fun text(
            text: String,
            builder: Consumer<Text.Builder> = noOpConsumer()
        ): Text = Text.Builder().also {
            it.text = text
            builder.accept(it)
        }.build()
    }

}
