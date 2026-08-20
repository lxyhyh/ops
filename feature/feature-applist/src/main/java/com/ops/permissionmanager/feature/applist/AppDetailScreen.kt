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
import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.OpGroup
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.ui.ErrorState
import com.ops.permissionmanager.core.ui.StatusChip
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
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

    // miuix Scaffold + TopAppBar（MIUI X 顶栏，兼作 OverlayDialog 弹窗宿主）
    Scaffold(
        modifier = Modifier.statusBarsPadding(), // 详情页为独立导航目的地，需自行避让状态栏（主界面外层已处理）
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // 外层已避让状态栏，禁止 Scaffold 再叠加系统 insets 造成内容区多余空白
        topBar = {
            // 标题不重复：应用名大标题已在内容区展示，顶栏只保留返回箭头
            TopAppBar(
                title = "",
                color = Color.Transparent,
                defaultWindowInsetsPadding = false, // 已由外层 statusBarsPadding 处理，避免双倍内边距
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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