package com.ops.permissionmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.core.model.ModifyMode
import com.ops.permissionmanager.data.appops.CommandExecutorRouter
import com.ops.permissionmanager.data.appops.ModifyModeRepository
import com.ops.permissionmanager.data.appops.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页面的 ViewModel。
 *
 * 汇聚 [SettingsRepository]（主题）、[ModifyModeRepository]（修改方式）以及
 * [ShizukuManager]（Shizuku 可用性）的可观察状态，统一暴露为 [uiState]。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val commandExecutorRouter: CommandExecutorRouter,
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

    /** 设置页面 UI 状态流。 */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            modifyModeRepository.modifyMode.collect { mode ->
                _uiState.value = _uiState.value.copy(modifyMode = mode)
            }
        }
        viewModelScope.launch {
            shizukuManager.isBinderAvailable.collect { available ->
                _uiState.value = _uiState.value.copy(isShizukuBinderAvailable = available)
            }
        }
        viewModelScope.launch {
            shizukuManager.isPermissionGranted.collect { granted ->
                _uiState.value = _uiState.value.copy(isShizukuPermissionGranted = granted)
            }
        }
        checkAvailability()
    }

    /** 设置主题模式。 */
    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    /** 设置权限修改方式。 */
    fun setModifyMode(mode: ModifyMode) {
        modifyModeRepository.setModifyMode(mode)
    }

    /** 请求 Shizuku 授权。 */
    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    /** 检测 Root 可用性并刷新状态。 */
    fun checkAvailability() {
        viewModelScope.launch {
            val available = commandExecutorRouter.isRootAvailable()
            _uiState.value = _uiState.value.copy(isRootAvailable = available)
        }
    }
}