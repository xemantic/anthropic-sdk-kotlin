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

package com.xemantic.ai.anthropic.message

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class MessageTest {

    @Test
    fun `Default empty Message should have Role USER`() {
        Message() should {
            have(role == Role.USER)
            have(content.isEmpty())
        }
    }

    @Test
    fun `should copy Message without alterations`() {
        Message {
            role = Role.ASSISTANT
            content = listOf(Text("foo"))
        }.copy() should {
            have(role == Role.ASSISTANT)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have(text == "foo")
                have(cacheControl == null)
            }
        }
    }

    @Test
    fun `should copy Message while altering the role and content CacheControl`() {
        Message {
            role = Role.ASSISTANT
            content = listOf(Text("foo"))
        }.copy {
            role = Role.USER
            content = content.map {
                it.alterCacheControl(CacheControl.Ephemeral())
            }
        } should {
            have(role == Role.USER)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                have(text == "foo")
                cacheControl should {
                    be<CacheControl.Ephemeral>()
                }
            }
        }
    }

}
