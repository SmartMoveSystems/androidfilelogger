package com.smartmove.logsenderexample

import android.app.Application
import com.smartmove.androidfilelogger.*
import timber.log.Timber

class LogSenderApp : Application() {

    private lateinit var logManager: LogManagerInterface
    private lateinit var apiConfig: ApiConfig

    companion object {
        var instance: LogSenderApp? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val tree = FileLogTree(filesDir, BuildConfig.DEBUG)
        Timber.plant(tree)
        logManager = tree
        apiConfig =  ApiConfig(
            getString(R.string.crash_log_upload_url),
            "Logs Uploaded",
            getString(R.string.type)
        )
        val crashLogger = CrashLogger(
            this,
            tree,
            getString(R.string.crash_log_file),
            apiConfig
        )
        crashLogger.prepareCrashLog()
    }

    fun sendLogs() {
        val sender = LogSender(apiConfig, logManager)
        sender.sendLogs("This is a test", object : LogSenderCallback {
            override fun onSuccess() {
                Timber.i("Congrats, your logs were sent")
            }

            override fun onFailure() {
                Timber.e("Oops, looks like something went wrong")
            }
        })
    }
}