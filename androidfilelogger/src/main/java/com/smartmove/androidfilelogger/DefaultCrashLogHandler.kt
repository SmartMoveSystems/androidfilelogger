package com.smartmove.androidfilelogger

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
            logSender.sendLogs(content, object : LogSenderCallback {
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
            })
        }
    }
}
