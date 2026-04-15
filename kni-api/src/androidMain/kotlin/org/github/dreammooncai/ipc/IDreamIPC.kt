package org.github.dreammooncai.ipc

import io.github.dreammooncai.yukireflection.factory.classLoader
import io.github.dreammooncai.yukireflection.factory.name
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

interface IDreamIPC {
    companion object {
        private val instances = ConcurrentHashMap<Long, Pair<KType, Any>>()

        private var instanceId = 1L
    }

    interface IInstance {
        val ipcInstanceId: Long?

        fun toInstance(): Instance = Instance(ipcInstanceId)
    }

    @Serializable
    class Instance(override val ipcInstanceId: Long?) :IInstance

    fun callInstanceId(rpcCall: DreamRpcCall, thisRef: Long? = null): Pair<String, Long>? {
        val thisRef = if (thisRef == null) this else instances[thisRef]?.second ?: this
        val callable = rpcCall.getCallable(thisRef::class.classLoader)
        val result = rpcCall.callAny(callable, thisRef, onTransact = { parameter, element ->
            if (element is IInstance) instances[element.ipcInstanceId]?.second else element
        }) ?: return null
        val id = synchronized(this) {
            val id = instanceId++
            instances[id] = callable.returnType to result
            id
        }
        return result::class.name to id
    }

    fun callInstanceJson(rpcCall: DreamRpcCall, thisRef: Long? = null): JsonElement {
        val thisRef = if (thisRef == null) this else instances[thisRef]?.second ?: this
        return rpcCall.call(this::class.classLoader,thisRef, onTransact = { parameter, element ->
            if (element is IInstance) instances[element.ipcInstanceId]?.second else element
        }) ?: JsonNull
    }
}