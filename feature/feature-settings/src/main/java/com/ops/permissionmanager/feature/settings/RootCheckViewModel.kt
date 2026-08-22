package com.ops.permissionmanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.data.appops.ExecutionAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val shizukuInstallDetector: ShizukuInstallDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootCheckUiState())
    val uiState: StateFlow<RootCheckUiState> = _uiState.asStateFlow()

    /** 当前探测任务：重入时取消旧任务，避免并发探测导致结果乱序覆盖。 */
    private var checkJob: Job? = null

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        // 取消上一次未完成的探测（用户快速点重试时只保留最后一次的结果）
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            // 与原版一致：无条件先进入检查态，再执行可用性探测。
            _uiState.value = RootCheckUiState(isChecking = true)
            val root = runCatching { executionAvailability.isRootAvailable() }
                .getOrDefault(false)
            val shizuku = runCatching { executionAvailability.isShizukuAvailable() }
                .getOrDefault(false)
            val shizukuInstalled = shizukuInstallDetector.isInstalled()
            _uiState.value = RootCheckUiState(
                isChecking = false,
                isAnyAvailable = root || shizuku,
                isRootAvailable = root,
                isShizukuAvailable = shizuku,
                isShizukuInstalled = shizukuInstalled
            )
        }
    }
}
