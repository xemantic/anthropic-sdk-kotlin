package com.xemantic.anthropic.tool

import com.xemantic.anthropic.anthropicJson
import com.xemantic.anthropic.message.CacheControl
import com.xemantic.anthropic.message.Tool
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.message.ToolUse
import com.xemantic.anthropic.schema.jsonSchemaOf
import com.xemantic.anthropic.schema.toJsonSchema
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MetaSerializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class SerializableTool(
  val name: String,
  val description: String
)

@OptIn(ExperimentalSerializationApi::class)
interface UsableTool {

  fun use(
    toolUseId: String
  ): ToolResult

}

fun Tool.cacheControl(
  cacheControl: CacheControl? = null
): Tool = if (cacheControl == null) this else Tool(
  name,
  description,
  inputSchema,
  cacheControl
)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : UsableTool> toolOf(): Tool {
  val serializer = try {
    serializer<T>()
  } catch (e :SerializationException) {
    throw SerializationException("The class ${T::class.qualifiedName} must be annotated with @SerializableTool", e)
  }
  val description = checkNotNull(
    serializer
      .descriptor
      .annotations
      .filterIsInstance<SerializableTool>()
      .firstOrNull()
  ) {
    "No @Description annotation found for ${T::class.qualifiedName}"
  }
  return Tool(
    name = description.name,
    description = description.description,
    inputSchema = jsonSchemaOf<T>(),
    cacheControl = null
  )
}

//@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
//fun <T : UsableTool> KClass<T>.verify() {
//  // TODO how to get class serializer correctly?
//  checkNotNull(serializer()) {
//    "Invalid tool definition, not serializer for class ${this@verify}"
//  }
//  checkNotNull(serializer().descriptor.annotations.filterIsInstance<Description>().firstOrNull()) {
//    "Not @Description annotation specified for the tool"
//  }
//}

//@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
//fun <T : UsableTool> KClass<T>.instance(
//  cacheControl: CacheControl? = null
//): Tool {
//  val descriptor = serializer().descriptor
//  val description = descriptor.annotations.filterIsInstance<Description>().firstOrNull()!!.value
//  return Tool(
//    name = descriptor.serialName,
//    description = description,
//    inputSchema = toJsonSchema(),
//    cacheControl = cacheControl
//  )
//}

//inline fun <reified T> anthropicTypeOf(): String =
//  T::class.qualifiedName!!.replace('.', '_')


@OptIn(InternalSerializationApi::class)
fun <T : UsableTool> List<KClass<T>>.toSerializersModule(): SerializersModule = SerializersModule {
  polymorphic(UsableTool::class) {
    forEach { subclass(it, it.serializer())  }
  }
}

//inline fun <reified T : UsableTool> Tool(
//  description: String,
//  cacheControl: CacheControl? = null
//): Tool = Tool(
//  name = anthropicTypeOf<T>(),
//  description = description,
//  inputSchema = jsonSchemaOf<T>(),
//  cacheControl = cacheControl
//)


fun <T : UsableTool> ToolUse.use(
  map: Map<String, KSerializer<T>>
): ToolResult {
  val serializer = map[name]!!
  val tool = anthropicJson.decodeFromJsonElement(serializer, input)
  return tool.use(toolUseId = id)
}
