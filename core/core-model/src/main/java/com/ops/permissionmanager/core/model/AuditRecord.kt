package com.ops.permissionmanager.core.model

/**
 * 一条权限修改审计记录（本地持久化，不联网）。
 *
 * @property timestampMillis 修改时间（epoch 毫秒）
 * @property packageName 目标应用包名
 * @property opName 权限操作名（如 "READ_CLIPBOARD"）
 * @property opDisplayName 权限显示名（快照，防目录变更后显示错乱）
 * @property oldMode 修改前模式（旧值查询失败时不可信，见 [oldModeUnknown]）
 * @property newMode 修改后模式
 * @property channel 执行通道（Root / Shizuku / 自动）
 * @property oldModeUnknown 旧值查询失败时为 true（此时 [oldMode] 无意义，界面不提供撤销）；
 *   默认 false 兼容旧数据
 */
data class AuditRecord(
    val timestampMillis: Long,
    val packageName: String,
    val opName: String,
    val opDisplayName: String,
    val oldMode: OpMode,
    val newMode: OpMode,
    val channel: ModifyMode,
    val oldModeUnknown: Boolean = false
)