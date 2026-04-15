package org.github.dreammooncai.ipc

import io.github.dreammooncai.yukireflection.factory.allFunctionsAndSuper
import io.github.dreammooncai.yukireflection.factory.allPropertysAndSuper
import io.github.dreammooncai.yukireflection.factory.buildByDefaultArgs
import io.github.dreammooncai.yukireflection.factory.declaringClass
import io.github.dreammooncai.yukireflection.factory.isInstance
import io.github.dreammooncai.yukireflection.factory.name
import io.github.dreammooncai.yukireflection.factory.singletonInstance
import io.github.dreammooncai.yukireflection.factory.toKClass
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import org.github.dreammooncai.kni.json
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty

@Serializable
data class DreamRpcCall(
    val callableClass: String,
    val callableType: Type,
    val callableDescriptor: String,
    val args: Map<Int, JsonElement>
) {
    @Serializable
    enum class Type {
        Function, PropertyGetter, PropertySetter
    }

    companion object {

        private val cacheFunctions = mutableMapOf<KClass<*>, Map<String, KFunction<*>>>()

        private val cacheProperties = mutableMapOf<KClass<*>, Map<String, KProperty<*>>>()

        @Suppress("UNCHECKED_CAST")
        fun create(
            function: KFunction<*>,
            args: Array<Any?>,
            onAccidentTransact: (KParameter, Any?) -> JsonElement = { _, _ -> JsonNull }
        ): DreamRpcCall {
            return DreamRpcCall(
                function.declaringClass?.name ?: "",
                when (function) {
                    is KProperty.Getter<*> -> Type.PropertyGetter
                    is KMutableProperty.Setter<*> -> Type.PropertySetter
                    else -> Type.Function
                },
                when (function) {
                    is KProperty.Accessor<*> -> function.property
                    else -> function
                }.toString(),
                (function.buildByDefaultArgs(
                    null,
                    args
                ) { parameter, value ->
                    val serializer = json.serializersModule.serializerOrNull(parameter.type)
                    if (serializer == null || serializer is PolymorphicSerializer)
                        parameter.index to onAccidentTransact(parameter, value)
                    else
                        parameter.index to json.encodeToJsonElement(
                            serializer,
                            value
                        )
                }.values.toList() as List<Pair<Int, JsonElement>>).toMap()
            )
        }
    }

    fun getCallable(classLoader: ClassLoader): KCallable<*> {
        val thisRefClass = callableClass.toKClass(classLoader)
        return when (callableType) {
            Type.Function -> {
                cacheFunctions.getOrPut(thisRefClass) {
                    thisRefClass.allFunctionsAndSuper.associateBy { it.toString() }
                }[callableDescriptor]
                    ?: throw IllegalArgumentException("Function $callableDescriptor not found in ${thisRefClass.simpleName}")
            }

            Type.PropertyGetter, Type.PropertySetter -> {
                cacheProperties.getOrPut(thisRefClass) {
                    thisRefClass.allPropertysAndSuper.associateBy { it.toString() }
                }[callableDescriptor]
                    ?: throw IllegalArgumentException("Property $callableDescriptor not found in ${thisRefClass.simpleName}")
            }
        }
    }

    fun callAny(callable: KCallable<*>, thisRef: Any? = null, onTransact: (KParameter, Any?) -> Any? = { _, value -> value }, onAccidentTransact: (KParameter, JsonElement) -> Any? = { _, _ -> null }): Any? {
        fun buildArgs(callable: KCallable<*>) = callable.parameters.filter { it.index in args.keys }.map { parameter ->
            val serializer = json.serializersModule.serializerOrNull(parameter.type)
            if (parameter.isInstance) {
                if (thisRef != null) return@map parameter to thisRef
                if (args[parameter.index] == JsonNull) {
                    val value = callable.declaringClass?.singletonInstance ?: serializer?.takeUnless { serializer is PolymorphicSerializer }?.let { json.decodeFromJsonElement(it,args[parameter.index]!!) }
                    return@map parameter to (value ?: onAccidentTransact(parameter, args[parameter.index]!!) ?: throw IllegalArgumentException("Cannot decode ${parameter.name} to ${parameter.type}"))
                }
            }
            if (serializer == null || serializer is PolymorphicSerializer)
                parameter to onAccidentTransact(parameter, args[parameter.index]!!)
            else parameter to onTransact(parameter,json.decodeFromJsonElement(
                    json.serializersModule.serializer(parameter.type),
                    args[parameter.index]!!
                ))
        }.toMap()

        val result = when (callable) {
            is KMutableProperty<*> -> {
                if (callableType == Type.PropertyGetter) {
                    callable.getter.callBy(buildArgs(callable.getter))
                } else {
                    callable.setter.callBy(buildArgs(callable.setter))
                }
            }

            is KProperty<*> -> {
                callable.getter.callBy(buildArgs(callable.getter))
            }

            else -> callable.callBy(buildArgs(callable))
        }
        return result
    }

    fun call(classLoader: ClassLoader, thisRef: Any? = null, onTransact: (KParameter, Any?) -> Any? = { _, value -> value },onAccidentTransact: (KParameter, JsonElement) -> Any? = { _, _ -> null }): JsonElement? = call(getCallable(classLoader), thisRef,onTransact,onAccidentTransact)

    fun call(callable: KCallable<*>, thisRef: Any? = null, onTransact: (KParameter, Any?) -> Any? = { _, value -> value },onAccidentTransact: (KParameter, JsonElement) -> Any? = { _, _ -> null }): JsonElement? {
        val serializer = json.serializersModule.serializerOrNull(callable.returnType)

        return if (serializer != null && serializer !is PolymorphicSerializer) json.encodeToJsonElement(
            serializer,
            callAny(callable, thisRef,onTransact,onAccidentTransact)
        ) else null
    }
}