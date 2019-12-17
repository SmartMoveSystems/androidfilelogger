package com.smartmove.androidfilelogger

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Detects crashes and writes stack traces to file for later extraction
 */
class CrashLogger(context: Context, fileName: String, private val listener: CrashLogListener?) {

    constructor(
        context: Context,
        logManager: LogManagerInterface,
        fileName: String,
        apiConfig: ApiConfig
    ) : this(context, fileName, DefaultCrashLogHandler(apiConfig, logManager)) {
        (listener as DefaultCrashLogHandler).crashLogger = this
    }

    private val crashLogFile: File = context.getFileStreamPath(fileName)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ zzzz")

    private val handledCallback = object : CrashLogHandledCallback {
        override fun crashLogHandled(success: Boolean) {
            if (success) {
                deleteCrashLog()
            } else {
                Timber.e("Failed to handle crash log")
            }
        }
    }

    init {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val customHandler = CustomUncaughtExceptionHandler(this, defaultHandler)
        Thread.setDefaultUncaughtExceptionHandler(customHandler)
    }

    fun crashLogExists(): Boolean {
        return crashLogFile.exists()
    }

    fun deleteCrashLog() {
        crashLogFile.delete()
    }

    fun logCrash(ex: Throwable) {
        val crashLogOutputStream: FileOutputStream
        try {
            crashLogOutputStream = FileOutputStream(crashLogFile, true)
        } catch (ioEx: IOException) {
            Timber.e(ioEx, "Error opening crash report file")
            return
        }

        try {
            val crashLogPrintStream = PrintStream(crashLogOutputStream)

            crashLogPrintStream.println("crash_timestamp=" + dateFormat.format(Date()))

            ex.printStackTrace(crashLogPrintStream)
            crashLogOutputStream.flush()
        } catch (ioEx: IOException) {
            Timber.e(ioEx, "Error writing crash report file")
        } finally {
            try {
                crashLogOutputStream.close()
            } catch (ioEx: IOException) {
                Timber.e(ioEx, "Error closing crash report file")
            }
        }
    }

    fun prepareCrashLog() {
        if (crashLogExists()) {
            val crashLogContent = StringBuilder("crash log:\n")
            crashLogContent.append("<pre>\n")

            try {
                crashLogContent.append(crashLogFile.readText())
            } catch (ex: IOException) {
                Timber.e(ex, "Error opening crash log file for input and sending")
            }
            crashLogContent.append("\n</pre>\n")

            listener?.handleCrashLog(crashLogContent.toString(), handledCallback)
        }
    }
}
