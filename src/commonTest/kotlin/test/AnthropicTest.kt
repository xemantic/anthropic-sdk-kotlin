package com.xemantic.anthropic.test

import kotlin.reflect.KClass
import kotlin.test.assertTrue

fun <T> then(value: T, block: T.() -> Unit) {
  block(value)
}

infix fun <T> T.shouldBe(expected: T): Unit = assert(expected == this)

fun <T> T.shouldBe(expected: T, message: () -> String): Unit = assert(this == expected, message)

//infix fun <T : Any> T.shouldBe(expected: KClass<T>) {
//  assert(this is expected)
//}
