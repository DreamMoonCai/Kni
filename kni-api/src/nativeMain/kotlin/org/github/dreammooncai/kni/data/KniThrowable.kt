@file:OptIn(ExperimentalForeignApi::class)

package org.github.dreammooncai.kni.data

import com.martmists.multiplatform.reflect.getter
import kotlinx.cinterop.ExperimentalForeignApi
import org.github.dreammooncai.kni.KniBridge
import org.github.dreammooncai.kni.factory.JavaStringClass
import org.github.dreammooncai.kni.factory.ThrowableClass
import platform.jni.jthrowable

class KniThrowable(bridge: KniBridge,val jThrowable: jthrowable):KniAny(bridge,jThrowable) {
    class KniInJavaException : Exception {
        constructor() : super()

        constructor(message: String?) : super(message)

        constructor(message: String?, cause: Throwable?) : super(message, cause)

        constructor(cause: Throwable?) : super(cause)
    }

    override val ref: KniThrowable by lazy { KniThrowable(bridge,jThrowable.ref) }

    val message: String get() = Throwable::message.getter.asKniMethod(this).string()

    val localizedMessage: String get() = ThrowableClass.method(this) {
        name = "getLocalizedMessage"
        returnType = JavaStringClass
    }.string()

    fun stackTraceToString(): String = "kotlin.ExceptionsKt".toClass().methods.first { it.name.contains("stackTraceToString") }.asKni(jObject).string(jThrowable)

    fun asKotlinThrowable(): KniInJavaException = KniInJavaException(toString())

    override fun toString(): String = "${toStringInJava()} \n${stackTraceToString()}"
}