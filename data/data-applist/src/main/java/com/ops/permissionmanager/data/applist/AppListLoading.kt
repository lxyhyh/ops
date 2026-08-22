package com.ops.permissionmanager.data.applist

import com.ops.permissionmanager.core.model.AppInfo

/**
 * 双阶段加载封装：先展示磁盘缓存（首屏秒开），再刷新为最新列表。
 *
 * AppList / Batch 两个页面共享同一加载策略，避免各 ViewModel 重复实现
 * 「先 getCachedInstalledApps 再 getInstalledApps」的顺序约束。
 *
 * - [onCached]：有有效磁盘缓存时调用（无缓存/已过期则不调用）；
 * - [onFresh]：始终调用一次，参数为最新列表；此调用可能抛异常，由调用方负责捕获。
 */
suspend fun AppListRepository.loadCachedThenFresh(
    onCached: (List<AppInfo>) -> Unit,
    onFresh: (List<AppInfo>) -> Unit
) {
    getCachedInstalledApps()?.let(onCached)
    onFresh(getInstalledApps())
}