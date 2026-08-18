package com.ops.permissionmanager.core.model

/**
 * 已安装应用的信息。
 *
 * @property packageName 应用包名
 * @property appName 应用显示名称
 * @property isSystemApp 是否为系统应用
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)