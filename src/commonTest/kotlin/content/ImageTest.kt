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

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testDataDir
import com.xemantic.ai.file.magic.MediaType
import com.xemantic.ai.file.magic.readBytes
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.isBrowserPlatform
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.test.Test
import kotlin.test.assertFailsWith

const val TEST_IMAGE = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAACXBIWXMAABOvAAATrwFj5o7DAAAAGXRFWHRTb2Z0d2FyZ" +
        "QB3d3cuaW5rc2NhcGUub3Jnm+48GgAAA61JREFUeJztmNtrFVcUxn/b+lYwxEuN8RKR1giiQmqxEomCghSU0krBUpqiTyraUn3oU+m/UPsoiD6J" +
        "iiBeW4T64P1FKBSLGlBDjE21NV7r/SwfZp1zhmHPmRnjnPWQ/cFmcfZe69tfvuyZWTNORBjNGGMtwBrBAGsB1ggGWAuwRjDAWoA1ggHWAqwRDLA" +
        "WYI1ggLUAawQDrAVYIxhgLcAawQBrAdYIBlgLsMbYZm/onGsBJhOZPyQi90bINw5oA94B/hGRu4Xq0z6LO+d2AT0FuM6IyDcpXJOArcBnQGdi+T" +
        "KwD/glr3jnXCvwPfA5MDex3AfsV77bmWQi4h3Ar4AUGCdSeNYC9xO5j4Gnibn/gDVpemJ8nwJ3E7X/A08Sc/eBrzL5chjwB/BFjtHj4dgAVJTnJ" +
        "rAZmKJrY4CZwI/AHc15BaxvoOlr4KXmDhGdqum65oDpwA+6Jrr3lpEa8FuWiyn1i4DnyvE70NogtwP4U3OfAQs8OfP1Py3AWWBSA7524KLmvgA+" +
        "tjDgtNb3AS058juAR2mXE3Bc1waAiTn42oBhrTnXVAOArti1mHqkPXU/xY5uZ2y+M3YpfVeAb1tMR5cvp6w+YIXGCnCgQN1ejQ5YFZtfrnMQPTG" +
        "K8FUfc6t9CWUZsEjjZRF5kLdIRK4At/TnHA/fgIgMFeAbBK56+GrIY8BK55xkjPWJmvc0/p1XbAxVA6aWyFdDWSdgnMZHb1Bb7QzHv2W+Cb7FPK" +
        "3weWBLRk5/4ve/Gt/NwZ9Ei8b4H/u2+WrIY8ADEblYcNObGjuKFDnnHDBLf14bKZ/ifQ9fDWVdAqc1fuCcm1agbh71o3ohNn9KY5tzznsz88E5N" +
        "wuY4eGroSwDDhB1dAAbC9Rt0vgMOBqbPwI8TOQU4XsFHPRmlNgJ7tT6J6Q0IYn8Hup9/g7P+s/UW9vuHHwL1UgB9jS1E9T6yUSPICF62VnaIPcT" +
        "oru1AIN4+nygFbiuOcPAygZ8y4hunNW9pzbdAOXoov6mVwEOAb1AN7AEWEe9x6+K/bAB39yYqQIcU45uHb26R7VtHs46LaUaoDwdwImYaN+o6H4" +
        "zc/C1A4cz+AQ4CczO4mv0RehbdfwvEdnuTSoA59xioo8jHwFTiO4N/UTfG3ZL1AYX4VsIfEnUJrcTvXrfIHqt3i0il3LxpBkwWjDqvwoHA6wFWC" +
        "MYYC3AGsEAawHWCAZYC7BGMMBagDWCAdYCrBEMsBZgjWCAtQBrBAOsBVgjGGAtwBqvASNwHwnSLggJAAAAAElFTkSuQmCC"

class ImageTest {

    @Test
    fun `Should read text from test image`() = runTest {
        // given
        val anthropic = Anthropic()

        // when
        val response = anthropic.messages.create {
            +Message {
                +Image {
                    source = Source.Base64 {
                        mediaType(MediaType.PNG)
                        data = TEST_IMAGE
                    }
                }
                +"What's on this picture?"
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
    fun `Should read text from test image file`() = runTest {
        if (isBrowserPlatform) return@runTest // we cannot access files in the browser
        // given
        val anthropic = Anthropic()

        // when
        val response = anthropic.messages.create {
            +Message {
                // you can also submit the path string directly, relative to the current working dir,
                // See JvmImageTest in jvmTest
                +Image(Path(testDataDir, "foo.png"))
                +"What's on this picture?"
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
    fun `Should create cacheable Image`() {
        if (isBrowserPlatform) return
        Image(Path(testDataDir, "foo.png")) {
            cacheControl = CacheControl.Ephemeral()
        } should {
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
            source should {
                be<Source.Base64>()
                have(mediaType == MediaType.PNG.mime)
                have(data.isNotEmpty())
            }
        }
    }

    @Test
    fun `Should create Image from bytes`() {
        if (isBrowserPlatform) return
        Image(Path(testDataDir, "foo.png").readBytes()).source should {
            be<Source.Base64>()
            have(mediaType == MediaType.PNG.mime)
            have(data.isNotEmpty())
        }
    }

    @Test
    fun `should fail to create Image without attributes`() {
        assertFailsWith<IllegalArgumentException> {
            Image {}
        } should {
            have(message == "source cannot be null")
        }
    }

    @Test
    fun `Should fail to create Image from text file`() {
        if (isBrowserPlatform) return
        assertFailsWith<IllegalArgumentException> {
            Image(Path(testDataDir, "zero.txt"))
        } should {
            have(
                message != null && message!! matches Regex(
                    "Unsupported file at path \".*zero\\.txt\": Cannot detect media type"
                )
            )
        }
    }

    @Test
    fun `Should fail to create Image from PDF document file`() {
        if (isBrowserPlatform) return
        assertFailsWith<IllegalArgumentException> {
            Image(Path(testDataDir, "test.pdf"))
        } should {
            have(
                message != null && message!! matches Regex(
                    "Unsupported file at path \".*test\\.pdf\": " +
                            "Unsupported media type \"application/pdf\", " +
                            @Suppress("RegExpRedundantEscape") // it's not redundant since it's needed in JS
                            "supported: \\[\"image/jpeg\", \"image/png\", \"image/gif\", \"image/webp\"\\]"
                )
            )
        }
    }

    @Test
    fun `Should fail to create Image with null path specified in the builder`() {
        if (isBrowserPlatform) return
        assertFailsWith<IllegalArgumentException> {
            Image {
                path = null
            }
        } should {
            have(message == "The path of binary content cannot be null")
        }
    }

    @Test
    fun `Should fail to create image with null bytes specified in the builder`() {
        assertFailsWith<IllegalArgumentException> {
            Image {
                bytes = null
            }
        } should {
            have(message == "The bytes of binary content cannot be null")
        }
    }

    @Test
    fun `Should return string representation of Image`() {
        Image {
            source = Source.Base64 {
                mediaType(MediaType.PNG)
                data = TEST_IMAGE
            }
            cacheControl = CacheControl.Ephemeral()
        }.toString() shouldEqualJson /* language=json */ """
            {
              "source": {
                "type": "base64",
                "media_type": "image/png",
                "data": "$TEST_IMAGE"
              },
              "cache_control": {
                "type": "ephemeral"
              }
            }
        """
    }

    @Test
    fun `Should copy Image`() {
        Image {
            source = Source.Base64 {
                mediaType(MediaType.PNG)
                data = TEST_IMAGE
            }
            cacheControl = CacheControl.Ephemeral()
        }.copy() should {
            source should {
                be<Source.Base64>()
                have(mediaType == MediaType.PNG.mime)
                have(data == TEST_IMAGE)
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `Should copy Image while altering properties`() {
        Image {
            source = Source.Base64 {
                mediaType(MediaType.PNG)
                data = TEST_IMAGE
            }
            cacheControl = CacheControl.Ephemeral()
        }.copy {
            source = Source.Url("https://example.com/image.png")
            cacheControl = null
        } should {
            source should {
                be<Source.Url>()
                have(url == "https://example.com/image.png")
            }
            have(cacheControl == null)
        }
    }

}
