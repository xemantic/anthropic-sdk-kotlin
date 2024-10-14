package com.xemantic.anthropic.schema

import com.xemantic.anthropic.anthropicJson
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

@Serializable
@SerialName("address")
data class Address(
  val street: String? = null,
  val city: String? = null,
  val zipCode: String,
  val country: String
)

@Serializable
data class Person(
  val name: String,
  val age: Int,
  val email: String?,
  val hobbies: List<String> = emptyList<String>(),
  val address: Address? = null
)

class JsonSchemaGeneratorTest {

  private val json = Json(from = anthropicJson) {
    prettyPrint = true
    @OptIn(ExperimentalSerializationApi::class)
    prettyPrintIndent = "  "
  }

  @Test
  fun generateJsonSchemaForAddress() {
    // when
    val schema = jsonSchemaOf<Address>()
    val schemaJson = json.encodeToString(schema)

    // then
    schemaJson shouldEqualJson """
    {
      "type": "object",      
      "properties": {
        "street": {
          "type": "string"
        },
        "city": {
          "type": "string"
        },
        "zipCode": {
          "type": "string"
        },
        "country": {
          "type": "string"
        }
      },
      "required": [
        "zipCode",
        "country"
      ]
    }
    """.trimIndent()
  }

  @Test
  fun generateSchemaForJson() {
    // when
    val schema = jsonSchemaOf<Person>()
    val schemaJson = json.encodeToString(schema)

    // then
    schemaJson shouldEqualJson """
    {
      "type": "object",
      "definitions": {
        "address": {
          "type": "object",
          "properties": {
            "street": {
              "type": "string"
            },
            "city": {
              "type": "string"
            },
            "zipCode": {
              "type": "string"
            },
            "country": {
              "type": "string"
            }
          },
          "required": [
            "zipCode",
            "country"
          ]
        }
      },
      "properties": {
        "name": {
          "type": "string"
        },
        "age": {
          "type": "integer"
        },
        "email": {
          "type": "string"
        },
        "hobbies": {
          "type": "array",
          "items": {
            "type": "string"
          }
        },
        "address": {
          "${'$'}ref": "#/definitions/address"
        }
      },
      "required": [
        "name",
        "age",
        "email"
      ]
    }
    """.trimIndent()
  }

}
