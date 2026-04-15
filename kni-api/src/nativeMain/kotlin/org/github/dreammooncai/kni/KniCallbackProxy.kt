@file:OptIn(ExperimentalForeignApi::class)

package org.github.dreammooncai.kni

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import org.github.dreammooncai.kni.factory.kniResultJava
import platform.jni.jlong
import platform.jni.jobject

private val callbackLock = reentrantLock()

actual object KniCallbackProxy : IKniRegister {
    typealias Callback = (args: List<Any?>) -> Any?

    private val callbacks = mutableMapOf<Long, Callback>()

    private var nextHandle = 1L

    override fun KniRegister.onRegister() {
        ::invoke.register(staticCFunction { _, _, handle: jlong, args: jobject ->
            kniResultJava { invoke(handle, args.asList).asObjJni() }
        })
    }

    actual fun invoke(handle: Long, args: List<Any?>): Any? {
        val callback = callbackLock.withLock { callbacks[handle] }
        if (callback == null) {
            logger.warn { "当前回调未记录 : $handle" }
            return null
        }
        return callback.invoke(args)
    }

    fun put(callback: Callback): Long {
        val handle = nextHandle
        callbackLock.withLock { callbacks[handle] = callback }
        nextHandle++
        return handle
    }
}