package com.smartmove.androidfilelogger

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("LogNotTimber")
class FileLogTree(baseDir: File, private val debug: Boolean) : Timber.DebugTree(), LogManagerInterface {
    private val logDir: File?
    private var currentLogFile: File? = null

    companion object {
        private const val FILE_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS"
        private const val LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
        private const val LOG_DIR_NAME = "logs"
        private const val LOG_PREFIX = "log"
        private const val LOG_EXT = ".log"
        private const val FILE_MAX_LENGTH: Long = 1000000
        private const val TOTAL_MAX_LENGTH: Long = 5000000
    }

    init {
        val logDir = File(baseDir.absolutePath + File.separator + LOG_DIR_NAME)
        if (!logDir.exists()) {
            logDir.mkdir()
        }
        this.logDir = logDir
        checkCurrentFileReady()
    }

    /**
     * Log to debug and file streams
     * @param priority
     * @param tag
     * @param message
     * @param t
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (debug) {
            super.log(priority, tag, message, t)
        }
        if (logDir == null || !logDir.exists()) {
            // Not much we can do about this
            Log.e(FileLogTree::class.java.simpleName, "File logging directory doesn't exist")
            return
        }
        if (checkCurrentFileReady()) {
            var fileOutputStream: OutputStream? = null
            try {
                val logDateFormat = SimpleDateFormat(LOG_DATE_FORMAT, Locale.UK)
                fileOutputStream = FileOutputStream(currentLogFile!!, true)
                val sb = StringBuilder()
                sb.append("[").append(logDateFormat.format(Date())).append("] ")
                sb.append(tag).append("|")
                sb.append(message)
                if (t != null) {
                    sb.append(": ")
                    sb.append(t.toString())
                    sb.append("\r\n")
                    sb.append(t.stackTrace)
                }
                sb.append("\r\n")
                fileOutputStream.write(sb.toString().toByteArray())
                fileOutputStream.flush()
            } catch (e: IOException) {
                Log.e(FileLogTree::class.java.simpleName, "Could not write to file!", e)
            } finally {
                tryToCloseOutputStream(fileOutputStream)
            }
        }
    }

    /**
     * Tries to close a stream
     * @param stream
     */
    private fun tryToCloseOutputStream(stream: OutputStream?) {
        try {
            stream?.close()
        } catch (e: Exception) {
            Log.e(FileLogTree::class.java.simpleName, "Could not close stream!", e)
        }

    }

    private fun checkCurrentFileReady(): Boolean {
        if (currentLogFile == null) {
            // Current log file not defined
            Log.d(FileLogTree::class.java.simpleName, "No current log file")
            val latestLogFile = getLatestLogFile()
            if (latestLogFile != null) {
                Log.d(
                    FileLogTree::class.java.simpleName,
                    "Existing log file found under max size: " + latestLogFile.name
                )
                currentLogFile = latestLogFile
                return true
            } else {
                val logFile = createNewFile()
                if (logFile != null) {
                    Log.d(
                        FileLogTree::class.java.simpleName,
                        "No usable existing log file found, created new log file: " + logFile.name
                    )
                    currentLogFile = logFile
                    return true
                }
            }
            // Couldn't find or create a log file
            Log.e(FileLogTree::class.java.simpleName, "Could not create new log file!")
            return false
        } else if (currentLogFile!!.length() > FILE_MAX_LENGTH) {
            // Current log file has reached max size. Try to roll over
            Log.d(FileLogTree::class.java.simpleName, "Current files exceeds max length")
            val logFile = createNewFile()
            if (logFile != null) {
                Log.d(
                    FileLogTree::class.java.simpleName,
                    "Rolled over to new log file: " + logFile.name
                )
                currentLogFile = logFile
                cleanupAndReturnFileList()
            } else {
                Log.w(FileLogTree::class.java.simpleName, "Could not create new file for rollover!")
            }
            // If we couldn't create a new file, must keep using the old one as a fallback
        }
        // Current file is defined
        return true
    }

    private fun cleanupAndReturnFileList(): List<File>? {
        val fileList = getLogFiles()
        if (fileList.isNotEmpty()) {
            var total: Long = 0
            for (file in fileList) {
                total += file.length()
            }
            if (total > TOTAL_MAX_LENGTH) {
                val toDelete = fileList[0]
                Log.d(
                    FileLogTree::class.java.simpleName,
                    "Max allowable log directory size exceeded. Deleting oldest file: " + toDelete.name
                )
                if (toDelete.delete()) {
                    return cleanupAndReturnFileList()
                } else {
                    // There was an error deleting the file, no choice but to bail out
                    Log.w(FileLogTree::class.java.simpleName, "Failed to delete file")
                }
            }
        }
        return fileList
    }


    private fun getLatestLogFile(): File? {
        val currentFiles = cleanupAndReturnFileList()
        if (currentFiles != null && currentFiles.isNotEmpty()) {
            val latest = currentFiles[currentFiles.size - 1]
            return if (latest.length() >= FILE_MAX_LENGTH) null else latest
        }
        return null
    }

    private fun createNewFile(): File? {
        val fileDateFormat = SimpleDateFormat(FILE_DATE_FORMAT, Locale.UK)
        val fileName = LOG_PREFIX + fileDateFormat.format(Date()) + LOG_EXT
        val newFile = File(logDir!!.absolutePath + File.separator + fileName)
        try {
            newFile.createNewFile()
            if (newFile.exists()) {
                return newFile
            } else {
                Log.e(
                    FileLogTree::class.java.simpleName,
                    "Newly created file doesn't exist: " + newFile.name
                )
            }
        } catch (e: IOException) {
            Log.e(FileLogTree::class.java.simpleName, "Could not create new log file!", e)
        }

        return null
    }

    override fun getLogFiles(): List<File> {
        val currentFiles = logDir!!.listFiles()
        var fileList: MutableList<File> = ArrayList()
        if (currentFiles != null && currentFiles.isNotEmpty()) {
            // Make sure the list isn't fixed size
            fileList = mutableListOf(*currentFiles)
            val removeList = ArrayList<File>()
            for (file in fileList) {
                if (!file.name.startsWith(LOG_PREFIX)) {
                    removeList.add(file)
                }
            }
            fileList.removeAll(removeList)
            Log.d(
                FileLogTree::class.java.simpleName,
                "Found " + fileList.size + " existing log files"
            )
            // Sort by name (effectively by date)
            fileList.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name) })
        }
        return fileList
    }

    override fun getLogFileNames(): Array<String>? {
        val logPath = getLogDir()
        // Get a list of the current log files available
        val file = File(logPath!!)
        val filter = FilenameFilter { _, filename -> filename.startsWith(LOG_PREFIX) }
        return file.list(filter)
    }

    override fun getLogDir(): String? {
        return logDir?.absolutePath
    }
}