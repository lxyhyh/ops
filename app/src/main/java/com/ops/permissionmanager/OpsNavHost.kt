package com.ops.permissionmanager

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ops.permissionmanager.core.ui.TabIcons
import com.ops.permissionmanager.feature.applist.AppDetailRoute
import com.ops.permissionmanager.feature.applist.AppListRoute
import com.ops.permissionmanager.feature.batch.BatchRoute
import com.ops.permissionmanager.feature.history.HistoryRoute
import com.ops.permissionmanager.feature.settings.SettingsRoute
import kotlinx.coroutines.launch

private const val TRANSITION_MS = 350

private data class TopLevelDestination(
    val id: Int,
    val label: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    // Apps/Tune/History 为自建 TabIcons（避免引入 material-icons-extended 全量库），Settings 用 icons-core
    TopLevelDestination(0, "应用", TabIcons.Apps),
    TopLevelDestination(1, "批量", TabIcons.Tune),
    TopLevelDestination(2, "历史", TabIcons.History),
    TopLevelDestination(3, "设置", Icons.Filled.Settings)
)

@Composable
fun OpsNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = modifier
    ) {
        composable("main") {
            MainScaffold(
                onAppClick = { packageName, appName ->
                    navController.navigate("app_detail/" + Uri.encode(packageName) + "/" + Uri.encode(appName))
                }
            )
        }
        composable(
            route = "app_detail/{packageName}/{appName}",
            enterTransition = {
                scaleIn(
                    animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
                    initialScale = 0.92f
                ) + fadeIn(animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                scaleOut(
                    animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
                    targetScale = 0.92f
                ) + fadeOut(animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                scaleIn(
                    animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
                    initialScale = 0.92f
                ) + fadeIn(animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                scaleOut(
                    animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
                    targetScale = 0.92f
                ) + fadeOut(animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing))
            }
        ) { entry ->
            val packageName = Uri.decode(entry.arguments?.getString("packageName") ?: "")
            val appName = Uri.decode(entry.arguments?.getString("appName") ?: "")
            AppDetailRoute(
                packageName = packageName,
                appName = appName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainScaffold(onAppClick: (String, String) -> Unit) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { topLevelDestinations.size }
    )
    val scope = rememberCoroutineScope()
    val appListState = rememberLazyListState()
    val batchListState = rememberLazyListState()
    val historyListState = rememberLazyListState()

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> AppListRoute(onAppClick = onAppClick, listState = appListState)
                1 -> BatchRoute(listState = batchListState)
                2 -> HistoryRoute(listState = historyListState)
                3 -> SettingsRoute()
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 48.dp) // 悬浮胶囊底栏左右 48dp（用户反馈 16dp 视觉过大，恢复原尺寸）
                .navigationBarsPadding()
                .padding(bottom = 12.dp), // 距底 12dp
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            shadowElevation = 8.dp
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(4.dp)) {
                val f = maxWidth / topLevelDestinations.size
                val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                // MIUI X：选中指示胶囊用 Monet 主题色 15% 底衬（跟随壁纸动态取色）
                Box(
                    modifier = Modifier
                        .offset(x = f * position)
                        .width(f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                )
                Row(Modifier.fillMaxWidth()) {
                    topLevelDestinations.forEachIndexed { index, dest ->
                        val selected = pagerState.currentPage == index
                        NavPill(
                            destination = dest,
                            selected = selected,
                            onClick = {
                                if (pagerState.currentPage != index) {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                } else {
                                    val target = when (index) {
                                        0 -> appListState
                                        1 -> batchListState
                                        2 -> historyListState
                                        else -> null
                                    }
                                    scope.launch { target?.animateScrollToItem(0) }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavPill(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            if (selected) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = destination.label,
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold // MIUI X：选中标签加粗（对齐 miuix NavigationBarItem）
                )
            }
        }
    }
}