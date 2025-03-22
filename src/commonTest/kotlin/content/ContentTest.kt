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
import com.xemantic.kotlin.test.should
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test

class ContentTest {

    @Test
    fun `should alter Text CacheControl`() {
        Text("foo").alterCacheControl(CacheControl.Ephemeral()) should {
            be<Text>()
            have(text == "foo")
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should alter Image CacheControl`() {
        Image {
            source = Source.Url("https://example.com/image.png")
        }.alterCacheControl(CacheControl.Ephemeral()) should {
            be<Image>()
            source should {
                be<Source.Url>()
                have(url == "https://example.com/image.png")
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should alter Document CacheControl`() {
        Document {
            source = Source.Url("https://example.com/document.pdf")
        }.alterCacheControl(CacheControl.Ephemeral()) should {
            be<Document>()
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
    fun `should alter ToolUse CacheControl`() {
        ToolUse {
            id = "42"
            name = "foo"
            input = buildJsonObject {}
        }.alterCacheControl(CacheControl.Ephemeral()) should {
            be<ToolUse>()
            have(id == "42")
            have(name == "foo")
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

    @Test
    fun `should alter ToolResult CacheControl`() {
        ToolResult {
            toolUseId = "42"
            content = listOf(Text("foo"))
        }.alterCacheControl(CacheControl.Ephemeral()) should {
            be<ToolResult>()
            have(toolUseId == "42")
            content should {
                have(isNotEmpty())
                first() should {
                    be<Text>()
                    have(text == "foo")
                }
            }
            cacheControl should {
                be<CacheControl.Ephemeral>()
            }
        }
    }

}
