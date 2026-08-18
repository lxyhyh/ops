package com.ops.permissionmanager.core.model

/**
 * 单个应用内所有 AppOps 操作的状态集合。
 *
 * @property packageName 应用包名
 * @property states 操作状态列表
 */
data class AppOpsState(
    val packageName: String,
    val states: List<AppOpState>
) {
    /** 按操作分组 */
    val grouped: Map<OpGroup, List<AppOpState>> =
        states.groupBy { it.op.group }
}