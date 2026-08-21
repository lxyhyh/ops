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
    val message: String,
    /** 本次执行的权限（失败项重试用），旧数据为 null。 */
    val op: AppOp? = null,
    /** 本次执行的目标模式（失败项重试用），旧数据为 null。 */
    val mode: OpMode? = null
)

data class BatchUiState(
    val isLoading: Boolean = false,
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
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                // 第一步：先读磁盘缓存，有则立即展示，避免 PackageManager 遍历的等待
                appListRepository.getCachedInstalledApps()?.let { cached ->
                    _uiState.update { it.copy(isLoading = false, apps = cached) }
                }
                // 第二步：后台构建最新列表（内存/磁盘缓存也在此写入），对比后更新
                val apps = appListRepository.getInstalledApps()
                _uiState.update { it.copy(isLoading = false, apps = apps) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 与原版一致：加载失败提示走 message（Snackbar），不动 error。
                _uiState.update { it.copy(isLoading = false, message = e.message) }
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
                        message = result.exceptionOrNull()?.message ?: "成功",
                        op = op,
                        mode = mode
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
        _uiState.update { it.copy(isExecuting = false, error = null, message = "已取消批量操作") }
    }

    /**
     * 单条重试：仅对失败项重新执行同一权限操作，并就地更新结果列表。
     * 失败项缺少 op/mode（旧数据）时忽略。
     */
    fun retryItem(result: BatchResultItem) {
        val op = result.op ?: return
        val mode = result.mode ?: return
        viewModelScope.launch {
            ensureActive()
            val newResult = appOpsRepository.setAppOp(result.packageName, op, mode).fold(
                onSuccess = {
                    BatchResultItem(result.packageName, result.appName, true, "成功", op, mode)
                },
                onFailure = { e ->
                    BatchResultItem(
                        result.packageName,
                        result.appName,
                        false,
                        e.message ?: "失败",
                        op,
                        mode
                    )
                }
            )
            _uiState.update { state ->
                state.copy(
                    results = state.results.map {
                        if (it.packageName == result.packageName && it.op?.name == op.name) newResult else it
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
