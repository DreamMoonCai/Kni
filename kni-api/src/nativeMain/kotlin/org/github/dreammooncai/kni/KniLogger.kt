@file:OptIn(ExperimentalForeignApi::class)

package org.github.dreammooncai.kni

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import org.github.dreammooncai.kni.factory.kni
import org.github.dreammooncai.kni.factory.kniResultJava
import platform.jni.jlong
import platform.jni.jobject
import kotlin.collections.component1
import kotlin.collections.component2

private val loggerLock = reentrantLock()

actual object KniLogger:IKniRegister {
    private var kLevel: Level? = null
    private fun createLogger():MutableList<String> {
        val logs = mutableListOf<String>()
        logger += object : KLogger {
            override val name: String
                get() = "Dream-JniLog"

            override fun at(
                level: Level,
                marker: Marker?,
                block: KLoggingEventBuilder.() -> Unit
            ) {
                if (isLoggingEnabledFor(level,marker)) {
                    val log = KLoggingEventBuilder().apply(block).run {
                        buildString {
                            // Level + marker
                            level.name.let {
                                append(it)
                                append(":")
                            }

                            // 1) message
                            append(message ?: "<no message>")

                            // 2) arguments
                            arguments?.let { args ->
                                if (args.isNotEmpty()) {
                                    append("\n  arguments=")
                                    append(args.joinToString(prefix = "[", postfix = "]") { it.toStringOrDump() })
                                }
                            }

                            // 3) payload
                            payload?.let { p ->
                                if (p.isNotEmpty()) {
                                    append("\n  payload={\n")
                                    p.forEach { (k, v) ->
                                        append("    $k=")
                                        append(v.toStringOrDump())
                                        append("\n")
                                    }
                                    append("  }")
                                }
                            }

                            // 5) cause (stacktrace)
                            cause?.let { ex ->
                                append("\n  cause=\n")
                                append(ex.stackTraceToString().prependIndent("    "))
                            }
                        }
                    }
//                    SystemFileSystem.sink(Path("/Users/macbookpro/IdeaProjects/DreamMemory/composeApp/logs.txt"),true).buffered().use { it.writeString(log) }
                    loggerLock.withLock { logs += log }
                }
            }

            private fun Any?.toStringOrDump(): String =
                when (this) {
                    null -> "null"
                    is String -> "\"$this\""
                    is Throwable -> this.stackTraceToString()
                    is Map<*, *> -> this.entries.joinToString("{", "}") { (k, v) -> "$k=${v.toStringOrDump()}" }
                    is Collection<*> -> this.joinToString("[", "]") { it.toStringOrDump() }
                    is Array<*> -> this.joinToString("[", "]") { it.toStringOrDump() }
                    is IntArray -> this.joinToString("[", "]")
                    is LongArray -> this.joinToString("[", "]")
                    is DoubleArray -> this.joinToString("[", "]")
                    is FloatArray -> this.joinToString("[", "]")
                    is BooleanArray -> this.joinToString("[", "]")
                    else -> this.toString()
                }

            override fun isLoggingEnabledFor(level: Level, marker: Marker?): Boolean {
                return kLevel != null && kLevel!!.toInt() <= level.toInt()
            }
        }
        return logs
    }

    private val logs: MutableList<String> = createLogger()

    override fun KniRegister.onRegister() {
        ::setLogLevel.register(staticCFunction { _, _, level: jobject? ->
            kniResultJava { setLogLevel(level?.asEnum<Level>()) }
        })
        ::getLogs.register(staticCFunction { _, _, ->
            kniResultJava { getLogs().asJni }
        })
    }

    actual fun setLogLevel(level: Level?) {
        kLevel = level
        if (kLevel == null) loggerLock.withLock { logs.clear() }
    }

    actual fun getLogs(): List<String> = loggerLock.withLock { logs.filterNot { it == "null" }.also { logs.clear() } }
}