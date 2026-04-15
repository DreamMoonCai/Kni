@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package org.github.dreammooncai.kni.factory


import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import org.github.dreammooncai.kni.KniBridge
import org.github.dreammooncai.kni.KniCallbackProxy
import platform.jni.JNIEnvVar
import platform.jni.jlong
import platform.jni.jobject
import kotlin.experimental.ExperimentalNativeApi

context(kni: KniBridge)
fun internalCreateCallback(arity: Int, callback: KniCallbackProxy.Callback): jobject = with(kni) {
    KniCallbackProxy::class.java.method {
        name = "create"
        param(LongType, IntType)
        returnType = AnyClass
    }.call(KniCallbackProxy.put(callback), arity)!!
}

context(kni: KniBridge)
fun <R> (() -> R).asJni(): jobject = with(kni) {
    internalCreateCallback(0) {
        this@asJni.invoke()
    }
}

context(kni: KniBridge)
inline fun <reified P1, R> ((P1) -> R).asJni(): jobject = with(kni) {
    internalCreateCallback(1) { args ->
        this@asJni.invoke(args[0].asAnyKni<P1>() as P1)
    }
}

context(kni: KniBridge)
inline fun <reified P1, reified P2, R> ((P1, P2) -> R).asJni(): jobject = with(kni) {
    internalCreateCallback(2) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[2].asAnyKni<P2>() as P2
        )
    }
}

// ----------------------------------------
// Function3
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, R>
        ((P1, P2, P3) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(3) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3
        )
    }
}

// ----------------------------------------
// Function4
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, R>
        ((P1, P2, P3, P4) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(4) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4
        )
    }
}

// ----------------------------------------
// Function5
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, R>
        ((P1, P2, P3, P4, P5) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(5) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4,
            args[4].asAnyKni<P5>() as P5
        )
    }
}

// ----------------------------------------
// Function6
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, R>
        ((P1, P2, P3, P4, P5, P6) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(6) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4,
            args[4].asAnyKni<P5>() as P5,
            args[5].asAnyKni<P6>() as P6
        )
    }
}

// ----------------------------------------
// Function7
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, R>
        ((P1, P2, P3, P4, P5, P6, P7) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(7) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4,
            args[4].asAnyKni<P5>() as P5,
            args[5].asAnyKni<P6>() as P6,
            args[6].asAnyKni<P7>() as P7
        )
    }
}

// ----------------------------------------
// Function8
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, R>
        ((P1, P2, P3, P4, P5, P6, P7, P8) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(8) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4,
            args[4].asAnyKni<P5>() as P5,
            args[5].asAnyKni<P6>() as P6,
            args[6].asAnyKni<P7>() as P7,
            args[7].asAnyKni<P8>() as P8
        )
    }
}

// ----------------------------------------
// Function9
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, R>
        ((P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(9) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4,
            args[4].asAnyKni<P5>() as P5,
            args[5].asAnyKni<P6>() as P6,
            args[6].asAnyKni<P7>() as P7,
            args[7].asAnyKni<P8>() as P8,
            args[8].asAnyKni<P9>() as P9
        )
    }
}

// ----------------------------------------
// Function10
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5,
        reified P6, reified P7, reified P8, reified P9, reified P10, R>
        ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(10) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4,
            args[4].asAnyKni<P5>() as P5,
            args[5].asAnyKni<P6>() as P6,
            args[6].asAnyKni<P7>() as P7,
            args[7].asAnyKni<P8>() as P8,
            args[8].asAnyKni<P9>() as P9,
            args[9].asAnyKni<P10>() as P10
        )
    }
}

// ----------------------------------------
// Function11
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5,
        reified P6, reified P7, reified P8, reified P9, reified P10,
        reified P11, R>
        ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(11) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4,
            args[4].asAnyKni<P5>() as P5,
            args[5].asAnyKni<P6>() as P6,
            args[6].asAnyKni<P7>() as P7,
            args[7].asAnyKni<P8>() as P8,
            args[8].asAnyKni<P9>() as P9,
            args[9].asAnyKni<P10>() as P10,
            args[10].asAnyKni<P11>() as P11
        )
    }
}

// ----------------------------------------
// Function12
// ----------------------------------------
context(kni: KniBridge)
inline fun <reified P1, reified P2, reified P3, reified P4, reified P5,
        reified P6, reified P7, reified P8, reified P9, reified P10,
        reified P11, reified P12, R>
        ((P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R).asJni(): jobject = with(kni) {

    internalCreateCallback(12) { args ->
        this@asJni.invoke(
            args[0].asAnyKni<P1>() as P1,
            args[1].asAnyKni<P2>() as P2,
            args[2].asAnyKni<P3>() as P3,
            args[3].asAnyKni<P4>() as P4,
            args[4].asAnyKni<P5>() as P5,
            args[5].asAnyKni<P6>() as P6,
            args[6].asAnyKni<P7>() as P7,
            args[7].asAnyKni<P8>() as P8,
            args[8].asAnyKni<P9>() as P9,
            args[9].asAnyKni<P10>() as P10,
            args[10].asAnyKni<P11>() as P11,
            args[11].asAnyKni<P12>() as P12
        )
    }
}