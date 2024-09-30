package com.xemantic.anthropic.message

fun <T> List<T>.toNullIfEmpty(): List<T>? = if (isEmpty()) null else this
