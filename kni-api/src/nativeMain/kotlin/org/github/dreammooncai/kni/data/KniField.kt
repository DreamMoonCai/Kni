@file:OptIn(InternalAPI::class, ExperimentalForeignApi::class)

package org.github.dreammooncai.kni.data

import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.locks.reentrantLock
import io.ktor.utils.io.locks.withLock
import org.github.dreammooncai.kni.data.sign.KniFieldSign
import org.github.dreammooncai.kni.util.DexSignUtil.getTypeSign
import org.github.dreammooncai.kni.KniBridge
import org.github.dreammooncai.kni.data.base.JniField
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import org.github.dreammooncai.kni.factory.BooleanType
import org.github.dreammooncai.kni.factory.ByteType
import org.github.dreammooncai.kni.factory.CharType
import org.github.dreammooncai.kni.factory.DoubleType
import org.github.dreammooncai.kni.factory.FieldClass
import org.github.dreammooncai.kni.factory.FloatType
import org.github.dreammooncai.kni.factory.IntType
import org.github.dreammooncai.kni.factory.LongType
import org.github.dreammooncai.kni.factory.ShortType
import org.github.dreammooncai.kni.factory.UnitType
import platform.jni.JNI_FALSE
import platform.jni.JNI_TRUE
import platform.jni.jclass
import platform.jni.jfieldID
import platform.jni.jobject
import kotlin.reflect.KProperty

private val fieldCache = mutableMapOf<String, KniField>()
private val fieldCacheLock = reentrantLock()

class KniField private constructor(bridge: KniBridge, override val jFieldID: jfieldID, var thisRef: KniAny) :
    JniField(bridge) {

    override var declaringClass = thisRef.asKniClass

    override var modifiers: Int = 0

    companion object {

        private fun KniBridge.getModifiers(clazz: jclass, jFieldID: jfieldID,isStatic: Boolean): Int {
            return runJavaCatching {
                arrayOf<Any?>().toJValue { param ->
                    val id = fGetMethodID(env, FieldClass.asClass, "getModifiers".cstr.ptr, "()I".cstr.ptr)
                    val jField = fToReflectedField(env, clazz, jFieldID, isStatic.asJni)
                    fCallIntMethodA(env, jField, id, param)
                }
            }.getOrElse { 0 }
        }

        fun create(bridge: KniBridge, block: Build.() -> Unit) = memScoped {
            Build(bridge).apply(block).let {
                val thisRef = it.fieldThisRef ?: it.fieldDeclaringClass ?: error("缺少所在类定义/缺少this实例")
                val clazz = it.fieldDeclaringClass ?: thisRef.asKniClass
                val name = it.fieldName
                val type = getTypeSign(it.fieldType)
                val isStatic = it.fieldIsStatic ?: thisRef.isClass
                val cacheKey = generateCacheKey(clazz.name, name, type)
                fieldCacheLock.withLock {
                    val cache = fieldCache[cacheKey]
                    if (cache == null) {
                        val kni = createBasis(bridge, name, type, clazz, thisRef, isStatic)
                        fieldCache[cacheKey] = kni
                        kni
                    } else {
                        cache.thisRef = thisRef
                        cache
                    }
                }
            }
        }

        fun create(bridge: KniBridge, jFieldID: jfieldID, jniField: JniField, thisRef: KniAny? = null) = KniField(
            bridge, jFieldID, thisRef ?: jniField.declaringClass
        ).also {
            it.declaringClass = jniField.declaringClass
            it.modifiers = jniField.modifiers
        }

        fun createBasis(
            bridge: KniBridge,
            fieldName: String,
            fieldTypeSign: String,
            thisRefClass: KniClass,
            thisRef: KniAny? = null,
            isStatic: Boolean = thisRef == null
        ) = with(bridge) {
            memScoped {
                val jFieldID = runJavaCatching {
                    (if (isStatic) bridge.fGetStaticFieldID else bridge.fGetFieldID)(
                        bridge.env,
                        thisRefClass.asClass,
                        fieldName.cstr.ptr,
                        fieldTypeSign.cstr.ptr
                    )
                }.getOrNull() ?: error("无法在 ${thisRefClass.asKniClass.name} 找到 $fieldName 字段")
                val thisRef = thisRef ?: thisRefClass
                KniField(bridge, jFieldID, thisRef).also { kni ->
                    kni.declaringClass = thisRefClass
                    kni.modifiers = getModifiers(thisRefClass.asClass, jFieldID,isStatic)
                }
            }
        }

        private fun generateCacheKey(declaringClassName: String, fieldName: String, fieldType: String): String = "$declaringClassName#$fieldName:$fieldType"
    }

    class Build(bridge: KniBridge) : KniBridge(bridge) {
        internal var fieldName = ""
        internal var fieldType = ""
        internal var fieldThisRef: KniAny? = null
        internal var fieldDeclaringClass: KniClass? = null
        internal var fieldIsStatic: Boolean? = null

        /**
         * 设置当前字段所使用的实例对象
         *
         * 如果未设置并且[isStatic]未指定，则默认为此字段为静态字段
         */
        var thisRef: KniAny
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                if (fieldDeclaringClass == null)
                    fieldDeclaringClass = value.asKniClass
                fieldThisRef = value
            }

        /**
         * 手动指定当前方法是否是静态
         */
        var isStatic: Boolean
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                fieldIsStatic = value
            }

        /**
         * The field descriptor.
         * ----------------
         * 字段描述符。
         *
         *     descriptor = "Lorg/luckypray/dexkit/demo/MainActivity;->mText:Ljava/lang/String;"
         */
        var descriptor: String
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                val kniFieldSign = KniFieldSign(value)
                name = kniFieldSign.name
                declaringClass = kniFieldSign.className
                type = kniFieldSign.typeName
            }

        /**
         * The field declared class.
         * ----------------
         * 字段声明类。
         */
        var declaringClass: Any
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                fieldDeclaringClass = value.asKniClass
            }

        /**
         * The field name.
         * ----------------
         * 字段名。
         */
        var name: String
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                fieldName = value
            }

        /**
         * The field type.
         * ----------------
         * 字段类型。
         */
        var type: Any
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                fieldType = value as? String ?: value.asKniClass.name
            }
    }

    val ref by lazy {
        KniField(this, jFieldID, thisRef.ref).also {
            it.declaringClass = declaringClass.ref
            it.modifiers = modifiers
        }
    }

    override val jField by lazy {
        runJavaCatching { fToReflectedField(env, declaringClass.asClass, jFieldID, isStatic.asJni)?.ref }.getOrNull()
            ?: error("获取 Field 失败")
    }

    fun any(): Any? = when (type) {
        IntType -> int()
        BooleanType -> boolean()
        ShortType -> short()
        LongType -> long()
        CharType -> char()
        FloatType -> float()
        DoubleType -> double()
        ByteType -> byte()
        UnitType -> null
        else -> obj()?.asKni
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> any(): T? = any().let {
        if (it is KniAny) it.jObject.asAnyKni<T>() else it
    } as? T

    fun obj(): jobject? = runJavaCatching {
        if (isStatic)
            fGetStaticObjectField(env, declaringClass.asClass, jFieldID)
        else
            fGetObjectField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID)
    }.getOrNull()

    fun int(): Int = runJavaCatching {
        if (isStatic)
            fGetStaticIntField(env, declaringClass.asClass, jFieldID)
        else
            fGetIntField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID)
    }.getOrThrow()

    fun byte(): Byte = runJavaCatching {
        if (isStatic)
            fGetStaticByteField(env, declaringClass.asClass, jFieldID)
        else
            fGetByteField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID)
    }.getOrThrow()

    fun long(): Long = runJavaCatching {
        if (isStatic)
            fGetStaticLongField(env, declaringClass.asClass, jFieldID)
        else
            fGetLongField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID)
    }.getOrThrow()

    fun short(): Short = runJavaCatching {
        if (isStatic)
            fGetStaticShortField(env, declaringClass.asClass, jFieldID)
        else
            fGetShortField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID)
    }.getOrThrow()

    fun double(): Double = runJavaCatching {
        if (isStatic)
            fGetStaticDoubleField(env, declaringClass.asClass, jFieldID)
        else
            fGetDoubleField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID)
    }.getOrThrow()

    fun float(): Float = runJavaCatching {
        if (isStatic)
            fGetStaticFloatField(env, declaringClass.asClass, jFieldID)
        else
            fGetFloatField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID)
    }.getOrThrow()

    fun char(): Char = runJavaCatching {
        (if (isStatic)
            fGetStaticCharField(env, declaringClass.asClass, jFieldID)
        else
            fGetCharField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID)).toInt().toChar()
    }.getOrThrow()

    fun boolean(): Boolean = runJavaCatching {
        if (isStatic)
            fGetStaticBooleanField(env, declaringClass.asClass, jFieldID) != JNI_FALSE.toUByte()
        else
            fGetBooleanField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID) != JNI_FALSE.toUByte()
    }.getOrThrow()

    fun string() = obj()?.asString ?: error("获取字符串返回值失败")

    fun list() = obj()?.asArray ?: error("获取列表返回值失败")

    @Suppress("UNCHECKED_CAST")
    fun set(any: Any?) = runJavaCatching {
        if (isStatic) {
            // 静态字段
            when (any) {
                is Int -> fSetStaticIntField(env, declaringClass.asClass, jFieldID, any)
                is Byte -> fSetStaticByteField(env, declaringClass.asClass, jFieldID, any)
                is Char -> fSetStaticCharField(env, declaringClass.asClass, jFieldID, any.code.toUShort())
                is Long -> fSetStaticLongField(env, declaringClass.asClass, jFieldID, any)
                is Float -> fSetStaticFloatField(env, declaringClass.asClass, jFieldID, any)
                is Boolean -> fSetStaticBooleanField(
                    env,
                    declaringClass.asClass,
                    jFieldID,
                    if (any) JNI_TRUE.toUByte() else JNI_FALSE.toUByte()
                )

                is Double -> fSetStaticDoubleField(env, declaringClass.asClass, jFieldID, any)
                is Short -> fSetStaticShortField(env, declaringClass.asClass, jFieldID, any)
                else -> any.localLocalJniRelease { any ->
                    fSetStaticObjectField(env, declaringClass.asClass, jFieldID, any.also { if (it.isInvalidRef) error("在${name}中用于Set的 参数 对象已释放") })
                }
            }
        } else {
            // 实例字段
            when (any) {
                is Int -> fSetIntField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID, any)
                is Byte -> fSetByteField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID, any)
                is Char -> fSetCharField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID, any.code.toUShort())
                is Long -> fSetLongField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID, any)
                is Float -> fSetFloatField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID, any)
                is Boolean -> fSetBooleanField(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    jFieldID,
                    if (any) JNI_TRUE.toUByte() else JNI_FALSE.toUByte()
                )

                is Double -> fSetDoubleField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID, any)
                is Short -> fSetShortField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID, any)
                else -> any.localLocalJniRelease { any ->
                    fSetObjectField(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jFieldID, any.also { if (it.isInvalidRef) error("在${name}中用于Set的 参数 对象已释放") })
                }
            }
        }
    }.getOrThrow()

    /**
     * 设置当前 [KProperty] 实例为 true
     *
     * - 请确保示例对象类型为 [Boolean]
     */
    fun setTrue() = set(true)

    /**
     * 设置当前 [KProperty] 实例为 true
     *
     * - 请确保示例对象类型为 [Boolean]
     */
    fun setFalse() = set(false)

    /** 设置当前 [KProperty] 实例为 null */
    fun setNull() = set(null)
}