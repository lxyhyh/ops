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
            } catch (e: CancellationException) {
                // 协程取消：runInterruptible 已中断 waitFor 并转为 CancellationException
                // （源码见 kotlinx.coroutines.InterruptibleKt，不会直接抛 InterruptedException）。
                // 强制销毁进程，避免仅靠 finally 的温和 destroy() 让挂起进程的
                // stdout/stderr reader 协程迟迟不结束（coroutineScope 会等待全部子协程）。
                process.destroyForcibly()
                process.waitFor(DESTROY_WAIT_SECONDS, TimeUnit.SECONDS)
                throw e
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
