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

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TextTest {

    @Test
    fun `should return string representation of Text`() {
        Text("foo").toString() sameAsJson """
            {
              "type": "text",
              "text": "foo"
            }
        """.trimIndent()
    }

    @Test
    fun `should fail to create Text without text attribute`() {
        assertFailsWith<IllegalArgumentException> {
            Text {}
        } should {
            have(message == "text cannot be null")
        }
    }

    @Test
    fun `should copy Text`() {
        Text {
            text = "foo"
            cacheControl = CacheControl.Ephemeral()
        }.copy() should {
            have(text == "foo")
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should copy Text while altering properties`() {
        Text {
            text = "foo"
            cacheControl = CacheControl.Ephemeral()
        }.copy {
            text = "bar"
            cacheControl = null
        } should {
            have(text == "bar")
            have(cacheControl == null)
        }
    }

}
