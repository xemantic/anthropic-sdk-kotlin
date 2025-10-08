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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test

class EphemeralCacheControlToStringTest {

    @Test
    fun `should return pretty toString JSON of Ephemeral CacheControl`() {
        CacheControl.Ephemeral().toString() shouldEqualJson """
            {
              "type": "ephemeral"
            }
        """
    }

    @Test
    fun `should return pretty toString JSON of Ephemeral CacheControl with additional properties`() {
        CacheControl.Ephemeral {
            additionalProperties["booleanProperty"] = true
            additionalProperties["intProperty"] = 42
            additionalProperties["doubleProperty"] = 1234.5678
            additionalProperties["stringProperty"] = "foo"
            additionalProperties["objectProperty"] = buildJsonObject {
                put("foo", "bar")
            }
            additionalProperties["nullProperty"] = null
        }.toString() shouldEqualJson """
            {
              "type": "ephemeral",
              "booleanProperty": true,
              "intProperty": 42,
              "doubleProperty": 1234.5678,
              "stringProperty": "foo",
              "objectProperty": {
                "foo": "bar"
              },
              "nullProperty": null
            }
        """
    }

}
