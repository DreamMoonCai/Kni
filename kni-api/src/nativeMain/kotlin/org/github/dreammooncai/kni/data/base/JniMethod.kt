@file:OptIn(ExperimentalNativeApi::class)

package org.github.dreammooncai.kni.data.base

import org.github.dreammooncai.kni.data.sign.KniMethodSign
import org.github.dreammooncai.kni.factory.MethodClass
import org.github.dreammooncai.kni.KniBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import org.github.dreammooncai.kni.data.KniAny
import org.github.dreammooncai.kni.data.KniMethod
import platform.jni.jmethodID
import platform.jni.jobject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.lazy

@OptIn(ExperimentalForeignApi::class)
abstract class JniMethod(bridge: KniBridge) : KniBridge(bridge) {
    internal abstract val jMethod: jobject
    internal open val jMethodID: jmethodID by lazy {
        memScoped {
            runJavaCatching {
                (if (isStatic) bridge.fGetStaticMethodID else bridge.fGetMethodID)(
                    bridge.env,
                    declaringClass.asClass,
                    name.cstr.ptr,
                    asSign.methodSign.cstr.ptr
                )
            }.getOrNull()
        } ?: error("无法在 ${declaringClass.name} 找到 $name 方法")
    }

    companion object {
        fun create(bridge: KniBridge, jMethod: jobject) = object : JniMethod(bridge) {
            override val jMethod: jobject = jMethod.ref
        }
    }

    fun asKni(thisRef: KniAny? = null) = KniMethod.create(this, jMethodID,this, thisRef)

    fun asKni(thisRef: jobject) = asKni(thisRef.asKni)

    val asSign by lazy {
        KniMethodSign(this)
    }

    /**
     * The method name.
     * ----------------
     * 方法名。
     */
    val name by lazy {
        methodBasis("getName", "()Ljava/lang/String;", MethodClass, jMethod.asKni).string()
    }

    /**
     * The method declared class.
     * ----------------
     * 方法声明类。
     */
    open val declaringClass by lazy {
        methodBasis("getDeclaringClass", "()Ljava/lang/Class;", MethodClass, jMethod.asKni).call()!!.asKniClass
    }

    /**
     * The method return type.
     * ----------------
     * 方法返回值类型。
     */
    val returnType by lazy {
        methodBasis("getReturnType", "()Ljava/lang/Class;", MethodClass, jMethod.asKni).call()!!.asKniClass
    }

    /**
     * The method parameter types.
     * ----------------
     * 方法参数类型
     */
    val parameterTypes by lazy {
        methodBasis(
            "getParameterTypes",
            "()[Ljava/lang/Class;",
            MethodClass,
            jMethod.asKni
        ).call()!!.asArray.map { it!!.asKniClass }
    }

    open val modifiers: Int by lazy {
        methodBasis(
            "getModifiers",
            "()I",
            MethodClass,
            jMethod.asKni
        ).int()
    }

    val isStatic by lazy {
        // java.lang.reflect.Modifier.STATIC = 0x0008
        (modifiers and 0x0008) != 0
    }

    val isPublic by lazy {
        // java.lang.reflect.Modifier.PUBLIC = 0x0001
        (modifiers and 0x0001) != 0
    }

    override fun toString(): String {
        return "$asSign"
    }
}