package com.ops.permissionmanager.feature.history

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.permissionmanager.core.model.AppOpCatalog
import com.ops.permissionmanager.core.model.OpUsageRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRoute(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        HistoryScreen(
            uiState = uiState,
            onRetry = viewModel::loadHistory,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
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
        uiState.records.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无权限使用记录")
            }
        }
        else -> {
            val grouped = uiState.records.groupBy { it.packageName }
            LazyColumn(modifier.fillMaxSize()) {
                grouped.forEach { (packageName, records) ->
                    item(key = "header_$packageName") {
                        Text(
                            text = packageName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(records, key = { "${packageName}_${it.opName}_${it.timestampMillis}" }) { record ->
                        HistoryRecordRow(record)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordRow(record: OpUsageRecord) {
    val opDisplay = AppOpCatalog.find(record.opName)?.displayName ?: record.opName
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = opDisplay,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatTime(record.timestampMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val TIME_FORMATTER = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

private fun formatTime(millis: Long): String = TIME_FORMATTER.format(Date(millis))
