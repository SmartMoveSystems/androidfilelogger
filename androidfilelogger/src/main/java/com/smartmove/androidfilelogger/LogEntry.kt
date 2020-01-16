package com.smartmove.androidfilelogger

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(val priority: Int, val tag: String?, val message: String, val t: Throwable?)

fun LogEntry.print(logDateFormatString: String): String {
    val logDateFormat = SimpleDateFormat(logDateFormatString, Locale.UK)
    val sb = StringBuilder()
    sb.append("[").append(logDateFormat.format(Date())).append("] ")
    sb.append(tag).append("|")
    sb.append(getPriorityName(priority)).append("|")
    sb.append(message)
    if (t != null) {
        sb.append(": ")
        sb.append(t.toString())
        sb.append("\r\n")
        sb.append(t.stackTrace)
    }
    sb.append("\r\n")
    return sb.toString()
}

private fun getPriorityName(priority: Int): String {
    return when {
        priority >= Log.ASSERT -> "ASSERT"
        priority >= Log.ERROR -> "ERROR"
        priority >= Log.WARN -> "WARN"
        priority >= Log.INFO -> "INFO"
        priority >= Log.DEBUG -> "DEBUG"
        else -> "VERBOSE"
    }
}
