package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.ModifyMode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 命令执行路由：按当前修改模式在 Root / Shizuku 执行器间选择。
 *
 * 与原版反编译逐项对齐：
 * - 构造仅注入 root/shizuku 两个执行器与模式仓库（不持有 ShizukuManager）；
 * - root/shizuku 可用性各带一个 5s TTL 的 AvailabilityCache；
 * - isAvailable 直接透传所选执行器结果（无 runCatching 包裹）；
 * - AUTO 兜底：两者均不可用时回退 RootExecutor（不抛异常）。
 */
@Singleton
class CommandExecutorRouter @Inject constructor(
    @Named("root") private val rootExecutor: CommandExecutor,
    @Named("shizuku") private val shizukuExecutor: CommandExecutor,
    private val modifyModeRepository: ModifyModeRepository
) : CommandExecutor, ExecutionAvailability {

    private val rootAvailable = AvailabilityCache()
    private val shizukuAvailable = AvailabilityCache()

    override suspend fun execute(command: String): ShellResult =
        resolveExecutor().execute(command)

    override suspend fun isAvailable(): Boolean =
        resolveExecutor().isAvailable()

    override suspend fun isAnyAvailable(): Boolean {
        // 性能优化：root/shizuku 探测并行化，启动等待从“两者之和”降为“两者较大”，
        // 语义不变（任一可用即返回 true），不影响后续逐项 isRoot/isShizuku 查询。
        return coroutineScope {
            val root = async { cachedRootAvailable() }
            val shizuku = async { cachedShizukuAvailable() }
            root.await() || shizuku.await()
        }
    }

    override suspend fun isRootAvailable(): Boolean = cachedRootAvailable()

    override suspend fun isShizukuAvailable(): Boolean = cachedShizukuAvailable()

    private suspend fun resolveExecutor(): CommandExecutor =
        when (modifyModeRepository.modifyMode.value) {
            ModifyMode.ROOT -> rootExecutor
            ModifyMode.SHIZUKU -> shizukuExecutor
            ModifyMode.AUTO ->
                if (cachedRootAvailable()) rootExecutor
                else if (cachedShizukuAvailable()) shizukuExecutor
                else rootExecutor // 与原版一致：兜底 Root，不抛异常
        }

    private suspend fun cachedRootAvailable(): Boolean =
        rootAvailable.get { rootExecutor.isAvailable() }

    private suspend fun cachedShizukuAvailable(): Boolean =
        shizukuAvailable.get { shizukuExecutor.isAvailable() }

    /** 可用性缓存：无锁快速路径 + 锁内二次检查（double-checked locking，与原版一致）。 */
    private class AvailabilityCache {

        companion object {
            const val TTL_MS = 5000L
        }

        private val mutex = Mutex()

        @Volatile
        private var cached: Boolean? = null

        @Volatile
        private var cachedAt: Long = 0

        suspend fun get(probe: suspend () -> Boolean): Boolean {
            // 无锁快速路径（与原版一致）
            val now = System.currentTimeMillis()
            val current = cached
            if (current != null && now - cachedAt < TTL_MS) return current

            return mutex.withLock {
                val now2 = System.currentTimeMillis()
                val current2 = cached
                if (current2 != null && now2 - cachedAt < TTL_MS) {
                    current2
                } else {
                    val fresh = probe()
                    cached = fresh
                    cachedAt = now2
                    fresh
                }
            }
        }
    }
}