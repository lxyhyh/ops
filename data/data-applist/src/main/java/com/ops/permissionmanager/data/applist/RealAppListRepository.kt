package com.ops.permissionmanager.data.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.ops.permissionmanager.core.model.AppDetailInfo
import com.ops.permissionmanager.core.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
        writeCacheFile(apps)
        apps
    }

    /**
     * 读磁盘缓存中的应用列表（用于冷启动首屏秒开），无缓存返回 null。
     * 磁盘缓存能避免每次冷启动都重新遍历 PackageManager（该操作在部分设备上可达数百毫秒）。
     */
    override suspend fun getCachedInstalledApps(): List<AppInfo>? = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        readCacheFile()
    }

    /**
     * 详情页按需查询单个应用的诊断信息。
     * 用 `PackageInfo` 补充版本/UID/目标SDK/安装时间等；失败（如包被卸载）返回 null。
     * 性能：只查单个包，不影响应用列表的轻量加载（列表页不逐应用调 getPackageInfo）。
     */
    override suspend fun getAppDetail(packageName: String): AppDetailInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val info = pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                val appInfo = info.applicationInfo ?: return@withContext null
                AppDetailInfo(
                    packageName = info.packageName,
                    appName = pm.getApplicationLabel(appInfo)?.toString() ?: packageName,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    versionName = info.versionName,
                    versionCode = info.longVersionCode,
                    uid = appInfo.uid,
                    targetSdk = appInfo.targetSdkVersion,
                    enabled = appInfo.enabled,
                    firstInstallTime = info.firstInstallTime.takeIf { it > 0 },
                    lastUpdateTime = info.lastUpdateTime.takeIf { it > 0 }
                )
            }.getOrNull()
        }

    private fun cacheFile(): File {
        val dir = File(context.filesDir, "ops_cache")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "app_list.json")
    }

    private fun readCacheFile(): List<AppInfo>? = runCatching {
        val file = cacheFile()
        if (!file.exists()) return null
        val arr = JSONArray(file.readText())
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    AppInfo(
                        packageName = o.getString("p"),
                        appName = o.getString("n"),
                        isSystemApp = o.getBoolean("s")
                    )
                )
            }
        }
    }.getOrNull()

    private fun writeCacheFile(apps: List<AppInfo>) = runCatching {
        val arr = JSONArray()
        apps.forEach { app ->
            arr.put(
                JSONObject()
                    .put("p", app.packageName)
                    .put("n", app.appName)
                    .put("s", app.isSystemApp)
            )
        }
        cacheFile().writeText(arr.toString())
    }

    private companion object {
        /** 缓存有效期：应用列表在进程内变化频率低，30s 足够避免重复查询又不至于陈旧。 */
        const val CACHE_TTL_MS = 30_000L
    }
}