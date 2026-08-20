package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root 命令执行器（保留原实现：libsu 需 JitPack 仓库且当前网络环境不可用）。
 *
 * 每条命令通过 ProcessBuilder 启动 su 进程执行，executeProcess 提供超时与流读取。
 */
@Singleton
class RootCommandExecutor @Inject constructor() : CommandExecutor {

    override suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(false)
            .start()
        executeProcess(process)
    }

    override suspend fun isAvailable(): Boolean {
        // 与原版一致：执行 id 并检查 uid=0
        val result = try {
            execute("id")
        } catch (_: Exception) {
            return false
        }
        return result.exitCode == 0 && result.stdout.contains("uid=0")
    }
}