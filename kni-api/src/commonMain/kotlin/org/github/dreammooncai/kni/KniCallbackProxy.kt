package org.github.dreammooncai.kni

expect object KniCallbackProxy {
    fun invoke(handle: Long, args: List<Any?>): Any?
}