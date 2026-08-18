package com.ops.permissionmanager.feature.applist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.OpGroup
import com.ops.permissionmanager.core.model.OpMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailRoute(
    packageName: String,
    appName: String,
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.appName.ifEmpty { packageName }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AppDetailScreen(
            uiState = uiState,
            onModeSelect = viewModel::setMode,
            onRetry = viewModel::load,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun AppDetailScreen(
    uiState: AppDetailUiState,
    onModeSelect: (AppOpState, OpMode) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingOp by remember { mutableStateOf<AppOpState?>(null) }

    when {
        uiState.isLoading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("加载失败：${uiState.error}")
                    Text(
                        "重试",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { onRetry() }
                    )
                }
            }
        }
        else -> {
            val state = uiState.appOps
            if (state == null || state.states.isEmpty()) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("未获取到权限数据")
                }
            } else {
                LazyColumn(modifier.fillMaxSize()) {
                    state.grouped.forEach { (group, items) ->
                        item(key = "header_${group.name}") {
                            Text(
                                text = group.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(items, key = { it.op.name }) { opState ->
                            AppOpRow(
                                opState = opState,
                                onClick = { pendingOp = opState }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    pendingOp?.let { opState ->
        ModePickerDialog(
            opState = opState,
            onDismiss = { pendingOp = null },
            onSelect = { mode ->
                onModeSelect(opState, mode)
                pendingOp = null
            }
        )
    }
}

@Composable
private fun ModePickerDialog(
    opState: AppOpState,
    onDismiss: () -> Unit,
    onSelect: (OpMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(opState.op.displayName) },
        text = {
            Column {
                Text(
                    text = "修改可能导致应用异常，请谨慎操作",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OpMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (mode == opState.mode) {
                            Text(
                                text = "当前",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AppOpRow(
    opState: AppOpState,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = opState.op.displayName,
                fontWeight = FontWeight.Medium
            )
        },
        trailingContent = {
            Text(
                text = opState.mode.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = when (opState.mode) {
                    OpMode.ALLOW -> MaterialTheme.colorScheme.primary
                    OpMode.DENY, OpMode.IGNORE -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
