package com.smartmove.androidfilelogger

import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Date

class LogSender(
    private val apiConfig: ApiConfig,
    private val logManager: LogManagerInterface?
) {
    private val endpoint: LogEndpoint = ApiServiceGenerator().createService(LogEndpoint::class.java)

    fun sendLogs(body: String, callback: LogSenderCallback) {
        logManager?.let {
            var zipFile: File? = null
            try {
                try {
                    val logFiles = it.getLogFiles()
                    val logDir = it.getLogDir()

                    if (logDir != null && logFiles.isNotEmpty()) {
                        zipFile = File(logDir + File.separator + Date())
                        if (zipFile.createNewFile()) {
                            addToZip(logFiles, zipFile)
                        } else {
                            Timber.e("Failed to create zip file")
                            zipFile = null
                        }
                    }
                } catch (e: Exception) {
                    zipFile = null
                    Timber.e(e, "Error generating zipped logs")
                }

                val subject = RequestBody.create(MediaType.parse("text/plain"), apiConfig.subject)
                val message = RequestBody.create(MediaType.parse("text/plain"), body)
                val type = RequestBody.create(MediaType.parse("text/plain"), apiConfig.type)
                val logs = zipFile?.let { RequestBody.create(MediaType.parse("image/jpeg"), zipFile) }
                endpoint.sendLogs(apiConfig.url, subject, message, type, logs).enqueue(object : retrofit2.Callback<Void> {
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Timber.e(t, "Failed to send log")
                        callback.onFailure()
                    }

                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            callback.onSuccess()
                        } else {
                            callback.onFailure()
                            Timber.e("Failed to send log: received response ${response.code()}")
                        }
                    }
                })
            } catch (ex: IOException) {
                Timber.e(ex, "Error sending crash report")
                callback.onFailure()
            } finally {
                try {
                    zipFile?.delete()
                } catch (ex: Exception) {
                    Timber.e(ex, "Error closing crash log file after reading.")
                }
            }
        } ?: callback.onFailure()
    }
}
