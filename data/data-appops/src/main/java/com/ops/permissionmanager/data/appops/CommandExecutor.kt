package com.ops.permissionmanager.data.appops

/**
 * 命令执行器抽象。
 *
 * 用于以不同特权方式（Root / Shizuku）执行系统命令，
 * 返回统一的 [ShellResult]。
 */
interface CommandExecutor {
    /** 执行一条命令，返回执行结果（stdout / stderr / exitCode）。 */
    suspend fun execute(command: String): ShellResult

    /** 检测该执行器当前是否可用。 */
    suspend fun isAvailable(): Boolean
}