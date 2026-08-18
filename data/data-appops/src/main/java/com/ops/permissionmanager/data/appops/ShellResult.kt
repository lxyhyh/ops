package com.ops.permissionmanager.data.appops

/**
 * 一条命令的执行结果。
 *
 * @property stdout 标准输出
 * @property stderr 标准错误
 * @property exitCode 退出码（非 0 表示失败）
 */
data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)