package org.github.dreammooncai.kni

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class)
actual fun <T> getSerializer(clazz: KClass<T & Any>, json: Json): KSerializer<T>? = clazz.serializerOrNull() as? KSerializer<T>?