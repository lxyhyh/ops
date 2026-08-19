package com.ops.permissionmanager.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.permissionmanager.core.model.ModifyMode
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showModifyModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "设置",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                ArrowPreference(
                    title = "显示模式",
                    summary = "当前：${uiState.themeMode.label}",
                    onClick = { showThemeDialog = true }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                ArrowPreference(
                    title = "修改方式",
                    summary = "当前：${uiState.modifyMode.displayName}",
                    onClick = { showModifyModeDialog = true }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ArrowPreference(
                    title = "Root 权限",
                    summary = if (uiState.isRootAvailable) "已授予" else "未检测到",
                    endActions = {
                        StatusChip(
                            label = if (uiState.isRootAvailable) "可用" else "不可用",
                            ok = uiState.isRootAvailable
                        )
                    }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ArrowPreference(
                    title = "ADB (Shizuku)",
                    summary = shizukuSubtitle(uiState),
                    endActions = {
                        if (uiState.isShizukuBinderAvailable && !uiState.isShizukuPermissionGranted) {
                            TextButton(onClick = viewModel::requestShizukuPermission) {
                                Text("去授权", color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            StatusChip(
                                label = shizukuStatusLabel(uiState),
                                ok = uiState.isShizukuBinderAvailable && uiState.isShizukuPermissionGranted
                            )
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                ArrowPreference(
                    title = "应用版本",
                    summary = "OPS 权限管家",
                    endActions = {
                        Text(
                            text = "v" + uiState.versionName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ArrowPreference(
                    title = "说明",
                    summary = "通过 Root 或 ADB (Shizuku) 管理 AppOps 应用操作权限"
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ArrowPreference(
                    title = "风险提示",
                    summary = "修改系统权限可能导致应用异常或系统不稳定，请谨慎操作"
                )
            }
        }
        Spacer(Modifier.height(120.dp))
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            current = uiState.themeMode,
            onSelect = viewModel::setThemeMode,
            onDismiss = { showThemeDialog = false }
        )
    }
    if (showModifyModeDialog) {
        ModifyModeDialog(
            current = uiState.modifyMode,
            onSelect = viewModel::setModifyMode,
            onDismiss = { showModifyModeDialog = false }
        )
    }
}

private fun shizukuSubtitle(state: SettingsUiState): String = when {
    state.isShizukuBinderAvailable ->
        if (!state.isShizukuPermissionGranted) "已连接，尚未授权" else "已授权，无需 Root 即可修改"
    else -> "未启动，需安装并启动 Shizuku"
}

private fun shizukuStatusLabel(state: SettingsUiState): String = when {
    state.isShizukuBinderAvailable ->
        if (!state.isShizukuPermissionGranted) "待授权" else "已就绪"
    else -> "未启动"
}

@Composable
private fun StatusChip(label: String, ok: Boolean) {
    val color =
        if (ok) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error
    Text(
        text = label,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun ThemeModeDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("显示模式", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(mode)
                                onDismiss()
                            }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = {
                                onSelect(mode)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = mode.label,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ModifyModeDialog(
    current: ModifyMode,
    onSelect: (ModifyMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("修改方式", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                ModifyMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(mode)
                                onDismiss()
                            }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = {
                                onSelect(mode)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = mode.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
