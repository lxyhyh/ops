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

@Singleton
class RealAppListRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AppListRepository {

    /** 进程内应用列表缓存：应用列表 + 批量页会各自加载，共享一份避免重复 IO/内存双份。 */
    @Volatile
    private var cached: List<AppInfo>? = null

    @Volatile
    private var cachedAt: Long = 0

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cached?.let { list ->
            if (now - cachedAt < CACHE_TTL_MS) {
                return@withContext list
            }
        }
        val pm = context.packageManager
        // 与原版一致：不过滤自身包名，全部已安装应用都展示。
        val apps = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo)?.toString() ?: appInfo.packageName,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.appName.lowercase() }
        cached = apps
        cachedAt = now
        apps
    }

    private companion object {
        /** 缓存有效期：应用列表在进程内变化频率低，30s 足够避免重复查询又不至于陈旧。 */
        const val CACHE_TTL_MS = 30_000L
    }
}
