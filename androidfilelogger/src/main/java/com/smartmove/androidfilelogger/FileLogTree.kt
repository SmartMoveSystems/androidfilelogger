package com.smartmove.androidfilelogger

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FilenameFilter
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.Array
import kotlin.Boolean
import kotlin.Comparator
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Throwable
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableListOf
import kotlin.collections.sortWith

@SuppressLint("LogNotTimber")
open class FileLogTree(
    baseDir: File,
    private val debug: Boolean,
    private val config: LoggerConfig = LoggerConfig()
) : Timber.DebugTree(), LogManagerInterface {

    private val logDir: File?

    private var currentLogFile: File? = null

    override var logLevel: Int = if (debug) Log.VERBOSE else Log.INFO

    private val queue = ConcurrentLinkedQueue<LogEntry>()

    private val singleThreadContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    // Lock for use across processes in the same app to keep things synchronised
    private val lock = SpinLock(android.os.Process.myUid().toString())

    private val pid = config.tag ?: android.os.Process.myPid().toString()

    override fun createStackElementTag(element: StackTraceElement): String? {
        return "($pid) ${super.createStackElementTag(element)}"
    }

    init {
        logIfDebug(Log.INFO, "init: ${android.os.Process.myUid().toString()}")
        val logDir = File(baseDir.absolutePath + File.separator + config.logDirName)
        lock.lock()
        if (!logDir.exists()) {
            logDir.mkdir()
        }
        this.logDir = logDir
        checkCurrentFileReady()
        lock.release()
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
        queue.add(LogEntry(priority, tag, message, t))
        processQueue()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun processQueue() {
        GlobalScope.launch(singleThreadContext) {
            lock.timedLock(1000)
            while (queue.isNotEmpty()) {
                val entry = queue.poll()
                if (entry != null && checkCurrentFileReady()) {
                    var fileOutputStream: OutputStream? = null
                    try {
                        fileOutputStream = FileOutputStream(currentLogFile!!, true)
                        fileOutputStream.write(entry.print(config.logDateFormat).toByteArray())
                        fileOutputStream.flush()
                    } catch (e: IOException) {
                        logIfDebug(Log.ERROR, "Could not write to file!", e)
                    } finally {
                        tryToCloseOutputStream(fileOutputStream)
                    }
                }
            }
            lock.release()
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
        } else if (currentLogFile!!.length() > config.fileMaxLength) {
            fileMaxLengthExceeded()
        }
        // Current file is defined
        return true
    }

    private fun fileMaxLengthExceeded() {
        // Current log file has reached max size. Try to roll over
        logIfDebug(Log.DEBUG, "Current files exceeds max length")
        val latest = getLatestLogFile()
        // Check if another process in the same sandbox has already rolled over. If not, create new file
        val logFile = if (latest == null || latest.name == currentLogFile?.name) createNewFile() else latest
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
            if (total > config.totalMaxLength) {
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
            return if (latest.length() >= config.fileMaxLength) null else latest
        }
        return null
    }

    private fun createNewFile(): File? {
        val fileDateFormat = SimpleDateFormat(config.fileDateFormat, Locale.UK)
        val fileName = config.logPrefix + fileDateFormat.format(Date()) + config.logExt
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
                if (!file.name.startsWith(config.logPrefix)) {
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
        val filter = FilenameFilter { _, filename -> filename.startsWith(config.logPrefix) }
        return file.list(filter)
    }

    override fun getLogDir(): String? {
        return logDir?.absolutePath
    }
}
