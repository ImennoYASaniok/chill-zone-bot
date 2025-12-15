package core.utils

import java.time.LocalDateTime

object Logger {
    private val logFormat = "[%s] [%s] %s"

    fun info(tag: String, message: String) {
        val timestamp = LocalDateTime.now()
        println(logFormat.format(timestamp, tag, message))
    }

    fun error(tag: String, message: String, e: Exception? = null) {
        val timestamp = LocalDateTime.now()
        System.err.println(logFormat.format(timestamp, tag, message))
        e?.printStackTrace()
    }
}