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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.tool.schema.meta.Description
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import org.junit.Test

/**
 * This test is showing how to use tools with external dependencies,
 * e.g. DB connection or HTTP clients.
 */
class ToolWithDependenciesTest {

    // given
    // dependency interface
    interface Database {
        suspend fun execute(sql: String): List<String>
    }

    // and test implementation of the dependency
    class TestDatabase(
        var executedQuery: String? = null
    ) : Database {
        override suspend fun execute(
            query: String
        ): List<String> {
            executedQuery = query
            return listOf("foo", "bar", "buzz")
        }
    }
    // we create a test db instance
    val testDatabase = TestDatabase()

    // define the tool
    @SerialName("query_database")
    @Description("Executes database query")
    data class QueryDatabase(val sql: String)

    @Test
    fun `should use tool with dependencies`() = runTest {
        // given
        val dbTools = listOf(
            Tool<QueryDatabase> {
                testDatabase.execute(sql)
            }
        )
        val anthropic = Anthropic()

        // when
        val response = anthropic.messages.create {
            +Message { +"List data in CUSTOMER table" }
            tools = dbTools
            toolChoice = ToolChoice.Tool<QueryDatabase>()
        }

        // then
        response should {
            have(stopReason == StopReason.TOOL_USE)
            toolUse should {
                have(name == "query_database")
            }
        }

        // when
        response.useTools()

        // then
        testDatabase should {
            have(executedQuery != null)
            have(executedQuery!!.uppercase().startsWith("SELECT * FROM CUSTOMER"))
            // startsWith, because depending on the LLMs response, the statement might
            // end up with semicolon, which we discard in assertion
        }

    }

}
