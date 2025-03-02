package com.jay.localtrace.utils

import android.util.Log

interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

object TraceLogger {
    private var logger: Logger? = null

    // 设置自定义 Logger
    fun setLogger(customLogger: Logger) {
        logger = customLogger
    }

    // 恢复默认的 Android Log
    fun resetLogger() {
        logger = null
    }

    fun d(tag: String, message: String) {
        logger?.d(tag, message) ?: Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        logger?.i(tag, message) ?: Log.i(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logger?.e(tag, message, throwable) ?: Log.e(tag, message, throwable)
    }
}
