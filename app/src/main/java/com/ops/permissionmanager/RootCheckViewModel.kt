package com.ops.permissionmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.data.appops.ExecutionAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RootAvailability {
    UNKNOWN,
    CHECKING,
    AVAILABLE,
    UNAVAILABLE
}

data class RootCheckUiState(
    val isChecking: Boolean = true,
    val isAnyAvailable: Boolean = false,
    val availability: RootAvailability = RootAvailability.UNKNOWN
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
            val prior = _uiState.value
            if (prior.availability != RootAvailability.AVAILABLE) {
                _uiState.value = prior.copy(
                    isChecking = true,
                    availability = RootAvailability.CHECKING
                )
            }
            val available = runCatching { executionAvailability.isAnyAvailable() }
                .getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                isChecking = false,
                isAnyAvailable = available,
                availability = if (available) RootAvailability.AVAILABLE else RootAvailability.UNAVAILABLE
            )
        }
    }
}
