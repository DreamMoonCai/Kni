@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class, InternalAPI::class)

package org.github.dreammooncai.kni.data

import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.locks.reentrantLock
import io.ktor.utils.io.locks.withLock
import org.github.dreammooncai.kni.data.sign.KniMethodSign
import org.github.dreammooncai.kni.util.DexSignUtil.getTypeSign
import org.github.dreammooncai.kni.KniBridge
import org.github.dreammooncai.kni.data.base.JniMethod
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import org.github.dreammooncai.kni.factory.BooleanType
import org.github.dreammooncai.kni.factory.ByteType
import org.github.dreammooncai.kni.factory.CharType
import org.github.dreammooncai.kni.factory.ConstructorClass
import org.github.dreammooncai.kni.factory.DoubleType
import org.github.dreammooncai.kni.factory.FloatType
import org.github.dreammooncai.kni.factory.IntType
import org.github.dreammooncai.kni.factory.LongType
import org.github.dreammooncai.kni.factory.MethodClass
import org.github.dreammooncai.kni.factory.ShortType
import org.github.dreammooncai.kni.factory.UnitType
import platform.jni.JNI_FALSE
import platform.jni.jclass
import platform.jni.jmethodID
import platform.jni.jobject
import kotlin.experimental.ExperimentalNativeApi

private val methodCache = mutableMapOf<String, KniMethod>()
private val methodCacheLock = reentrantLock()

class KniMethod private constructor(bridge: KniBridge, override val jMethodID: jmethodID, var thisRef: KniAny) :
    JniMethod(bridge) {

    override var declaringClass = thisRef.asKniClass
    override var modifiers: Int = 0
    private var isReturnVoid = false

    companion object {
        private fun KniBridge.getModifiers(clazz: jclass, jMethodID: jmethodID,isStatic:  Boolean,isConstructor: Boolean): Int {
            return arrayOf<Any?>().toJValue { param ->
                runJavaCatching {
                    val id = fGetMethodID(env, (if (isConstructor) ConstructorClass else MethodClass).asClass, "getModifiers".cstr.ptr, "()I".cstr.ptr)
                    val jMethod = fToReflectedMethod(env, clazz, jMethodID, isStatic.asJni)
                    fCallIntMethodA(env, jMethod, id, param)
                }.getOrElse { 0 }
            }
        }
        fun create(bridge: KniBridge, block: Build.() -> Unit) = Build(bridge).apply(block).let {
            val thisRef = it.methodThisRef ?: it.methodDeclaringClass ?: error("缺少所在类定义/缺少this实例")
            val clazz = it.methodDeclaringClass ?: thisRef.asKniClass
            val name = it.methodName
            val returnType = getTypeSign(it.methodReturnType ?: "void")
            val isStatic = (it.methodIsStatic ?: thisRef.isClass) && it.methodName != "<init>"
            val methodSign = buildString {
                append("(")
                append(it.methodParamTypes.joinToString("") { getTypeSign(it) })
                append(")")
                append(returnType)
            }
            val cacheKey = generateCacheKey(clazz.name, name, methodSign)
            methodCacheLock.withLock {
                val cache = methodCache[cacheKey]
                if (cache == null) {
                    val kni = createBasis(bridge, name, methodSign, clazz, thisRef,isStatic)
                    methodCache[cacheKey] = kni
                    kni
                } else {
                    cache.thisRef = thisRef
                    cache
                }
            }
        }

        fun create(bridge: KniBridge, jMethodID: jmethodID,jniMethod: JniMethod, thisRef: KniAny?) = KniMethod(
            bridge, jMethodID,
            thisRef ?: jniMethod.declaringClass
        ).also {
            it.declaringClass = jniMethod.declaringClass
            it.modifiers = jniMethod.modifiers
        }

        fun createBasis(
            bridge: KniBridge,
            methodName: String,
            methodSign: String,
            thisRefClass: KniClass,
            thisRef: KniAny? = null,
            isStatic: Boolean = thisRef == null && methodName != "<init>"
        ): KniMethod = with(bridge) {
            memScoped {
                val jMethodID = runJavaCatching {
                    (if (isStatic) bridge.fGetStaticMethodID else bridge.fGetMethodID)(
                        bridge.env,
                        thisRefClass.asClass,
                        methodName.cstr.ptr,
                        methodSign.cstr.ptr
                    )
                }.getOrNull() ?: error("无法在 ${thisRefClass.name} 找到 $methodName 方法")
                val thisRef = thisRef ?: thisRefClass
                KniMethod(bridge, jMethodID, thisRef).also { kni ->
                    kni.declaringClass = thisRefClass
                    kni.isReturnVoid = methodSign.endsWith(")V")
                    kni.modifiers = getModifiers(thisRefClass.asClass,jMethodID,isStatic,isConstructor = methodName == "<init>")
                }
            }
        }

        private fun generateCacheKey(declaringClassName: String, methodName: String, methodSign: String): String = "$declaringClassName#$methodName:$methodSign"
    }

    class Build(bridge: KniBridge) : KniBridge(bridge) {
        internal var methodName = ""
        internal var methodThisRef: KniAny? = null
        internal var methodDeclaringClass: KniClass? = null
        internal var methodReturnType: String? = null
        internal var methodParamTypes: List<String> = listOf()
        internal var methodIsStatic: Boolean? = null

        /**
         * 设置当前方法所使用的实例对象
         *
         * 如果未设置并且[isStatic]未指定，则默认为此方法为静态方法
         */
        var thisRef: KniAny
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                if (methodDeclaringClass == null)
                    methodDeclaringClass = value.asKniClass
                methodThisRef = value
            }

        /**
         * 手动指定当前方法是否是静态
         */
        var isStatic: Boolean
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                methodIsStatic = value
            }

        /**
         * The method descriptor.
         * ----------------
         * 方法描述符。
         *
         *     descriptor = "Ljava/lang/String;->length()I"
         */
        var descriptor: String
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                val kniMethodSign = KniMethodSign(value)
                name = kniMethodSign.name
                declaringClass = kniMethodSign.className
                returnType = kniMethodSign.returnTypeName
                paramTypes = kniMethodSign.paramTypeNames
            }

        /**
         * The method name.
         * ----------------
         * 方法名。
         */
        var name: String
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                methodName = value
            }

        /**
         * The method declared class.
         * ----------------
         * 方法声明类。
         */
        var declaringClass: Any
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                methodDeclaringClass = value.asKniClass
            }

        /**
         * The method return type.
         * ----------------
         * 方法返回值类型。
         */
        var returnType: Any
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                methodReturnType = value as? String ?: value.asKniClass.name
            }

        /**
         * The method parameter types.
         * ----------------
         * 方法参数类型
         */
        var paramTypes: List<Any>
            @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
            get() = throw NotImplementedError()
            set(value) {
                methodParamTypes = value.map { it as? String ?: it.asKniClass.name }
            }

        /**
         * The method parameter types.
         * ----------------
         * 方法参数类型
         */
        fun param(vararg type: Any) {
            paramTypes = type.toList()
        }
    }

    val ref by lazy {
        KniMethod(this, jMethodID, thisRef.ref).also {
            it.declaringClass = declaringClass.ref
            it.modifiers = modifiers
            it.isReturnVoid = isReturnVoid
            it.isCallOverride = isCallOverride
            it.isObjParam = isObjParam
        }
    }

    override val jMethod by lazy {
        runJavaCatching { fToReflectedMethod(env, declaringClass.asClass, jMethodID, isStatic.asJni)?.ref }.getOrNull()
            ?: error("获取 Method 失败")
    }

    private var isCallOverride = true

    private var isObjParam = false

    /**
     * 禁用执行重写的方法改为父类中的目标方法
     */
    fun disableCallOverride() = also { it.isCallOverride = false }

    /**
     * 使用对象参数执行 自动装箱
     */
    fun enableObjParam() = also { it.isObjParam = true }

    fun new(vararg arguments: Any?): jobject = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            fNewObject(env, declaringClass.asClass, jMethodID, arguments)!!
        }
    }.getOrThrow()

    @Suppress("UNCHECKED_CAST")
    fun any(vararg arguments: Any?): Any? = when (returnType) {
        IntType -> int(*arguments)
        BooleanType -> boolean(*arguments)
        ShortType -> short(*arguments)
        LongType -> long(*arguments)
        CharType -> char(*arguments)
        FloatType -> float(*arguments)
        DoubleType -> double(*arguments)
        ByteType -> byte(*arguments)
        UnitType -> void(*arguments)
        else -> call(*arguments)?.asKni
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> invoke(vararg arguments: Any?): T? = any(*arguments).let {
        if (T::class == Unit::class) return@let Unit
        if (it is KniAny) it.jObject.asAnyKni<T>() else it
    } as? T

    /**
     * 注意只处理 Obj 返回值，基本类型不在范围 如果需要类型检查请使用 [invoke]
     *
     * @param arguments 参数
     * @return 调用结果
     */
    fun call(vararg arguments: Any?): jobject? = runJavaCatching {
        if (isReturnVoid) {
            void(*arguments)
            null
        } else
            arguments.toJValue(isObjParam) { arguments ->
                if (isStatic)
                    fCallStaticObjectMethodA(env, declaringClass.asClass, jMethodID, arguments)
                else if (isCallOverride)
                    fCallObjectMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
                else
                    fCallNonvirtualObjectMethodA(
                        env,
                        thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                        declaringClass.asClass,
                        jMethodID,
                        arguments
                    )
            }
    }.getOrNull()

    fun void(vararg arguments: Any?) {
        runJavaCatching {
            arguments.toJValue(isObjParam) { arguments ->
                if (isStatic)
                    fCallStaticVoidMethodA(env, declaringClass.asClass, jMethodID, arguments)
                else if (isCallOverride)
                    fCallVoidMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
                else
                    fCallNonvirtualVoidMethodA(
                        env,
                        thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                        declaringClass.asClass,
                        jMethodID,
                        arguments
                    )
            }
        }
    }

    fun int(vararg arguments: Any?) = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            if (isStatic)
                fCallStaticIntMethodA(env, declaringClass.asClass, jMethodID, arguments)
            else if (isCallOverride)
                fCallIntMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
            else
                fCallNonvirtualIntMethodA(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                )
        }
    }.getOrThrow()

    fun byte(vararg arguments: Any?) = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            if (isStatic)
                fCallStaticByteMethodA(env, declaringClass.asClass, jMethodID, arguments)
            else if (isCallOverride)
                fCallByteMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
            else
                fCallNonvirtualByteMethodA(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                )
        }
    }.getOrThrow()

    fun long(vararg arguments: Any?) = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            if (isStatic)
                fCallStaticLongMethodA(env, declaringClass.asClass, jMethodID, arguments)
            else if (isCallOverride)
                fCallLongMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
            else
                fCallNonvirtualLongMethodA(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                )
        }
    }.getOrThrow()

    fun short(vararg arguments: Any?) = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            if (isStatic)
                fCallStaticShortMethodA(env, declaringClass.asClass, jMethodID, arguments)
            else if (isCallOverride)
                fCallShortMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
            else
                fCallNonvirtualShortMethodA(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                )
        }
    }.getOrThrow()

    fun double(vararg arguments: Any?) = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            if (isStatic)
                fCallStaticDoubleMethodA(env, declaringClass.asClass, jMethodID, arguments)
            else if (isCallOverride)
                fCallDoubleMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
            else
                fCallNonvirtualDoubleMethodA(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                )
        }
    }.getOrThrow()

    fun float(vararg arguments: Any?) = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            if (isStatic)
                fCallStaticFloatMethodA(env, declaringClass.asClass, jMethodID, arguments)
            else if (isCallOverride)
                fCallFloatMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
            else
                fCallNonvirtualFloatMethodA(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                )
        }
    }.getOrThrow()

    fun char(vararg arguments: Any?) = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            if (isStatic)
                fCallStaticCharMethodA(env, declaringClass.asClass, jMethodID, arguments)
            else if (isCallOverride)
                fCallCharMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments)
            else
                fCallNonvirtualCharMethodA(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                )
        }
    }.getOrThrow()

    fun boolean(vararg arguments: Any?) = runJavaCatching {
        arguments.toJValue(isObjParam) { arguments ->
            if (isStatic)
                fCallStaticBooleanMethodA(
                    env,
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                ) != JNI_FALSE.toUByte()
            else if (isCallOverride)
                fCallBooleanMethodA(env, thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") }, jMethodID, arguments) != JNI_FALSE.toUByte()
            else
                fCallNonvirtualBooleanMethodA(
                    env,
                    thisRef.jObject.also { if (it.isInvalidRef) error("在${name}中 this 对象已释放") },
                    declaringClass.asClass,
                    jMethodID,
                    arguments
                ) != JNI_FALSE.toUByte()
        }
    }.getOrThrow()

    fun string(vararg arguments: Any?) = memScoped {
        call(*arguments)?.asString ?: error("获取字符串返回值失败")
    }

    fun list(vararg arguments: Any?) = memScoped {
        call(*arguments)?.asArray ?: error("获取列表返回值失败")
    }
}