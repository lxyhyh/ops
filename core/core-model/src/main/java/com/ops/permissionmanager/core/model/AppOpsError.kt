package com.ops.permissionmanager.core.model

/**
 * AppOps 操作相关的异常类型。
 */
sealed class AppOpsError : Exception() {
    /** 命令执行失败 */
    data class CommandFailed(
        val exitCode: Int,
        val stderr: String = ""
    ) : AppOpsError()

    /** 包名非法/为空 */
    data object InvalidPackage : AppOpsError()
}