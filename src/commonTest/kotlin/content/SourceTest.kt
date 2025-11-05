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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.json.set
import com.xemantic.ai.file.magic.MediaType
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class SourceTest {

    @Test
    fun `should create Base64 Source with additional properties`() {
        val source = Source.Base64 {
            data = "foo"
            mediaType(MediaType.PNG)
            additionalProperties["bar"] = "buzz"
        }
        anthropicJson.encodeToString<Source>(source) sameAsJson """
            {
              "type": "base64",
              "media_type": "image/png",
              "data": "foo",
               "bar": "buzz"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize Base64 SourceSource with additional properties`() {
        // given
        val json = """
            {
              "type": "base64",
              "media_type": "image/png",
              "data": "foo",
              "bar": "buzz"
            }
        """

        // when
        val source = anthropicJson.decodeFromString<Source>(json)

        // then
        source should {
            be<Source.Base64>()
            have(mediaType == MediaType.PNG.mime)
            have(data == "foo")
            have(additionalProperties != null && additionalProperties.isNotEmpty())
            have(additionalProperties!!["bar"] == JsonPrimitive("buzz"))
        }
    }

    @Test
    fun `should create URL Source`() {
        val source = Source.Url("https://example.com/image.png")
        anthropicJson.encodeToString<Source>(source) sameAsJson """
            {
              "type": "url",
              "url": "https://example.com/image.png"
            }
        """.trimIndent()
    }

    @Test
    fun `should create URL Source with additional properties`() {
        val source = Source.Url("https://example.com/image.png") {
            additionalProperties["bar"] = "buzz"
        }
        anthropicJson.encodeToString<Source>(source) sameAsJson """
            {
              "type": "url",
              "url": "https://example.com/image.png",
              "bar": "buzz"
            }
        """.trimIndent()
    }

    @Test
    fun `should create URL Source with url passed in builder and with additional properties`() {
        val source = Source.Url {
            url = "https://example.com/image.png"
            additionalProperties["bar"] = "buzz"
        }
        anthropicJson.encodeToString<Source>(source) sameAsJson """
            {
              "type": "url",
              "url": "https://example.com/image.png",
              "bar": "buzz"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize Url Source with additional properties`() {
        // given
        val json = """
            {
              "type": "url",
              "url": "https://example.com/image.png",
              "bar": "buzz"
            }
        """

        // when
        val source = anthropicJson.decodeFromString<Source>(json)

        // then
        source should {
            be<Source.Url>()
            have(url == "https://example.com/image.png")
            have(additionalProperties != null && additionalProperties.isNotEmpty())
            have(additionalProperties!!["bar"] == JsonPrimitive("buzz"))
        }
    }

    @Test
    fun `should create Text Source with additional properties`() {
        val source = Source.Text {
            data = "Hello World"
            additionalProperties["bar"] = "buzz"
        }
        anthropicJson.encodeToString<Source>(source) sameAsJson """
            {
              "type": "text",
              "media_type": "text/plain",
              "data": "Hello World",
              "bar": "buzz"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize Text Source with additional properties`() {
        // given
        val json = """
            {
              "type": "text",
              "media_type": "text/plain",
              "data": "Hello World",
              "bar": "buzz"
            }
        """

        // when
        val source = anthropicJson.decodeFromString<Source>(json)

        // then
        source should {
            be<Source.Text>()
            have(mediaType == "text/plain")
            have(data == "Hello World")
            have(additionalProperties != null && additionalProperties.isNotEmpty())
            have(additionalProperties!!["bar"] == JsonPrimitive("buzz"))
        }
    }

    @Test
    fun `should created Unknown Source like if it was URL source`() {
        val source = Source.Unknown {
            type = "url"
            additionalProperties["url"] = "https://example.com/image.png"
        }
        anthropicJson.encodeToString<Source>(source) sameAsJson """
            {
              "type": "url",
              "url": "https://example.com/image.png"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize Unknown Source like if it was URL2 source`() {
        // given
        val json = """
            {
              "type": "url2",
              "url": "https://example.com/image.png"
            }
        """

        // when
        val source = anthropicJson.decodeFromString<Source>(json)

        // then
        source should {
            be<Source.Unknown>()
            have(type == "url2")
            additionalProperties should {
                have(isNotEmpty())
                have(get("url") == JsonPrimitive("https://example.com/image.png"))
            }
            have(additionalProperties.isNotEmpty())
        }
    }

}
