package org.github.dreammooncai.kni

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker
import kotlin.String
import kotlin.Unit
import kotlin.apply
import kotlin.io.println
import kotlin.let
import kotlin.run
import kotlin.stackTraceToString
import kotlin.text.buildString

var logger = KotlinLogging.logger("Kni")
    private set

fun applyPrintKniLogger(name: String = "Kni") {
    logger += object : KLogger by logger {
        override val name: String = name

        override fun at(level: Level, marker: Marker?, block: KLoggingEventBuilder.() -> Unit) {
            if (isLoggingEnabledFor(level, marker)) {
                KLoggingEventBuilder().apply(block).run {
                    val formattedMessage: String = buildString {
                        marker?.getName()?.let {
                            append(it)
                            append(" ")
                        }
                        append(message)
                        cause?.stackTraceToString()?.let {
                            append('\n')
                            append(it)
                        }
                    }
                    println("${level.name} $name - $formattedMessage")
                }
            }
        }
    }
}

operator fun KLogger.plusAssign(other: KLogger) {
    val oldField = logger
    logger = object : KLogger {
        override val name: String
            get() = "代理日志"

        override fun at(
            level: Level,
            marker: Marker?,
            block: KLoggingEventBuilder.() -> Unit
        ) {
            oldField.at(level, marker, block)
            other.at(level, marker, block)
        }

        override fun isLoggingEnabledFor(
            level: Level,
            marker: Marker?
        ): Boolean = true
    }
}
fun <R> notImplemented(): R = throw NotImplementedError("An operation is not implemented: ")

fun notImplementedUnit(): Unit = throw NotImplementedError("An operation is not implemented: ")