package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.AuditRecord
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RealAppOpsRepository @Inject constructor(
    private val commandExecutor: CommandExecutor,
    private val appOpsParser: AppOpsParser,
    private val auditRepository: AuditRepository,
    private val modifyModeRepository: ModifyModeRepository
) : AppOpsRepository {

    override suspend fun getAppOps(packageName: String): AppOpsState {
        val safe = validatePackageName(packageName)
        val result = commandExecutor.execute("cmd appops get $safe")
        if (result.exitCode != 0) {
            throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
        }
        // 性能优化：命令已在 IO 执行，剩下的是纯 CPU 解析（正则/去重），切到 Default 避免占用主线程
        val states = withContext(Dispatchers.Default) {
            appOpsParser.parseGetOutput(result.stdout)
                .distinctBy { it.op.name }
        }
        return AppOpsState(packageName, states)
    }

    override suspend fun setAppOp(packageName: String, op: AppOp, mode: OpMode): Result<Unit> {
        val safe = validatePackageName(packageName)
        return try {
            // 审计：修改前先查当前模式作为旧值（查询失败不阻断修改，旧值记 null 语义）
            val oldMode = queryCurrentMode(safe, op.name)
            val result = commandExecutor.execute(
                "cmd appops set $safe ${op.name} ${mode.commandValue}"
            )
            if (result.exitCode != 0) {
                throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
            }
            if (oldMode != null && oldMode != mode) {
                auditRepository.recordChange(
                    AuditRecord(
                        timestampMillis = System.currentTimeMillis(),
                        packageName = safe,
                        opName = op.name,
                        opDisplayName = op.displayName,
                        oldMode = oldMode,
                        newMode = mode,
                        channel = modifyModeRepository.modifyMode.value
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 查询单个权限当前模式（供审计旧值），失败或未找到返回 null。
     * 使用 `cmd appops get <pkg> <op>` 轻量单查，避免全量输出。
     */
    private suspend fun queryCurrentMode(packageName: String, opName: String): OpMode? {
        val result = commandExecutor.execute("cmd appops get $packageName $opName")
        if (result.exitCode != 0) return null
        return withContext(Dispatchers.Default) {
            appOpsParser.parseGetOutput(result.stdout).firstOrNull()?.mode
        }
    }

    /** 历史记录内存缓存（TTL 内不重复执行慢速 dumpsys）。 */
    @Volatile
    private var cachedHistory: List<OpUsageRecord>? = null

    @Volatile
    private var cachedHistoryAt: Long = 0

    override suspend fun getHistory(): List<OpUsageRecord> {
        // 性能：dumpsys appops 全量输出慢（数百 KB~MB 级），TTL 内复用结果，
        // 覆盖导航重建/错误重试等短时间重复加载场景
        val now = System.currentTimeMillis()
        cachedHistory?.let { history ->
            if (now - cachedHistoryAt < HISTORY_TTL_MS) return history
        }
        val result = commandExecutor.execute("dumpsys appops")
        if (result.exitCode != 0) {
            throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
        }
        // 性能优化：逐条记录的时间戳解析（LocalDateTime/atZone）是纯 CPU 重活，
        // 历史量大时原实现会把主线程全部占满；切到 Default 线程池执行。
        val records = withContext(Dispatchers.Default) {
            appOpsParser.parseHistoryOutput(result.stdout)
        }
        cachedHistory = records
        cachedHistoryAt = System.currentTimeMillis()
        return records
    }

    private fun validatePackageName(packageName: String): String {
        if (!PACKAGE_NAME_REGEX.matches(packageName)) {
            throw AppOpsError.InvalidPackage
        }
        return packageName
    }

    private companion object {
        val PACKAGE_NAME_REGEX = Regex("[a-zA-Z0-9._]{1,200}")

        /** 历史记录缓存有效期：60s 内重复加载不重跑 dumpsys。 */
        const val HISTORY_TTL_MS = 60_000L
    }
}
