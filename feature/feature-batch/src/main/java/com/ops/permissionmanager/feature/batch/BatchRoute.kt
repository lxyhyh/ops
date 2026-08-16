package com.ops.permissionmanager.feature.batch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ops.permissionmanager.core.model.AppInfo
import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.data.appops.AppOpCatalog

@Composable
fun BatchRoute(viewModel: BatchViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        BatchScreen(
            uiState = uiState,
            onToggleApp = viewModel::toggleApp,
            onSelectAll = viewModel::selectAll,
            onClearSelection = viewModel::clearSelection,
            onSelectOp = viewModel::selectOp,
            onSelectMode = viewModel::selectMode,
            onExecute = viewModel::executeBatch,
            onCancel = viewModel::cancelBatch,
            onRetry = viewModel::loadApps,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun BatchScreen(
    uiState: BatchUiState,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectOp: (AppOp) -> Unit,
    onSelectMode: (OpMode) -> Unit,
    onExecute: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            LazyColumn(modifier.fillMaxSize()) {
                item { SectionHeader("选择权限") }
                item {
                    OpSelector(
                        selectedOp = uiState.selectedOp,
                        onSelectOp = onSelectOp
                    )
                }

                item { SectionHeader("目标状态") }
                item {
                    Row(Modifier.padding(horizontal = 16.dp)) {
                        OpMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.selectedMode == mode,
                                onClick = { onSelectMode(mode) },
                                label = { Text(mode.displayName) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择应用（已选 ${uiState.selectedPackages.size}）",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onSelectAll) { Text("全选") }
                        TextButton(onClick = onClearSelection) { Text("清空") }
                    }
                }

                items(uiState.apps, key = { it.packageName }) { app ->
                    AppCheckRow(
                        app = app,
                        checked = app.packageName in uiState.selectedPackages,
                        onToggle = { onToggleApp(app.packageName) }
                    )
                    HorizontalDivider()
                }

                item {
                    if (uiState.isExecuting) {
                        Column(Modifier.padding(16.dp)) {
                            LinearProgressIndicator(
                                progress = {
                                    if (uiState.total == 0) 0f
                                    else uiState.progress.toFloat() / uiState.total
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "正在处理 ${uiState.progress} / ${uiState.total}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) { Text("取消") }
                        }
                    } else {
                        val canExecute = uiState.selectedOp != null && uiState.selectedPackages.isNotEmpty()
                        Button(
                            onClick = onExecute,
                            enabled = canExecute,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("开始批量操作（${uiState.selectedPackages.size} 个应用）")
                        }
                    }
                }

                items(uiState.results) { result ->
                    ResultRow(result)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpSelector(
    selectedOp: AppOp?,
    onSelectOp: (AppOp) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val ops = AppOpCatalog.all()

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = selectedOp?.displayName ?: "请选择要修改的权限",
            style = MaterialTheme.typography.bodyLarge,
            color = if (selectedOp != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp)
        )
        if (expanded) {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(ops, key = { it.name }) { op ->
                    Text(
                        text = "${op.displayName}（${op.name}）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectOp(op)
                                expanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCheckRow(
    app: AppInfo,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = app.appName,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (app.isSystemApp) "系统" else "用户",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResultRow(result: BatchResultItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = result.appName,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (result.success) "成功" else "失败",
            color = if (result.success) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}
