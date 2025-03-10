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
import com.xemantic.kotlin.test.should
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class SourceTest {

    @Test
    fun `Should created Source with additional properties`() {
        val source = Source.Base64 {
            data = "foo"
            mediaType(MediaType.PNG)
            additionalProperties["bar"] = "buzz"
        }
        anthropicJson.encodeToString<Source>(source) shouldEqualJson /* language=json */ """
            {
              "type": "base64",
              "media_type": "image/png",
              "data": "foo",
               "bar": "buzz"
            }
        """
    }

    @Test
    fun `Should deserialize Source with additional properties`() {
        // given
        val json = /* language=json */ """
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
    fun `Should created URL Source with additional properties`() {
        val source = Source.Url {
            url = "https://example.com/image.png"
            additionalProperties["bar"] = "buzz"
        }
        anthropicJson.encodeToString<Source>(source) shouldEqualJson /* language=json */ """
            {
              "type": "url",
              "url": "https://example.com/image.png",
              "bar": "buzz"
            }
        """
    }

    @Test
    fun `Should deserialize Url Source with additional properties`() {
        // given
        val json = /* language=json */ """
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

}
