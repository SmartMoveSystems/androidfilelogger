package com.smartmove.androidfilelogger

data class LoggerConfig(
    val fileDateFormat: String = "yyyy-MM-dd_HH-mm-ss-SSS",
    val logDateFormat: String = "yyyy-MM-dd HH:mm:ss.SSS",
    val logDirName: String = "logs",
    val logPrefix: String = "log",
    val logExt: String = ".log",
    val fileMaxLength: Long = 1000000,
    val totalMaxLength: Long = 5000000,
    val tag: String? = null
)
