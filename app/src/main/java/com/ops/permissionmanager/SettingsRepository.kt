package com.ops.permissionmanager

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(SettingsPrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode: MutableStateFlow<ThemeMode> = MutableStateFlow(loadThemeMode())

    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(SettingsPrefKeys.KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode {
        val stored = prefs.getString(SettingsPrefKeys.KEY_THEME_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
        if (stored != null) return stored
        return if (prefs.getBoolean(SettingsPrefKeys.KEY_LEGACY_DARK_MODE, false)) {
            ThemeMode.DARK
        } else {
            ThemeMode.SYSTEM
        }
    }
}

internal object SettingsPrefKeys {
    const val PREFS_NAME = "ops_settings"
    const val KEY_THEME_MODE = "theme_mode"
    @Deprecated("迁移到 KEY_THEME_MODE")
    const val KEY_LEGACY_DARK_MODE = "dark_mode"
}
