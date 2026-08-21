package com.ops.permissionmanager.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.data.appops.ExecutionAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 环境检测状态：除总可用性外，细分 Root / Shizuku 可用性及 Shizuku 是否安装，用于引导页差异化提示。 */
data class RootCheckUiState(
    val isChecking: Boolean = false,
    val isAnyAvailable: Boolean = false,
    val isRootAvailable: Boolean = false,
    val isShizukuAvailable: Boolean = false,
    val isShizukuInstalled: Boolean = false
)

@HiltViewModel
class RootCheckViewModel @Inject constructor(
    private val executionAvailability: ExecutionAvailability,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootCheckUiState())
    val uiState: StateFlow<RootCheckUiState> = _uiState.asStateFlow()

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        viewModelScope.launch {
            // 与原版一致：无条件先进入检查态，再执行可用性探测。
            _uiState.value = RootCheckUiState(isChecking = true)
            val root = runCatching { executionAvailability.isRootAvailable() }
                .getOrDefault(false)
            val shizuku = runCatching { executionAvailability.isShizukuAvailable() }
                .getOrDefault(false)
            val shizukuInstalled = runCatching {
                context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
                true
            }.getOrDefault(false)
            _uiState.value = RootCheckUiState(
                isChecking = false,
                isAnyAvailable = root || shizuku,
                isRootAvailable = root,
                isShizukuAvailable = shizuku,
                isShizukuInstalled = shizukuInstalled
            )
        }
    }

    private companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
