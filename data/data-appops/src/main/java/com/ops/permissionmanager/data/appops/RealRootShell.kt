package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 通过 su 执行命令的真实实现。
 * 兼容 Magisk / KernelSU / SuperSU 等常见 root 环境。
 */
class RealRootShell @Inject constructor() : RootShell {

    override suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(false)
            .start()

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

    override suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        val result = execute("id")
        result.exitCode == 0 && result.stdout.contains("uid=0")
    }

    private fun readStream(stream: java.io.InputStream): String =
        BufferedReader(InputStreamReader(stream)).use { it.readText() }
}
