package org.github.dreammooncai.kni

import io.github.oshai.kotlinlogging.Level

expect object KniLogger {

    /**
     * 设置日志等级
     *
     * 默认: null 表示关闭并清空缓存
     *
     * @param level 日志等级
     */
    fun setLogLevel(level: Level?)

    /**
     * 获取日志行并清空日志
     *
     * @return 日志
     */
    fun getLogs(): List<String>
}