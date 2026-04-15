package org.github.dreammooncai.ipc

import android.os.Build
import android.os.IBinder
import android.util.Log
import io.github.dreammooncai.yukireflection.factory.isInterface
import io.github.dreammooncai.yukireflection.factory.kotlinFunctionDefault
import io.github.dreammooncai.yukireflection.factory.toKClass
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import org.github.dreammooncai.kni.json
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

object DreamSmartIPC {
    /**
     * IPC 服务端 用于实际执行逻辑的跨进程通信接口
     *
     * @param impl 执行时所使用的实现类实例
     * @param stub 实例统一接口需与客户端一致，用于标识，不一样将抛出错误
     * @return IBinder 跨进程通信接口
     */
    fun <T : IDreamIPC> asServer(impl: T, stub: KClass<T>, isChunk: Boolean = true): IBinder = if (!isChunk)
        DreamJsonBinder(impl, stub)
    else if (Build.VERSION.SDK_INT >= 27)
        DreamSharedMemoryBinder(impl, stub)
    else
        DreamJsonChunkBinder(impl, stub)

    /**
     * IPC 客户端 提供用于获取执行服务端能力的客户端接口
     *
     * @param binder 服务端提供的跨进程通信接口
     * @param stub 统一接口需与服务端一致，不一样将抛出错误，用于代理生成必须是接口
     * @return 服务端提供的接口
     */
    fun <T : IDreamIPC> asClient(
        binder: IBinder,
        stub: KClass<T>,
        isChunk: Boolean = true
    ): T = if (!isChunk)
        DreamJsonBinder.asInterface(binder, stub)
    else if (Build.VERSION.SDK_INT >= 27)
        DreamSharedMemoryBinder.asInterface(binder, stub)
    else
        DreamJsonChunkBinder.asInterface(binder, stub)

    /**
     * IPC 客户端 提供用于获取执行服务端能力的客户端接口
     *
     * 此方法将自动代理所有参数与返回值，以及内部返回值，只有无法序列化并且类型有实现接口或就是接口时才会进行代理
     *
     * @param binder 服务端提供的跨进程通信接口
     * @param stub 统一接口需与服务端一致，不一样将抛出错误，用于代理生成必须是接口
     * @return 服务端提供的接口
     */
    fun <T : IDreamIPC> asClientAutoProxy(
        binder: IBinder,
        stub: KClass<T>,
        isChunk: Boolean = true
    ): T = autoProxy(asClient(binder, stub,isChunk))

    /**
     * IPC 客户端自动代理实现
     *
     * 此方法将自动代理所有参数与返回值，以及内部返回值，只有无法序列化并且类型有实现接口或就是接口时才会进行代理
     *
     * @param ipc 服务端提供的接口
     * @return 服务端提供的接口
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : IDreamIPC> autoProxy(
        ipc: T
    ): T {
        val kClass = ipc::class.java
        val interfaces = kClass.interfaces.ifEmpty { if (kClass.isInterface) arrayOf(kClass) else arrayOf() }
        return Proxy.newProxyInstance(
            kClass.classLoader,
            arrayOf(IDreamIPC.IInstance::class.java, *interfaces)
        ) { _, method, args ->
            if (method.name == "getIpcInstanceId") return@newProxyInstance null
            val args = argsConversion(args)
            val function =
                method.kotlinFunctionDefault ?: return@newProxyInstance method.invoke(ipc, *(args ?: emptyArray()))
            callProxy(ipc, function, args, null) {
                val result = ipc.callInstanceJson(DreamRpcCall.create(function, args ?: emptyArray()), null)
                if (result == JsonNull)
                    null
                else
                    runCatching {
                        json.decodeFromJsonElement(
                            json.serializersModule.serializer(function.returnType), result
                        )
                    }.getOrElse { error ->
                        runCatching {
                            Log.e("DreamSmartIPC","serverSideError in $function", json.decodeFromJsonElement<DreamIPCThrowable>(result).toThrowable())
                        }.onFailure { Log.e("DreamSmartIPC","clientError in $function",error) }
                        null
                    }
            }
        } as T
    }

    private fun argsConversion(args: Array<Any?>?) =
        args?.map { if (it is IDreamIPC.IInstance) it.toInstance() else it }?.toTypedArray()

    private fun callProxy(
        ipc: IDreamIPC,
        function: KFunction<*>,
        args: Array<Any?>?,
        id: Long?,
        call: () -> Any?
    ): Any? {
        val serializer = json.serializersModule.serializerOrNull(function.returnType)
        if (serializer == null || serializer is PolymorphicSerializer) {
            val data = ipc.callInstanceId(DreamRpcCall.create(function, args ?: emptyArray()), id) ?: return null
            val kClass = data.first.toKClass(ipc::class.java.classLoader)
            val id = data.second
            val interfaces =
                kClass.java.interfaces.ifEmpty { if (kClass.isInterface) arrayOf(kClass.java) else arrayOf() }
            if (interfaces.isEmpty()) {
                error("这是一个无法序列化的类，也无法进行代理，请手动使用 callInstanceId 获取方法结果ID 再使用ID执行这个结果实例，或将返回类型更换一个为可代理的接口")
            }
            return Proxy.newProxyInstance(
                ipc::class.java.classLoader,
                arrayOf(IDreamIPC.IInstance::class.java, *interfaces)
            ) { _, method, args ->
                if (method.name == "getIpcInstanceId") return@newProxyInstance id
                val function = method.kotlinFunctionDefault ?: return@newProxyInstance null
                val args = argsConversion(args)
                callProxy(ipc, function, args, id) {
                    val result = ipc.callInstanceJson(DreamRpcCall.create(function, args ?: emptyArray()), id)
                    if (result == JsonNull)
                        null
                    else
                        runCatching {
                            json.decodeFromJsonElement(
                                json.serializersModule.serializer(function.returnType), result
                            )
                        }.getOrElse { error ->
                            runCatching {
                                Log.e("DreamSmartIPC","serverSideError in $function", json.decodeFromJsonElement<DreamIPCThrowable>(result).toThrowable())
                            }.onFailure { Log.e("DreamSmartIPC","clientError in $function",error) }
                            null
                        }
                }
            }
        }
        return call()
    }
}