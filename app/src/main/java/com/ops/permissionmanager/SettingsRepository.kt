package com.ops.permissionmanager

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用设置仓库。
 *
 * 通过 SharedPreferences 持久化主题模式，并以 [StateFlow] 暴露可观察状态，
 * 供 [MainActivity] 应用主题、[SettingsViewModel] 读取/修改使用。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode: MutableStateFlow<ThemeMode> = MutableStateFlow(loadThemeMode())

    /** 当前主题模式（StateFlow，可观察变化）。 */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /** 保存并更新主题模式。 */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    /** 从 SharedPreferences 加载主题模式，优先使用新 key，回退到旧深色开关。 */
    private fun loadThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
        if (stored != null) return stored
        return if (prefs.getBoolean(KEY_LEGACY_DARK_MODE, false)) {
            ThemeMode.DARK
        } else {
            ThemeMode.SYSTEM
        }
    }

    private companion object {
        const val PREFS_NAME = "ops_settings"

        const val KEY_THEME_MODE = "theme_mode"

        @Deprecated("迁移到 KEY_THEME_MODE")
        const val KEY_LEGACY_DARK_MODE = "dark_mode"
    }
}