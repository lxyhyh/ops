package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

private const val MAX_READ_BYTES = 4 * 1024 * 1024
private const val MAX_WAIT_SECONDS = 30L
private const val DESTROY_WAIT_SECONDS = 10L

internal suspend fun executeProcess(process: Process): ShellResult = withContext(Dispatchers.IO) {
    try {
        coroutineScope {
            val stdoutDeferred = async { readStream(process.inputStream) }
            val stderrDeferred = async { readStream(process.errorStream) }

            // runInterruptible：协程取消时中断阻塞的 waitFor，使批量取消能立即生效，
            // 不再等命令自然结束（原实现取消最多延迟 30s+10s）。
            val exitCode: Int = try {
                val finished = runInterruptible { process.waitFor(MAX_WAIT_SECONDS, TimeUnit.SECONDS) }
                if (!finished) {
                    process.destroyForcibly()
                    process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS)
                    -1
                } else {
                    process.exitValue()
                }
            } catch (e: InterruptedException) {
                // 协程取消触发的中断：立即清理进程并重新抛出取消异常
                process.destroyForcibly()
                process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS)
                throw CancellationException("进程等待被取消", e)
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
