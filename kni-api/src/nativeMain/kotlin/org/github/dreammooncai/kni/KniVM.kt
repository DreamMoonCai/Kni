@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package org.github.dreammooncai.kni

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import org.github.dreammooncai.kni.factory.KniDslMarker
import org.github.dreammooncai.kni.factory.kni
import platform.jni.JNIEnvVar
import platform.jni.JNI_OK
import platform.jni.JNI_VERSION_1_6
import platform.jni.JNI_VERSION_21
import platform.jni.JavaVMVar
import platform.jni.jlong
import platform.jni.jobject
import platform.jni.jstring
import platform.posix.pthread_self
import platform.posix.pthread_t
import kotlin.experimental.ExperimentalNativeApi

object KniVM {
    var kniJavaVM:CPointer<JavaVMVar>? = null

    private val fAttachCurrentThread get() = kniJavaVM!!.pointed.pointed!!.AttachCurrentThread!!
    private val fDetachCurrentThread get() = kniJavaVM!!.pointed.pointed!!.DetachCurrentThread!!
    private val fGetEnv get() = kniJavaVM!!.pointed.pointed!!.GetEnv!!

    fun onLoad(vm: CPointer<JavaVMVar>,vararg registers: IKniRegister,block: KniRegister.() -> Unit = {}) {
        kniJavaVM = vm
        kni {
            register {
                register(KniCallbackProxy, KniLogger,*registers)
                block()
            }
        }
    }

    var onUnloads = mutableListOf<() -> Unit>()

    fun addUnload(block: () -> Unit) {
        onUnloads.add(block)
    }

    fun onUnload() {
        kniJavaVM  = null
        onUnloads.forEach { it() }
        onUnloads.clear()
    }

    fun attachCurrentThread(): CPointer<JNIEnvVar> {
        return run {
            val vm = kniJavaVM ?: throw IllegalStateException("JavaVM not initialized")

            memScoped {
                val envPtr = alloc<CPointerVar<JNIEnvVar>>()
                val result = fAttachCurrentThread(
                    vm,
                    envPtr.ptr.reinterpret(),
                    null
                )

                if (result != JNI_OK) {
                    throw RuntimeException("Failed to attach thread: $result")
                }

                envPtr.value ?: throw RuntimeException("Failed to get JNIEnv")
            }
        }
    }

    fun detachCurrentThread() {
        fDetachCurrentThread(kniJavaVM ?: return)
    }

    fun <T> withEnv(block: (env: CPointer<JNIEnvVar>) -> T): T {
        val currentEnv = getCurrentThreadEnv()
        if (currentEnv != null) {
            return block(currentEnv)
        }

        val env = attachCurrentThread()
        return try {
            block(env)
        } finally {
            detachCurrentThread()
        }
    }

    fun getCurrentThreadEnv(): CPointer<JNIEnvVar>? {
        return memScoped {
            val vm = kniJavaVM ?: return null
            val envPtr = alloc<CPointerVar<JNIEnvVar>>()
            val result = fGetEnv(
                vm,
                envPtr.ptr.reinterpret(),
                JNI_VERSION_1_6
            )
            if (result == JNI_OK) envPtr.value else null
        }
    }

    /**
     * 检查当前线程是否已附加到JVM
     * @return true 如果已附加，false 如果未附加
     */
    fun isThreadAttached(): Boolean = getCurrentThreadEnv() != null
}