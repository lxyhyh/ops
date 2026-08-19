package com.ops.permissionmanager.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 原版「初音绿」主题（静态配色，禁用动态取色）。
 * 颜色完全还原自原 APK 反编译结果，深浅两套。
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF39C5BB),
    onPrimary = Color(0xFF00332F),
    primaryContainer = Color(0xFFD2F4F0),
    onPrimaryContainer = Color(0xFF0A4F4A),
    secondary = Color(0xFF5F6368),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0F0F0),
    onSecondaryContainer = Color(0xFF3A3A3A),
    tertiary = Color(0xFF2BA99F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2F4F0),
    onTertiaryContainer = Color(0xFF0A4F4A),
    background = Color(0xFFF2F2F2),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF666666),
    error = Color(0xFFFA5151),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF8C1D18),
    outline = Color(0xFFD8D8D8),
    outlineVariant = Color(0xFFE8E8E8),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF8F8F8),
    surfaceContainerHigh = Color(0xFFF0F0F0),
    surfaceContainerHighest = Color(0xFFE8E8E8),
    surfaceDim = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DD8C8),
    onPrimary = Color(0xFF00332F),
    primaryContainer = Color(0xFF0E5F5A),
    onPrimaryContainer = Color(0xFFB9F1EB),
    secondary = Color(0xFFB0B0B5),
    onSecondary = Color(0xFF2A2A2A),
    secondaryContainer = Color(0xFF3A3A3C),
    onSecondaryContainer = Color(0xFFE0E0E5),
    tertiary = Color(0xFF4DD8C8),
    onTertiary = Color(0xFF00332F),
    tertiaryContainer = Color(0xFF0E5F5A),
    onTertiaryContainer = Color(0xFFB9F1EB),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA0A0A5),
    error = Color(0xFFFF453A),
    onError = Color(0xFF1A1A1A),
    errorContainer = Color(0xFF4C1D1D),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF3A3A3C),
    outlineVariant = Color(0xFF2C2C2E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3A3C),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF333333),
    surfaceDim = Color(0xFF161616),
    surfaceContainerLow = Color(0xFF0C0C0C),
    surfaceContainerLowest = Color(0xFF121212)
)

/**
 * MIUI X 风格主题：外层用 miuix MiuixTheme 提供澎湃 UI（Monet 动态取色，初音绿 #39C5BB 种子色），
 * 内层保留 MaterialTheme 原初音绿色板，供仍使用 Material3 组件的老页面无感过渡。
 */
@Composable
fun OpsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val controller = remember(darkTheme) {
        ThemeController(
            ColorSchemeMode.MonetSystem,
            keyColor = Color(0xFF39C5BB),
            isDark = darkTheme
        )
    }
    MiuixTheme(controller = controller) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}