package com.ops.permissionmanager.feature.settings

/**
 * 应用版本号提供者（可测试 seam）。
 *
 * 原实现直接读 PackageManager；抽为接口后 ViewModel 不再依赖 Android Context，
 * 测试可注入固定值。
 */
fun interface VersionNameProvider {
    fun versionName(): String
}