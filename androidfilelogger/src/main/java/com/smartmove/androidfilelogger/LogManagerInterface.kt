package com.smartmove.androidfilelogger

import java.io.File

/**
 * Interface for retrieving log files
 */
interface LogManagerInterface {
    abstract fun getLogFiles(): List<File>

    abstract fun getLogFileNames(): Array<String>?

    abstract fun getLogDir(): String?
}