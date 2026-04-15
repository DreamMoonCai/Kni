@file:OptIn(ExperimentalForeignApi::class)

package org.github.dreammooncai.kni.data

import kotlinx.atomicfu.locks.reentrantLock
import org.github.dreammooncai.kni.factory.JavaClass
import org.github.dreammooncai.kni.KniBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import org.github.dreammooncai.kni.IKniSerialization
import org.github.dreammooncai.kni.factory.AnyClass
import org.github.dreammooncai.kni.factory.ArrayListClass
import org.github.dreammooncai.kni.factory.BooleanClass
import org.github.dreammooncai.kni.factory.ByteClass
import org.github.dreammooncai.kni.factory.CharClass
import org.github.dreammooncai.kni.factory.DeserializationStrategyClass
import org.github.dreammooncai.kni.factory.DoubleClass
import org.github.dreammooncai.kni.factory.FloatClass
import org.github.dreammooncai.kni.factory.IntClass
import org.github.dreammooncai.kni.factory.IntType
import org.github.dreammooncai.kni.factory.JavaSetClass
import org.github.dreammooncai.kni.factory.JavaStringClass
import org.github.dreammooncai.kni.factory.KSerializerClass
import org.github.dreammooncai.kni.factory.KniExpansionClass
import org.github.dreammooncai.kni.factory.LinkedHashMapClass
import org.github.dreammooncai.kni.factory.ListClass
import org.github.dreammooncai.kni.factory.LongClass
import org.github.dreammooncai.kni.factory.MapClass
import org.github.dreammooncai.kni.factory.SerializationClass
import org.github.dreammooncai.kni.factory.SerializationDefaultClass
import org.github.dreammooncai.kni.factory.SerializationStrategyClass
import org.github.dreammooncai.kni.factory.ShortClass
import org.github.dreammooncai.kni.json
import platform.jni.jclass
import platform.jni.jobject
import kotlin.concurrent.Volatile

open class KniAny internal constructor(bridge: KniBridge,val jObject: jobject): KniBridge(bridge) {

    companion object {
        fun create(bridge: KniBridge, jObject: jobject) = with(bridge) { KniAny(bridge,if (jObject.isClass) jObject.ref else jObject.refToWeak) }
    }

    open val ref by lazy { KniAny(bridge,jObject.ref) }

    val isValid: Boolean get() = !jObject.isInvalidRef

    val isClass by lazy { this is KniClass || jObject.isClass }

    val asClass: jclass by lazy {
        if (isClass)
            jObject
        else
            runJavaCatching { fGetObjectClass(env,jObject)?.ref }.getOrNull() ?: error("无法获取类")
    }

    val asKniClass by lazy {
        KniClass(bridge, asClass)
    }

    val hashCode: Int by lazy {
        asKniClass.method {
            name = "hashCode"
            returnType = IntType
            thisRef = this@KniAny
        }.int()
    }

    @Volatile
    private var _serializerConfig: KniAny? = null

    /**
     * 获取当前 Java 类所使用的序列化配置
     *
     * 来自 [IKniSerialization.serializerConfig] 如果无，默认使用 [json]
     */
    private val serializerConfig: KniAny get() = _serializerConfig?.takeIf { it.isValid } ?: run {
        val config = if (!isClass && asKniClass.isInstanceOf(IKniSerialization::class.java)) {
            asKniClass.method {
                name = "getSerializerConfig"
                returnType = SerializationClass
                thisRef = this@KniAny
            }.call()?.refToWeak?.asKni ?: error("serializerConfig is null")
        } else SerializationDefaultClass.method {
            name = "getJson"
            returnType = SerializationClass
        }.call()?.refToWeak?.asKni ?: error("failed to get the common configuration")
        _serializerConfig = config
        config
    }

    @Volatile
    private var _serializer: KniAny? = null

    /**
     * 获取当前 Java 类的序列化器
     *
     * @return 当前 Java 类的序列化器
     */
    val serializer: KniAny get()  = _serializer?.takeIf { it.isValid } ?: run {
        val serializer = KniExpansionClass.method {
            name = "getSerializer"
            returnType = KSerializerClass
            param(JavaClass,SerializationClass)
        }.call(asClass,serializerConfig)?.refToWeak?.asKni ?: error("序列化描述符获取失败 是否生成了 serializer ?")
        _serializer = serializer
        serializer
    }

    /**
     * 将 [from] 使用当前 Java类 转为 JSON 字符串
     *
     * @param from 所需转换的对象
     * @return JSON 转换后的字符串
     */
    fun toJson(from: KniAny = this): String {
        if (from.isClass) {
            error("仅类无数据无法序列化转换Json")
        }
        return tryLocalFrame {
            SerializationClass.method {
                name = "encodeToString"
                param(SerializationStrategyClass,AnyClass)
                returnType = JavaStringClass
                thisRef = this@KniAny.serializerConfig
            }.string(serializer,from)
        }
    }

    fun toStringInJava(): String = AnyClass.method {
        name = "toString"
        returnType = JavaStringClass
        thisRef = this@KniAny
    }.string()

    /**
     * 将 native 对象 [form] 使用当前 Java类 转换成 Java 对象
     *
     * @param T native 对象的类型
     * @param form native 对象
     * @return Java 对象
     */
    inline fun <reified T> serialize(form: T) = serialize(json.encodeToString<T>(form))

    /**
     * 将 [json] 字符串 使用当前 Java类 转换成 Java 对象
     *
     * @param json json 字符串
     * @return Java 对象
     */
    fun serialize(json: String = "{}"): KniAny = tryLocalFrameResultJava {
        SerializationClass.method {
            name = "decodeFromString"
            param(DeserializationStrategyClass,JavaStringClass)
            returnType = AnyClass
            thisRef = this@KniAny.serializerConfig
        }.call(serializer,json)!!.asKni
    }

    /**
     * 将 [from] 使用当前 Java类 转为 native 对象
     *
     * @param from 所需转换的对象
     * @return native 对象
     */
    inline fun <reified T> deserialize(from: KniAny = this) = json.decodeFromString<T>(toJson(from))

    fun dump(
        indent: String = "",
        visited: MutableSet<Int> = hashSetOf()
    ): String {

        if (!visited.add(hashCode)) {
            return "${indent}<circular reference: ${asKniClass.name}>"
        }

        // 2) class object
        if (isClass) {
            return "${indent}Class(${asKniClass.name})"
        }

        // 3) primitives
        return when (val clazz = asKniClass) {
            IntClass -> "${indent}Int(${jObject.asAnyKni<Int>()})"
            LongClass -> "${indent}Long(${jObject.asAnyKni<Long>()})"
            FloatClass -> "${indent}Float(${jObject.asAnyKni<Float>()})"
            DoubleClass -> "${indent}Double(${jObject.asAnyKni<Double>()})"
            ShortClass -> "${indent}Short(${jObject.asAnyKni<Short>()})"
            ByteClass -> "${indent}Byte(${jObject.asAnyKni<Byte>()})"
            BooleanClass -> "${indent}Boolean(${jObject.asAnyKni<Boolean>()})"
            CharClass -> "${indent}Char('${jObject.asAnyKni<Char>()}')"

            // 4) String
            JavaStringClass -> "${indent}String(\"${jObject.asAnyKni<String>()}\")"

            else -> {
                // 5) arrays
                if (clazz.isArray) {
                    val arr = jObject.asAnyKni<Array<*>>() ?: return "${indent}[]"
                    return buildString {
                        append("${indent}${clazz.name}[")
                        if (arr.isNotEmpty()) append("\n")
                        arr.forEach { e ->
                            append(e.asAnyKni<KniAny>()?.dump("$indent  "))
                            append("\n")
                        }
                        append("${indent}]")
                    }
                }

                // 6) Collection
                if (isInstanceOf(ArrayListClass) ||
                    isInstanceOf(ListClass) ||
                    isInstanceOf(JavaSetClass) ||
                    isInstanceOf(MapClass)
                ) {
                    return dumpJavaCollection(indent,visited)
                }

                // 7) Enum
                if (clazz.isEnum) {
                    val enumName = clazz.method {
                        name = "name"
                        returnType = JavaStringClass
                        thisRef = this@KniAny
                    }.string()
                    return "${indent}${clazz.name}.$enumName"
                }

                // 8) 普通 Java 对象，递归打印字段
                dumpJavaObject(indent,visited)
            }
        }
    }

    private fun dumpJavaCollection(
        indent: String,
        visited: MutableSet<Int>
    ): String {
        // List
        if (isInstanceOf(ArrayListClass) || isInstanceOf(ListClass)) {
            return buildString {
                append("${indent}${this@KniAny.asKniClass.name}[\n")
                for (e in jObject.asList<KniAny?>()) {
                    append(e?.dump("$indent  ",visited))
                    append("\n")
                }
                append("${indent}]")
            }
        }

        // Set
        if (isInstanceOf(JavaSetClass)) {
            return buildString {
                append("${indent}${this@KniAny.asKniClass.name}[\n")
                for (e in jObject.asSet<KniAny?>()) {
                    append(e?.dump("$indent  ",visited))
                    append("\n")
                }
                append("${indent}]")
            }
        }

        // Map
        if (isInstanceOf(MapClass) || isInstanceOf(LinkedHashMapClass)) {
            return buildString {
                append("${indent}${this@KniAny.asKniClass.name}{\n")
                for ((k, v) in jObject.asMap<KniAny, KniAny?>()) {
                    append("$indent  ")
                    append(k.dump("$indent  ", visited))
                    append(" = ")
                    append(v?.dump("$indent  ", visited) ?: "null")
                    append("\n")
                }
                append("${indent}}")
            }
        }

        return "${indent}${this@KniAny.asKniClass.name}(?)"
    }

    private fun dumpJavaObject(
        indent: String,
        visited: MutableSet<Int>
    ): String = buildString {
        val cls = this@KniAny.asKniClass

        append("${indent}${cls.name} {\n")

        // 获取所有字段（包括继承字段）
        cls.fields.forEach { field ->
            val field = field.asKni(jObject)
            append(indent + "  ${field.name} = ")
            val value = field.any()
            if (value is KniAny) {
                append(value.dump("$indent  ", visited) )
            } else {
                append(value ?: "null")
            }
            append("\n")
        }
        append("${indent}}")
    }

    fun isInstanceOf(clazz: KniAny) = if (isClass) clazz.asKniClass.isAssignableFrom(asKniClass) else jObject.isInstanceOf(clazz)

    override fun toString(): String {
        return "KniAny(isClass=$isClass, Class=${asKniClass.name}, value=${dump()}), toString=${toStringInJava()}"
    }
}