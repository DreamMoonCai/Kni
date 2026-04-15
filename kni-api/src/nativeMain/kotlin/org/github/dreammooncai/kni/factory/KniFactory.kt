@file:OptIn(ExperimentalForeignApi::class)

package org.github.dreammooncai.kni.factory

import org.github.dreammooncai.kni.KniBridge
import kotlinx.cinterop.ExperimentalForeignApi
import org.github.dreammooncai.kni.KniVM

@DslMarker
internal annotation class KniDslMarker

val kni get() = KniBridge()

@KniDslMarker
fun <T> err(block: KniBridge.(err: Throwable)->T): KniBridge = kni.setErrorCallback(block)
@KniDslMarker
fun <T> kni(localFrameCapacity: Int = 1024,block: KniBridge.() -> T): T = kni.internalKni(false,localFrameCapacity,block)
@KniDslMarker
fun <T> kniResultJava(localFrameCapacity: Int = 1024,block: KniBridge.() -> T): T = kni.internalKni(true,localFrameCapacity,block)
@Suppress("UnusedReceiverParameter")
@KniDslMarker
fun KniBridge.detachedKni() = KniVM.detachCurrentThread()