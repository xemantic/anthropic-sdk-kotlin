package com.xemantic.anthropic.tool.computer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
  CURSOR_POSITION,
}

@Serializable
data class Resolution(
  val width: Int,
  val height: Int
)
