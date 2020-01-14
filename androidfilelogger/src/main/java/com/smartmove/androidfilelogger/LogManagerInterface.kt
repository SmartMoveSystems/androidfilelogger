package com.smartmove.androidfilelogger

import java.io.File

/**
 * Interface for retrieving log files
 */
interface LogManagerInterface {
    fun getLogFiles(): List<File>

    fun getLogFileNames(): Array<String>?

    fun getLogDir(): String?

    var logLevel: Int
}
