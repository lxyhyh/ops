package com.ops.permissionmanager.core.model

/**
 * 一个可管理的 AppOps 操作项。
 *
 * @property name AppOps 操作名，如 [AppOpCatalog] 中的 "RUN_IN_BACKGROUND"
 * @property displayName 面向用户的显示名称
 * @property group 所属分组
 * @property isHighRisk 是否属于高风险操作（后台运行/定位/剪贴板/相机/麦克风/通知等），
 *   批量与详情页据此展示警示，默认 false 以兼容既有构造
 */
data class AppOp(
    val name: String,
    val displayName: String,
    val group: OpGroup,
    val isHighRisk: Boolean = false
)