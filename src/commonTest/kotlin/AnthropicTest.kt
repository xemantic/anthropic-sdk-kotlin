package com.xemantic.anthropic

import com.xemantic.anthropic.event.ContentBlockDelta
import com.xemantic.anthropic.event.Delta.TextDelta
import com.xemantic.anthropic.message.Image
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.MessageResponse
import com.xemantic.anthropic.message.Role
import com.xemantic.anthropic.message.StopReason
import com.xemantic.anthropic.message.Text
import com.xemantic.anthropic.message.Tool
import com.xemantic.anthropic.message.ToolChoice
import com.xemantic.anthropic.message.ToolUse
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnthropicTest {

  @Test
  fun shouldReceiveAnIntroductionFromClaude() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.create {
      +Message {
        +"Hello World! What's your name?"
      }
      model = "claude-3-opus-20240229"
      maxTokens = 1024
    }

    // then
    response.apply {
      assertTrue(type == MessageResponse.Type.MESSAGE)
      assertTrue(role == Role.ASSISTANT)
      assertTrue(model == "claude-3-opus-20240229")
      assertTrue(content.size == 1)
      assertTrue(content[0] is Text)
      val text = content[0] as Text
      assertTrue(text.text.contains("Claude"))
      assertTrue(stopReason == StopReason.END_TURN)
      assertNull(stopSequence)
      assertEquals(usage.inputTokens, 15)
      assertTrue(usage.outputTokens > 0)
    }
  }

  @Test
  fun shouldReadTextFooFromTestImage() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.create {
      +Message {
        +Image(
          source = Image.Source(
            data = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAACXBIWXMAABOvAAATrwFj5o7DAAAAGXRFWHRTb2Z0d2FyZQB3d3cuaW5rc2NhcGUub3Jnm+48GgAAA61JREFUeJztmNtrFVcUxn/b+lYwxEuN8RKR1giiQmqxEomCghSU0krBUpqiTyraUn3oU+m/UPsoiD6JiiBeW4T64P1FKBSLGlBDjE21NV7r/SwfZp1zhmHPmRnjnPWQ/cFmcfZe69tfvuyZWTNORBjNGGMtwBrBAGsB1ggGWAuwRjDAWoA1ggHWAqwRDLAWYI1ggLUAawQDrAVYIxhgLcAawQBrAdYIBlgLsMbYZm/onGsBJhOZPyQi90bINw5oA94B/hGRu4Xq0z6LO+d2AT0FuM6IyDcpXJOArcBnQGdi+TKwD/glr3jnXCvwPfA5MDex3AfsV77bmWQi4h3Ar4AUGCdSeNYC9xO5j4Gnibn/gDVpemJ8nwJ3E7X/A08Sc/eBrzL5chjwB/BFjtHj4dgAVJTnJrAZmKJrY4CZwI/AHc15BaxvoOlr4KXmDhGdqum65oDpwA+6Jrr3lpEa8FuWiyn1i4DnyvE70NogtwP4U3OfAQs8OfP1Py3AWWBSA7524KLmvgA+tjDgtNb3AS058juAR2mXE3Bc1waAiTn42oBhrTnXVAOArti1mHqkPXU/xY5uZ2y+M3YpfVeAb1tMR5cvp6w+YIXGCnCgQN1ejQ5YFZtfrnMQPTGK8FUfc6t9CWUZsEjjZRF5kLdIRK4At/TnHA/fgIgMFeAbBK56+GrIY8BK55xkjPWJmvc0/p1XbAxVA6aWyFdDWSdgnMZHb1Bb7QzHv2W+Cb7FPK3weWBLRk5/4ve/Gt/NwZ9Ei8b4H/u2+WrIY8ADEblYcNObGjuKFDnnHDBLf14bKZ/ifQ9fDWVdAqc1fuCcm1agbh71o3ohNn9KY5tzznsz88E5NwuY4eGroSwDDhB1dAAbC9Rt0vgMOBqbPwI8TOQU4XsFHPRmlNgJ7tT6J6Q0IYn8Hup9/g7P+s/UW9vuHHwL1UgB9jS1E9T6yUSPICF62VnaIPcToru1AIN4+nygFbiuOcPAygZ8y4hunNW9pzbdAOXoov6mVwEOAb1AN7AEWEe9x6+K/bAB39yYqQIcU45uHb26R7VtHs46LaUaoDwdwImYaN+o6H4zc/C1A4cz+AQ4CczO4mv0RehbdfwvEdnuTSoA59xioo8jHwFTiO4N/UTfG3ZL1AYX4VsIfEnUJrcTvXrfIHqt3i0il3LxpBkwWjDqvwoHA6wFWCMYYC3AGsEAawHWCAZYC7BGMMBagDWCAdYCrBEMsBZgjWCAtQBrBAOsBVgjGGAtwBqvASNwHwnSLggJAAAAAElFTkSuQmCC",
            mediaType = Image.MediaType.IMAGE_PNG
          )
        )
        +"What's on this picture?"
      }
    }

    // then
    response.apply {
      assertTrue(1 == content.size)
      assertTrue(content[0] is Text)
      val text = content[0] as Text
      assertTrue(text.text.lowercase().contains("foo"))
    }
  }

  @Test
  fun shouldStreamTheResponse() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.stream {
        +Message {
          role = Role.USER
          +"Say: 'The quick brown fox jumps over the lazy dog'"
        }
      }
        .filterIsInstance<ContentBlockDelta>()
        .map { (it.delta as TextDelta).text }
        .toList()
        .joinToString(separator = "")

    // then
    assertTrue(response == "The quick brown fox jumps over the lazy dog.")
  }

  @Test
  fun shouldUseSimpleTool() = runTest {
    // given
    val client = Anthropic()


    // when
    val response = client.messages.stream {
      +Message {
        role = Role.USER
        +"Say: 'The quick brown fox jumps over the lazy dog'"
      }
    }
      .filterIsInstance<ContentBlockDelta>()
      .map { (it.delta as TextDelta).text }
      .toList()
      .joinToString(separator = "")

    // then
    assertTrue(response == "The quick brown fox jumps over the lazy dog.")
  }

  @Test
  fun shouldUseCalculatorTool() = runTest {
    // given
    val client = Anthropic()
    // soon the Tool will use generic serializable type and the schema
    // will be generated automatically
    val calculatorTool = Tool(
      name = "calculator",
      description = "Perform basic arithmetic operations",
      inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
          put("operation", buildJsonObject {
            put("type", "string")
            putJsonArray("enum") {
              add("add")
              add("subtract")
              add("multiply")
              add("divide")
            }
          })
          put("a", buildJsonObject { put("type", "number") })
          put("b", buildJsonObject { put("type", "number") })
        })
        putJsonArray("required") {
          add("operation")
          add("a")
          add("b")
        }
      },
      cacheControl = null
    )

    // when
    val response = client.messages.create {
      +Message {
        role = Role.USER
        +"What's 15 multiplied by 7?"
      }
      tools = listOf(calculatorTool)
      toolChoice = ToolChoice.Any()
    }

    // then
    response.apply {
      assertTrue(content.size == 1)
      assertTrue(content[0] is ToolUse)
      val toolUse = content[0] as ToolUse
      assertTrue(toolUse.name == "calculator")
      val input = toolUse.input.jsonObject
      assertTrue(input["operation"]?.jsonPrimitive?.content == "multiply")
      assertTrue(input["a"]?.jsonPrimitive?.double == 15.0)
      assertTrue(input["b"]?.jsonPrimitive?.double == 7.0)
    }
  }

}
