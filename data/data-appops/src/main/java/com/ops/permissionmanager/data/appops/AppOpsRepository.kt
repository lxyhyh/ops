package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord

/**
 * AppOps 数据仓库（核心深模块）。
 * 界面层只依赖此接口，命令构造、解析、错误处理全部藏在实现内部。
 */
interface AppOpsRepository {
    /** 获取某应用全部权限状态。 */
    suspend fun getAppOps(packageName: String): AppOpsState

    /** 修改某应用某权限的模式。 */
    suspend fun setAppOp(packageName: String, op: AppOp, mode: OpMode): Result<Unit>

    /** 获取权限使用历史记录。 */
    suspend fun getHistory(): List<OpUsageRecord>
}
