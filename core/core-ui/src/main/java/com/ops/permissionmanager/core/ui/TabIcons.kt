package com.ops.permissionmanager.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部 Tab 图标：直接使用 material-icons-extended 标准图标（R8 会在 release 裁剪未用图标）。
 */
object TabIcons {
    val History: ImageVector = Icons.Filled.History
    val Tune: ImageVector = Icons.Filled.Tune
    val Apps: ImageVector = Icons.Filled.GridView
}