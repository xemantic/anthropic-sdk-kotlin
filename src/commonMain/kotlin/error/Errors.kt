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
