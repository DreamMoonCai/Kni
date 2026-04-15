@file:Suppress("UNCHECKED_CAST")

package org.github.dreammooncai.ipc

import android.os.Build
import android.os.Parcel
import android.os.IBinder
import android.os.SharedMemory
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.github.dreammooncai.kni.json
import kotlin.reflect.KClass

@RequiresApi(Build.VERSION_CODES.O_MR1)
class DreamSharedMemoryBinder<T : IDreamIPC>(
    impl: T,
    stub: KClass<T>,
    private val chunkSize: Int = 32 * 1024
) : DreamBaseJsonBinder<T>(impl, stub) {
    override fun writeResult(reply: Parcel, chunk: String) {
        val bytes = chunk.encodeToByteArray()
        if (bytes.size < chunkSize) {
            reply.writeString(chunk)
            return
        }
        // -------- 大数据 → SharedMemory --------
        val shm = SharedMemory.create("ipc_shared_json", bytes.size)
        val buffer = shm.mapReadWrite()

        buffer.put(bytes)
        SharedMemory.unmap(buffer)

        reply.writeString(FLAG_SHARED)
        reply.writeParcelable(shm, 0)
    }

    companion object {
        private const val FLAG_SHARED = "SHM"

        fun <T : IDreamIPC> asInterface(
            binder: IBinder,
            stub: KClass<T>,
            chunkSize: Int = 32 * 1024
        ): T = DreamJsonChunkBinder.asInterface(
            binder, stub,chunkSize = chunkSize
        ) { data, reply, rpc, callable ->

            // 小数据 or SharedMemory？
            val flagOrString = reply.readString() ?: return@asInterface null

            if (flagOrString == FLAG_SHARED) {
                // ---- 共享内存模式 ----

                @Suppress("DEPRECATION")
                val shm = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) reply.readParcelable(
                    SharedMemory::class.java.classLoader,
                    SharedMemory::class.java
                ) else reply.readParcelable<SharedMemory>(
                    SharedMemory::class.java.classLoader
                )) ?: return@asInterface null

                val buffer = shm.mapReadOnly()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                SharedMemory.unmap(buffer)

                val jsonString = bytes.decodeToString()
                val elem = runCatching {
                    json.decodeFromString(JsonElement.serializer(), jsonString)
                }.getOrElse {
                    Log.e("DreamSharedMemoryBinder", "transact error in $rpc", it)
                    return@asInterface null
                }
                return@asInterface runCatching {
                    json.decodeFromJsonElement(
                        json.serializersModule.serializer(callable.returnType), elem
                    )
                }.getOrElse { error ->
                    runCatching {
                        Log.e("DreamSharedMemoryBinder","serverSideError", json.decodeFromJsonElement<DreamIPCThrowable>(elem).toThrowable())
                    }.onFailure { Log.e("DreamSharedMemoryBinder","clientError",error) }
                    null
                }
            }

            // ---- 小数据模式 ----

            val elem = runCatching {
                json.decodeFromString(JsonElement.serializer(), flagOrString)
            }.getOrElse {
                Log.e("DreamSharedMemoryBinder", "transact error in $rpc", it)
                return@asInterface null
            }
            runCatching {
                json.decodeFromJsonElement(
                    json.serializersModule.serializer(callable.returnType), elem
                )
            }.getOrElse { error ->
                runCatching {
                    Log.e("DreamSharedMemoryBinder","serverSideError in $rpc", json.decodeFromJsonElement<DreamIPCThrowable>(elem).toThrowable())
                }.onFailure { Log.e("DreamSharedMemoryBinder","clientError in $rpc",error) }
                null
            }
        }
    }


}

