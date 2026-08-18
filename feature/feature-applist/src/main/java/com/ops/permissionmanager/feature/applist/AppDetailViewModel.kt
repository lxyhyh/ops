package com.ops.permissionmanager.feature.applist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.data.appops.AppOpsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val appOps: AppOpsState? = null,
    val error: String? = null,
    val message: String? = null,
    val appName: String = ""
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val appOpsRepository: AppOpsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>("packageName").orEmpty()
    private val appName: String = savedStateHandle.get<String>("appName").orEmpty()

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(appName = appName) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { appOpsRepository.getAppOps(packageName) }
                .onSuccess { state ->
                    _uiState.update { it.copy(isLoading = false, appOps = state) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun setMode(opState: AppOpState, mode: OpMode) {
        viewModelScope.launch {
            appOpsRepository.setAppOp(packageName, opState.op, mode)
                .onSuccess {
                    _uiState.update { it.copy(message = "已设置为 ${mode.displayName}") }
                    load()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = "修改失败：${e.message}") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
