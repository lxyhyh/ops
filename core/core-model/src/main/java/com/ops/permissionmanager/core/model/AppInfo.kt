package com.ops.permissionmanager.core.model

/** 应用信息。 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)
