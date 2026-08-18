package com.ops.permissionmanager.feature.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.core.model.AppInfo
import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.data.applist.AppListRepository
import com.ops.permissionmanager.data.appops.AppOpsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

data class BatchResultItem(
    val packageName: String,
    val appName: String,
    val success: Boolean,
    val message: String
)

data class BatchUiState(
    val isLoading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val selectedOp: AppOp? = null,
    val selectedMode: OpMode = OpMode.DENY,
    val isExecuting: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val results: List<BatchResultItem> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val appListRepository: AppListRepository,
    private val appOpsRepository: AppOpsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchUiState())
    val uiState: StateFlow<BatchUiState> = _uiState.asStateFlow()

    private var executeJob: Job? = null

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val apps = appListRepository.getInstalledApps()
                _uiState.update { it.copy(isLoading = false, apps = apps) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleApp(packageName: String) {
        _uiState.update { state ->
            val selected = state.selectedPackages.toMutableSet()
            if (!selected.add(packageName)) selected.remove(packageName)
            state.copy(selectedPackages = selected)
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedPackages = it.apps.map { a -> a.packageName }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPackages = emptySet()) }
    }

    fun selectOp(op: AppOp) {
        _uiState.update { it.copy(selectedOp = op) }
    }

    fun selectMode(mode: OpMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun executeBatch() {
        val state = _uiState.value
        val op = state.selectedOp ?: return
        val targets = state.apps.filter { it.packageName in state.selectedPackages }
        if (targets.isEmpty() || executeJob?.isActive == true) return

        executeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExecuting = true,
                    progress = 0,
                    total = targets.size,
                    results = emptyList(),
                    message = null
                )
            }
            val results = mutableListOf<BatchResultItem>()
            var done = 0
            val mode = state.selectedMode
            for (app in targets) {
                ensureActive()
                val result = appOpsRepository.setAppOp(app.packageName, op, mode)
                ensureActive()
                results.add(
                    BatchResultItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        success = result.isSuccess,
                        message = result.exceptionOrNull()?.message ?: "成功"
                    )
                )
                done++
                _uiState.update { it.copy(progress = done) }
            }
            _uiState.update {
                it.copy(
                    isExecuting = false,
                    results = results,
                    message = "批量操作完成：成功 ${results.count { r -> r.success }} / ${results.size}"
                )
            }
        }
    }

    fun cancelBatch() {
        executeJob?.cancel()
        _uiState.update { it.copy(isExecuting = false, message = "已取消批量操作") }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
