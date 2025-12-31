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

import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.json.set
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.kotlin.test.should
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test

class CacheControlSerializationTest {

    @Test
    fun `should serialize Ephemeral CacheControl`() {
        anthropicJson.encodeToString(
            serializer = CacheControl.serializer(),
            value = CacheControl.Ephemeral()
        ) sameAsJson """
            {
              "type": "ephemeral"
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize Ephemeral CacheControl with additional properties`() {
        anthropicJson.encodeToString(
            serializer = CacheControl.serializer(),
            value = CacheControl.Ephemeral {
                additionalProperties["booleanProperty"] = true
                additionalProperties["intProperty"] = 42
                additionalProperties["doubleProperty"] = 1234.5678
                additionalProperties["stringProperty"] = "foo"
                additionalProperties["objectProperty"] = buildJsonObject {
                    put("foo", "bar")
                }
                additionalProperties["nullProperty"] = null
            }
        ) sameAsJson """
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
        """.trimIndent()
    }

    @Test
    fun `should deserialize Ephemeral CacheControl`() {
        anthropicJson.decodeFromString<CacheControl>(
            """
            {
              "type": "ephemeral"
            }
            """.trimIndent()
        ) should {
            be<CacheControl.Ephemeral>()
            have(ttl == null)
        }
    }

    @Test
    fun `should serialize Ephemeral CacheControl with 5m ttl`() {
        anthropicJson.encodeToString(
            serializer = CacheControl.serializer(),
            value = CacheControl.Ephemeral {
                ttl = CacheControl.Ephemeral.TTL.FIVE_MINUTES
            }
        ) shouldEqualJson """
            {
              "type": "ephemeral",
              "ttl": "5m"
            }
        """
    }

    @Test
    fun `should serialize Ephemeral CacheControl with 1h ttl`() {
        anthropicJson.encodeToString(
            serializer = CacheControl.serializer(),
            value = CacheControl.Ephemeral {
                ttl = CacheControl.Ephemeral.TTL.ONE_HOUR
            }
        ) shouldEqualJson """
            {
              "type": "ephemeral",
              "ttl": "1h"
            }
        """
    }

    @Test
    fun `should deserialize Ephemeral CacheControl with 5m ttl`() {
        anthropicJson.decodeFromString<CacheControl>(
            """
            {
              "type": "ephemeral",
              "ttl": "5m"
            }
            """
        ) should {
            be<CacheControl.Ephemeral>()
            have(ttl == CacheControl.Ephemeral.TTL.FIVE_MINUTES)
        }
    }

    @Test
    fun `should deserialize Ephemeral CacheControl with 1h ttl`() {
        anthropicJson.decodeFromString<CacheControl>(
            """
            {
              "type": "ephemeral",
              "ttl": "1h"
            }
            """
        ) should {
            be<CacheControl.Ephemeral>()
            have(ttl == CacheControl.Ephemeral.TTL.ONE_HOUR)
        }
    }

    @Test
    fun `should deserialize CacheControl with additional properties`() {
        anthropicJson.decodeFromString<CacheControl>(
            """
            {
              "type": "ephemeral",
              "foo": "bar"
            }
            """.trimIndent()
        ) should {
            be<CacheControl.Ephemeral>()
            additionalProperties should {
                have(size == 1)
                have(this["foo"] == JsonPrimitive("bar"))
            }
        }
    }

    @Test
    fun `should serialize hypothetical new CacheControl with additional properties`() {
        anthropicJson.encodeToString(
            serializer = CacheControl.serializer(),
            value = CacheControl.Unknown {
                type = "persistent"
                additionalProperties["foo"] = "bar"
            }
        ) sameAsJson """
            {
              "type": "persistent",
              "foo": "bar"
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize hypothetical CacheControl with additional properties`() {
        anthropicJson.encodeToString(
            serializer = CacheControl.serializer(),
            value = CacheControl.Unknown {
                type = "persistent"
                additionalProperties["foo"] = "bar"
            }
        ) sameAsJson """
            {
              "type": "persistent",
              "foo": "bar"
            }
        """.trimIndent()
    }

    @Test
    fun `should deserialize hypothetical future CacheControl instance`() {
        anthropicJson.decodeFromString<CacheControl>(
            """
            {
              "type": "persistent"
            }
            """.trimIndent()
        ) should {
            be<CacheControl.Unknown>()
            have(type == "persistent")
        }
    }

    @Test
    fun `should deserialize hypothetical future CacheControl instance with additional properties`() {
        anthropicJson.decodeFromString<CacheControl>(
            """
            {
              "type": "persistent",
              "max_storage": 10000
            }
            """.trimIndent()
        ) should {
            be<CacheControl.Unknown>()
            have(type == "persistent")
            additionalProperties should {
                have(size == 1)
                have(this["max_storage"] == JsonPrimitive(10000))
            }
        }
    }

}
