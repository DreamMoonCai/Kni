package org.github.dreammooncai.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.withTimeout as globalWithTimeout
import kotlinx.coroutines.withTimeoutOrNull as globalWithTimeoutOrNull
import java.util.concurrent.TimeUnit

fun <T> CoroutineScope.withTimeout(timeout: Long, block: suspend CoroutineScope.() -> T): T {
    val result = async {
        globalWithTimeout(timeout) { block() }
    }
    return try {
        result.asCompletableFuture().get(timeout + 500, TimeUnit.MILLISECONDS)
    } finally {
        result.cancel()
    }
}

fun <T> CoroutineScope.withTimeoutOrNull(timeout: Long, block: suspend CoroutineScope.() -> T): T? {
    val result = async {
        globalWithTimeoutOrNull(timeout) { block() }
    }
    return try {
        result.asCompletableFuture().get(timeout + 500, TimeUnit.MILLISECONDS)
    } catch (_: Exception) {
        null
    } finally {
        result.cancel()
    }
}