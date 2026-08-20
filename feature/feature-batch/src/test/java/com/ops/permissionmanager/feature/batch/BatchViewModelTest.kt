package com.ops.permissionmanager.feature.batch

import com.ops.permissionmanager.core.model.AppInfo
import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.OpGroup
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.data.applist.AppListRepository
import com.ops.permissionmanager.data.appops.AppOpsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BatchViewModel 状态流转测试（feature-batch 核心逻辑）。
 *
 * 覆盖：应用列表加载成功/失败、选择/全选/清空、批量执行进度与结果、取消。
 * 原则：ViewModel 只依赖接口（AppListRepository / AppOpsRepository），
 * 测试注入假实现，穿透与生产相同的 seam。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BatchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val appA = AppInfo("com.a", "AppA", isSystemApp = false)
    private val appB = AppInfo("com.b", "AppB", isSystemApp = true)

    private val testOp = AppOp("READ_CLIPBOARD", "读取剪贴板", OpGroup.BACKGROUND)

    // ---------- 应用列表加载 ----------

    @Test
    fun `loadApps 成功时填充列表并结束加载`() = runTest(dispatcher) {
        val vm = BatchViewModel(
            FakeAppListRepository(listOf(appA, appB)),
            FakeAppOpsRepository()
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals(listOf(appA, appB), vm.uiState.value.apps)
        assertEquals(null, vm.uiState.value.message)
    }

    @Test
    fun `loadApps 失败时走 message 而不动 error，与原版一致`() = runTest(dispatcher) {
        val vm = BatchViewModel(
            FakeAppListRepository(throwOnLoad = RuntimeException("boom")),
            FakeAppOpsRepository()
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals(null, vm.uiState.value.error)
        assertTrue(vm.uiState.value.message?.contains("boom") == true)
    }

    // ---------- 选择逻辑 ----------

    @Test
    fun `toggleApp 选中再取消`() = runTest(dispatcher) {
        val vm = BatchViewModel(FakeAppListRepository(listOf(appA)), FakeAppOpsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggleApp("com.a")
        assertTrue("com.a" in vm.uiState.value.selectedPackages)

        vm.toggleApp("com.a")
        assertFalse("com.a" in vm.uiState.value.selectedPackages)
    }

    @Test
    fun `selectAll 与 clearSelection`() = runTest(dispatcher) {
        val vm = BatchViewModel(FakeAppListRepository(listOf(appA, appB)), FakeAppOpsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAll()
        assertEquals(setOf("com.a", "com.b"), vm.uiState.value.selectedPackages)

        vm.clearSelection()
        assertTrue(vm.uiState.value.selectedPackages.isEmpty())
    }

    // ---------- 批量执行 ----------

    @Test
    fun `executeBatch 逐条执行并汇总进度与结果`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository()
        val vm = BatchViewModel(FakeAppListRepository(listOf(appA, appB)), repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAll()
        vm.selectOp(testOp)
        vm.selectMode(OpMode.DENY)
        vm.executeBatch()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isExecuting)
        assertEquals(2, state.total)
        assertEquals(2, state.progress)
        assertEquals(2, state.results.size)
        assertTrue(state.results.all { it.success })
        assertEquals(listOf("com.a", "com.b"), repo.setCalls)
        assertTrue(state.message?.contains("成功 2 / 2") == true)
    }

    @Test
    fun `executeBatch 未选操作或无目标时不执行`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository()
        val vm = BatchViewModel(FakeAppListRepository(listOf(appA)), repo)
        dispatcher.scheduler.advanceUntilIdle()

        // 未选择 op
        vm.selectAll()
        vm.selectMode(OpMode.DENY)
        vm.executeBatch()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.setCalls.isEmpty())

        // 清空选择后无目标
        vm.selectOp(testOp)
        vm.clearSelection()
        vm.executeBatch()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.setCalls.isEmpty())
    }

    @Test
    fun `executeBatch 部分失败时结果标记 success=false`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository(failOn = setOf("com.b"))
        val vm = BatchViewModel(FakeAppListRepository(listOf(appA, appB)), repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAll()
        vm.selectOp(testOp)
        vm.selectMode(OpMode.DENY)
        vm.executeBatch()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.results.size)
        assertEquals(true, state.results[0].success)
        assertEquals(false, state.results[1].success)
        assertTrue(state.message?.contains("成功 1 / 2") == true)
    }

    @Test
    fun `cancelBatch 取消执行并更新状态`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository(delayEachMs = 100)
        val vm = BatchViewModel(FakeAppListRepository(listOf(appA, appB, appC())), repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAll()
        vm.selectOp(testOp)
        vm.selectMode(OpMode.DENY)
        vm.executeBatch()
        dispatcher.scheduler.advanceTimeBy(150) // 执行中

        vm.cancelBatch()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isExecuting)
        assertFalse(repo.setCalls.size >= 3) // 被取消，不该跑完 3 个
        assertTrue(vm.uiState.value.message?.contains("已取消") == true)
    }

    @Test
    fun `取消异常不会被包装为结果`() = runTest(dispatcher) {
        val repo = FakeAppOpsRepository(throwCancellation = true)
        val vm = BatchViewModel(FakeAppListRepository(listOf(appA)), repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAll()
        vm.selectOp(testOp)
        vm.selectMode(OpMode.DENY)
        vm.executeBatch()
        dispatcher.scheduler.advanceUntilIdle()

        // 与反编译原版一致：协程内无 try/finally 复位 isExecuting，
        // 取消时直接消亡、不执行收尾；关键保证是取消不被包装成失败结果记录。
        assertTrue(vm.uiState.value.results.isEmpty())
        assertFalse(vm.uiState.value.results.any { it.success || !it.success })
    }

    // ---------- 假实现 ----------

    private fun appC() = AppInfo("com.c", "AppC", isSystemApp = false)

    private class FakeAppListRepository(
        private val apps: List<AppInfo> = emptyList(),
        private val throwOnLoad: RuntimeException? = null
    ) : AppListRepository {
        override suspend fun getInstalledApps(): List<AppInfo> {
            throwOnLoad?.let { throw it }
            return apps
        }

        override suspend fun getCachedInstalledApps(): List<AppInfo>? = null
    }

    private class FakeAppOpsRepository(
        private val failOn: Set<String> = emptySet(),
        private val delayEachMs: Long = 0,
        private val throwCancellation: Boolean = false
    ) : AppOpsRepository {
        val setCalls = mutableListOf<String>()

        override suspend fun getAppOps(packageName: String) =
            throw UnsupportedOperationException("本测试不覆盖 getAppOps")

        override suspend fun getHistory() =
            throw UnsupportedOperationException("本测试不覆盖 getHistory")

        override suspend fun setAppOp(
            packageName: String,
            op: AppOp,
            mode: OpMode
        ): Result<Unit> {
            setCalls.add(packageName)
            if (throwCancellation) throw CancellationException("cancelled")
            // 注意：不得用 runCatching 包裹 delay——否则会吞掉取消，
            // 破坏 cancelBatch 测试要验证的"取消传播"语义。
            if (delayEachMs > 0) delay(delayEachMs)
            return if (packageName in failOn) {
                Result.failure(AppOpsError.CommandFailed(1, "denied"))
            } else {
                Result.success(Unit)
            }
        }
    }
}