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
    val query: String = "",
    val filter: AppFilter = AppFilter.All,
    /** 按 query + filter 组合过滤后的结果，供列表直接展示。 */
    val filteredApps: List<AppInfo> = emptyList(),
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            apps = cached,
                            filteredApps = filterApps(cached, it.query, it.filter)
                        )
                    }
                }
                // 第二步：后台构建最新列表（内存/磁盘缓存也在此写入），对比后更新
                val apps = appListRepository.getInstalledApps()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        apps = apps,
                        filteredApps = filterApps(apps, it.query, it.filter)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update {
            it.copy(query = query, filteredApps = filterApps(it.apps, query, it.filter))
        }
    }

    fun setFilter(filter: AppFilter) {
        _uiState.update {
            it.copy(filter = filter, filteredApps = filterApps(it.apps, it.query, filter))
        }
    }
}

/** 纯函数过滤：query 按名称/包名模糊匹配（忽略大小写、忽略首尾空格），与 filter 类型组合。 */
private fun filterApps(
    apps: List<AppInfo>,
    query: String,
    filter: AppFilter
): List<AppInfo> {
    val keyword = query.trim()
    return apps.filter { app ->
        val matchFilter = when (filter) {
            AppFilter.All -> true
            AppFilter.System -> app.isSystemApp
            AppFilter.User -> !app.isSystemApp
        }
        matchFilter && (
            app.appName.contains(keyword, ignoreCase = true) ||
                app.packageName.contains(keyword, ignoreCase = true)
            )
    }
}
