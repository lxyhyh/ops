package com.ops.permissionmanager.core.model

/**
 * 单个 AppOps 操作项的当前状态。
 *
 * @property op 操作项
 * @property mode 当前模式
 */
data class AppOpState(
    val op: AppOp,
    val mode: OpMode
)