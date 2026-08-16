package com.ops.permissionmanager.core.model

/** 统一错误模型，界面层据此统一处理。 */
sealed class AppOpsError : Exception() {
    object NoRoot : AppOpsError()
    data class CommandFailed(val exitCode: Int, val stderr: String = "") : AppOpsError()
    object OpNotFound : AppOpsError()
    object InvalidPackage : AppOpsError()
}
