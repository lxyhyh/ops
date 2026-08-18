package com.ops.permissionmanager

/**
 * 主题模式。
 *
 * @property label 界面上展示的显示名称
 */
enum class ThemeMode(val label: String) {
    /** 跟随系统。 */
    SYSTEM("跟随系统"),

    /** 浅色模式。 */
    LIGHT("浅色模式"),

    /** 深色模式。 */
    DARK("深色模式"),
}