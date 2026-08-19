package com.ops.permissionmanager.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.ops.permissionmanager.core.model.AppOpCatalog
import com.ops.permissionmanager.core.model.OpUsageRecord
import com.ops.permissionmanager.core.ui.CollapsingTitle
import com.ops.permissionmanager.core.ui.ErrorState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip

@Composable
fun HistoryRoute(
    listState: LazyListState = rememberLazyListState(),
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 与原版一致：外层 MainScaffold 已处理状态栏内边距，这里必须跳过系统栏 inset，
    // 否则顶部状态栏内边距会叠加两倍导致大面积空白。
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        HistoryScreen(
            uiState = uiState,
            onRetry = viewModel::loadHistory,
            listState = listState,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onRetry: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            ErrorState(message = uiState.error, onRetry = onRetry, modifier = modifier)
        }
        uiState.records.isEmpty() -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "暂无权限使用记录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "使用应用时会自动记录权限调用",
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        else -> {
            // 性能优化：历史分组结果缓存，避免每次重组都全量 groupBy。
            // 仅在 records 变化时重算（本页 records 只在加载完成后变化一次）。
            val grouped = remember(uiState.records) { uiState.records.groupBy { it.packageName } }
            // 与其它主页面（应用/批量）一致：标题使用 CollapsingTitle，
            // 下滑时字体缩小并向中间移动，且固定在顶部不随列表滚走。
            val collapsed by remember(listState) {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
            }
            Column(modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    CollapsingTitle(
                        title = "历史",
                        subtitle = "共 ${uiState.records.size} 条记录",
                        collapsed = collapsed
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    grouped.forEach { (packageName, records) ->
                        item(key = "card_$packageName") {
                            HistoryGroupCard(packageName = packageName, records = records)
                        }
                    }
                }
            }
        }
    }
}

/** 单包历史记录默认最多展示条数；超出部分折叠为“展开其余”一行，避免超大卡片全量渲染拖慢滚动。 */
private const val GROUP_DEFAULT_LIMIT = 20

@Composable
private fun HistoryGroupCard(
    packageName: String,
    records: List<OpUsageRecord>
) {
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) records else records.take(GROUP_DEFAULT_LIMIT)
    val overflow = records.size - visible.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // MIUI X：分组大卡片改用 G2 连续曲线圆角容器（squircleBackground）
            .squircleBackground(MaterialTheme.colorScheme.surfaceContainer, 20.dp)
    ) {
        Column {
            Text(
                text = packageName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            visible.forEachIndexed { index, record ->
                HistoryRecordRow(record)
                if (index != visible.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            if (overflow > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .squircleClip(10.dp)
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "展开其余 $overflow 条记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordRow(
    record: OpUsageRecord
) {
    val opDisplay = AppOpCatalog.find(record.opName)?.displayName ?: record.opName
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .squircleClip(10.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = opDisplay,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
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