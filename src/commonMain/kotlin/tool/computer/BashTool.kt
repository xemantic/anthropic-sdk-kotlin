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

package com.xemantic.ai.anthropic.tool.computer

import com.xemantic.ai.anthropic.tool.BuiltInTool
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
@SerialName("bash")
class BashTool private constructor() : BuiltInTool(
    name = "bash",
    type = "bash_20250124"
) {

    /**
     * Creates `bash` tool input.
     *
     * @param command The bash command to run. Required unless the tool is being restarted.
     * @param restart Specifying true will restart this tool. Otherwise, leave this unspecified.
     */
    @Serializable
    class Input private constructor(
        val command: String?,
        val restart: Boolean?
    ) {

        class Builder {

            var command: String? = null
            var restart: Boolean? = null

            fun build(): Input = Input(
                command,
                restart
            )

        }

    }

    class Builder {

        fun build(): BashTool = BashTool()

    }

    companion object {

        fun Input(
            block: Input.Builder.() -> Unit
        ): Input = Input.Builder().apply(block).build()

    }

}

fun BashTool(
    builder: BashTool.Builder.() -> Unit = {},
    run: suspend BashTool.Input.() -> Unit
): BashTool = BashTool.Builder().apply(builder).build().apply {
    inputSerializer = serializer<BashTool.Input>()
    runner = { input -> run(input as BashTool.Input) }
}
