package com.ops.permissionmanager.feature.applist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.core.model.AppDetailInfo
import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.data.applist.AppListRepository
import com.ops.permissionmanager.data.appops.AppOpsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

data class AppDetailUiState(
    val isLoading: Boolean = false,
    val appOps: AppOpsState? = null,
    /** 应用详情诊断信息（版本/UID/目标SDK/安装时间等），查询失败为 null。 */
    val detail: AppDetailInfo? = null,
    val error: String? = null,
    val message: String? = null,
    val appName: String = ""
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val appOpsRepository: AppOpsRepository,
    private val appListRepository: AppListRepository,
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
                // 权限状态与诊断信息相互独立，并行拉取互不阻塞
                val (state, detail) = coroutineScope {
                    val ops = async { appOpsRepository.getAppOps(packageName) }
                    val info = async { appListRepository.getAppDetail(packageName) }
                    ops.await() to info.await()
                }
                _uiState.update { it.copy(isLoading = false, appOps = state, detail = detail) }
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
