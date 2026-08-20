package com.ops.permissionmanager.feature.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.core.model.AppInfo
import com.ops.permissionmanager.data.applist.AppListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

data class AppListUiState(
    val isLoading: Boolean = false,
    val apps: List<AppInfo> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appListRepository: AppListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
