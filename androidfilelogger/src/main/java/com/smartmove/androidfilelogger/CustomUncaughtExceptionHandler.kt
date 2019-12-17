package com.smartmove.androidfilelogger

import timber.log.Timber

class CustomUncaughtExceptionHandler(
    private val crashLogger: CrashLogger,
    private val nextHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val message = "uncaughtException in thread [$thread]"
        Timber.e(ex, message)

        crashLogger.logCrash(ex)

        nextHandler?.uncaughtException(thread, ex)
    }
}
