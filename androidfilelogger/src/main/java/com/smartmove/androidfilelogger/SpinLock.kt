package com.smartmove.androidfilelogger

import android.net.LocalServerSocket
import java.io.IOException

/**
 * Cross-process socket lock
 * Inspired by https://stackoverflow.com/a/29361462
 */
class SpinLock(private val name: String) {

    private var server: LocalServerSocket? = null

    @Synchronized
    @Throws(IOException::class)
    fun tryLock() {
        server = if (server == null) {
            LocalServerSocket(name)
        } else {
            throw IllegalStateException("tryLock but has locked")
        }
    }

    @Synchronized
    fun timedLock(ms: Int): Boolean {
        val expiredTime = System.currentTimeMillis() + ms
        while (true) {
            if (System.currentTimeMillis() > expiredTime) {
                return false
            }
            try {
                try {
                    tryLock()
                    return true
                } catch (e: IOException) { // ignore the exception
                }
                Thread.sleep(10, 0)
            } catch (e: InterruptedException) {
                continue
            }
        }
    }

    @Synchronized
    fun lock() {
        while (true) {
            try {
                try {
                    tryLock()
                    return
                } catch (e: IOException) { // ignore the exception
                }
                Thread.sleep(10, 0)
            } catch (e: InterruptedException) {
                continue
            }
        }
    }

    @Synchronized
fun release() {
        try {
            server?.close()
            server = null
        } catch (e: IOException) { // ignore the exception
        }
    }
}
