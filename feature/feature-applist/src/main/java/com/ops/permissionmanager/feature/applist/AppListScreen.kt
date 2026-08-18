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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * 应用列表入口（导航目标）。
 *
 * @param onAppClick 点击某项时的回调，参数为应用包名
 */
@Composable
fun AppListRoute(
    onAppClick: (String) -> Unit,
    viewModel: AppListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppListScreen(
        uiState = uiState,
        onAppClick = onAppClick,
        onRetry = viewModel::loadApps
    )
}

/**
 * 应用列表页。
 *
 * @param onAppClick 点击某项时的回调，参数为应用包名
 * @param onRetry 加载失败后的重试回调
 */
@Composable
fun AppListScreen(
    uiState: AppListUiState,
    onAppClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    var filter by remember { mutableStateOf(AppFilter.All) }

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("加载失败：${uiState.error}")
                    Text(
                        text = "重试",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { onRetry() }
                    )
                }
            }
        }
        else -> {
            val filtered = uiState.apps.filter {
                when (filter) {
                    AppFilter.All -> true
                    AppFilter.System -> it.isSystemApp
                    AppFilter.User -> !it.isSystemApp
                }
            }
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppFilter.entries.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppListItem(app = app, onClick = { onAppClick(app.packageName) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(app: AppInfo, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = app.appName, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Text(
                text = if (app.isSystemApp) "系统" else "用户",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}