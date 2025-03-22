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
@SerialName("computer")
class ComputerTool private constructor(
    @SerialName("display_width_px")
    val displayWidthPx: Int,
    @SerialName("display_height_px")
    val displayHeightPx: Int,
    @SerialName("display_number")
    val displayNumber: Int? = null
) : BuiltInTool(
    name = "computer",
    type = "computer_20250124"
) {

    @Suppress("unused")
    enum class Action {

        @SerialName("key")
        KEY,

        @SerialName("type")
        TYPE,

        @SerialName("mouse_move")
        MOUSE_MOVE,

        @SerialName("left_click")
        LEFT_CLICK,

        @SerialName("left_click_drag")
        LEFT_CLICK_DRAG,

        @SerialName("right_click")
        RIGHT_CLICK,

        @SerialName("middle_click")
        MIDDLE_CLICK,

        @SerialName("double_click")
        DOUBLE_CLICK,

        @SerialName("screenshot")
        SCREENSHOT,

        @SerialName("cursor_position")
        CURSOR_POSITION
    }

    @Serializable
    data class Coordinate(val x: Int, val y: Int)

    @Serializable
    class Input private constructor(
        val action: Action,
        val coordinate: Coordinate?,
        val text: String?
    ) {

        class Builder {

            var action: Action? = null
            var coordinate: Coordinate? = null
            var text: String? = null

            fun build(): Input = Input(
                requireNotNull(action) { "action cannot be null" },
                coordinate,
                text
            )

        }

    }

    class Builder {

        var displayWidthPx: Int? = null
        var displayHeightPx: Int? = null
        var displayNumber: Int? = null

        fun build(): ComputerTool = ComputerTool(
            requireNotNull(displayWidthPx) { "displayWidthPx cannot be null" },
            requireNotNull(displayHeightPx) { "displayHeightPx cannot be null" },
            displayNumber
        )

    }

    companion object {

        fun Input(
            block: Input.Builder.() -> Unit
        ): Input = Input.Builder().apply(block).build()

    }

}

fun ComputerTool(
    builder: ComputerTool.Builder.() -> Unit,
    run: suspend ComputerTool.Input.() -> Unit
): ComputerTool = ComputerTool.Builder().apply(builder).build().apply {
    inputSerializer = serializer<ComputerTool.Input>()
    runner = { input -> run(input as ComputerTool.Input) }
}
