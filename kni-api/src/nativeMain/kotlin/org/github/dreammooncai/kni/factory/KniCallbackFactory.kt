@file:OptIn(ExperimentalForeignApi::class)

package org.github.dreammooncai.kni.factory

import kotlinx.cinterop.ExperimentalForeignApi
import org.github.dreammooncai.kni.KniBridge
import platform.jni.jobject

/**
 * 获取回调方法引用
 * 优先查找名为 "invoke" 的方法，否则找第一个 public 方法
 */
@PublishedApi
context(kni: KniBridge)
internal fun jobject.getInvokeMethod() = with(kni) {
    val methods = this.asKniClass.methods
    val target = methods.firstOrNull { it.name == "invoke" } ?: methods.firstOrNull { it.isPublic }
    target?.asKni(this@getInvokeMethod)?.ref ?: error("无法获取 invoke 方法")
}

context(kni: KniBridge)
inline fun <reified R> jobject.asKniCallback(): () -> R = with(kni) {
    val invoke = this@asKniCallback.getInvokeMethod()
    return { tryException { invoke.invoke<R>() as R } }
}

context(kni: KniBridge)
inline fun <reified R> jobject.asKniArgsCallback(): (args: List<*>) -> R = with(kni) {
    val invoke = this@asKniArgsCallback.getInvokeMethod()
    return { args ->
        tryException { invoke.invoke<R>(*args.toTypedArray()) as R }
    }
}

context(kni: KniBridge)
inline fun <reified P1, reified R> jobject.asKniCallback(): (p1: P1?) -> R = with(kni) {
    val invoke = this@asKniCallback.getInvokeMethod()
    return { p1 ->
        tryException { invoke.invoke<R>(p1) as R }
    }
}

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified R> jobject.asKniCallback(): (p1: P1?, p2: P2?) -> R = with(kni) {
    val invoke = this@asKniCallback.getInvokeMethod()
    return { p1, p2 ->
        tryException { invoke.invoke<R>(p1, p2) as R }
    }
}

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified R> jobject.asKniCallback(): (P1?, P2?, P3?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3 ->
            tryException { invoke.invoke<R>(p1, p2, p3) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?, P5?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4, p5 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4, p5) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?, P5?, P6?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4, p5, p6 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4, p5, p6) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?, P5?, P6?, P7?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4, p5, p6, p7 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4, p5, p6, p7) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?, P5?, P6?, P7?, P8?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4, p5, p6, p7, p8 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4, p5, p6, p7, p8) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?, P5?, P6?, P7?, P8?, P9?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4, p5, p6, p7, p8, p9 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4, p5, p6, p7, p8, p9) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?, P5?, P6?, P7?, P8?, P9?, P10?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4, p5, p6, p7, p8, p9, p10 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?, P5?, P6?, P7?, P8?, P9?, P10?, P11?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11) as R }
        }
    }

context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified R> jobject.asKniCallback(): (P1?, P2?, P3?, P4?, P5?, P6?, P7?, P8?, P9?, P10?, P11?, P12?) -> R =
    with(kni) {
        val invoke = this@asKniCallback.getInvokeMethod()
        return { p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12 ->
            tryException { invoke.invoke<R>(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12) as R }
        }
    }
