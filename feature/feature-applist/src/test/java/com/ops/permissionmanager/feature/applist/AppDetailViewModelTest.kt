package com.ops.permissionmanager.feature.applist

import androidx.lifecycle.SavedStateHandle
import com.ops.permissionmanager.core.model.AppDetailInfo
import com.ops.permissionmanager.core.model.AppInfo
import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.AppOpsState
import com.ops.permissionmanager.core.model.AuditRecord
import com.ops.permissionmanager.core.model.ModifyMode
import com.ops.permissionmanager.core.model.OpGroup
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.data.applist.AppListRepository
import com.ops.permissionmanager.data.appops.AppOpsRepository
import com.ops.permissionmanager.data.appops.AuditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AppDetailViewModel 状态流转测试。
 *
 * 覆盖：加载（权限+诊断+最近审计并行）、设置模式、撤销（有审计→恢复旧值；无审计→忽略）。
 * 原则：ViewModel 只依赖接口，测试注入假实现，穿透与生产相同的 seam。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val packageName = "com.example.app"
    private val appName = "示例应用"
    private val op = AppOp("READ_CLIPBOARD", "读取剪贴板", OpGroup.PRIVACY, isHighRisk = true)
    private val opState = AppOpState(op, OpMode.ALLOW)

    private fun savedState(): SavedStateHandle = SavedStateHandle(
        mapOf("packageName" to packageName, "appName" to appName)
    )

    private fun appOpsState(mode: OpMode = OpMode.ALLOW): AppOpsState =
        AppOpsState(packageName, listOf(AppOpState(op, mode)))

    // ---------- 加载 ----------

    @Test
    fun `loadApps 成功时填充权限详情与最近审计`() = runTest(dispatcher) {
        val audit = FakeAuditRepository()
        audit.records.add(
            AuditRecord(
                timestampMillis = 1000,
                packageName = packageName,
                opName = op.name,
                opDisplayName = op.displayName,
                oldMode = OpMode.DENY,
                newMode = OpMode.ALLOW,
                channel = ModifyMode.ROOT
            )
        )
        val vm = AppDetailViewModel(
            FakeAppOpsRepository(appOpsState()),
            FakeAppListRepository(AppDetailInfo(packageName, appName, false, null, null, null, null, null, null, null)),
            audit,
            savedState()
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(appName, state.appName)
        assertEquals(1, state.appOps?.states?.size)
        assertEquals(op.name, state.recentAudits[op.name]?.opName)
        assertNull(state.error)
    }

    @Test
    fun `load 失败时进入 error 状态`() = runTest(dispatcher) {
        val vm = AppDetailViewModel(
            FakeAppOpsRepository(error = RuntimeException("boom")),
            FakeAppListRepository(null),
            FakeAuditRepository(),
            savedState()
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.error?.contains("boom") == true)
    }

    // ---------- 修改 ----------

    @Test
    fun `setMode 成功后重载并提示`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository(appOpsState())
        val vm = AppDetailViewModel(
            repo,
            FakeAppListRepository(null),
            FakeAuditRepository(),
            savedState()
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.setMode(opState, OpMode.DENY)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("READ_CLIPBOARD"), repo.setOps)
        assertTrue(vm.uiState.value.message?.contains("已设置") == true)
    }

    @Test
    fun `setMode 失败时提示失败`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository(appOpsState(), failSet = true)
        val vm = AppDetailViewModel(
            repo,
            FakeAppListRepository(null),
            FakeAuditRepository(),
            savedState()
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.setMode(opState, OpMode.DENY)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.message?.contains("修改失败") == true)
    }

    // ---------- 撤销 ----------

    @Test
    fun `undo 有审计记录且旧值不同时恢复旧值`() = runTest(dispatcher) {
        val audit = FakeAuditRepository()
        audit.records.add(
            AuditRecord(
                timestampMillis = 1000,
                packageName = packageName,
                opName = op.name,
                opDisplayName = op.displayName,
                oldMode = OpMode.DENY,
                newMode = OpMode.ALLOW,
                channel = ModifyMode.AUTO
            )
        )
        val repo = FakeAppOpsRepository(appOpsState(OpMode.ALLOW))
        val vm = AppDetailViewModel(repo, FakeAppListRepository(null), audit, savedState())
        dispatcher.scheduler.advanceUntilIdle()

        vm.undo(opState)
        dispatcher.scheduler.advanceUntilIdle()

        // 撤销应重新设置为旧值 DENY
        assertEquals(listOf("READ_CLIPBOARD"), repo.setOps)
        assertEquals(OpMode.DENY, repo.setModes.single())
        assertTrue(vm.uiState.value.message?.contains("已恢复") == true)
    }

    @Test
    fun `undo 无审计记录时忽略`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository(appOpsState())
        val vm = AppDetailViewModel(
            repo,
            FakeAppListRepository(null),
            FakeAuditRepository(), // 空审计
            savedState()
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.undo(opState)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue("无审计记录不应触发 set", repo.setOps.isEmpty())
    }

    @Test
    fun `undo 旧值等于当前值时忽略`() = runTest(dispatcher) {
        val audit = FakeAuditRepository()
        audit.records.add(
            AuditRecord(
                timestampMillis = 1000,
                packageName = packageName,
                opName = op.name,
                opDisplayName = op.displayName,
                oldMode = OpMode.ALLOW,
                newMode = OpMode.ALLOW,
                channel = ModifyMode.AUTO
            )
        )
        val repo = FakeAppOpsRepository(appOpsState(OpMode.ALLOW))
        val vm = AppDetailViewModel(repo, FakeAppListRepository(null), audit, savedState())
        dispatcher.scheduler.advanceUntilIdle()

        vm.undo(opState)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo.setOps.isEmpty())
    }

    @Test
    fun `undo 旧值未知时忽略不执行`() = runTest(dispatcher) {
        val audit = FakeAuditRepository()
        audit.records.add(
            AuditRecord(
                timestampMillis = 1000,
                packageName = packageName,
                opName = op.name,
                opDisplayName = op.displayName,
                oldMode = OpMode.DENY,
                newMode = OpMode.ALLOW,
                channel = ModifyMode.AUTO,
                oldModeUnknown = true
            )
        )
        val repo = FakeAppOpsRepository(appOpsState(OpMode.ALLOW))
        val vm = AppDetailViewModel(repo, FakeAppListRepository(null), audit, savedState())
        dispatcher.scheduler.advanceUntilIdle()

        vm.undo(opState)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue("旧值未知不应触发撤销", repo.setOps.isEmpty())
    }

@Test
    fun `setMode 成功后局部更新不触发全量重查`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository(appOpsState())
        val vm = AppDetailViewModel(
            repo,
            FakeAppListRepository(null),
            FakeAuditRepository(),
            savedState()
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repo.getCalls) // init load 一次

        vm.setMode(opState, OpMode.DENY)
        dispatcher.scheduler.advanceUntilIdle()

        // 性能回归保护：修改成功后就地更新，不再全量重查（省 getAppOps/getAppDetail/审计查询）
        assertEquals(1, repo.getCalls)
        assertEquals(OpMode.DENY, vm.uiState.value.appOps?.states?.first()?.mode)
    }

    // ---------- 假实现 ----------

    private class FakeAppOpsRepository(
        private val state: AppOpsState? = null,
        private val error: RuntimeException? = null,
        private val failSet: Boolean = false
    ) : AppOpsRepository {
        val setOps = mutableListOf<String>()
        val setModes = mutableListOf<OpMode>()

        /** getAppOps 调用次数（用于断言不重复全量重查）。 */
        var getCalls = 0

        override suspend fun getAppOps(packageName: String): AppOpsState {
            getCalls++
            error?.let { throw it }
            return state ?: AppOpsState(packageName, emptyList())
        }

        override suspend fun getHistory() = emptyList<com.ops.permissionmanager.core.model.OpUsageRecord>()

        override suspend fun setAppOp(packageName: String, op: AppOp, mode: OpMode): Result<Unit> {
            setOps.add(op.name)
            setModes.add(mode)
            return if (failSet) {
                Result.failure(AppOpsError.CommandFailed(1, "denied"))
            } else {
                Result.success(Unit)
            }
        }
    }

    private class FakeAppListRepository(private val detail: AppDetailInfo?) : AppListRepository {
        override suspend fun getInstalledApps(): List<AppInfo> = emptyList()

        override suspend fun getCachedInstalledApps(): List<AppInfo>? = null

        override suspend fun getAppDetail(packageName: String): AppDetailInfo? = detail
    }

    private class FakeAuditRepository : AuditRepository {
        val records = mutableListOf<AuditRecord>()

        override suspend fun recordChange(record: AuditRecord) {
            records.add(0, record)
        }

        override suspend fun latestFor(packageName: String, opName: String): AuditRecord? =
            records.firstOrNull { it.packageName == packageName && it.opName == opName }

        override suspend fun all(): List<AuditRecord> = records.toList()

        override suspend fun clear() {
            records.clear()
        }
    }
}