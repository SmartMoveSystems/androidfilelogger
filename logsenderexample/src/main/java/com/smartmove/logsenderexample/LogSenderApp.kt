package com.smartmove.logsenderexample

import android.app.Application
import com.smartmove.androidfilelogger.*
import timber.log.Timber

class LogSenderApp : Application() {

    private lateinit var logManager: LogManagerInterface
    private lateinit var apiConfig: ApiConfig

    override fun onCreate() {
        super.onCreate()
        instance = this
        // This will start writing Timber logs to file
        val tree = FileLogTree(filesDir, BuildConfig.DEBUG)
        Timber.plant(tree)
        logManager = tree
        apiConfig =  ApiConfig(
            getString(R.string.crash_log_upload_url), // Must be Retrofit-compatible
            mapOf("paramOne" to "valueOne") // Optional string parameters to be sent with every request
        )
        // The below is optional; use only if you want crashes to be logged
        val crashLogger = CrashLogger(
            this,
            tree,
            getString(R.string.crash_log_file), // name of crash log file (i.e. crash.txt)
            apiConfig
        )
        // This will send logs from previous crash to your endpoint if a crash occurred on last run
        crashLogger.prepareCrashLog()
    }

    fun sendLogs() {
        val sender = LogSender(
            apiConfig,
            logManager // This is your FileLogTree instance
        )
        sender.sendLogs(object : LogSenderCallback {
            override fun onSuccess() {
                Timber.i("Congrats, your logs were sent")
            }

            override fun onFailure() {
                Timber.e("Oops, looks like something went wrong")
            }
        })

        sender.sendLogs(object : LogSenderCallback {
            override fun onSuccess() {
                Timber.i("Congrats, your logs were sent")
            }

            override fun onFailure() {
                Timber.e("Oops, looks like something went wrong")
            }
        }, LogSender.ALL_FILES, mapOf("extraPart" to "extraValue"))
    }

    companion object {
        var instance: LogSenderApp? = null
    }
}