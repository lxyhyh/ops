package com.ops.permissionmanager.feature.batch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.permissionmanager.core.model.AppInfo
import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpCatalog
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.ui.AppIcon
import com.ops.permissionmanager.core.ui.AppTypeLabel
import com.ops.permissionmanager.core.ui.CollapsingTitle
import com.ops.permissionmanager.core.ui.ErrorState
import com.ops.permissionmanager.core.ui.StatusChip
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip

@Composable
fun BatchRoute(
    listState: LazyListState = rememberLazyListState(),
    viewModel: BatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        BatchScreen(
            uiState = uiState,
            listState = listState,
            onToggleApp = viewModel::toggleApp,
            onSelectAll = viewModel::selectAll,
            onClearSelection = viewModel::clearSelection,
            onSelectOp = viewModel::selectOp,
            onSelectMode = viewModel::selectMode,
            onExecute = viewModel::executeBatch,
            onCancel = viewModel::cancelBatch,
            onRetryItem = viewModel::retryItem,
            onRetry = viewModel::loadApps
        )
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun BatchScreen(
    uiState: BatchUiState,
    listState: LazyListState,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectOp: (AppOp) -> Unit,
    onSelectMode: (OpMode) -> Unit,
    onExecute: () -> Unit,
    onCancel: () -> Unit,
    onRetryItem: (BatchResultItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collapsed by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    // 执行前确认窗：点 FAB 先弹确认，防误操作
    var showConfirm by remember { mutableStateOf(false) }

    when {
        uiState.isLoading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            ErrorState(message = uiState.error, onRetry = onRetry)
        }
        else -> {
            Box(modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    // 与原版一致：标题与卡片整体带左右 16dp 内边距。
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        CollapsingTitle(
                            title = "批量",
                            subtitle = null,
                            collapsed = collapsed
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                // MIUI X：批量配置大卡片改用 G2 连续曲线圆角容器（squircleBackground）
                                .squircleBackground(MaterialTheme.colorScheme.surfaceContainer, 20.dp)
                        ) {
                                Column {
                                SectionHeader("选择权限")
                                OpSelector(
                                    selectedOp = uiState.selectedOp,
                                    onSelectOp = onSelectOp
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                SectionHeader("目标状态")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    OpMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = uiState.selectedMode == mode,
                                            onClick = { onSelectMode(mode) },
                                            label = { Text(mode.displayName) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) // MIUI X：未选中描边也用主题色系
                                            ),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryFixed,
                                                labelColor = MaterialTheme.colorScheme.primary // 未选中文字用主题色
                                            )
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
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
                        }
                        items(uiState.results) { result ->
                            ResultRow(
                                result = result,
                                onRetry = { onRetryItem(result) }
                            )
                        }
                    }
                }

                BatchFab(
                    uiState = uiState,
                    onExecute = { showConfirm = true },
                    onCancel = onCancel,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 76.dp)
                )
            }

            // 执行前确认窗（点击 FAB 弹出）
            if (showConfirm) {
                BatchConfirmDialog(
                    uiState = uiState,
                    onConfirm = {
                        showConfirm = false
                        onExecute()
                    },
                    onDismiss = { showConfirm = false }
                )
            }
        }
    }
}

@Composable
private fun BatchFab(
    uiState: BatchUiState,
    onExecute: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(56.dp), contentAlignment = Alignment.TopStart) {
        if (uiState.isExecuting) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onCancel)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val fraction =
                        if (uiState.total == 0) 0f
                        else uiState.progress.toFloat() / uiState.total
                    CircularProgressIndicator(
                        progress = fraction,
                        modifier = Modifier.size(42.dp),
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = MaterialTheme.colorScheme.onPrimary,
                            backgroundColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                        ),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = if (uiState.total == 0) "0%" else "${uiState.progress * 100 / uiState.total}%",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        } else {
            val canExecute = uiState.selectedOp != null && uiState.selectedPackages.isNotEmpty()
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (canExecute) 1f else 0.4f),
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = canExecute, onClick = onExecute)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "开始批量操作",
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun AppCheckRow(
    app: AppInfo,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val background = if (checked) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .squircleClip(16.dp)
            .background(background)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                uncheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) // MIUI X：未选中描边用主题色系
            )
        )
        AppIcon(
            packageName = app.packageName,
            appName = app.appName,
            size = 36.dp,
            cornerRadius = 10.dp
        )
        Text(
            text = app.appName,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        AppTypeLabel(isSystemApp = app.isSystemApp)
    }
}

@Composable
private fun ResultRow(
    result: BatchResultItem,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .squircleClip(14.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = result.appName,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            if (!result.success) {
                Text(
                    text = result.message,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
        StatusChip(
            text = if (result.success) "成功" else "失败",
            color = if (result.success) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        if (!result.success && result.op != null) {
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/** 批量执行前确认窗：展示权限 → 目标模式、目标应用数与包名预览，高危权限加警示。 */
@Composable
private fun BatchConfirmDialog(
    uiState: BatchUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val op = uiState.selectedOp ?: return
    val targets = uiState.apps.filter { it.packageName in uiState.selectedPackages }
    if (targets.isEmpty()) return

    OverlayDialog(
        show = true,
        title = "确认批量操作",
        onDismissRequest = onDismiss
    ) {
        Column {
            Text(
                text = "将对 ${targets.size} 个应用执行：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${op.displayName} → ${uiState.selectedMode.displayName}",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (op.isHighRisk) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (op.isHighRisk) {
                Text(
                    text = "⚠ 该权限属于高风险操作，可能影响应用行为或隐私",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = targets.take(5).joinToString("、") { it.appName } +
                    if (targets.size > 5) " 等 ${targets.size} 个应用" else "",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    cornerRadius = 24.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    cornerRadius = 24.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("确认执行")
                }
            }
        }
    }
}
