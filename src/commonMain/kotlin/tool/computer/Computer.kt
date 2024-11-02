package com.xemantic.anthropic.tool.computer

import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.content.Image
import com.xemantic.anthropic.tool.BuiltInTool
import com.xemantic.anthropic.tool.ToolInput
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("computer")
@OptIn(ExperimentalSerializationApi::class)
data class Computer(
  override val cacheControl: CacheControl? = null,
  @SerialName("display_width_px")
  val displayWidthPx: Int,
  @SerialName("display_height_px")
  val displayHeightPx: Int,
  @SerialName("display_number")
  val displayNumber: Int? = null
) : BuiltInTool(
  name = "computer",
  type = "computer_20241022"
) {

  init {
    inputSerializer = Input.serializer()
  }

  @Serializable
  data class Input(
    val action: Action,
    val coordinate: Coordinate?,
    val text: String
  ) : ToolInput() {

    @Transient
    lateinit var service: ComputerService

    init {
      use {
        when (action) {
          Action.SCREENSHOT -> +service.screenshot()
          else -> TODO("Not implemented yet")
        }
      }
    }

  }

}

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
data class Resolution(val width: Int, val height: Int)

@Serializable
data class Coordinate(val x: Int, val y: Int)

interface ComputerService {

  fun screenshot(): Image

}
