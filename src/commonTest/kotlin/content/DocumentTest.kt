/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.anthropic.test.testDataDir
import com.xemantic.ai.file.magic.MediaType
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.isBrowserPlatform
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertFailsWith

const val testPdf = "JVBERi0xLjEKJcKlwrHDqwoKMSAwIG9iagogIDw8IC9UeXBlIC9DYXRhbG9nCiAgICAgL1BhZ2VzIDIgMCBSCiAgPj4KZW5" +
        "kb2JqCgoyIDAgb2JqCiAgPDwgL1R5cGUgL1BhZ2VzCiAgICAgL0tpZHMgWzMgMCBSXQogICAgIC9Db3VudCAxCiAgICAgL01lZGlhQm94IFswID" +
        "AgMzAwIDE0NF0KICA+PgplbmRvYmoKCjMgMCBvYmoKICA8PCAgL1R5cGUgL1BhZ2UKICAgICAgL1BhcmVudCAyIDAgUgogICAgICAvUmVzb3VyY" +
        "2VzCiAgICAgICA8PCAvRm9udAogICAgICAgICAgIDw8IC9GMQogICAgICAgICAgICAgICA8PCAvVHlwZSAvRm9udAogICAgICAgICAgICAgICAg" +
        "ICAvU3VidHlwZSAvVHlwZTEKICAgICAgICAgICAgICAgICAgL0Jhc2VGb250IC9UaW1lcy1Sb21hbgogICAgICAgICAgICAgICA+PgogICAgICA" +
        "gICAgID4+CiAgICAgICA+PgogICAgICAvQ29udGVudHMgNCAwIFIKICA+PgplbmRvYmoKCjQgMCBvYmoKICA8PCAvTGVuZ3RoIDU1ID4+CnN0cm" +
        "VhbQogIEJUCiAgICAvRjEgMTggVGYKICAgIDAgMCBUZAogICAgKEhlbGxvIFdvcmxkKSBUagogIEVUCmVuZHN0cmVhbQplbmRvYmoKCnhyZWYKM" +
        "CA1CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAxOCAwMDAwMCBuIAowMDAwMDAwMDc3IDAwMDAwIG4gCjAwMDAwMDAxNzggMDAwMDAgbiAK" +
        "MDAwMDAwMDQ1NyAwMDAwMCBuIAp0cmFpbGVyCiAgPDwgIC9Sb290IDEgMCBSCiAgICAgIC9TaXplIDUKICA+PgpzdGFydHhyZWYKNTY1CiUlRU9" +
        "GCg=="

class DocumentTest {

    @Test
    fun `should read text from test PDF`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val response = anthropic.messages.create {
            +Message {
                +Document {
                    source = Source.Base64 {
                        mediaType(MediaType.PDF)
                        data = testPdf
                    }
                }
                +"What's in the document?"
            }
        }

        // then
        response should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("HELLO WORLD" in text.uppercase())
            }
        }
    }

    @Test
    fun `should read text from test PDF file`() = runTest {
        if (isBrowserPlatform) return@runTest // we cannot access files in the browser
        // given
        val anthropic = testAnthropic()

        // when
        val response = anthropic.messages.create {
            +Message {
                // you can also submit the path string directly, relative to the current working dir,
                // See JvmDocumentTest in jvmTest
                +Document(Path(testDataDir, "test.pdf"))
                +"What's in the document?"
            }
        }

        // then
        response should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have("FOO" in text.uppercase())
            }
        }
    }

    @Test
    fun `should create cacheable Document`() {
        if (isBrowserPlatform) return // we cannot access files in the browser
        Document(Path(testDataDir, "test.pdf")) {
            cacheControl = CacheControl.Ephemeral()
        } should {
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
            source should {
                be<Source.Base64>()
                have(mediaType == MediaType.PDF.mime)
                have(data.isNotEmpty())
            }
        }
    }

    @Test
    fun `should create Document from bytes`() {
        Document(Base64.decode(testPdf)).source should {
            be<Source.Base64>()
            have(mediaType == MediaType.PDF.mime)
            have(data.isNotEmpty())
        }
    }

    @Test
    fun `should fail to create Document from text file`() {
        if (isBrowserPlatform) return // we cannot access files in the browser
        assertFailsWith<IllegalArgumentException> {
            Document(Path(testDataDir, "zero.txt"))
        } should {
            have(
                message != null && message!! matches Regex(
                    "Unsupported file at path \".*zero\\.txt\": Cannot detect media type"
                )
            )
        }
    }

    @Test
    fun `should fail to create PDF Document from image file`() {
        if (isBrowserPlatform) return // we cannot access files in the browser
        assertFailsWith<IllegalArgumentException> {
            Document(Path(testDataDir, "foo.png"))
        } should {
            have(
                message != null && message!! matches Regex(
                    "Unsupported file at path \".*foo\\.png\": " +
                            @Suppress("RegExpRedundantEscape") // it's not redundant since it's needed in JS
                            "Unsupported media type \"image/png\".*, supported: \\[\"application/pdf\"\\]"
                )
            )
        }
    }

    @Test
    fun `should fail to create Document with null path specified in the builder`() {
        assertFailsWith<IllegalArgumentException> {
            Document {
                path = null
            }
        } should {
            have(message == "The path of binary content cannot be null")
        }
    }

    @Test
    fun `should fail to create document with null bytes specified in the builder`() {
        assertFailsWith<IllegalArgumentException> {
            Document {
                bytes = null
            }
        } should {
            have(message == "The bytes of binary content cannot be null")
        }
    }

    @Test
    fun `should return string representation of Document`() {
        Document {
            source = Source.Url("https://example.com/document.pdf")
            cacheControl = CacheControl.Ephemeral()
        }.toString() shouldEqualJson """
            {
              "type": "document",
              "source": {
                "type": "url",
                "url": "https://example.com/document.pdf"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """
    }

    @Test
    fun `should copy Document`() {
        Document {
            source = Source.Url("https://example.com/document.pdf")
            cacheControl = CacheControl.Ephemeral()
        }.copy() should {
            source should {
                be<Source.Url>()
                have(url == "https://example.com/document.pdf")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy Document while altering properties`() {
        Document {
            source = Source.Url("https://example.com/document.pdf")
            cacheControl = CacheControl.Ephemeral()
        }.copy {
            source = Source.Url("https://example.com/document-new.pdf")
            cacheControl = null
        } should {
            source should {
                be<Source.Url>()
                have(url == "https://example.com/document-new.pdf")
            }
            have(cacheControl == null)
        }
    }

}
