package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 appops 命令的真实数据仓库实现。
 *
 * 通过 [CommandExecutor]（内部为 [CommandExecutorRouter]，按修改模式选择 Root / Shizuku）
 * 执行 `cmd appops` 与 `dumpsys` 命令，并用 [AppOpsParser] 解析输出。
 */
@Singleton
class RealAppOpsRepository @Inject constructor(
    private val commandExecutor: CommandExecutor
) : AppOpsRepository {

    override suspend fun getAppOps(packageName: String): AppOpsState {
        if (packageName.isBlank()) throw AppOpsError.InvalidPackage

        val result = commandExecutor.execute("cmd appops get $packageName")
        if (result.exitCode != 0) {
            throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
        }

        // 按操作名去重，返回包下全部操作状态
        val states = AppOpsParser.parseGetOutput(result.stdout)
            .distinctBy { it.op.name }

        return AppOpsState(packageName, states)
    }

    override suspend fun setAppOp(packageName: String, op: AppOp, mode: OpMode): Result<Unit> {
        if (packageName.isBlank()) {
            return Result.failure(AppOpsError.InvalidPackage)
        }
        return runCatching {
            val result = commandExecutor.execute(
                "cmd appops set $packageName ${op.name} ${mode.commandValue}"
            )
            if (result.exitCode != 0) {
                throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
            }
        }
    }

    override suspend fun getHistory(): List<OpUsageRecord> {
        val result = commandExecutor.execute("dumpsys appops")
        if (result.exitCode != 0) {
            throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
        }
        return AppOpsParser.parseHistoryOutput(result.stdout)
    }
}