package com.ops.permissionmanager.feature.settings

/**
 * Shizuku 管理器应用安装检测（可测试 seam）。
 *
 * 原实现直接查 PackageManager；抽为接口后 RootCheckViewModel 不再依赖
 * Android Context，测试可注入固定结果。
 */
fun interface ShizukuInstallDetector {
    fun isInstalled(): Boolean
}