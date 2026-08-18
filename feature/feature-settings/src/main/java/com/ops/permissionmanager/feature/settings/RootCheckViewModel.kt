package com.ops.permissionmanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.data.appops.ExecutionAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 与原版反编译一致：仅两个字段，默认 isChecking=false。 */
data class RootCheckUiState(
    val isChecking: Boolean = false,
    val isAnyAvailable: Boolean = false
)

@HiltViewModel
class RootCheckViewModel @Inject constructor(
    private val executionAvailability: ExecutionAvailability
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootCheckUiState())
    val uiState: StateFlow<RootCheckUiState> = _uiState.asStateFlow()

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        viewModelScope.launch {
            // 与原版一致：无条件先进入检查态，再执行可用性探测。
            _uiState.value = RootCheckUiState(isChecking = true, isAnyAvailable = false)
            val available = runCatching { executionAvailability.isAnyAvailable() }
                .getOrDefault(false)
            _uiState.value = RootCheckUiState(isChecking = false, isAnyAvailable = available)
        }
    }
}
