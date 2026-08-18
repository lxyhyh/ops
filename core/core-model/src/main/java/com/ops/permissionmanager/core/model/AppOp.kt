package com.ops.permissionmanager.core.model

/**
 * 一个可管理的 AppOps 操作项。
 *
 * @property name AppOps 操作名，如 [AppOpCatalog] 中的 "RUN_IN_BACKGROUND"
 * @property displayName 面向用户的显示名称
 * @property group 所属分组
 */
data class AppOp(
    val name: String,
    val displayName: String,
    val group: OpGroup
)