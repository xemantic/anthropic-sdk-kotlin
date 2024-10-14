package com.xemantic.anthropic.schema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JsonSchema(
  val type: String = "object",
  val definitions: Map<String, JsonSchema>? = null,
  val properties: Map<String, JsonSchemaProperty>? = null,
  val required: List<String>? = null,
  @SerialName("\$ref")
  var ref: String? = null
)

@Serializable
data class JsonSchemaProperty(
  val type: String? = null,
  val items: JsonSchemaProperty? = null,
  val enum: List<String>? = null,
  @SerialName("\$ref")
  val ref: String? = null
) {

  companion object {
    val STRING = JsonSchemaProperty("string")
    val INTEGER = JsonSchemaProperty("integer")
    val NUMBER = JsonSchemaProperty("number")
    val BOOLEAN = JsonSchemaProperty("boolean")
  }

}