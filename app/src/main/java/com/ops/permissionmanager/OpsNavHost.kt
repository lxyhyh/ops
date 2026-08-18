package com.ops.permissionmanager

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ops.permissionmanager.feature.applist.AppDetailRoute
import com.ops.permissionmanager.feature.applist.AppListRoute
import com.ops.permissionmanager.feature.batch.BatchRoute
import com.ops.permissionmanager.feature.history.HistoryRoute

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination("app_list", "应用", Icons.Filled.Apps),
    TopLevelDestination("batch", "批量", Icons.Filled.List),
    TopLevelDestination("history", "历史", Icons.Filled.History),
    TopLevelDestination("settings", "设置", Icons.Filled.Settings)
)

@Composable
fun OpsNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "app_list",
            modifier = Modifier.padding(padding)
        ) {
            composable("app_list") {
                AppListRoute(
                    onAppClick = { packageName ->
                        navController.navigate("app_detail/$packageName")
                    }
                )
            }
            composable("app_detail/{packageName}") { entry ->
                val packageName = entry.arguments?.getString("packageName").orEmpty()
                AppDetailRoute(
                    packageName = packageName,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("batch") { BatchRoute() }
            composable("history") { HistoryRoute() }
            composable("settings") { SettingsRoute() }
        }
    }
}
