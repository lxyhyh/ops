package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealAppOpsRepository @Inject constructor(
    private val rootShell: RootShell
) : AppOpsRepository {

    override suspend fun getAppOps(packageName: String): AppOpsState {
        if (packageName.isBlank()) throw AppOpsError.InvalidPackage
        val result = rootShell.execute("cmd appops get $packageName")
        if (result.exitCode != 0) {
            throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
        }
        val states = AppOpsParser.parseGetOutput(result.stdout)
        return AppOpsState(packageName, states)
    }

    override suspend fun setAppOp(packageName: String, op: AppOp, mode: OpMode): Result<Unit> {
        if (packageName.isBlank()) return Result.failure(AppOpsError.InvalidPackage)
        return runCatching {
            val result = rootShell.execute("cmd appops set $packageName ${op.name} ${mode.commandValue}")
            if (result.exitCode != 0) {
                throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
            }
        }
    }

    override suspend fun getHistory(): List<OpUsageRecord> {
        val result = rootShell.execute("dumpsys appops")
        if (result.exitCode != 0) {
            throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
        }
        return AppOpsParser.parseHistoryOutput(result.stdout)
    }
}
