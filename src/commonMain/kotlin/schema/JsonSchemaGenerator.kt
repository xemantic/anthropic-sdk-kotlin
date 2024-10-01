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
    val property = generateSchemaProperty(elementDescriptor, definitions)
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
  definitions: MutableMap<String, JsonSchema>
): JsonSchemaProperty {
  return when (descriptor.kind) {
    PrimitiveKind.STRING -> JsonSchemaProperty.STRING
    PrimitiveKind.INT, PrimitiveKind.LONG -> JsonSchemaProperty.INTEGER
    PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> JsonSchemaProperty.NUMBER
    PrimitiveKind.BOOLEAN -> JsonSchemaProperty.BOOLEAN
    SerialKind.ENUM -> enumProperty(descriptor)
    StructureKind.LIST -> JsonSchemaProperty(
      type = "array",
      items = generateSchemaProperty(
        descriptor.getElementDescriptor(0),
        definitions
      )
    )
    StructureKind.MAP -> JsonSchemaProperty("object")
    StructureKind.CLASS -> {
      val refName = descriptor.serialName.trimEnd('?')
      definitions[refName] = generateSchema(descriptor)
      JsonSchemaProperty("\$ref", ref = "#/definitions/$refName")
    }
    else -> JsonSchemaProperty("object") // Default case
  }
}

private fun enumProperty(
  descriptor: SerialDescriptor
) = JsonSchemaProperty(
  enum = descriptor.elementNames()
)

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.elementNames(): List<String> = buildList {
  for (i in 0 until elementsCount) {
    val name = getElementName(i)
    add(name)
  }
}
