package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通过 su 命令执行的真实 Root 执行器。
 *
 * 使用 `su -c <command>` 以 root 身份执行系统命令；
 * 可用性通过执行 `id` 并检查输出含 `uid=0` 判定。
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
        val result = execute("id")
        return result.exitCode == 0 && result.stdout.contains("uid=0")
    }
}