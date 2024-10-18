package com.xemantic.anthropic.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@MetaSerializable
annotation class Description(
  val value: String
)

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
  val description: String? = null,
  val items: JsonSchemaProperty? = null,
  val enum: List<String>? = null,
  @SerialName("\$ref")
  val ref: String? = null
)
