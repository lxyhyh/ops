package com.ops.permissionmanager.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.permissionmanager.core.model.ModifyMode
import com.ops.permissionmanager.core.ui.MiuiShapes
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showThemeSheet by remember { mutableStateOf(false) }
    var showModifyModeSheet by remember { mutableStateOf(false) }

    // miuix Scaffold 提供弹窗容器（OverlayBottomSheet 依赖），透明背景不改变页面观感
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) {
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
                shape = MiuiShapes.squircle(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    // MIUI X：当前值显示在右侧（endActions），点击向下弹出选择窗
                    ArrowPreference(
                        title = "显示模式",
                        endActions = {
                            Text(
                                text = uiState.themeMode.label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = { showThemeSheet = true }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MiuiShapes.squircle(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    // MIUI X：当前值显示在右侧（endActions），点击向下弹出选择窗
                    ArrowPreference(
                        title = "修改方式",
                        endActions = {
                            Text(
                                text = uiState.modifyMode.displayName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = { showModifyModeSheet = true }
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
                shape = MiuiShapes.squircle(20.dp),
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

        if (showThemeSheet) {
            OverlayBottomSheet(
                show = true,
                title = "显示模式",
                onDismissRequest = { showThemeSheet = false }
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ChoiceRow(
                            label = mode.label,
                            selected = uiState.themeMode == mode,
                            onClick = {
                                viewModel.setThemeMode(mode)
                                showThemeSheet = false
                            }
                        )
                    }
                }
            }
        }
        if (showModifyModeSheet) {
            OverlayBottomSheet(
                show = true,
                title = "修改方式",
                onDismissRequest = { showModifyModeSheet = false }
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    ModifyMode.entries.forEach { mode ->
                        ChoiceRow(
                            label = mode.displayName,
                            description = mode.description,
                            selected = uiState.modifyMode == mode,
                            onClick = {
                                viewModel.setModifyMode(mode)
                                showModifyModeSheet = false
                            }
                        )
                    }
                }
            }
        }
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

/** MIUI X：下拉选择窗中的选项行（左侧文字，右侧初音绿勾选框）。 */
@Composable
private fun ChoiceRow(
    label: String,
    description: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Checkbox(
            state = if (selected) ToggleableState.On else ToggleableState.Off,
            onClick = onClick,
            colors = CheckboxDefaults.checkboxColors(
                checkedBackgroundColor = MaterialTheme.colorScheme.primary,
                checkedForegroundColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}