package com.smartmove.androidfilelogger

import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.utils.IOUtils
import timber.log.Timber
import java.io.OutputStream
import java.io.InputStream
import java.io.File
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.FileInputStream

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
    var archiveStream: OutputStream? = null
    var archive: ArchiveOutputStream? = null
    var input: BufferedInputStream? = null
    try {
        archiveStream = FileOutputStream(zipFile)
        archive = ArchiveStreamFactory().createArchiveOutputStream(
            ArchiveStreamFactory.ZIP,
            archiveStream
        )
        for (file in inputFiles) {
            val entryName = file.name
            val entry = ZipArchiveEntry(entryName)
            archive!!.putArchiveEntry(entry)
            input = BufferedInputStream(FileInputStream(file))

            IOUtils.copy(input, archive)
            input.close()
            archive.closeArchiveEntry()
        }

        archive!!.finish()
        archiveStream.close()
    } catch (e: Exception) {
        Timber.e(e, "Error zipping files")
    } finally {
        tryToCloseInputStream(input)
        tryToCloseOutputStream(archiveStream)
        tryToCloseOutputStream(archive)
    }
}
