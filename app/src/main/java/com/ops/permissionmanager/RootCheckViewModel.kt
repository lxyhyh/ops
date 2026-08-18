package com.ops.permissionmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.data.appops.CommandExecutorRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RootCheckUiState(
    val isChecking: Boolean = true,
    val isAnyAvailable: Boolean = false
)

@HiltViewModel
class RootCheckViewModel @Inject constructor(
    private val commandExecutorRouter: CommandExecutorRouter
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootCheckUiState())
    val uiState: StateFlow<RootCheckUiState> = _uiState.asStateFlow()

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        viewModelScope.launch {
            _uiState.value = RootCheckUiState(isChecking = true, isAnyAvailable = false)
            val available = runCatching { commandExecutorRouter.isAnyAvailable() }
                .getOrDefault(false)
            _uiState.value = RootCheckUiState(isChecking = false, isAnyAvailable = available)
        }
    }
}