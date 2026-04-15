package org.github.dreammooncai.kni

import io.github.oshai.kotlinlogging.Level

actual object KniLogger {
    init {
        KniLoader.loader()
    }
    actual external fun setLogLevel(level: Level?)

    actual external fun getLogs(): List<String>
}