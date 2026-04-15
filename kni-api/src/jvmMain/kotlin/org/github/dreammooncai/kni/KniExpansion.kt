package org.github.dreammooncai.kni

import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal fun toKClass(clazz: Class<*>): KClass<*> = clazz.kotlin

fun <T: Any> getSerializer(clazz: Class<T>, json: Json) = getSerializer(clazz.kotlin, json)
