package com.ops.permissionmanager.feature.settings

import com.ops.permissionmanager.data.appops.ExecutionAvailability
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
 * RootCheckViewModel 环境探测测试。
 *
 * 覆盖：Root/Shizuku/任意可用判定、Shizuku 安装检测、探测异常降级、
 * 并发重入（checkAvailability 快速连续调用只保留最后一次探测结果）。
 * 原则：ViewModel 只依赖接口（ExecutionAvailability / ShizukuInstallDetector），
 * 测试注入假实现，穿透与生产相同的 seam。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RootCheckViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeAvailability(
        root: Boolean = false,
        shizuku: Boolean = false
    ) = object : ExecutionAvailability {
        override suspend fun isAnyAvailable(): Boolean = root || shizuku
        override suspend fun isRootAvailable(): Boolean = root
        override suspend fun isShizukuAvailable(): Boolean = shizuku
    }

    @Test
    fun `init 探测 Root 可用且 Shizuku 已安装`() = runTest(dispatcher) {
        val vm = RootCheckViewModel(
            executionAvailability = fakeAvailability(root = true, shizuku = false),
            shizukuInstallDetector = ShizukuInstallDetector { true }
        )

        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isChecking)
        assertTrue(state.isRootAvailable)
        assertFalse(state.isShizukuAvailable)
        assertTrue(state.isShizukuInstalled)
        assertTrue(state.isAnyAvailable)
    }

    @Test
    fun `init 探测仅 Shizuku 可用`() = runTest(dispatcher) {
        val vm = RootCheckViewModel(
            executionAvailability = fakeAvailability(root = false, shizuku = true),
            shizukuInstallDetector = ShizukuInstallDetector { false }
        )

        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isShizukuAvailable)
        assertFalse(state.isShizukuInstalled)
        assertTrue(state.isAnyAvailable)
        assertFalse(state.isRootAvailable)
    }

    @Test
    fun `全部不可用时 isAnyAvailable 为 false`() = runTest(dispatcher) {
        val vm = RootCheckViewModel(
            executionAvailability = fakeAvailability(),
            shizukuInstallDetector = ShizukuInstallDetector { false }
        )

        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isChecking)
        assertFalse(state.isRootAvailable)
        assertFalse(state.isShizukuAvailable)
        assertFalse(state.isShizukuInstalled)
        assertFalse(state.isAnyAvailable)
    }

    @Test
    fun `探测异常时降级为不可用而非崩溃`() = runTest(dispatcher) {
        val throwing = object : ExecutionAvailability {
            override suspend fun isAnyAvailable(): Boolean = throw IllegalStateException("boom")
            override suspend fun isRootAvailable(): Boolean = throw IllegalStateException("boom")
            override suspend fun isShizukuAvailable(): Boolean = throw IllegalStateException("boom")
        }
        val vm = RootCheckViewModel(
            executionAvailability = throwing,
            shizukuInstallDetector = ShizukuInstallDetector { true }
        )

        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isChecking)
        assertFalse(state.isAnyAvailable)
        assertTrue(state.isShizukuInstalled)
    }

    @Test
    fun `重入时取消旧探测 旧结果不覆盖新结果`() = runTest(dispatcher) {
        // 第一次探测慢（500ms，返回 root=true）；
        // 重入后的第二次探测快（10ms，返回 root=false，行为不同）。
        // 若旧任务未被取消：第二次先完成写 false，第一次后完成写 true → 最终 root=true（错误）；
        // 若旧任务被取消：只有第二次完成 → 最终 root=false（正确）。
        var callCount = 0
        val availability = object : ExecutionAvailability {
            override suspend fun isAnyAvailable(): Boolean = isRootAvailable()
            override suspend fun isRootAvailable(): Boolean {
                callCount++
                return if (callCount == 1) {
                    delay(500)
                    true
                } else {
                    delay(10)
                    false
                }
            }
            override suspend fun isShizukuAvailable(): Boolean = false
        }
        val vm = RootCheckViewModel(
            executionAvailability = availability,
            shizukuInstallDetector = ShizukuInstallDetector { false }
        )

        // 先让 init 的第一次探测真正启动并挂起在 delay(500)（否则它会在入队后被 cancel，从未执行）
        dispatcher.scheduler.runCurrent()
        assertEquals(1, callCount)
        assertTrue(vm.uiState.value.isChecking)

        // 重入（内部 cancel 旧任务 + 启动新任务）
        vm.checkAvailability()
        dispatcher.scheduler.advanceUntilIdle()

        // 最后一次探测正常完成、状态可用、不被旧探测覆盖
        val state = vm.uiState.value
        assertFalse(state.isChecking)
        assertFalse(state.isRootAvailable) // 新探测（第二次）结果生效
        assertEquals(2, callCount) // 旧任务虽被取消，两次探测各发起一次
    }
}