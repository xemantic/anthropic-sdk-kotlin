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

package com.xemantic.ai.anthropic.cache

import com.xemantic.ai.anthropic.json.set
import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

class UnknownCacheControlToStringTest {

    @Test
    fun `should return pretty toString JSON of Unknown CacheControl`() {
        CacheControl.Unknown {
            type = "persistent"
            additionalProperties["max_storage"] = 100000
        }.toString() shouldEqualJson """
            {
              "type": "persistent",
              "max_storage": 100000
            }
        """
    }

}
