package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.ModifyMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 命令执行器路由器。
 *
 * 根据 [ModifyModeRepository] 配置的修改模式（AUTO / ROOT / SHIZUKU）
 * 在 Root 与 Shizuku 执行器之间选择。
 * 对两种执行器的可用性做短时（5 秒）缓存，避免频繁探测。
 */
@Singleton
class CommandExecutorRouter @Inject constructor(
    @Named("root") private val rootExecutor: CommandExecutor,
    @Named("shizuku") private val shizukuExecutor: CommandExecutor,
    private val modifyModeRepository: ModifyModeRepository
) : CommandExecutor {

    private val rootAvailable = AvailabilityCache()
    private val shizukuAvailable = AvailabilityCache()

    override suspend fun execute(command: String): ShellResult =
        resolveExecutor().execute(command)

    override suspend fun isAvailable(): Boolean =
        resolveExecutor().isAvailable()

    /** Root 或 Shizuku 任一可用即视为整体可用。 */
    suspend fun isAnyAvailable(): Boolean {
        if (cachedRootAvailable()) return true
        return cachedShizukuAvailable()
    }

    /** 当前 Root 执行器是否可用（带缓存）。 */
    suspend fun isRootAvailable(): Boolean = cachedRootAvailable()

    /** 当前 Shizuku 执行器是否可用（带缓存）。 */
    suspend fun isShizukuAvailable(): Boolean = cachedShizukuAvailable()

    /** 按当前修改模式选择执行器。 */
    private suspend fun resolveExecutor(): CommandExecutor =
        when (modifyModeRepository.modifyMode.value) {
            ModifyMode.ROOT -> rootExecutor
            ModifyMode.SHIZUKU -> shizukuExecutor
            ModifyMode.AUTO ->
                if (cachedRootAvailable()) rootExecutor
                else if (cachedShizukuAvailable()) shizukuExecutor
                else rootExecutor
        }

    private suspend fun cachedRootAvailable(): Boolean =
        rootAvailable.get { rootExecutor.isAvailable() }

    private suspend fun cachedShizukuAvailable(): Boolean =
        shizukuAvailable.get { shizukuExecutor.isAvailable() }

    /** 可用性短时缓存。 */
    private class AvailabilityCache {

        companion object {
            /** 缓存有效期（毫秒）。 */
            const val TTL_MS = 5000L
        }

        private val mutex = Mutex()

        @Volatile
        private var cached: Boolean? = null

        @Volatile
        private var cachedAt: Long = 0

        /** 缓存未过期直接返回；否则用 [probe] 重新探测并刷新缓存。 */
        suspend fun get(probe: suspend () -> Boolean): Boolean = mutex.withLock {
            val now = System.currentTimeMillis()
            val current = cached
            if (current != null && now - cachedAt < TTL_MS) {
                current
            } else {
                val fresh = probe()
                cached = fresh
                cachedAt = now
                fresh
            }
        }
    }
}