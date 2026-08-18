package com.ops.permissionmanager.data.appops

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shizuku 管理中心。
 *
 * 负责维护 Shizuku 服务可用性（Binder 连接）与授权状态，
 * 通过 StateFlow 暴露可观察状态，并提供请求授权的能力。
 *
 * - 通过 v11+ 的 Binder 机制使用 Shizuku；
 * - v11 之前则回退为启动 Shizuku 管理器应用。
 */
@Singleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** 请求授权的 Request Code。 */
        const val REQUEST_CODE = 10001

        /** Shizuku 管理器（授权提供方）的包名。 */
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    private val _isBinderAvailable = MutableStateFlow(isBinderAvailableSafe())
    private val _isPermissionGranted = MutableStateFlow(isPermissionGrantedSafe())

    /** Binder 服务是否可用。 */
    val isBinderAvailable: StateFlow<Boolean> = _isBinderAvailable.asStateFlow()

    /** 是否已获得 Shizuku 授权。 */
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    init {
        Shizuku.addBinderReceivedListenerSticky {
            _isBinderAvailable.value = true
            _isPermissionGranted.value = isPermissionGrantedSafe()
        }
        Shizuku.addBinderDeadListener {
            _isBinderAvailable.value = false
            _isPermissionGranted.value = false
        }
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                _isPermissionGranted.value = grantResult == 0
            }
        }
    }

    /** Binder 服务与授权都就绪时才认为 Shizuku 可用。 */
    fun isAvailable(): Boolean =
        _isBinderAvailable.value && _isPermissionGranted.value

    /** 请求 Shizuku 授权（v11 之前引导打开管理器应用）。 */
    fun requestPermission() {
        if (Shizuku.isPreV11()) {
            val intent = context.packageManager
                .getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return
        }
        Shizuku.requestPermission(REQUEST_CODE)
    }

    /** 安全地探测 Binder 可用性，异常时返回 false。 */
    private fun isBinderAvailableSafe(): Boolean = runCatching {
        Shizuku.pingBinder()
    }.getOrDefault(false)

    /** 安全地检查授权状态，异常时返回 false。 */
    private fun isPermissionGrantedSafe(): Boolean = runCatching {
        Shizuku.checkSelfPermission() == 0
    }.getOrDefault(false)
}