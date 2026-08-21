package com.ops.permissionmanager.data.applist

import com.ops.permissionmanager.core.model.AppDetailInfo
import com.ops.permissionmanager.core.model.AppInfo

/** 应用列表数据仓库。 */
interface AppListRepository {
    suspend fun getInstalledApps(): List<AppInfo>

    /** 读磁盘缓存中的应用列表（冷启动首屏秒开用），无缓存返回 null。 */
    suspend fun getCachedInstalledApps(): List<AppInfo>?

    /** 按包名查询应用详情诊断信息（版本/UID/目标SDK/安装时间等），查询失败返回 null。 */
    suspend fun getAppDetail(packageName: String): AppDetailInfo?
}
