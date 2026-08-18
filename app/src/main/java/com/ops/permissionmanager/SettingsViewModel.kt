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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel。
 *
 * 与原版反编译逐项对齐：
 * - uiState 由 MutableStateFlow.asStateFlow() 直接暴露（不用 combine/stateIn）；
 * - init 中四个独立常驻 collect 分别监听 themeMode / modifyMode / Shizuku Binder / 授权；
 * - checkAvailability 单独探测 Root 可用性写入 isRootAvailable。
 */
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

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 与原版一致：四个独立常驻 collect（不随订阅生命周期暂停）
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            modifyModeRepository.modifyMode.collect { mode ->
                _uiState.update { it.copy(modifyMode = mode) }
            }
        }
        viewModelScope.launch {
            shizukuManager.isBinderAvailable.collect { available ->
                _uiState.update { it.copy(isShizukuBinderAvailable = available) }
            }
        }
        viewModelScope.launch {
            shizukuManager.isPermissionGranted.collect { granted ->
                _uiState.update { it.copy(isShizukuPermissionGranted = granted) }
            }
        }
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
            _uiState.update { it.copy(isRootAvailable = available) }
        }
    }
}