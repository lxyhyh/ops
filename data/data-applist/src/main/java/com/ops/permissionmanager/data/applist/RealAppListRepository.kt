package com.ops.permissionmanager.data.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.ops.permissionmanager.core.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 使用 PackageManager 枚举已安装应用的实现。 */
@Singleton
class RealAppListRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AppListRepository {

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val ownPackageName = context.packageName
        pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            .asSequence()
            // 过滤掉自身
            .filter { it.packageName != ownPackageName }
            // 过滤掉系统应用
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo)?.toString() ?: appInfo.packageName,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            // 按显示名称排序
            .sortedBy { it.appName.lowercase() }
            .toList()
    }
}