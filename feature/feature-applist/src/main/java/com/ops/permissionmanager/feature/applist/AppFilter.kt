package com.ops.permissionmanager.feature.applist

/**
 * 应用列表的可搜索 / 过滤条件。
 *
 * @property label 过滤项显示名称
 */
enum class AppFilter(val label: String) {
    All("所有应用"),
    System("系统应用"),
    User("用户应用")
}