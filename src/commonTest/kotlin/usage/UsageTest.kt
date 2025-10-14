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

    // given - CacheCreation instances
    val cacheCreation1 = CacheCreation {
        ephemeral5mInputTokens = 10
        ephemeral1hInputTokens = 20
    }

    val cacheCreation2 = CacheCreation {
        ephemeral5mInputTokens = 10
        ephemeral1hInputTokens = 20
    }

    val cacheCreation3 = CacheCreation {
        ephemeral5mInputTokens = 30
        ephemeral1hInputTokens = 40
    }

    // given - Usage instances
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

    @Test
    fun `should add two Usage objects`() {
        // when
        val result = usage1 + usage3

        // then
        result should {
            have(inputTokens == 6)
            have(outputTokens == 8)
            have(cacheCreationInputTokens == 10)
            have(cacheReadInputTokens == 12)
        }
    }

    @Test
    fun `should add Usage objects with null cache properties`() {
        // given
        val usageWithoutCache = Usage {
            inputTokens = 10
            outputTokens = 20
        }

        // when
        val result = usage1 + usageWithoutCache

        // then
        result should {
            have(inputTokens == 11)
            have(outputTokens == 22)
            have(cacheCreationInputTokens == 3)
            have(cacheReadInputTokens == 4)
        }
    }

    @Test
    fun `should add Usage objects with CacheCreation`() {
        // given
        val usageWithCache1 = Usage {
            inputTokens = 100
            outputTokens = 200
            cacheCreation = cacheCreation1
        }
        val usageWithCache2 = Usage {
            inputTokens = 300
            outputTokens = 400
            cacheCreation = cacheCreation3
        }

        // when
        val result = usageWithCache1 + usageWithCache2

        // then
        result should {
            have(inputTokens == 400)
            have(outputTokens == 600)
            have(cacheCreation != null)
        }
        result.cacheCreation!! should {
            have(ephemeral5mInputTokens == 40)
            have(ephemeral1hInputTokens == 60)
        }
    }

    @Test
    fun `should create CacheCreation instance`() {
        CacheCreation {
            ephemeral5mInputTokens = 10
            ephemeral1hInputTokens = 20
        } should {
            have(ephemeral5mInputTokens == 10)
            have(ephemeral1hInputTokens == 20)
        }
    }

    @Test
    fun `should fail to create CacheCreation instance without properties`() {
        assertFails {
            CacheCreation {}
        } should {
            have(message == "ephemeral5mInputTokens cannot be null")
        }
    }

    @Test
    fun `should return true for equal CacheCreation objects and false for non-equal`() {
        @Suppress("KotlinConstantConditions")
        assert(cacheCreation1 == cacheCreation1)
        assert(cacheCreation1 == cacheCreation2)
        assert(cacheCreation1 != cacheCreation3)
    }

    @Test
    fun `should return the same hashCode for equal CacheCreation objects`() {
        assert(cacheCreation1.hashCode() == cacheCreation1.hashCode())
        assert(cacheCreation1.hashCode() == cacheCreation2.hashCode())
        assert(cacheCreation1.hashCode() != cacheCreation3.hashCode())
    }

    @Test
    fun `should add two CacheCreation objects`() {
        // when
        val result = cacheCreation1 + cacheCreation3

        // then
        result should {
            have(ephemeral5mInputTokens == 40)
            have(ephemeral1hInputTokens == 60)
        }
    }

    @Test
    fun `should use CacheCreation ZERO constant`() {
        CacheCreation.ZERO should {
            have(ephemeral5mInputTokens == 0)
            have(ephemeral1hInputTokens == 0)
        }
    }

    @Test
    fun `should use Usage ZERO constant`() {
        Usage.ZERO should {
            have(inputTokens == 0)
            have(outputTokens == 0)
            have(cacheCreationInputTokens == 0)
            have(cacheReadInputTokens == 0)
        }
    }

}