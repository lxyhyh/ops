package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 进程执行工具，负责把启动好的 [Process] 读到结果。
 *
 * 在 IO 调度器上运行：读取 stdout / stderr，等待进程结束（超时 30 秒），
 * 汇总成 [ShellResult]。
 */
internal suspend fun executeProcess(process: Process): ShellResult = withContext(Dispatchers.IO) {
    try {
        val stdout = readStream(process.inputStream)
        val stderr = readStream(process.errorStream)
        val finished = process.waitFor(30, TimeUnit.SECONDS)
        val exitCode = if (finished) process.exitValue() else {
            process.destroyForcibly()
            -1
        }
        ShellResult(stdout, stderr, exitCode)
    } finally {
        process.destroy()
    }
}

/** 读取输入流全部文本。 */
private fun readStream(stream: InputStream): String =
    BufferedReader(InputStreamReader(stream)).use { it.readText() }