@file:Suppress("UNCHECKED_CAST")

package org.github.dreammooncai.ipc

import android.os.IBinder
import android.util.Log
import io.github.dreammooncai.yukireflection.factory.name
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import org.github.dreammooncai.kni.json
import kotlin.reflect.KClass

class DreamJsonBinder<T : IDreamIPC>(
    impl: T,
    stub: KClass<T>,
) : DreamBaseJsonBinder<T>(impl, stub) {

    companion object {
        fun <T : IDreamIPC> asInterface(binder: IBinder,stub: KClass<T>): T =
            asInterface(stub) { data, reply, rpc, callable ->
                data.writeInterfaceToken(stub.name)
                data.writeString(json.encodeToString(DreamRpcCall.serializer(), rpc))

                binder.transact(TRANSACTION_SEND_FINISH, data, reply, 0)
                runCatching {
                    reply.readException()
                }.getOrElse {
                    Log.e("DreamJsonBinder", "transact error in $rpc", it)
                    return@asInterface null
                }

                val resultStr = reply.readString() ?: return@asInterface null
                val elem = runCatching {
                    json.decodeFromString(JsonElement.serializer(), resultStr)
                }.getOrElse {
                    Log.e("DreamJsonBinder", "transact error in $rpc", it)
                    return@asInterface null
                }

                runCatching {
                    json.decodeFromJsonElement(
                        json.serializersModule.serializer(callable.returnType), elem
                    )
                }.getOrElse { error ->
                    runCatching {
                        Log.e("DreamJsonBinder","serverSideError in $rpc", json.decodeFromJsonElement<DreamIPCThrowable>(elem).toThrowable())
                    }.onFailure { Log.e("DreamJsonBinder","clientError in $rpc",error) }
                    null
                }
            }
    }
}