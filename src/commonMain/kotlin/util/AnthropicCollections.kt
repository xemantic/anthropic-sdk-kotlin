/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic.util

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

fun <T> List<T>.transformLast(
    transform: (T) -> T
): List<T> = mapIndexed { index, item ->
    if (index == lastIndex) transform(item) else item
}

/**
 * Extension function that implements atomic update operation
 * by repeatedly trying to update the value until successful.
 */
@OptIn(ExperimentalAtomicApi::class)
inline fun <T> AtomicReference<T>.update(transform: (T) -> T) {
    var current: T
    var next: T
    do {
        current = load()
        next = transform(current)
    } while (!compareAndSet(current, next))
}
