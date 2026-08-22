package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.common.TtlCache
import com.ops.permissionmanager.core.model.ModifyMode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 命令执行路由：按当前修改模式在 Root / Shizuku 执行器间选择。
 *
 * 与原版反编译逐项对齐：
 * - 构造仅注入 root/shizuku 两个执行器与模式仓库（不持有 ShizukuManager）；
 * - root/shizuku 可用性各带一个 5s TTL 的缓存（[TtlCache]，double-checked locking）；
 * - isAvailable 直接透传所选执行器结果（无 runCatching 包裹）；
 * - AUTO 兜底：两者均不可用时回退 RootExecutor（不抛异常）。
 */
@Singleton
class CommandExecutorRouter @Inject constructor(
    @Named("root") private val rootExecutor: CommandExecutor,
    @Named("shizuku") private val shizukuExecutor: CommandExecutor,
    private val modifyModeRepository: ModifyModeRepository
) : CommandExecutor, ExecutionAvailability {

    private val rootAvailable = TtlCache<Boolean>(AVAILABILITY_TTL_MS)
    private val shizukuAvailable = TtlCache<Boolean>(AVAILABILITY_TTL_MS)

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
        rootAvailable.getOrRefresh { rootExecutor.isAvailable() }

    private suspend fun cachedShizukuAvailable(): Boolean =
        shizukuAvailable.getOrRefresh { shizukuExecutor.isAvailable() }

    private companion object {
        /** 可用性缓存有效期：5s 内不重复探测（su 探测/Shizuku 检查有进程开销）。 */
        const val AVAILABILITY_TTL_MS = 5000L
    }
}