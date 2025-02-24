/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.tool.test.FibonacciCalculator
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

class ToolSerializationTest {

    @Test
    fun `should serialize Calculator tool`() {
        val tool = Tool<FibonacciCalculator>()
        anthropicJson.encodeToString(tool) shouldEqualJson /* language=json */ """
            {
              "name": "fibonacci_calculator",
              "description": "Calculates the n-th fibonacci number",
              "input_schema": {
                "type": "object",
                "properties": {
                  "n": {
                    "type": "integer"
                  }
                },
                "required": [
                  "n"
                ]
              }
            }
        """
    }

    @Test
    fun `should deserialize Calculator tool`() {
        anthropicJson.decodeFromString<Tool>(/* language=json */ """
            {
              "name": "fibonacci_calculator",
              "description": "Calculates the n-th fibonacci number",
              "input_schema": {
                "type": "object",
                "properties": {
                  "n": {
                    "type": "integer"
                  }
                },
                "required": [
                  "n"
                ]
              }
            }
        """
        ) should {
            have(name == "fibonacci_calculator")
            have(description == "Calculates the n-th fibonacci number")
            inputSchema.toString() shouldEqualJson """
                {
                  "type": "object",
                  "properties": {
                    "n": {
                      "type": "integer"
                    }
                  },
                  "required": [
                    "n"
                  ]
                }
            """
        }
    }

}
