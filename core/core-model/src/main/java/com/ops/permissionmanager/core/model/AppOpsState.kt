package com.ops.permissionmanager.core.model

/** 单个权限的当前状态。 */
data class AppOpState(
    val op: AppOp,
    val mode: OpMode
)

/** 某应用的全部权限状态，按权限组分类。 */
data class AppOpsState(
    val packageName: String,
    val states: List<AppOpState>
) {
    val grouped: Map<OpGroup, List<AppOpState>> = states.groupBy { it.op.group }
}
