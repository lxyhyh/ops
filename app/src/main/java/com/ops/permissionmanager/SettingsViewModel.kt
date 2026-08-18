package com.ops.permissionmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.core.model.ModifyMode
import com.ops.permissionmanager.data.appops.ExecutionAvailability
import com.ops.permissionmanager.data.appops.ModifyModeRepository
import com.ops.permissionmanager.data.appops.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val executionAvailability: ExecutionAvailability,
    private val modifyModeRepository: ModifyModeRepository,
    private val shizukuManager: ShizukuManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = settingsRepository.themeMode.value,
            modifyMode = modifyModeRepository.modifyMode.value,
            versionName = BuildConfig.VERSION_NAME
        )
    )

    private val _rootAvailable = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        modifyModeRepository.modifyMode,
        shizukuManager.isBinderAvailable,
        shizukuManager.isPermissionGranted,
        _rootAvailable
    ) { theme, mode, binderAvailable, permissionGranted, rootAvailable ->
        _uiState.value.copy(
            themeMode = theme,
            modifyMode = mode,
            isShizukuBinderAvailable = binderAvailable,
            isShizukuPermissionGranted = permissionGranted,
            isRootAvailable = rootAvailable
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _uiState.value
    )

    init {
        checkAvailability()
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun setModifyMode(mode: ModifyMode) {
        modifyModeRepository.setModifyMode(mode)
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun checkAvailability() {
        viewModelScope.launch {
            val available = runCatching { executionAvailability.isRootAvailable() }
                .getOrDefault(false)
            _rootAvailable.value = available
        }
    }
}
