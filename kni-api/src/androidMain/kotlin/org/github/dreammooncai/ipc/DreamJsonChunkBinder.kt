@file:Suppress("UNCHECKED_CAST")

package org.github.dreammooncai.ipc

import android.os.IBinder
import android.os.Parcel
import android.util.Log
import io.github.dreammooncai.yukireflection.factory.name
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import org.github.dreammooncai.kni.json
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class DreamJsonChunkBinder<T : IDreamIPC>(
    impl: T,
    stub: KClass<T>,
    private val chunkSize: Int = 32 * 1024
) : DreamBaseJsonBinder<T>(impl, stub) {

    override fun writeResult(reply: Parcel, chunk: String) {
        var offset = 0
        val length = chunk.length

        while (offset < length) {
            val end = minOf(offset + chunkSize, length)
            reply.writeString(chunk.substring(offset, end))
            offset = end
        }

        reply.writeString(TRANSACTION_RETURN_FINISH)
    }

    companion object {

        fun <T : IDreamIPC> asInterface(binder: IBinder,stub: KClass<T>, chunkSize: Int, onTransact: (data: Parcel, reply: Parcel, rpc: DreamRpcCall, callable: KCallable<*>) -> Any?): T =
            asInterface(stub) { data, reply, rpc, callable ->
                val rpcJson = json.encodeToString(DreamRpcCall.serializer(), rpc)

                // ---- 分片发送 request ----
                var offset = 0
                while (offset < rpcJson.length) {
                    val end = minOf(offset + chunkSize, rpcJson.length)
                    val chunk = rpcJson.substring(offset, end)

                    data.writeInterfaceToken(stub.name)
                    data.writeString(chunk)
                    binder.transact(TRANSACTION_SEND_CHUNK, data, reply, 0)
                    runCatching {
                        reply.readException()
                    }.getOrElse {
                        Log.e("DreamJsonChunkBinder", "transact error in $rpc", it)
                        return@asInterface null
                    }

                    data.setDataPosition(0)
                    reply.setDataPosition(0)

                    offset = end
                }

                // ---- 发送 FINISH，获取 response ----
                data.writeInterfaceToken(stub.name)
                binder.transact(TRANSACTION_SEND_FINISH, data, reply, 0)
                runCatching {
                    reply.readException()
                }.getOrElse {
                    Log.e("DreamJsonChunkBinder", "transact error in $rpc", it)
                    return@asInterface null
                }

                onTransact(data, reply, rpc, callable)
            }

        fun <T : IDreamIPC> asInterface(binder: IBinder,stub: KClass<T>, chunkSize: Int = 32 * 1024): T =
            asInterface(binder, stub,chunkSize = chunkSize) { data, reply, rpc, callable ->
                // ---- 读取返回数据（多分片）----
                val resultStr = buildString {
                    while (true) {
                        val piece = reply.readString() ?: break

                        if (piece == TRANSACTION_RETURN_FINISH) break

                        append(piece)
                    }
                }
                if (resultStr.isEmpty()) return@asInterface null

                // --- decode ---
                val elem = runCatching {
                    json.decodeFromString(JsonElement.serializer(), resultStr)
                }.getOrElse {
                    Log.e("DreamJsonChunkBinder", "transact error in $rpc", it)
                    return@asInterface null
                }
                runCatching {
                    json.decodeFromJsonElement(
                        json.serializersModule.serializer(callable.returnType), elem
                    )
                }.getOrElse { error ->
                    runCatching {
                        Log.e("DreamJsonChunkBinder","serverSideError in $rpc", json.decodeFromJsonElement<DreamIPCThrowable>(elem).toThrowable())
                    }.onFailure { Log.e("DreamJsonChunkBinder","clientError in $rpc",error) }
                    null
                }
            }
    }
}

