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

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.opsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _themeMode: MutableStateFlow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)

    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        scope.launch {
            val stored = dataStore.data.first()[SettingsPrefKeys.KEY_THEME_MODE]
            _themeMode.value = stored
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM
        }
    }

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