package com.smartmove.androidfilelogger

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.FilenameFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("LogNotTimber")
open class FileLogTree(baseDir: File, private val debug: Boolean) : Timber.DebugTree(), LogManagerInterface {
    private val logDir: File?
    private var currentLogFile: File? = null
    val logLevel: Int = Log.VERBOSE

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

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= logLevel
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
            logIfDebug(Log.ERROR, "File logging directory doesn't exist")
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
                logIfDebug(Log.ERROR, "Could not write to file!", e)
            } finally {
                tryToCloseOutputStream(fileOutputStream)
            }
        }
    }

    private fun logIfDebug(level: Int, message: String, err: Throwable? = null) {
        if (debug) {
            when (level) {
                Log.DEBUG -> Log.d(FileLogTree::class.java.simpleName, message, err)
                Log.INFO -> Log.i(FileLogTree::class.java.simpleName, message, err)
                Log.WARN -> Log.w(FileLogTree::class.java.simpleName, message, err)
                Log.ERROR -> Log.e(FileLogTree::class.java.simpleName, message, err)
            }
        }
    }

    private fun checkCurrentFileReady(): Boolean {
        if (currentLogFile == null) {
            // Current log file not defined
            logIfDebug(Log.DEBUG, "No current log file")
            val latestLogFile = getLatestLogFile()
            if (latestLogFile != null) {
                logIfDebug(Log.DEBUG, "Existing log file found under max size: " + latestLogFile.name)
                currentLogFile = latestLogFile
                return true
            } else {
                val logFile = createNewFile()
                if (logFile != null) {
                    logIfDebug(Log.DEBUG, "No usable existing log file found, created new log file: " + logFile.name)
                    currentLogFile = logFile
                    return true
                }
            }
            // Couldn't find or create a log file
            logIfDebug(Log.ERROR, "Could not create new log file!")
            return false
        } else if (currentLogFile!!.length() > FILE_MAX_LENGTH) {
            fileMaxLengthExceeded()
        }
        // Current file is defined
        return true
    }

    private fun fileMaxLengthExceeded() {
        // Current log file has reached max size. Try to roll over
        logIfDebug(Log.DEBUG, "Current files exceeds max length")
        val logFile = createNewFile()
        if (logFile != null) {
            logIfDebug(Log.DEBUG, "Rolled over to new log file: " + logFile.name)
            currentLogFile = logFile
            cleanupAndReturnFileList()
        } else {
            logIfDebug(Log.WARN, "Could not create new file for rollover!")
        }
        // If we couldn't create a new file, must keep using the old one as a fallback
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
                logIfDebug(Log.DEBUG, "Max allowable log directory size exceeded. Deleting oldest file: " + toDelete.name)
                if (toDelete.delete()) {
                    return cleanupAndReturnFileList()
                } else {
                    logIfDebug(Log.WARN, "Failed to delete file")
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
                logIfDebug(Log.ERROR, "Newly created file doesn't exist: " + newFile.name)
            }
        } catch (e: IOException) {
            logIfDebug(Log.ERROR, "Could not create new log file!", e)
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
            logIfDebug(Log.DEBUG, "Found " + fileList.size + " existing log files")
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
