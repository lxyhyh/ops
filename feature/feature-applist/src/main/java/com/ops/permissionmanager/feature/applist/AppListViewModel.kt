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
    val isLoading: Boolean = true,
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
