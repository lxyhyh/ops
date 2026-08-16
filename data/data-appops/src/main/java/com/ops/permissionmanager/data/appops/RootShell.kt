package com.ops.permissionmanager.data.appops

/** 命令执行结果。 */
data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)

/**
 * RootShell 适配器 Seam。
 * 生产用 RealRootShell（su 执行），测试用 FakeRootShell（预设输出）。
 */
interface RootShell {
    suspend fun execute(command: String): ShellResult

    /** 检测当前设备是否可用 root（su 是否可执行）。 */
    suspend fun isRootAvailable(): Boolean
}
