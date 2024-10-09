package com.xemantic.anthropic.tool

import com.xemantic.anthropic.message.CacheControl
import com.xemantic.anthropic.message.Tool
import com.xemantic.anthropic.message.UsableTool
import com.xemantic.anthropic.schema.toJsonSchema
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

annotation class Description(
  val value: String
)

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
fun <T : UsableTool> KClass<T>.verify() {
  // TODO how to get class serializer correctly?
  checkNotNull(serializer()) {
    "Invalid tool definition, not serializer for class ${this@verify}"
  }
  checkNotNull(serializer().descriptor.annotations.filterIsInstance<Description>().firstOrNull()) {
    "Not @Description annotation specified for the tool"
  }
}

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
fun <T : UsableTool> KClass<T>.instance(
  cacheControl: CacheControl? = null
): Tool {
  val descriptor = serializer().descriptor
  val description = descriptor.annotations.filterIsInstance<Description>().firstOrNull()!!.value
  return Tool(
    name = descriptor.serialName,
    description = description,
    inputSchema = toJsonSchema(),
    cacheControl = cacheControl
  )
}

//inline fun <reified T> anthropicTypeOf(): String =
//  T::class.qualifiedName!!.replace('.', '_')


@OptIn(InternalSerializationApi::class)
fun <T : UsableTool> List<KClass<T>>.toSerializersModule(): SerializersModule = SerializersModule {
  polymorphic(UsableTool::class) {
    forEach { subclass(it, it.serializer())  }
  }
}

inline fun <reified T : UsableTool> Tool(
  description: String,
  cacheControl: CacheControl? = null
): Tool = Tool(
  name = anthropicTypeOf<T>(),
  description = description,
  inputSchema = jsonSchemaOf<T>(),
  cacheControl = cacheControl
)
