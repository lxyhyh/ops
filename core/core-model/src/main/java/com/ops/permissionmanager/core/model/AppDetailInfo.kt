package com.ops.permissionmanager.core.model

/**
 * 应用详情诊断信息（详情页按需查询，列表页不加载以保持轻量）。
 *
 * @property packageName 应用包名
 * @property appName 应用显示名称
 * @property isSystemApp 是否为系统应用
 * @property versionName 版本名（如 "1.0.0"），无法获取时为 null
 * @property versionCode 版本号（long，API 28+ 语义），无法获取时为 null
 * @property uid 应用 UID（多用户下为当前用户维度），无法获取时为 null
 * @property targetSdk 目标 SDK 版本，无法获取时为 null
 * @property enabled 是否处于启用状态，无法获取时为 null
 * @property firstInstallTime 首次安装时间（epoch 毫秒），无法获取时为 null
 * @property lastUpdateTime 最近更新时间（epoch 毫秒），无法获取时为 null
 */
data class AppDetailInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val versionName: String?,
    val versionCode: Long?,
    val uid: Int?,
    val targetSdk: Int?,
    val enabled: Boolean?,
    val firstInstallTime: Long?,
    val lastUpdateTime: Long?
)
