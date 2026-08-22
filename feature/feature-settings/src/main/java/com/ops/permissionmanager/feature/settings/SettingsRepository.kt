package com.ops.permissionmanager.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ops.permissionmanager.data.appops.opsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.opsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 启动竞态防护：构造时同步读取持久化主题，避免首帧闪回默认 SYSTEM。 */
    private val _themeMode: MutableStateFlow<ThemeMode> = MutableStateFlow(
        runBlocking(Dispatchers.IO) {
            dataStore.data.first()[SettingsPrefKeys.KEY_THEME_MODE]
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM
        }
    )

    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch {
            dataStore.edit { it[SettingsPrefKeys.KEY_THEME_MODE] = mode.name }
        }
    }
}

internal object SettingsPrefKeys {
    val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
}