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

package com.xemantic.ai.anthropic.tool

import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.content.Content
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.ai.anthropic.content.ToolResult
import com.xemantic.ai.anthropic.content.ToolUse
import com.xemantic.ai.anthropic.json.ToolSerializer
import com.xemantic.ai.anthropic.json.anthropicJson
import com.xemantic.ai.anthropic.json.toPrettyJson
import com.xemantic.ai.tool.schema.JsonSchema
import com.xemantic.ai.tool.schema.ObjectSchema
import com.xemantic.ai.tool.schema.generator.jsonSchemaOf
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable(with = ToolSerializer::class)
abstract class Tool {

    abstract val name: String
    abstract val description: String?
    abstract val inputSchema: JsonSchema?
    abstract val cacheControl: CacheControl?

    override fun toString(): String = toPrettyJson()

}

@Serializable
class DefaultTool private constructor(
    override val name: String,
    override val description: String? = null,
    @SerialName("input_schema")
    override val inputSchema: JsonSchema? = null,
    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null
) : Tool() {

    class Builder {

        var name: String? = null
        var description: String? = null
        var inputSchema: JsonSchema? = null
        var cacheControl: CacheControl? = null

        fun build(): DefaultTool = DefaultTool(
            requireNotNull(name) { "Tool must have a 'name'" },
            description,
            inputSchema,
            cacheControl
        )

    }

}

@Serializable
abstract class BuiltInTool<Input>(
    override val name: String,
    val type: String
) : Tool() {

    @SerialName("cache_control")
    override val cacheControl: CacheControl? = null

    @SerialName("input_schema")
    override val inputSchema: JsonSchema? = null

    override val description: String? = null

}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> toolName(): String =
    serializer<T>().descriptor.serialName.normalizedToolName

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Tool(
    name: String? = toolName<T>(),
    description: String? = null,
    builder: DefaultTool.Builder.() -> Unit = {},
): Tool {

    val schema = jsonSchemaOf<T>()
    require(schema is ObjectSchema) {
        "Tool input class must be an object"
    }

    return DefaultTool.Builder().also {
        it.name = name
        it.description = schema.description ?: description
        it.inputSchema = schema.copy {
            this.description = null
        }
        it.builder()
    }.build()

}

@Serializable
sealed class ToolChoice {

    abstract val disableParallelToolUse: Boolean?

    @Serializable
    @SerialName("auto")
    class Auto private constructor(
        @SerialName("disable_parallel_tool_use")
        override val disableParallelToolUse: Boolean? = null
    ) : ToolChoice() {

        class Builder {

            var disableParallelToolUse: Boolean? = null

            fun build(): Auto = Auto(
                disableParallelToolUse = disableParallelToolUse
            )

        }

    }

    @Serializable
    @SerialName("any")
    class Any private constructor(
        @SerialName("disable_parallel_tool_use")
        override val disableParallelToolUse: Boolean? = null
    ) : ToolChoice() {

        class Builder {

            var disableParallelToolUse: Boolean? = null

            fun build(): Any = Any(
                disableParallelToolUse = disableParallelToolUse
            )

        }

    }

    @Serializable
    @SerialName("tool")
    class Tool private constructor(
        val name: String,
        @SerialName("disable_parallel_tool_use")
        override val disableParallelToolUse: Boolean? = null
    ) : ToolChoice() {

        class Builder {

            var name: String? = null
            var disableParallelToolUse: Boolean? = null

            fun build(): Tool = Tool(
                name = requireNotNull(name) { "name cannot be null" },
                disableParallelToolUse = disableParallelToolUse
            )

        }

    }

    companion object {

        fun Auto(
            block: Auto.Builder.() -> Unit = {}
        ): Auto = Auto.Builder().apply(block).build()

        fun Any(
            block: Any.Builder.() -> Unit = {}
        ): Any = Any.Builder().apply(block).build()

        fun Tool(
            name: String,
            block: Tool.Builder.() -> Unit = {}
        ): Tool = Tool.Builder().also {
            it.name = name
            block(it)
        }.build()

        inline fun <reified T> Tool(
            noinline block: Tool.Builder.() -> Unit = {}
        ): Tool = Tool(toolName<T>(), block)

    }

}

@PublishedApi
internal val String.normalizedToolName: String
    get() = trimEnd { it == '$' }
        .replace('.', '_')
        .replace('$', '_')
        .take(64)

fun Toolbox(block: Toolbox.Builder.() -> Unit): Toolbox {
    val builder = Toolbox.Builder()
    block(builder)
    return builder.build()
}

class Toolbox private constructor(
    val tools: List<Tool>,
    private val handlerMap: Map<String, Handler>,
    private val exceptionHandler: (Exception) -> Unit,
    private val json: Json
) {

    @PublishedApi
    internal class Handler(
        val serializer: DeserializationStrategy<Any>,
        val runner: suspend Any.() -> Any
    )

    class Builder {

        var exceptionHandler: (Exception) -> Unit = {}

        var json: Json = anthropicJson

        inline fun <reified T> tool(
            builder: DefaultTool.Builder.() -> Unit = {}, // TODO test builder
            name: String = toolName<T>(),
            noinline block: suspend T.() -> Any = { "ok" }
        ) {

            val strategy = serializer<T>()

            val tool = Tool<T>(name = name, builder = builder)

            @Suppress("UNCHECKED_CAST")
            toolMap[name] = ToolEntry(
                tool = tool,
                serializer = strategy as DeserializationStrategy<Any>,
                runner = block as suspend Any.() -> Any
            )
        }

        inline fun <reified T : BuiltInTool<I>, reified I> tool(
            tool: T,
            noinline block: suspend I.() -> Any = { "ok" }
        ) {

            @Suppress("UNCHECKED_CAST")
            toolMap[tool.name] = ToolEntry(
                tool = tool,
                serializer = json.serializersModule.serializer<I>() as DeserializationStrategy<Any>,
                runner = block as suspend Any.() -> Any
            )
        }

        fun build(): Toolbox = Toolbox(
            tools = toolMap.values.map { it.tool },
            handlerMap = toolMap.mapValues { (_, entry) -> entry.handler() },
            exceptionHandler = exceptionHandler,
            json = json
        )

        @PublishedApi
        internal class ToolEntry(
            val tool: Tool,
            val serializer: DeserializationStrategy<Any>,
            val runner: suspend Any.() -> Any
        ) {

            fun handler() = Handler(
                serializer = serializer,
                runner = runner
            )

        }

        @PublishedApi
        internal val toolMap = mutableMapOf<String, ToolEntry>()

    }

    /**
     * Executes the tool and returns the result.
     *
     * @return A [ToolResult] containing the outcome of executing the tool.
     */
    suspend fun use(
        toolUse: ToolUse
    ): ToolResult = ToolResult {

        toolUseId = toolUse.id

        try {

            val handler = requireNotNull(
                handlerMap[toolUse.name]
            ) {
                "No such tool in this box: ${toolUse.name}"
            }

            val input = json.decodeFromJsonElement(
                deserializer = handler.serializer,
                element = toolUse.input
            )
            val result = handler.runner.invoke(input)

            if (result !is Unit) {
                handle(result)
            } else {
                // sometimes an empty tool result is causing errors
                content += Text("ok")
            }

        } catch (e: Exception) {
            exceptionHandler(e)
            error(e.message ?: "Unknown error occurred")
        }
    }

    private fun ToolResult.Builder.handle(
        result: Any
    ) = when (result) {

        is Content -> {
            content += result
        }

        is List<*> -> {
            content += result.map {
                if (it == null) Text("null")
                else it as? Content ?: toText(it)
            }
        }

        else -> {
            content += toText(result)
        }

    }

    private fun toText(result: Any) = Text(
        result as? String ?: try {
            @OptIn(ExperimentalSerializationApi::class)
            val serializer = json.serializersModule.serializer(
                kClass = result::class,
                typeArgumentsSerializers = emptyList(),
                isNullable = false
            )
            json.encodeToString(serializer, result)
        } catch (e: SerializationException) {
            result.toString()
        }
    )

}
