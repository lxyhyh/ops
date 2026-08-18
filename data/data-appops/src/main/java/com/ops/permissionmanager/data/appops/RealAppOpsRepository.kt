package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class RealAppOpsRepository @Inject constructor(
    private val commandExecutor: CommandExecutor,
    private val appOpsParser: AppOpsParser
) : AppOpsRepository {

    override suspend fun getAppOps(packageName: String): AppOpsState {
        val safe = validatePackageName(packageName)
        val result = commandExecutor.execute("cmd appops get $safe")
        if (result.exitCode != 0) {
            throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
        }
        val states = appOpsParser.parseGetOutput(result.stdout)
        return AppOpsState(packageName, states)
    }

    override suspend fun setAppOp(packageName: String, op: AppOp, mode: OpMode): Result<Unit> {
        val safe = validatePackageName(packageName)
        return try {
            val result = commandExecutor.execute(
                "cmd appops set $safe ${op.name} ${mode.commandValue}"
            )
            if (result.exitCode != 0) {
                throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistory(): List<OpUsageRecord> {
        val result = commandExecutor.execute("dumpsys appops")
        if (result.exitCode != 0) {
            throw AppOpsError.CommandFailed(result.exitCode, result.stderr)
        }
        return appOpsParser.parseHistoryOutput(result.stdout)
    }

    private fun validatePackageName(packageName: String): String {
        if (!PACKAGE_NAME_REGEX.matches(packageName)) {
            throw AppOpsError.InvalidPackage
        }
        return packageName
    }

    private companion object {
        val PACKAGE_NAME_REGEX = Regex("[a-zA-Z0-9._]{1,200}")
    }
}
