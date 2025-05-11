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

package com.xemantic.ai.anthropic.usage

import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test
import kotlin.test.assertFails

class UsageTest {

    // given
    val usage1 = Usage {
        inputTokens = 1
        outputTokens = 2
        cacheCreationInputTokens = 3
        cacheReadInputTokens = 4
    }

    val usage2 = Usage {
        inputTokens = 1
        outputTokens = 2
        cacheCreationInputTokens = 3
        cacheReadInputTokens = 4
    }

    val usage3 = Usage {
        inputTokens = 5
        outputTokens = 6
        cacheCreationInputTokens = 7
        cacheReadInputTokens = 8
    }

    @Test
    fun `should create Usage instance`() {
        Usage {
            inputTokens = 1
            outputTokens = 2
            cacheCreationInputTokens = 3
            cacheReadInputTokens = 4
        } should {
            have(inputTokens == 1)
            have(outputTokens == 2)
            have(cacheCreationInputTokens == 3)
            have(cacheReadInputTokens == 4)
        }
    }

    @Test
    fun `should create Usage instance if cache properties are not provided`() {
        Usage {
            inputTokens = 1
            outputTokens = 2
        } should {
            have(inputTokens == 1)
            have(outputTokens == 2)
            have(cacheCreationInputTokens == null)
            have(cacheReadInputTokens == null)
        }
    }

    @Test
    fun `should fail to create Usage instance without properties`() {
        assertFails {
            Usage {}
        } should {
            have(message == "inputTokens cannot be null")
        }
    }

    @Test
    fun `should return true for equal Usage objects and false for non-equal`() {
        @Suppress("KotlinConstantConditions")
        assert(usage1 == usage1)
        assert(usage1 == usage2)
        assert(usage1 != usage3)
    }

    @Test
    fun `should return the same hashCode for equal Usage objects`() {
        assert(usage1.hashCode() == usage1.hashCode())
        assert(usage1.hashCode() == usage2.hashCode())
        assert(usage1.hashCode() != usage3.hashCode())
    }

}