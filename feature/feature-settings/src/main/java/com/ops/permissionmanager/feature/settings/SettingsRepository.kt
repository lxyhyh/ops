package com.ops.permissionmanager.feature.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * 设置仓库接口（可测试 seam）。
 * 暴露当前主题模式（StateFlow，可观察变化），并允许更新。
 */
interface SettingsRepository {

    /** 当前主题模式（StateFlow，可观察变化）。 */
    val themeMode: StateFlow<ThemeMode>

    /** 设置主题模式。 */
    fun setThemeMode(mode: ThemeMode)
}