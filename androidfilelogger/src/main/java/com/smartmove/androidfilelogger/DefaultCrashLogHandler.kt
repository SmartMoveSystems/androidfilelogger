package com.smartmove.androidfilelogger

class DefaultCrashLogHandler(
    apiConfig: ApiConfig,
    logManager: LogManagerInterface?
) : CrashLogListener {

    var crashLogger: CrashLogger ? = null
    private val logSender = LogSender(apiConfig, logManager)

    override fun handleCrashLog(content: String, callback: CrashLogHandledCallback) {
        logSender.sendLogs(content, object : LogSenderCallback {
            override fun onSuccess() {
                callback.crashLogHandled(true)
            }

            override fun onFailure() {
                callback.crashLogHandled(false)
            }
        })
    }
}
