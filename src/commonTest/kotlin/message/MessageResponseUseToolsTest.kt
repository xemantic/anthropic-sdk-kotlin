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

package com.xemantic.ai.anthropic.message

import com.xemantic.ai.anthropic.Model
import com.xemantic.ai.anthropic.content.*
import com.xemantic.ai.anthropic.tool.Toolbox
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.ai.file.magic.MediaType
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MessageResponseUseToolsTest {

    // given
    @Serializable
    @SerialName("foo")
    class Foo(val bar: String)

    class NonSerializable() {
        override fun toString() = "non-serializable"
    }

    private val useFooResponse = MessageResponse(
        id = "foo_42",
        role = Role.ASSISTANT,
        content = listOf(
            ToolUse {
                id = "bar_1234"
                name = "foo"
                input = buildJsonObject {
                    put("bar", JsonPrimitive("buzz"))
                }
            }
        ),
        model = Model.CLAUDE_SONNET_4_5_20250929.id,
        stopReason = StopReason.TOOL_USE,
        stopSequence = null,
        usage = Usage {
            inputTokens = 419
            outputTokens = 86
        }
    )

    @Test
    fun `should return ok text if no action specified for the tool`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo>()
        }

        //when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    have(text == "ok")
                }
            }
        }
    }

    @Test
    fun `should return text from the Tool input if the Tool is a pass-through`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo> {
                bar // it has value "buzz" by default
            }
        }

        //when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    have(text == "buzz")
                }
            }
        }
    }

    @Test
    fun `should return text and be an error if Tool throws an exception`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo> {
                throw Exception("error was thrown")
            }
        }

        // when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == true)
                content!![0] should {
                    be<Text>()
                    have(text == "error was thrown")
                }
            }
        }
    }

    @Test
    fun `should return number as text if the Tool returns Double`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo> {
                1.12345
            }
        }

        // when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    have(text == "1.12345")
                }
            }
        }
    }

    @Test
    fun `should return JSON if the Tool returns serializable class instance`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo> {
                Foo(bar = bar)
            }
        }

        // when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    text shouldEqualJson """
                        {
                          "bar": "buzz"
                        }
                    """
                }
            }
        }
    }

    @Test
    fun `should return toString result if the Tool returns non-serializable class instance`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo> {
                NonSerializable()
            }
        }

        // when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    have(text == "non-serializable")
                }
            }
        }
    }

    @Test
    fun `should return Image if the Tool returns Image`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo> {
                Image {
                    source = Source.Base64 {
                        mediaType(MediaType.PNG)
                        data = TEST_IMAGE
                    }
                }
            }
        }

        // when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Image>()
                    source should {
                        be<Source.Base64>()
                        have(data == TEST_IMAGE)
                    }
                }
            }
        }
    }

    @Test
    fun `should add document content if the Tool returns Document`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo> {
                Document {
                    source = Source.Base64 {
                        mediaType(MediaType.PDF)
                        data = "pdf-content"
                    }
                }
            }
        }

        // when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Document>()
                    source should {
                        be<Source.Base64>()
                        have(data == "pdf-content")
                    }
                }
            }
        }
    }

    @Test
    fun `should return Document and Image and Text and null Text when Tool returns list of Content elements`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<Foo> {
                listOf(
                    Image {
                        source = Source.Base64 {
                            mediaType(MediaType.PNG)
                            data = TEST_IMAGE
                        }
                    },
                    Document {
                        source = Source.Base64 {
                            mediaType(MediaType.PDF)
                            data = "pdf-content"
                        }
                    },
                    Text("foo"),
                    null
                )
            }
        }

        // when
        val response = useFooResponse.useTools(toolbox)

        // then
        response should {

            have(content.size == 1)

            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 4)
                have(isError == null)
                content!![0] should {
                    be<Image>()
                    source should {
                        be<Source.Base64>()
                        have(data == TEST_IMAGE)
                    }
                }
                content[1] should {
                    be<Document>()
                    source should {
                        be<Source.Base64>()
                        have(data == "pdf-content")
                    }
                }
                content[2] should {
                    be<Text>()
                    have(text == "foo")
                }
                content[3] should {
                    be<Text>()
                    have(text == "null")
                }
            }
        }
    }

    @Test
    fun `should throw exception if response StopReason is other than TOOL_USE`() = runTest {
        // given
        val toolbox = Toolbox { /* empty */ }
        val response = useFooResponse.copy(stopReason = StopReason.END_TURN)
        val message = assertFailsWith<IllegalStateException> {
            response.useTools(toolbox)
        }.message
        assert(message == "You can only use tools if the stopReason is TOOL_USE")
    }

}
