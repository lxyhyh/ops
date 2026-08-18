package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.ModifyMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CommandExecutorRouter @Inject constructor(
    @Named("root") private val rootExecutor: CommandExecutor,
    @Named("shizuku") private val shizukuExecutor: CommandExecutor,
    private val modifyModeRepository: ModifyModeRepository,
    private val shizukuManager: ShizukuManager
) : CommandExecutor, ExecutionAvailability {

    private val rootAvailable = AvailabilityCache()

    override suspend fun execute(command: String): ShellResult =
        resolveExecutor().execute(command)

    override suspend fun isAvailable(): Boolean {
        return runCatching { resolveExecutor().isAvailable() }.getOrDefault(false)
    }

    override suspend fun isAnyAvailable(): Boolean {
        if (cachedRootAvailable()) return true
        return isShizukuAvailable()
    }

    override suspend fun isRootAvailable(): Boolean = cachedRootAvailable()

    override suspend fun isShizukuAvailable(): Boolean =
        shizukuManager.isBinderAvailable.value && shizukuManager.isPermissionGranted.value

    private suspend fun resolveExecutor(): CommandExecutor =
        when (modifyModeRepository.modifyMode.value) {
            ModifyMode.ROOT -> rootExecutor
            ModifyMode.SHIZUKU -> shizukuExecutor
            ModifyMode.AUTO ->
                if (cachedRootAvailable()) rootExecutor
                else if (isShizukuAvailable()) shizukuExecutor
                else throw AppOpsError.CommandFailed(
                    exitCode = -1,
                    stderr = "无可用命令通道（AUTO）：Root 与 Shizuku 均不可用"
                )
        }

    private suspend fun cachedRootAvailable(): Boolean =
        rootAvailable.get { rootExecutor.isAvailable() }

    private class AvailabilityCache {

        companion object {
            const val TTL_MS = 5000L
        }

        private val mutex = Mutex()

        @Volatile
        private var cached: Boolean? = null

        @Volatile
        private var cachedAt: Long = 0

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
