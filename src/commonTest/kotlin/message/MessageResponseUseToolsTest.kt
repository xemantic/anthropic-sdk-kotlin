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
import com.xemantic.ai.anthropic.content.Document
import com.xemantic.ai.anthropic.content.Image
import com.xemantic.ai.anthropic.content.Source
import com.xemantic.ai.anthropic.content.TEST_IMAGE
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ToolResult
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.tool.Tool
import com.xemantic.ai.anthropic.usage.Usage
import com.xemantic.ai.file.magic.MediaType
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test

class MessageResponseUseToolsTest {

    @Test
    fun `should return ok text if no action specified for the tool`() = runTest {
        messageResponse(Tool<Foo>()).useTools() should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    assert(text == "ok")
                }
            }
        }
    }

    @Test
    fun `should return text from the Tool input if the Tool is a pass-through`() = runTest {
        messageResponse(Tool<Foo> {
            bar
        }).useTools() should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    assert(text == "buzz")
                }
            }
        }
    }

    @Test
    fun `should return text and be an error if Tools throws an exception`() = runTest {
        messageResponse(Tool<Foo> {
            throw Exception("error was thrown")
        }).useTools() should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == true)
                content!![0] should {
                    be<Text>()
                    assert(text == "error was thrown")
                }
            }
        }
    }

    @Test
    fun `should return number as text if the Tool returns Double`() = runTest {
        messageResponse(Tool<Foo> {
            1.12345
        }).useTools() should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    assert(text == "1.12345")
                }
            }
        }
    }

    @Test
    fun `should return object toString if the Tool returns object`() = runTest {
        messageResponse(Tool<Foo> {
            "foo" to "bar" // a Pair
        }).useTools() should {
            have(content.size == 1)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    assert(text == "(foo, bar)")
                }
            }
        }
    }

    @Test
    fun `should return Image if the Tool returns Image`() = runTest {
        messageResponse(Tool<Foo> {
            Image {
                source = Source.Base64 {
                    mediaType(MediaType.PNG)
                    data = TEST_IMAGE
                }
            }
        }).useTools() should {
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
        messageResponse(Tool<Foo> {
            Document {
                source = Source.Base64 {
                    mediaType(MediaType.PDF)
                    data = "pdf-content"
                }
            }
        }).useTools() should {
            have(content.size == 2)
            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 1)
                have(isError == null)
                content!![0] should {
                    be<Text>()
                    have(text == "Document tool_result added as separate content to the message")
                }
            }
            content[1] should {
                be<Document>()
                source should {
                    be<Source.Base64>()
                    have(data == "pdf-content")
                }
            }
        }
    }

    @Test
    fun `should return Document and Image when Tool returns list of Content elements`() = runTest {
        messageResponse(Tool<Foo> {
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
                Text("foo")
            )
        }).useTools() should {

            have(content.size == 2)

            content[0] should {
                be<ToolResult>()
                have(toolUseId == "bar_1234")
                have(content != null && content.size == 3)
                have(isError == null)
                content!![0] should {
                    be<Image>()
                    source should {
                        be<Source.Base64>()
                        have(data == TEST_IMAGE)
                    }
                }
                content[1] should {
                    be<Text>()
                    have(text == "Document tool_result added as separate content to the message")
                }
                content[2] should {
                    be<Text>()
                    have(text == "foo")
                }
            }

            content[1] should {
                be<Document>()
                source should {
                    be<Source.Base64>()
                    have(data == "pdf-content")
                }
            }
        }
    }

    @Serializable
    @SerialName("foo")
    class Foo(val bar: String)

    private fun messageResponse(tool: Tool) = MessageResponse(
        id = "foo_42",
        role = Role.ASSISTANT,
        content = listOf(
            ToolUse(
                id = "bar_1234",
                name = "foo",
                input = buildJsonObject {
                    put("bar", JsonPrimitive("buzz"))
                }
            ).apply {
                this.tool = tool
            }
        ),
        model = Model.CLAUDE_3_7_SONNET.id,
        stopReason = StopReason.TOOL_USE,
        stopSequence = null,
        usage = Usage(
            inputTokens = 419,
            outputTokens = 86
        )
    )

}
