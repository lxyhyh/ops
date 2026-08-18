package com.ops.permissionmanager.feature.history

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord
import com.ops.permissionmanager.data.appops.AppOpsRepository
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
 * HistoryViewModel 状态流转测试。
 *
 * 覆盖：加载成功填充记录、加载失败进 error、空记录列表。
 * 假实现穿透与生产相同的 AppOpsRepository seam。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadHistory 成功时填充记录并结束加载`() = runTest(dispatcher) {
        val records = listOf(
            OpUsageRecord("com.example.app", "RUN_IN_BACKGROUND", 1_700_000_000_000L)
        )
        val vm = HistoryViewModel(FakeHistoryRepository(records = records))

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals(records, vm.uiState.value.records)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `loadHistory 失败时进入 error 状态`() = runTest(dispatcher) {
        val vm = HistoryViewModel(FakeHistoryRepository(error = RuntimeException("no perm")))

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.records.isEmpty())
        assertTrue(vm.uiState.value.error?.contains("no perm") == true)
    }

    @Test
    fun `loadHistory 无记录时列表为空且无错误`() = runTest(dispatcher) {
        val vm = HistoryViewModel(FakeHistoryRepository(records = emptyList()))

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.records.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `重试可以恢复成功`() = runTest(dispatcher) {
        val fake = FakeHistoryRepository(error = RuntimeException("first fail"))
        val vm = HistoryViewModel(fake)

        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.error != null)

        // 修复假实现，触发重试
        fake.error = null
        fake.records = listOf(OpUsageRecord("com.a", "CAMERA", 1_700_000_000_000L))
        vm.loadHistory()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals(1, vm.uiState.value.records.size)
        assertNull(vm.uiState.value.error)
    }

    private class FakeHistoryRepository(
        var records: List<OpUsageRecord> = emptyList(),
        var error: RuntimeException? = null
    ) : AppOpsRepository {

        override suspend fun getHistory(): List<OpUsageRecord> {
            error?.let { throw it }
            return records
        }

        override suspend fun getAppOps(packageName: String) =
            throw UnsupportedOperationException("本测试不覆盖 getAppOps")

        override suspend fun setAppOp(packageName: String, op: AppOp, mode: OpMode): Result<Unit> =
            throw UnsupportedOperationException("本测试不覆盖 setAppOp")
    }
}