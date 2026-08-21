package com.ops.permissionmanager.feature.applist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.permissionmanager.core.model.AppDetailInfo
import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.OpGroup
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.ui.ErrorState
import com.ops.permissionmanager.core.ui.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip

/** 权限分组大圆角卡片。 */
private val GroupCardRadius = 20.dp

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

    // miuix Scaffold：弹窗宿主 + snackbar 容器；顶栏仅返回箭头（自定义行，避免 miuix 大标题 TopAppBar 的额外高度空白）
    Scaffold(
        modifier = Modifier.statusBarsPadding(), // 详情页为独立导航目的地，需自行避让状态栏（主界面外层已处理）
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // 外层已避让状态栏，禁止 Scaffold 再叠加系统 insets 造成内容区多余空白
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // 标准工具栏高度
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题不重复：应用名大标题已在内容区展示，顶栏只保留返回箭头
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) }
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
            ErrorState(message = uiState.error, onRetry = onRetry)
        }
        else -> {
            val state = uiState.appOps
            if (state == null || state.states.isEmpty()) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "未获取到权限数据")
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(), // 必须应用外层 modifier（含 Scaffold 顶栏 padding），否则内容区与返回按钮重叠
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "subtitle") {
                        Column {
                            Text(
                                text = uiState.appName,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = state.packageName,
                                modifier = Modifier.padding(top = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            // 应用详情诊断信息（按需查询，失败时静默隐藏）
                            uiState.detail?.let { detail ->
                                AppDetailInfoBlock(
                                    detail = detail,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    state.grouped.forEach { (group, items) ->
                        item(key = "card_${group.name}") {
                            AppGroupCard(
                                group = group,
                                items = items,
                                onRowClick = { pendingOp = it }
                            )
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
private fun AppGroupCard(
    group: OpGroup,
    items: List<AppOpState>,
    onRowClick: (AppOpState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // MIUI X：分组大卡片改用 G2 连续曲线圆角容器（squircleBackground）
            .squircleBackground(MaterialTheme.colorScheme.surfaceContainer, GroupCardRadius)
    ) {
        Column {
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
            items.forEachIndexed { index, opState ->
                AppOpRow(
                    opState = opState,
                    onClick = { onRowClick(opState) }
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppOpRow(
    opState: AppOpState,
    onClick: () -> Unit
) {
    val (labelText, labelColor) = when (opState.mode) {
        OpMode.ALLOW -> "允许" to MaterialTheme.colorScheme.primary
        OpMode.DENY -> "拒绝" to MaterialTheme.colorScheme.error
        OpMode.IGNORE -> "忽略" to MaterialTheme.colorScheme.error
        else -> opState.mode.displayName to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .squircleClip(12.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = opState.op.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        StatusChip(
            text = labelText,
            color = labelColor
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(start = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ModePickerDialog(
    opState: AppOpState,
    onDismiss: () -> Unit,
    onSelect: (OpMode) -> Unit
) {
    OverlayDialog(
        show = true,
        title = opState.op.displayName,
        onDismissRequest = onDismiss
    ) {
        Column {
            Text(
                text = "修改可能导致应用异常，请谨慎操作",
                modifier = Modifier.padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            OpMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .squircleClip(12.dp)
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
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
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/** 应用详情诊断信息：版本/UID/目标SDK/类型/启用状态/安装与更新时间，逐行小字展示。 */
@Composable
private fun AppDetailInfoBlock(
    detail: AppDetailInfo,
    modifier: Modifier = Modifier
) {
    val lines = buildList {
        // 版本行：优先 "版本 1.0.0 (100)"；无版本名时降级 "版本 (100)"
        val version = buildString {
            detail.versionName?.let { append("版本 $it") }
            detail.versionCode?.let {
                if (isNotEmpty()) append(" (") else append("版本 (")
                append(it).append(")")
            }
        }
        if (version.isNotEmpty()) add(version)

        val identity = buildList {
            detail.uid?.let { add("UID $it") }
            detail.targetSdk?.let { add("目标 SDK $it") }
        }
        if (identity.isNotEmpty()) add(identity.joinToString(" · "))

        add(if (detail.isSystemApp) "系统应用" else "用户应用")
        detail.enabled?.let { add(if (it) "已启用" else "已禁用") }
        detail.firstInstallTime?.let { add("安装于 ${formatDate(it)}") }
        detail.lastUpdateTime?.let { add("更新于 ${formatDate(it)}") }
    }

    Column(modifier = modifier) {
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val DETAIL_DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private fun formatDate(millis: Long): String = DETAIL_DATE_FORMATTER.format(Date(millis))