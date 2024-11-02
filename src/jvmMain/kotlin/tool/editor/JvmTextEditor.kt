package com.xemantic.anthropic.tool.editor

import com.xemantic.anthropic.content.ToolResult
import java.io.File

object JvmTextEditorService : TextEditorService {

  override suspend fun use(
    toolUseId: String,
    input: TextEditor.Input
  ): ToolResult {
    val content = if (input.command == Command.VIEW) {
      File(input.path).readText()
    } else {
      TODO("Not implemented yet")
    }
    return ToolResult(toolUseId) {
      content(content)
    }
  }

}

actual val textEditorService: TextEditorService get() = JvmTextEditorService
