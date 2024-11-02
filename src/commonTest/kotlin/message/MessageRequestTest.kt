package com.xemantic.anthropic.message

import com.xemantic.anthropic.content.ToolResult
import com.xemantic.anthropic.message.MessageRequestTest.TemperatureUnit
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.test.testJson
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.Tool
import com.xemantic.anthropic.tool.ToolChoice
import com.xemantic.anthropic.tool.ToolInput
import com.xemantic.anthropic.tool.bash.Bash
import com.xemantic.anthropic.tool.computer.Computer
import com.xemantic.anthropic.tool.editor.TextEditor
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlin.test.Test

// given
@AnthropicTool("get_weather")
@Description("Get the current weather in a given location")
data class GetWeather(
  @Description("The city and state, e.g. San Francisco, CA")
  val location: String,
  @Description("The unit of temperature, either 'celsius' or 'fahrenheit'")
  val unit: TemperatureUnit? = null
) : ToolInput {

  override suspend fun use(
    toolUseId: String
  ) = ToolResult(toolUseId) { +"42" }

}

/**
 * Tests the JSON serialization format of created Anthropic API message requests.
 */
class MessageRequestTest {

  @Test
  fun defaultMessageShouldHaveRoleUser() {
    // given
    val message = Message {}
    // then
    message.role shouldBe Role.USER
  }

  @Test
  fun shouldCreateTheSimplestMessageRequest() {
    // given
    val request = MessageRequest {
      +Message {
        +"Hey Claude!?"
      }
    }

    // when
    val json = testJson.encodeToString(request)

    // then
    json shouldEqualJson """
      {
        "model": "claude-3-5-sonnet-latest",
        "messages": [
          {
            "role": "user",
            "content": [
              {
                "type": "text",
                "text": "Hey Claude!?"
              }
            ]
          }
        ],
        "max_tokens": 8182
      }
    """.trimIndent()
  }

  enum class TemperatureUnit {
    @SerialName("celsius")
    CELSIUS,
    @SerialName("fahrenheit")
    FAHRENHEIT
  }

  @Test
  fun shouldCreateMessageRequestWithMultipleTools() {
    // given
    val request = MessageRequest {
      +Message {
        +"Hey Claude!?"
      }
      tools = listOf(
        Computer(
          displayWidthPx = 1024,
          displayHeightPx = 768,
          displayNumber = 1
        ),
        TextEditor(),
        Bash(),
        Tool<GetWeather>()
      )
    }

    // when
    val json = testJson.encodeToString(request)

    // then
    json shouldEqualJson """
      {
        "model": "claude-3-5-sonnet-latest",
        "messages": [
          {
            "role": "user",
            "content": [
              {
                "type": "text",
                "text": "Hey Claude!?"
              }
            ]
          }
        ],
        "max_tokens": 8182,
        "tools": [
          {
            "type": "computer_20241022",
            "name": "computer",
            "display_width_px": 1024,
            "display_height_px": 768,
            "display_number": 1
          },
          {
            "type": "text_editor_20241022",
            "name": "str_replace_editor"
          },
          {
            "type": "bash_20241022",
            "name": "bash"
          },
          {
            "name": "get_weather",
            "description": "Get the current weather in a given location",
            "input_schema": {
              "type": "object",
              "properties": {
                "location": {
                  "type": "string",
                  "description": "The city and state, e.g. San Francisco, CA"
                },
                "unit": {
                  "type": "string",
                  "enum": ["celsius", "fahrenheit"],
                  "description": "The unit of temperature, either 'celsius' or 'fahrenheit'"
                }
              },
              "required": ["location"]
            }
          }          
        ]
      }
    """.trimIndent()
  }

  @Test
  fun shouldCreateMessageRequestWithExplicitToolChoice() {
    // given
    val request = MessageRequest {
      +Message {
        +"What's the weather in Berlin?"
      }
      tools = listOf(
        Tool<GetWeather>()
      )
      toolChoice = ToolChoice.Tool(
        name = "get_weather",
        disableParallelToolUse = true
      )
    }

    // when
    val json = testJson.encodeToString(request)

    // then
    json shouldEqualJson """
      {
        "model": "claude-3-5-sonnet-latest",
        "messages": [
          {
            "role": "user",
            "content": [
              {
                "type": "text",
                "text": "What's the weather in Berlin?"
              }
            ]
          }
        ],
        "max_tokens": 8182,
        "tool_choice": {
          "type": "tool",
          "name": "get_weather",
          "disable_parallel_tool_use": true
        },
        "tools": [
          {
            "name": "get_weather",
            "description": "Get the current weather in a given location",
            "input_schema": {
              "type": "object",
              "properties": {
                "location": {
                  "type": "string",
                  "description": "The city and state, e.g. San Francisco, CA"
                },
                "unit": {
                  "type": "string",
                  "enum": ["celsius", "fahrenheit"],
                  "description": "The unit of temperature, either 'celsius' or 'fahrenheit'"
                }
              },
              "required": ["location"]
            }
          }          
        ]
      }
    """.trimIndent()
  }

  @Test
  fun shouldDeserializeMessageRequestForExampleStoredOnDisk() {
    // given
    val request = """
      {
        "model": "claude-3-5-sonnet-latest",
        "messages": [
          {
            "role": "user",
            "content": [
              {
                "type": "text",
                "text": "Hey Claude!?"
              }
            ]
          }
        ],
        "max_tokens": 8182,
        "tools": [
          {
            "type": "computer_20241022",
            "name": "computer",
            "display_width_px": 1024,
            "display_height_px": 768,
            "display_number": 1
          },
          {
            "type": "text_editor_20241022",
            "name": "str_replace_editor"
          },
          {
            "type": "bash_20241022",
            "name": "bash"
          },
          {
            "name": "get_weather",
            "description": "Get the current weather in a given location",
            "input_schema": {
              "type": "object",
              "properties": {
                "location": {
                  "type": "string",
                  "description": "The city and state, e.g. San Francisco, CA"
                },
                "unit": {
                  "type": "string",
                  "enum": ["celsius", "fahrenheit"],
                  "description": "The unit of temperature, either 'celsius' or 'fahrenheit'"
                }
              },
              "required": ["location"]
            }
          }          
        ]
      }
    """.trimIndent()

    // when
    val messageRequest = testJson.decodeFromString<MessageRequest>(request)

    // then
    // TODO assertions
    println(messageRequest)
  }

}
