package com.ops.permissionmanager.data.appops

import kotlinx.coroutines.flow.StateFlow

/**
 * Shizuku 管理中心接口（可测试 seam）。
 *
 * 维护 Shizuku 服务可用性（Binder 连接）与授权状态，
 * 通过 StateFlow 暴露可观察状态，并提供请求授权的能力。
 */
interface ShizukuManager {

    /** Binder 服务是否可用。 */
    val isBinderAvailable: StateFlow<Boolean>

    /** 是否已获得 Shizuku 授权。 */
    val isPermissionGranted: StateFlow<Boolean>

    /** Binder 服务与授权都就绪时才认为 Shizuku 可用。 */
    fun isAvailable(): Boolean

    /** 请求 Shizuku 授权（v11 之前引导打开管理器应用）。 */
    fun requestPermission()

    companion object {
        /** 请求授权的 Request Code。 */
        const val REQUEST_CODE = 10001

        /** Shizuku 管理器（授权提供方）的包名。 */
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}