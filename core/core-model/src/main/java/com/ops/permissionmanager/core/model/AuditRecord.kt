package com.ops.permissionmanager.core.model

/**
 * 一条权限修改审计记录（本地持久化，不联网）。
 *
 * @property timestampMillis 修改时间（epoch 毫秒）
 * @property packageName 目标应用包名
 * @property opName 权限操作名（如 "READ_CLIPBOARD"）
 * @property opDisplayName 权限显示名（快照，防目录变更后显示错乱）
 * @property oldMode 修改前模式
 * @property newMode 修改后模式
 * @property channel 执行通道（Root / Shizuku / 自动）
 */
data class AuditRecord(
    val timestampMillis: Long,
    val packageName: String,
    val opName: String,
    val opDisplayName: String,
    val oldMode: OpMode,
    val newMode: OpMode,
    val channel: ModifyMode
)