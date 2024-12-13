/*
 * Copyright 2024 Kazimierz Pogoda / Xemantic
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

package com.xemantic.anthropic.error

import com.xemantic.anthropic.Response
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("error")
data class ErrorResponse(
  val error: Error
) : Response(type = "error")

@Serializable
data class Error(
  val type: String, val message: String
)

/**
 * An exception thrown when API requests returns error.
 */
class AnthropicException(
  error: Error,
  httpStatusCode: HttpStatusCode
) : RuntimeException(error.toString())
