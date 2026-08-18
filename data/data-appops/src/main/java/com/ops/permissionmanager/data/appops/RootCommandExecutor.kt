package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootCommandExecutor @Inject constructor() : CommandExecutor {

    override suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(false)
            .start()
        executeProcess(process)
    }

    override suspend fun isAvailable(): Boolean {
        val result = try {
            execute("id -u")
        } catch (_: Exception) {
            return false
        }
        return result.exitCode == 0 && result.stdout.trim() == "0"
    }
}
