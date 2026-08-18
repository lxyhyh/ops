package com.ops.permissionmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.permissionmanager.core.model.ModifyMode

/**
 * 设置页面路由入口。
 */
@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onThemeModeSelected = viewModel::setThemeMode,
        onModifyModeSelected = viewModel::setModifyMode,
        onRequestShizukuPermission = viewModel::requestShizukuPermission
    )
}

/**
 * 设置页面内容。
 *
 * 含主题模式（跟随系统 / 浅色 / 深色）与修改方式（自动 / Root / ADB）的选择，
 * 并在 Shizuku 已选但未授权时提供授权入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onModifyModeSelected: (ModifyMode) -> Unit,
    onRequestShizukuPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle("主题模式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = uiState.themeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        SectionTitle("修改方式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModifyMode.entries.forEach { mode ->
                FilterChip(
                    selected = uiState.modifyMode == mode,
                    onClick = { onModifyModeSelected(mode) },
                    label = { Text(mode.displayName) }
                )
            }
        }

        // 已选择 Shizuku 但未授权时的提示与授权入口
        if (uiState.modifyMode == ModifyMode.SHIZUKU && !uiState.isShizukuPermissionGranted) {
            Text(
                text = if (uiState.isShizukuBinderAvailable) {
                    "Shizuku 服务可用但尚未授权，请授予权限。"
                } else {
                    "未检测到 Shizuku 服务，请先启动 Shizuku 或使用 ADB 授权。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            TextButton(onClick = onRequestShizukuPermission) {
                Text("请求 Shizuku 权限")
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        Row(Modifier.fillMaxWidth()) {
            Text(
                text = "版本",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = uiState.versionName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}