package com.ops.permissionmanager.core.model

/** 单条权限使用记录（来自 dumpsys appops 历史）。 */
data class OpUsageRecord(
    val packageName: String,
    val opName: String,
    val timestampMillis: Long,
    val durationMillis: Long? = null,
    val count: Int = 1
)
