package com.xemantic.anthropic.tool.editor

import com.xemantic.anthropic.cache.CacheControl
import com.xemantic.anthropic.tool.BuiltInTool
import com.xemantic.anthropic.tool.ToolInput
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("str_replace_editor")
@OptIn(ExperimentalSerializationApi::class)
data class TextEditor(
  override val cacheControl: CacheControl? = null
) : BuiltInTool(
  name = "str_replace_editor",
  type = "text_editor_20241022"
) {

  init {
    inputSerializer = Input.serializer()
  }

  @Serializable
  data class Input(
    val command: Command,
    @SerialName("file_text")
    val fileText: String? = null,
    @SerialName("insert_line")
    val insertLine: Int? = null,
    @SerialName("new_str")
    val newStr: String? = null,
    @SerialName("old_str")
    val oldStr: String? = null,
    @SerialName("path")
    val path: String,
    @SerialName("view_range")
    val viewRange: Int? = 0
  ) : ToolInput() {

    @Transient
    lateinit var service: TextEditorService

    init {
      use {
        when (command) {
          Command.VIEW -> service.view(path)
          else -> TODO("not implemented yet")
        }
      }
    }

  }

}

@Serializable
enum class Command {
  @SerialName("view")
  VIEW,
  @SerialName("create")
  CREATE,
  @SerialName("str_replace")
  STR_REPLACE,
  @SerialName("insert")
  INSERT,
  @SerialName("undo_edit")
  UNDO_EDIT
}

interface TextEditorService {

  fun view(path: String)

}
