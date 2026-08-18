package com.ops.permissionmanager

import com.ops.permissionmanager.core.model.ModifyMode

/**
 * 设置页面的 UI 状态。
 *
 * @property themeMode 当前主题模式
 * @property modifyMode 当前权限修改方式
 * @property isRootAvailable Root 是否可用
 * @property isShizukuBinderAvailable Shizuku Binder 服务是否可用
 * @property isShizukuPermissionGranted Shizuku 是否已授权
 * @property versionName 应用版本号
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val modifyMode: ModifyMode = ModifyMode.AUTO,
    val isRootAvailable: Boolean = false,
    val isShizukuBinderAvailable: Boolean = false,
    val isShizukuPermissionGranted: Boolean = false,
    val versionName: String = ""
)