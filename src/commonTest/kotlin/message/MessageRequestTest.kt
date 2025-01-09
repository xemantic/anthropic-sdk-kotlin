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

package com.xemantic.ai.anthropic.message

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.ai.anthropic.tool.AnthropicTool
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.tool.ToolChoice
import com.xemantic.ai.anthropic.tool.ToolInput
import com.xemantic.ai.anthropic.tool.bash.Bash
import com.xemantic.ai.anthropic.tool.computer.Computer
import com.xemantic.ai.anthropic.tool.editor.TextEditor
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Tests the JSON serialization format of created Anthropic API message requests.
 */
class MessageRequestTest {

  @Test
  fun `Should create simple MessageRequest`() {
    // given
    val request = MessageRequest {
      +Message {
        +"Hey Claude!?"
      }
    }

    // when
    val json = anthropicJson.encodeToString(request)

    // then
    json shouldEqualJson /* language=json */ """
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
    """
    // Note: max_tokens value will default to the max for a given model
    // claude-3-5-sonnet-latest ist the default model
  }

  // now we need some test tool
  @AnthropicTool("get_weather")
  @Description("Get the weather for a specific location")
  data class GetWeather(
    @Description("The city and state, e.g. San Francisco, CA")
    val location: String,
    val unit: TemperatureUnit? = null
  ) : ToolInput() {
    init {
      use {
        "42"
      }
    }
  }

  @Description("The unit of temperature, either 'celsius' or 'fahrenheit'")
  @Suppress("unused") // it is used by the serializer
  enum class TemperatureUnit {
    @SerialName("celsius")
    CELSIUS,
    @SerialName("fahrenheit")
    FAHRENHEIT
  }

  @Test
  fun `Should create MessageRequest with multiple tools`() {
    // given
    val request = MessageRequest {
      +Message {
        +"Hey Claude!?"
      }
      tools = listOf(
        // built in tools
        Computer(
          displayWidthPx = 1024,
          displayHeightPx = 768,
          displayNumber = 1
        ),
        TextEditor(),
        Bash(),
        // custom tool
        Tool<GetWeather>()
      )
    }

    // when
    val json = anthropicJson.encodeToString(request)

    // then
    json shouldEqualJson /* language=json */ """
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
            "description": "Get the weather for a specific location",
            "input_schema": {
              "type": "object",
              "properties": {
                "location": {
                  "type": "string",
                  "description": "The city and state, e.g. San Francisco, CA"
                },
                "unit": {
                  "type": "string",
                  "description": "The unit of temperature, either 'celsius' or 'fahrenheit'",
                  "enum": ["celsius", "fahrenheit"]
                }
              },
              "required": ["location"]
            }
          }          
        ]
      }
    """
  }

  @Test
  fun `Should create MessageRequest with explicit ToolChoice`() {
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
    val json = anthropicJson.encodeToString(request)

    // then
    json shouldEqualJson /* language=json */ """
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
            "description": "Get the weather for a specific location",
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
    """
  }

  @Test
  fun `Should deserialize MessageRequest - for example a JSON stored on disk`() {
    // given
    val request = /* language=json */ """
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
    """

    // when
    val messageRequest = anthropicJson.decodeFromString<MessageRequest>(request)

    // then
    messageRequest.toString() shouldEqualJson request
  }

  @Test
  @Ignore // TODO this test can be fixed only when the model is refactored to be configurable
  fun `Should fail to create a MessageRequest instance for unknown model`() {
    //val messageRequest = MessageRequest {  }
  }

}
