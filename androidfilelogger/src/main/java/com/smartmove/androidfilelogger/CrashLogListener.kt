package com.smartmove.androidfilelogger

/**
 * Called when a crash log is found
 */
interface CrashLogListener {
    fun handleCrashLog(content: String, callback: CrashLogHandledCallback)
}
