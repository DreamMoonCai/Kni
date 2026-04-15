@file:Suppress("unused")

package org.github.dreammooncai.kni

import java.lang.reflect.Proxy

actual object KniCallbackProxy {
    init {
        KniLoader.loader()
    }

    actual external fun invoke(handle: Long, args: List<Any?>): Any?

    @JvmStatic
    fun create(handle: Long, arity: Int): Any = createFunctionProxy(handle, arity)

    private fun createFunctionProxy(handle: Long, arity: Int): Any {
        val functionClass = Class.forName("kotlin.jvm.functions.Function$arity")

        return Proxy.newProxyInstance(
            functionClass.classLoader,
            arrayOf(functionClass)
        ) { _, method, args ->

            if (method.name == "invoke") {
                invoke(handle, (args ?: emptyArray()).toList())
            } else {
                error("不支持的方法调用")
            }
        }
    }
}