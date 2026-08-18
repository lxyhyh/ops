package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

private const val MAX_READ_BYTES = 256 * 1024
private const val MAX_WAIT_SECONDS = 30L
private const val DESTROY_WAIT_SECONDS = 10L

internal suspend fun executeProcess(process: Process): ShellResult = withContext(Dispatchers.IO) {
    try {
        coroutineScope {
            val stdoutDeferred = async { readStream(process.inputStream) }
            val stderrDeferred = async { readStream(process.errorStream) }

            val finished = process.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS)
            val exitCode: Int
            if (!finished) {
                process.destroyForcibly()
                process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS)
                exitCode = -1
            } else {
                exitCode = process.exitValue()
            }

            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()

            ShellResult(stdout, stderr, exitCode)
        }
    } finally {
        process.destroy()
    }
}

private fun readStream(stream: InputStream): String {
    val reader = BufferedReader(InputStreamReader(LimitedInputStream(stream, MAX_READ_BYTES)))
    return try {
        reader.readText()
    } catch (_: Exception) {
        ""
    } finally {
        try {
            reader.close()
        } catch (_: Exception) {
        }
    }
}

private class LimitedInputStream(
    private val source: InputStream,
    private val maxBytes: Int
) : FilterInputStream(source) {

    private var readBytes = 0

    override fun read(): Int {
        if (readBytes >= maxBytes) return -1
        val b = super.read()
        if (b != -1) readBytes++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val allowed = (maxBytes - readBytes).coerceAtLeast(0)
        if (allowed <= 0) return -1
        val n = super.read(b, off, minOf(len, allowed))
        if (n > 0) readBytes += n
        return n
    }
}
