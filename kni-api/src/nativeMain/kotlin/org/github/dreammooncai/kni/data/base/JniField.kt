package org.github.dreammooncai.kni.data.base

import org.github.dreammooncai.kni.factory.FieldClass
import org.github.dreammooncai.kni.data.sign.KniFieldSign
import org.github.dreammooncai.kni.KniBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import org.github.dreammooncai.kni.data.KniAny
import org.github.dreammooncai.kni.data.KniField
import platform.jni.jfieldID
import platform.jni.jobject

@OptIn(ExperimentalForeignApi::class)
abstract class JniField(bridge: KniBridge) : KniBridge(bridge) {
    protected abstract val jField: jobject
    protected open val jFieldID: jfieldID by lazy {
        memScoped {
            runJavaCatching {
                (if (isStatic) bridge.fGetStaticFieldID else bridge.fGetFieldID)(
                    bridge.env,
                    declaringClass.asClass,
                    name.cstr.ptr,
                    asSign.typeSign.cstr.ptr
                )
            }.getOrNull()
        } ?: error("无法在 ${declaringClass.name} 找到 $name 字段")
    }

    companion object {
        fun create(bridge: KniBridge, jField: jobject) = object : JniField(bridge) {
            override val jField: jobject = jField.ref
        }
    }

    fun asKni(thisRef: KniAny? = null) = KniField.create(this, jFieldID,this, thisRef)

    fun asKni(thisRef: jobject?) = asKni(thisRef?.asKni)

    val asSign by lazy {
        KniFieldSign(this)
    }

    /**
     * The field name.
     * ----------------
     * 字段名。
     */
    val name by lazy {
        methodBasis("getName", "()Ljava/lang/String;", FieldClass, jField.asKni).string()
    }

    /**
     * The field declared class.
     * ----------------
     * 字段声明类。
     */
    open val declaringClass by lazy {
        methodBasis("getDeclaringClass", "()Ljava/lang/Class;", FieldClass, jField.asKni).call()!!.asKniClass
    }

    /**
     * The field type.
     * ----------------
     * 字段类型。
     */
    val type by lazy {
        methodBasis("getType", "()Ljava/lang/Class;", FieldClass, jField.asKni).call()!!.asKniClass
    }

    open val modifiers: Int by lazy {
        methodBasis(
            "getModifiers",
            "()I",
            FieldClass,
            jField.asKni
        ).int()
    }

    val isStatic by lazy {
        // java.lang.reflect.Modifier.STATIC = 0x0008
        (modifiers and 0x0008) != 0
    }

    override fun toString(): String {
        return "$asSign"
    }
}