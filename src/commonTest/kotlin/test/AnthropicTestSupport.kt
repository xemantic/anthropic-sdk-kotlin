package com.xemantic.anthropic.test

import com.xemantic.anthropic.anthropicJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

/**
 * Asserts certain conditions on an object of type [T].
 *
 * @param T The type of the object to assert against.
 * @param block A lambda with receiver that defines the assertions to be performed on the object.
 */
@OptIn(ExperimentalContracts::class)
inline fun <reified T: Any> T.assert(block: T.() -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  block(this)
}
