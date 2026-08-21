package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AuditRecord

/** 权限修改审计仓库：记录每次修改（旧值/新值/通道），供撤销与审计查看。 */
interface AuditRepository {

    /** 追加一条修改记录。 */
    suspend fun recordChange(record: AuditRecord)

    /** 查询某应用某权限最近一次修改记录，无则返回 null。 */
    suspend fun latestFor(packageName: String, opName: String): AuditRecord?

    /** 全部审计记录（按时间倒序）。 */
    suspend fun all(): List<AuditRecord>

    /** 清空全部审计记录。 */
    suspend fun clear()
}