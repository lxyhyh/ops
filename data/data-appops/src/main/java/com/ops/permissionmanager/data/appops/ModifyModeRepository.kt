package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.ModifyMode
import kotlinx.coroutines.flow.StateFlow

/**
 * 权限修改方式的配置仓库。
 *
 * 暴露当前 [ModifyMode]（自动 / Root / Shizuku）的有状态流，并允许更新。
 */
interface ModifyModeRepository {
    /** 当前修改模式（StateFlow，可观察变化）。 */
    val modifyMode: StateFlow<ModifyMode>

    /** 设置修改模式。 */
    fun setModifyMode(mode: ModifyMode)
}