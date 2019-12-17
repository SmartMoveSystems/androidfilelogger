package com.smartmove.androidfilelogger

/**
 * Informs crash logger whether a crash log has been successfully handled/reported
 */
interface CrashLogHandledCallback {
    fun crashLogHandled(success: Boolean)
}
