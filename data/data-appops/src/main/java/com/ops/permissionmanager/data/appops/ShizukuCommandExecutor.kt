package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOpsError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuCommandExecutor @Inject constructor(
    private val shizukuManager: ShizukuManager
) : CommandExecutor {

    private companion object {
        val NEW_PROCESS_METHOD: Method? by lazy {
            try {
                val clazz = Class.forName("rikka.shizuku.Shizuku")
                val method = clazz.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                if (!method.isAccessible) method.setAccessible(true)
                method
            } catch (_: Exception) {
                null
            }
        }
    }

    override suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = newProcess(arrayOf("sh", "-c", command))
            executeProcess(process)
        } catch (e: AppOpsError.CommandFailed) {
            ShellResult("", e.stderr.printlnIfBlank("Shizuku 命令执行失败"), exitCode = e.exitCode)
        }
    }

    override suspend fun isAvailable(): Boolean = shizukuManager.isAvailable()

    private fun newProcess(cmd: Array<String>): ShizukuRemoteProcess {
        val method = NEW_PROCESS_METHOD
            ?: throw AppOpsError.CommandFailed(
                exitCode = -10,
                stderr = "Shizuku newProcess 反射初始化失败（Shizuku 类或方法不可用）"
            )
        return try {
            method.invoke(null, cmd, null, null) as ShizukuRemoteProcess
        } catch (e: InvocationTargetException) {
            throw AppOpsError.CommandFailed(
                exitCode = -11,
                stderr = "Shizuku newProcess 反射调用失败: ${e.targetException?.message ?: e.message}"
            )
        } catch (e: IllegalAccessException) {
            throw AppOpsError.CommandFailed(
                exitCode = -12,
                stderr = "Shizuku newProcess 反射无权限访问: ${e.message}"
            )
        }
    }
}

private fun String.printlnIfBlank(default: String): String =
    if (isBlank()) default else this
