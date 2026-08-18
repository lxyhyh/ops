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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
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

    private val packageName: String = savedStateHandle.get<String>(ARG_PACKAGE_NAME).orEmpty()
    private val appName: String = savedStateHandle.get<String>(ARG_APP_NAME).orEmpty()

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(appName = appName) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = appOpsRepository.getAppOps(packageName)
                _uiState.update { it.copy(isLoading = false, appOps = state) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setMode(opState: AppOpState, mode: OpMode) {
        viewModelScope.launch {
            val result = appOpsRepository.setAppOp(packageName, opState.op, mode)
            ensureActive()
            result.onSuccess {
                _uiState.update { it.copy(message = "$MSG_SET_MODE_SUCCESS${mode.displayName}") }
                load()
            }.onFailure { e ->
                _uiState.update { it.copy(message = "$MSG_SET_MODE_FAILURE${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private companion object {
        const val ARG_PACKAGE_NAME = "packageName"
        const val ARG_APP_NAME = "appName"
        const val MSG_SET_MODE_SUCCESS = "已设置为 "
        const val MSG_SET_MODE_FAILURE = "修改失败："
    }
}
