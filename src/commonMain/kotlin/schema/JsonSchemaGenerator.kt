package com.xemantic.anthropic.schema

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlin.collections.set

inline fun <reified T> jsonSchemaOf(): JsonSchema = generateSchema(
  serializer<T>().descriptor
)

@OptIn(ExperimentalSerializationApi::class)
fun generateSchema(descriptor: SerialDescriptor): JsonSchema {
  val properties = mutableMapOf<String, JsonSchemaProperty>()
  val required = mutableListOf<String>()
  val definitions = mutableMapOf<String, JsonSchema>()

  for (i in 0 until descriptor.elementsCount) {
    val name = descriptor.getElementName(i)
    val elementDescriptor = descriptor.getElementDescriptor(i)
    val elementAnnotations = descriptor.getElementAnnotations(i)
    val property = generateSchemaProperty(
      elementDescriptor,
      description = elementAnnotations
        .filterIsInstance<Description>()
        .firstOrNull()
        ?.value,
      definitions
    )
    properties[name] = property
    if (!descriptor.isElementOptional(i)) {
      required.add(name)
    }
  }

  return JsonSchema(
    type = "object",
    properties = properties,
    required = required,
    definitions = if (definitions.isNotEmpty()) definitions else null
  )
}

@OptIn(ExperimentalSerializationApi::class)
private fun generateSchemaProperty(
  descriptor: SerialDescriptor,
  description: String?,
  definitions: MutableMap<String, JsonSchema>
): JsonSchemaProperty {
  return when (descriptor.kind) {
    PrimitiveKind.STRING -> JsonSchemaProperty("string", description)
    PrimitiveKind.INT, PrimitiveKind.LONG -> JsonSchemaProperty("integer", description)
    PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> JsonSchemaProperty("number", description)
    PrimitiveKind.BOOLEAN -> JsonSchemaProperty("boolean", description)
    SerialKind.ENUM -> enumProperty(descriptor, description)
    StructureKind.LIST -> JsonSchemaProperty(
      type = "array",
      items = generateSchemaProperty(
        descriptor.getElementDescriptor(0),
        description,
        definitions
      )
    )
    StructureKind.MAP -> JsonSchemaProperty("object", description)
    StructureKind.CLASS -> {
      // dots are not allowed in JSON Schema name, if the @SerialName was not
      // specified, then fully qualified class name will be used, and we need
      // to translate it
      val refName = descriptor.serialName.replace('.', '_').trimEnd('?')
      definitions[refName] = generateSchema(descriptor)
      JsonSchemaProperty(
        ref = "#/definitions/$refName",
        description = description
      )
    }
    else -> JsonSchemaProperty("object", description) // Default case
  }
}

private fun enumProperty(
  descriptor: SerialDescriptor,
  description: String?
) = JsonSchemaProperty(
  type = "string",
  enum = descriptor.elementNames(),
  description = description,
)

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.elementNames(): List<String> = buildList {
  for (i in 0 until elementsCount) {
    val name = getElementName(i)
    add(name)
  }
}
