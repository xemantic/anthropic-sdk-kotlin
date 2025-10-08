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
import com.xemantic.ai.tool.schema.meta.Description
import kotlinx.coroutines.test.runTest
import java.sql.Connection
import javax.sql.DataSource
import kotlin.test.Ignore
import kotlin.test.Test

@Description("Executes SQL on the database")
data class QueryDatabase(val sql: String)

class QueryDatabaseToolTest {

    private lateinit var dataSource: DataSource // = mockk(DataSource::class)

    fun Connection.queryDatabase(sql: String) {
        prepareStatement(sql).use { statement ->
            statement.executeQuery().use { resultSet ->
                resultSet.toString()
            }
        }
    }

    @Test
    @Ignore // it would be nice to actually use embedded database here
    fun `should query database`() = runTest {
        // given
        val toolbox = Toolbox {
            tool<QueryDatabase> {
                dataSource.connection.use {
                    it.queryDatabase(sql)
                }
            }
        }

        val anthropic = Anthropic()

        val response = anthropic.messages.create {
            +Message { +"Select all the users who never logged into the the system" }
            tools = toolbox.tools
            toolChoice = ToolChoice.Tool<QueryDatabase>()
        }
        val result = response.useTools(toolbox)
        println(result)
    }

}
