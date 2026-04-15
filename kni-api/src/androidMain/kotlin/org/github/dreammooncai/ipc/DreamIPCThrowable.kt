package org.github.dreammooncai.ipc

import io.github.dreammooncai.yukireflection.factory.name
import kotlinx.serialization.Serializable

@Serializable
data class DreamIPCThrowable(val message: String, val stack: String) {
    constructor(e: Throwable): this(buildString {
        val name = e::class.name
        val message = e.localizedMessage
        if (message != null) {
            append(name)
            append(": ")
            append(message)
        } else {
            append(name)
        }
    }, e.stackTraceToString())

    fun toThrowable(): Throwable {
        return Throwable(message, Throwable(stack))
    }
}