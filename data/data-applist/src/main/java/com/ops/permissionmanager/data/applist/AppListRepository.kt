package com.ops.permissionmanager.data.applist

import com.ops.permissionmanager.core.model.AppInfo

/** 应用列表数据仓库。 */
interface AppListRepository {
    suspend fun getInstalledApps(): List<AppInfo>
}
