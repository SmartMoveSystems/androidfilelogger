package com.smartmove.androidfilelogger

import com.smartmove.androidfilelogger.LogSender.Companion.ALL_FILES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DefaultCrashLogHandler(
    apiConfig: ApiConfig,
    logManager: LogManagerInterface?
) : CrashLogListener {

    var crashLogger: CrashLogger ? = null
    private val logSender = LogSender(apiConfig, logManager)

    override fun handleCrashLog(content: String, callback: CrashLogHandledCallback) {
        GlobalScope.launch(Dispatchers.IO) {
            logSender.sendLogs(object : LogSenderCallback {
                override fun onSuccess() {
                    GlobalScope.launch(Dispatchers.Main) {
                        callback.crashLogHandled(true)
                    }
                }

                override fun onFailure() {
                    GlobalScope.launch(Dispatchers.Main) {
                        callback.crashLogHandled(false)
                    }
                }
            }, ALL_FILES, mapOf("body" to content))
        }
    }
}
