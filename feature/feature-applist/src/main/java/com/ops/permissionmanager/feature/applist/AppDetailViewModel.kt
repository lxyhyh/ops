package com.ops.permissionmanager.feature.applist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.permissionmanager.core.model.AppDetailInfo
import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.AuditRecord
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.data.applist.AppListRepository
import com.ops.permissionmanager.data.appops.AppOpsRepository
import com.ops.permissionmanager.data.appops.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

data class AppDetailUiState(
    val isLoading: Boolean = false,
    val appOps: AppOpsState? = null,
    /** 应用详情诊断信息（版本/UID/目标SDK/安装时间等），查询失败为 null。 */
    val detail: AppDetailInfo? = null,
    /** 本应用各权限最近一次修改记录（opName → AuditRecord），用于显示与撤销。 */
    val recentAudits: Map<String, AuditRecord> = emptyMap(),
    val error: String? = null,
    val message: String? = null,
    val appName: String = ""
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val appOpsRepository: AppOpsRepository,
    private val appListRepository: AppListRepository,
    private val auditRepository: AuditRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>(ARG_PACKAGE_NAME).orEmpty()
    private val appName: String = savedStateHandle.get<String>(ARG_APP_NAME).orEmpty()

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(appName = appName) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // 权限状态、诊断信息与审计记录相互独立，并行拉取互不阻塞
                val (state, detail, audits) = coroutineScope {
                    val ops = async { appOpsRepository.getAppOps(packageName) }
                    val info = async { appListRepository.getAppDetail(packageName) }
                    val audit = async {
                        auditRepository.all()
                            .filter { it.packageName == packageName }
                            .groupBy { it.opName }
                            .mapValues { (_, records) -> records.maxBy { r -> r.timestampMillis } }
                    }
                    Triple(ops.await(), info.await(), audit.await())
                }
                _uiState.update {
                    it.copy(isLoading = false, appOps = state, detail = detail, recentAudits = audits)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * 撤销最近一次修改：将权限恢复为审计记录中的旧值。
     * 无审计记录或旧值已等于当前值时忽略。
     */
    fun undo(opState: AppOpState) {
        val audit = _uiState.value.recentAudits[opState.op.name] ?: return
        if (audit.oldMode == opState.mode) return
        viewModelScope.launch {
            val result = appOpsRepository.setAppOp(packageName, opState.op, audit.oldMode)
            ensureActive()
            result.onSuccess {
                // 与 setMode 一致：局部更新，不触发全量 reload
                applyLocalUpdate(opState.op.name, audit.oldMode)
                _uiState.update { it.copy(message = "$MSG_UNDO_SUCCESS${audit.oldMode.displayName}") }
            }.onFailure { e ->
                _uiState.update { it.copy(message = "$MSG_UNDO_FAILURE${e.message}") }
            }
        }
    }

    fun setMode(opState: AppOpState, mode: OpMode) {
        viewModelScope.launch {
            val result = appOpsRepository.setAppOp(packageName, opState.op, mode)
            ensureActive()
            result.onSuccess {
                // 局部更新：避免修改后全量 reload（省去 getAppOps/getAppDetail/审计重查，且无加载闪烁）
                applyLocalUpdate(opState.op.name, mode)
                _uiState.update { it.copy(message = "$MSG_SET_MODE_SUCCESS${mode.displayName}") }
            }.onFailure { e ->
                _uiState.update { it.copy(message = "$MSG_SET_MODE_FAILURE${e.message}") }
            }
        }
    }

    /**
     * 修改成功后就地更新权限状态与该权限的最近审计记录，
     * 替代原先的全量 load()（省 3 次数据源查询 + 消除界面闪烁）。
     */
    private suspend fun applyLocalUpdate(opName: String, newMode: OpMode) {
        val latest = auditRepository.latestFor(packageName, opName)
        _uiState.update { state ->
            state.copy(
                appOps = state.appOps?.let { ops ->
                    ops.copy(
                        states = ops.states.map {
                            if (it.op.name == opName) it.copy(mode = newMode) else it
                        }
                    )
                },
                recentAudits = latest?.let { state.recentAudits + (it.opName to it) }
                    ?: state.recentAudits
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private companion object {
        const val ARG_PACKAGE_NAME = "packageName"
        const val ARG_APP_NAME = "appName"
        const val MSG_SET_MODE_SUCCESS = "已设置为 "
        const val MSG_SET_MODE_FAILURE = "修改失败："
        const val MSG_UNDO_SUCCESS = "已恢复为 "
        const val MSG_UNDO_FAILURE = "撤销失败："
    }
}
