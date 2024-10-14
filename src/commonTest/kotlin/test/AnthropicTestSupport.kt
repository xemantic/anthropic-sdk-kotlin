package com.xemantic.anthropic.test

import com.xemantic.anthropic.anthropicJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * A pretty JSON printing for testing. It's derived from [anthropicJson],
 * therefore should use the same rules for serialization/deserialization, but
 * it has `prettyPrint` and 2 space tab enabled in addition.
 */
val testJson = Json(from = anthropicJson) {
  prettyPrint = true
  @OptIn(ExperimentalSerializationApi::class)
  prettyPrintIndent = "  "
}
