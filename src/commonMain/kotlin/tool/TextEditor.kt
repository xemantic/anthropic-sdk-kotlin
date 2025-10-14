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

package com.xemantic.ai.anthropic.tool

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("str_replace_based_edit_tool")
class TextEditor private constructor(
    @SerialName("max_characters")
    val maxCharacters: Int? = null
) : BuiltInTool<TextEditor.Input>(
    name = "str_replace_based_edit_tool",
    type = "text_editor_20250728"
) {

    @Suppress("unused")
    enum class Command {

        @SerialName("view")
        VIEW,

        @SerialName("create")
        CREATE,

        @SerialName("str_replace")
        STR_REPLACE,

        @SerialName("insert")
        INSERT
    }

    @Serializable
    class Input private constructor(
        val command: Command,
        @SerialName("file_text")
        val fileText: String?,
        @SerialName("insert_line")
        val insertLine: Int?,
        @SerialName("new_str")
        val newStr: String?,
        @SerialName("old_str")
        val oldStr: String?,
        @SerialName("path")
        val path: String,
        @SerialName("view_range")
        val viewRange: List<Int>?
    ) {

        class Builder {

            var command: Command? = null
            var fileText: String? = null
            var insertLine: Int? = null
            var newStr: String? = null
            var oldStr: String? = null
            var path: String? = null
            var viewRange: List<Int>? = null

            fun build(): Input = Input(
                requireNotNull(command) { "command cannot be null" },
                fileText,
                insertLine,
                newStr,
                oldStr,
                requireNotNull(path) { "path cannot be null" },
                viewRange
            )

        }

    }

    class Builder {

        var maxCharacters: Int? = null

        fun build(): TextEditor = TextEditor(
            maxCharacters = maxCharacters
        )

    }

    companion object {

        fun Input(
            block: Input.Builder.() -> Unit
        ): Input = Input.Builder().apply(block).build()

    }

}

fun TextEditor(
    builder: TextEditor.Builder.() -> Unit = {},
): TextEditor = TextEditor.Builder().apply(builder).build()
