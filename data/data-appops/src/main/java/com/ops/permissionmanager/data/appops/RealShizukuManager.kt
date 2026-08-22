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
 * Shizuku 管理中心实现（绑定 rikka.shizuku 运行时）。
 *
 * - 通过 v11+ 的 Binder 机制使用 Shizuku；
 * - v11 之前则回退为启动 Shizuku 管理器应用。
 */
@Singleton
class RealShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ShizukuManager {

    private val _isBinderAvailable = MutableStateFlow(isBinderAvailableSafe())
    private val _isPermissionGranted = MutableStateFlow(isPermissionGrantedSafe())

    /** Binder 服务是否可用。 */
    override val isBinderAvailable: StateFlow<Boolean> = _isBinderAvailable.asStateFlow()

    /** 是否已获得 Shizuku 授权。 */
    override val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

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
            if (requestCode == ShizukuManager.REQUEST_CODE) {
                _isPermissionGranted.value = grantResult == 0
            }
        }
    }

    /** Binder 服务与授权都就绪时才认为 Shizuku 可用。 */
    override fun isAvailable(): Boolean =
        _isBinderAvailable.value && _isPermissionGranted.value

    /** 请求 Shizuku 授权（v11 之前引导打开管理器应用）。 */
    override fun requestPermission() {
        if (Shizuku.isPreV11()) {
            val intent = context.packageManager
                .getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return
        }
        Shizuku.requestPermission(ShizukuManager.REQUEST_CODE)
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