package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通过 Shizuku 执行命令的执行器。
 *
 * 依赖 [ShizukuManager] 判断可用性；
 * 通过 `newProcess("sh", "-c", command)` 以远程（ADB / root）身份执行命令。
 */
@Singleton
class ShizukuCommandExecutor @Inject constructor(
    private val shizukuManager: ShizukuManager
) : CommandExecutor {

    private companion object {
        /** 反射获取的 [rikka.shizuku.Shizuku.newProcess] 方法。 */
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
        val process = newProcess(arrayOf("sh", "-c", command))
        executeProcess(process)
    }

    override suspend fun isAvailable(): Boolean = shizukuManager.isAvailable()

    private fun newProcess(cmd: Array<String>): ShizukuRemoteProcess {
        val method = NEW_PROCESS_METHOD
            ?: throw IllegalStateException("Shizuku newProcess 反射初始化失败")
        return method.invoke(null, cmd, null, null) as ShizukuRemoteProcess
    }
}