package com.ops.permissionmanager.core.model

/**
 * 一次 AppOps 的使用/操作记录。
 *
 * @property packageName 应用包名
 * @property opName 操作名
 * @property timestampMillis 时间戳（毫秒）
 * @property durationMillis 持续时长（毫秒），可为空
 * @property count 出现次数
 * @property accessType 记录类型（"Access"=使用 / "Reject"=拒绝），旧数据为 null
 * @property uid 应用 UID（从 dumpsys "Uid xxxx:" 段头提取），旧数据为 null
 */
data class OpUsageRecord(
    val packageName: String,
    val opName: String,
    val timestampMillis: Long,
    val durationMillis: Long? = null,
    val count: Int = 1,
    val accessType: String? = null,
    val uid: Int? = null
)