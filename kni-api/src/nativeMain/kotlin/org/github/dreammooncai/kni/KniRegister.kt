@file:OptIn(ExperimentalForeignApi::class)

package org.github.dreammooncai.kni

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import org.github.dreammooncai.kni.data.KniClass
import org.github.dreammooncai.kni.data.KniMethod
import platform.jni.JNIEnvVar
import platform.jni.jobject
import kotlin.reflect.KFunction

class KniRegister(bridge: KniBridge):KniBridge(bridge) {
    val registerKniMap = mutableMapOf<KniMethod.Build, CPointer<out CPointed>>()
    val registerKFunctionMap = mutableMapOf<KFunction<*>, CPointer<out CPointed>>()

    fun build(block: KniMethod.Build.()-> Unit) = KniMethod.Build(this).apply(block)

    fun KniClass.build(block: KniMethod.Build.()-> Unit) = KniMethod.Build(this).apply {
        declaringClass = this@build.name
        block()
    }

    // ----------------------------------------
    // Function0
    // ----------------------------------------
    fun <R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function1
    // ----------------------------------------
    fun <P1, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject, p1: P1) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function2
    // ----------------------------------------
    fun <P1, P2, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject, p1: P1, p2: P2) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function3
    // ----------------------------------------
    fun <P1, P2, P3, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function4
    // ----------------------------------------
    fun <P1, P2, P3, P4, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function5
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function6
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function7
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function8
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function9
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function10
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function11
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // Function12
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> KniMethod.Build.register(
        function: CPointer<CFunction<(env: CPointer<JNIEnvVar>, thiz: jobject,
                                      p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12) -> R>>
    ) {
        this@KniRegister.registerKniMap[this] = function
    }

    // ----------------------------------------
    // KFunction0
    // ----------------------------------------
    fun <R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction1
    // ----------------------------------------
    fun <P1, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction2
    // ----------------------------------------
    fun <P1, P2, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction3
    // ----------------------------------------
    fun <P1, P2, P3, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction4
    // ----------------------------------------
    fun <P1, P2, P3, P4, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction5
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4, P5) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction6
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4, P5, P6) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction7
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4, P5, P6, P7) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction8
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4, P5, P6, P7, P8) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction9
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction10
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction11
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    // ----------------------------------------
    // KFunction12
    // ----------------------------------------
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> KFunction<*>.register(
        function: CPointer<CFunction<(CPointer<JNIEnvVar>, jobject, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R>>
    ) {
        registerKFunctionMap[this] = function
    }

    fun IKniRegister.register() { onRegister() }

    fun register(vararg register: IKniRegister) {
        register.forEach { r -> with(r) { onRegister() } }
    }
}
