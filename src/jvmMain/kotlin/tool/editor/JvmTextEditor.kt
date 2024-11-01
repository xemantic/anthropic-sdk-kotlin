package com.xemantic.anthropic.tool.editor

import com.xemantic.anthropic.tool.ToolResult
import java.io.File

object JvmTextEditorService : TextEditorService {

  override suspend fun use(
    toolUseId: String,
    input: TextEditor.Input
  ): ToolResult = ToolResult(
    toolUseId = toolUseId,
    if (input.command == Command.VIEW) {
      File(input.path).readText()
    } else {
      "Not implemented yet"
    }
  )

}

actual val textEditorService: TextEditorService get() = JvmTextEditorService
