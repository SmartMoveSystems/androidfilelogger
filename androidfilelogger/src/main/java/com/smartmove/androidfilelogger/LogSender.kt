package com.smartmove.androidfilelogger

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.util.Date

class LogSender(
    private val apiConfig: ApiConfig,
    private val logManager: LogManagerInterface?
) {
    private val endpoint: LogEndpoint? = try {
        ApiServiceGenerator(apiConfig.url).createService(LogEndpoint::class.java)
    } catch (e: Exception) {
        Timber.e(e, "Failed to configure log sender")
        null
    }

    @JvmOverloads
    fun sendLogs(callback: LogSenderCallback, numFiles: Int = ALL_FILES, additionalParams: Map<String, String>? = null) {
        if (logManager != null && endpoint != null) {
            var zipFile: File? = null
            try {
                try {
                    val logFiles = logManager.getLogFiles()
                    val logDir = logManager.getLogDir()

                    if (logDir != null && logFiles.isNotEmpty()) {
                        val filteredFiles = if (numFiles <= ALL_FILES) {
                            logFiles
                        } else {
                            logFiles.sortedBy { it.name }.takeLast(numFiles)
                        }
                        val zipFileName = (logDir + File.separator + Date()).replace(" ", "_")
                        zipFile = File(zipFileName)
                        if (zipFile.createNewFile()) {
                            addToZip(filteredFiles, zipFile)
                        } else {
                            Timber.e("Failed to create zip file")
                            zipFile = null
                        }
                    }
                } catch (e: Exception) {
                    zipFile = null
                    Timber.e(e, "Error generating zipped logs")
                }

                val logs = zipFile?.let {
                    val reqBody = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("file", zipFile.name, reqBody)
                }

                // Can't have a / on the end here
                val trimmedUrl = if (apiConfig.url.endsWith("/")) {
                    apiConfig.url.substring(0, apiConfig.url.length - 1)
                } else {
                    apiConfig.url
                }

                val extraParts = if (additionalParams.isNullOrEmpty() && apiConfig.stringParts.isNullOrEmpty()) {
                    null
                } else {
                    val parts = HashMap<String, RequestBody>()
                    apiConfig.stringParts?.entries?.forEach { parts[it.key] =
                        it.value.toRequestBody("text/plain".toMediaTypeOrNull())
                    }
                    additionalParams?.forEach { parts[it.key] =
                        it.value.toRequestBody("text/plain".toMediaTypeOrNull())
                    }
                    parts
                }

                val call = extraParts?.let {
                    endpoint.sendLogs(trimmedUrl, logs, extraParts)
                } ?: endpoint.sendLogs(trimmedUrl, logs)

                call.enqueue(object : retrofit2.Callback<Void> {
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Timber.e(t, "Failed to send log")
                        callback.onFailure()
                        zipFile?.delete()
                    }

                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            callback.onSuccess()
                        } else {
                            callback.onFailure()
                            Timber.e("Failed to send log: received response ${response.code()}")
                        }
                        zipFile?.delete()
                    }
                })
            } catch (ex: Exception) {
                Timber.e(ex, "Error sending crash report")
                zipFile?.delete()
                callback.onFailure()
            }
        } else {
            callback.onFailure()
        }
    }

    companion object {
        const val ALL_FILES = -1
    }
}
