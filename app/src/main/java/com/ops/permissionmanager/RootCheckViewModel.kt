package com.ops.permissionmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.data.appops.RootShell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RootCheckUiState(
    val isChecking: Boolean = true,
    val isRootAvailable: Boolean = false
)

@HiltViewModel
class RootCheckViewModel @Inject constructor(
    private val rootShell: RootShell
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootCheckUiState())
    val uiState: StateFlow<RootCheckUiState> = _uiState.asStateFlow()

    init {
        checkRoot()
    }

    fun checkRoot() {
        viewModelScope.launch {
            _uiState.value = RootCheckUiState(isChecking = true, isRootAvailable = false)
            val available = runCatching { rootShell.isRootAvailable() }.getOrDefault(false)
            _uiState.value = RootCheckUiState(isChecking = false, isRootAvailable = available)
        }
    }
}
