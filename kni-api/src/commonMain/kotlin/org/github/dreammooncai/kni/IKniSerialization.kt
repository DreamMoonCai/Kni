package org.github.dreammooncai.kni

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "__cls"
}

expect fun <T> getSerializer(clazz: KClass<T & Any>,json: Json): KSerializer<T>?

/**
 * 用于标识此类支持序列化并使用指定的序列化配置
 *
 * 不标识依然可以序列化取决于 [kotlinx.serialization.Serializable] 并使用默认的 [json]
 *
 */
interface IKniSerialization {
    val serializerConfig: Json get() = json
}