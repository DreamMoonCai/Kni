@file:Suppress("UNCHECKED_CAST")
package org.github.dreammooncai.ipc

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import io.github.dreammooncai.yukireflection.factory.classLoader
import io.github.dreammooncai.yukireflection.factory.kotlinFunctionDefault
import io.github.dreammooncai.yukireflection.factory.name
import kotlinx.serialization.json.*
import org.github.dreammooncai.kni.json
import java.lang.reflect.Proxy
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class DreamBaseJsonBinder<T : IDreamIPC>(
    private val impl: T,
    stub: KClass<T>
) : Binder(), IInterface {

    protected val descriptor: String = stub.name
    private val requestBuffer: StringBuilder = StringBuilder()

    init { attachInterface(this, descriptor) }

    override fun asBinder(): IBinder = this

    protected open fun readChunk(data: Parcel): String? = data.readString()

    protected open fun writeResult(reply: Parcel, chunk: String) = reply.writeString(chunk)

    private fun processRpc(requestStr: String): String = runCatching {
        val rpc = json.decodeFromString(DreamRpcCall.serializer(), requestStr)
        json.encodeToString(JsonElement.serializer(), impl.callInstanceJson(rpc))
    }.getOrElse { json.encodeToString(DreamIPCThrowable.serializer(),DreamIPCThrowable(it)) }

    // 统一的 RPC 调用逻辑
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        data.enforceInterface(descriptor)

        return when(code) {
            TRANSACTION_SEND_CHUNK -> {
                val piece = readChunk(data) ?: return false
                requestBuffer.append(piece)
                reply?.writeNoException()
                true
            }
            TRANSACTION_SEND_FINISH -> {
                val requestStr = requestBuffer.toString()
                requestBuffer.clear()

                // ……这里走你的 RPC 解析逻辑……
                val resultJsonStr = processRpc(requestStr)

                reply?.writeNoException()
                // 把 resultJsonStr 分片写回客户端
                writeResult(reply ?: return true, resultJsonStr)
                true
            }
            else -> super.onTransact(code, data, reply, flags)
        }
    }

    companion object {
        const val TRANSACTION_SEND_CHUNK = 3119

        const val TRANSACTION_SEND_FINISH = 3120

        const val TRANSACTION_RETURN_FINISH = "#END#"

        fun <T : IDreamIPC> asInterface(stub: KClass<T>, onTransact: (data: Parcel, reply: Parcel, rpc: DreamRpcCall, callable: KCallable<*>) -> Any?): T {
            return Proxy.newProxyInstance(
                stub.classLoader, arrayOf(stub.java)
            ) { _, method, args ->
                val function = method.kotlinFunctionDefault ?: throw IllegalArgumentException("Not a function in $method")

                val rpc = DreamRpcCall.create(function,args ?: arrayOf())

                val data = Parcel.obtain()
                val reply = Parcel.obtain()

                try {
                    onTransact(data, reply,rpc,when(function) {
                        is KProperty.Accessor<*> -> function.property
                        else -> function
                    })
                } finally {
                    data.recycle(); reply.recycle()
                }
            } as T
        }
    }
}