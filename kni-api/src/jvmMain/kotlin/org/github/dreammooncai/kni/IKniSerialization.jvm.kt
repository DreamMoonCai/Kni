package org.github.dreammooncai.kni

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType

@Suppress("UNCHECKED_CAST")
actual fun <T> getSerializer(clazz: KClass<T & Any>, json: Json): KSerializer<T>? = json.serializersModule.serializerOrNull(clazz.starProjectedType) as? KSerializer<T>?

