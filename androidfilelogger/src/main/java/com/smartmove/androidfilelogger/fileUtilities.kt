package com.smartmove.androidfilelogger

import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Tries to close a stream
 * @param stream
 */
fun tryToCloseOutputStream(stream: OutputStream?) {
    try {
        stream?.close()
    } catch (e: Exception) {
        Timber.e(e, "Could not close stream!")
    }
}

/**
 * Tries to close a stream
 * @param stream
 */
fun tryToCloseInputStream(stream: InputStream?) {
    try {
        stream?.close()
    } catch (e: Exception) {
        Timber.e(e, "Could not close stream!")
    }
}

fun addToZip(inputFiles: List<File>, zipFile: File) {
    var zipOutputStream: ZipOutputStream? = null
    var input: BufferedInputStream? = null
    try {
        zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))
        for (file in inputFiles) {
            val entry = ZipEntry(file.name)
            zipOutputStream.putNextEntry(entry)
            input = BufferedInputStream(FileInputStream(file))

            input.copyTo(zipOutputStream)

            input.close()
            zipOutputStream.closeEntry()
        }
    } catch (e: Exception) {
        Timber.e(e, "Error zipping files")
    } finally {
        tryToCloseInputStream(input)
        tryToCloseOutputStream(zipOutputStream)
    }
}
