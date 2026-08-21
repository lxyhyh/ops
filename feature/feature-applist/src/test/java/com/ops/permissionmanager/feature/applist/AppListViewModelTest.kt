package com.ops.permissionmanager.feature.applist

import com.ops.permissionmanager.core.model.AppInfo
import com.ops.permissionmanager.data.applist.AppListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AppListViewModel 状态流转测试（feature-applist 核心逻辑）。
 *
 * 覆盖：应用列表加载（含磁盘缓存优先）、搜索过滤（名称/包名/大小写/trim）、
 * 类型筛选（系统/用户）、搜索与筛选组合、空结果。
 * 原则：ViewModel 只依赖接口（AppListRepository），测试注入假实现，穿透与生产相同的 seam。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val appA = AppInfo("com.example.appa", "AppA", isSystemApp = false)
    private val appB = AppInfo("com.xiaomi.system", "SystemB", isSystemApp = true)
    private val appC = AppInfo("com.wechat.tencent", "微信", isSystemApp = false)

    // ---------- 加载 ----------

    @Test
    fun `loadApps 成功时填充列表，默认 All 过滤不过滤`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))

        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(3, state.apps.size)
        assertEquals(AppFilter.All, state.filter)
        assertEquals(3, state.filteredApps.size)
        assertEquals(null, state.error)
    }

    @Test
    fun `loadApps 磁盘缓存优先展示，随后刷新为最新列表`() = runTest(dispatcher) {
        val repo = FakeAppListRepository(
            installedApps = listOf(appA, appB),
            cachedApps = listOf(appA)
        )
        val vm = AppListViewModel(repo)

        // 第一次调度：缓存立即展示（首屏秒开路径）
        dispatcher.scheduler.advanceUntilIdle()
        // runTest 中两次 update 都发生，最终为最新列表
        assertEquals(listOf(appA, appB), vm.uiState.value.apps)
        assertEquals(listOf(appA, appB), vm.uiState.value.filteredApps)
    }

    @Test
    fun `loadApps 失败时进入 error 状态`() = runTest(dispatcher) {
        val vm = AppListViewModel(
            FakeAppListRepository(installedApps = emptyList(), throwOnLoad = RuntimeException("boom"))
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, vm.uiState.value.error?.contains("boom") == true)
    }

    // ---------- 搜索过滤 ----------

    @Test
    fun `setQuery 按应用名过滤且忽略大小写`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setQuery("appa")

        assertEquals(listOf("com.example.appa"), vm.uiState.value.filteredApps.map { it.packageName })
    }

    @Test
    fun `setQuery 按包名过滤`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setQuery("xiaomi")

        assertEquals(listOf("com.xiaomi.system"), vm.uiState.value.filteredApps.map { it.packageName })
    }

    @Test
    fun `setQuery 支持中文名称匹配`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setQuery("微")

        assertEquals(listOf("com.wechat.tencent"), vm.uiState.value.filteredApps.map { it.packageName })
    }

    @Test
    fun `setQuery 忽略首尾空格`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setQuery("  appa  ")

        assertEquals(listOf("com.example.appa"), vm.uiState.value.filteredApps.map { it.packageName })
    }

    @Test
    fun `setQuery 无匹配时 filteredApps 为空`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setQuery("zzz_nonexistent")

        assertTrue(vm.uiState.value.filteredApps.isEmpty())
        // 原始列表不受影响（空态由 UI 依据 filteredApps 展示）
        assertEquals(3, vm.uiState.value.apps.size)
    }

    @Test
    fun `setQuery 清空后恢复全量`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setQuery("appa")
        assertEquals(1, vm.uiState.value.filteredApps.size)

        vm.setQuery("")

        assertEquals(3, vm.uiState.value.filteredApps.size)
    }

    // ---------- 类型筛选 ----------

    @Test
    fun `setFilter 系统应用`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setFilter(AppFilter.System)

        assertEquals(listOf("com.xiaomi.system"), vm.uiState.value.filteredApps.map { it.packageName })
    }

    @Test
    fun `setFilter 用户应用`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setFilter(AppFilter.User)

        assertEquals(
            listOf("com.example.appa", "com.wechat.tencent"),
            vm.uiState.value.filteredApps.map { it.packageName }
        )
    }

    // ---------- 搜索 + 筛选组合 ----------

    @Test
    fun `setQuery 与 setFilter 组合生效`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB, appC)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setFilter(AppFilter.User)
        vm.setQuery("app")

        assertEquals(listOf("com.example.appa"), vm.uiState.value.filteredApps.map { it.packageName })
    }

    @Test
    fun `先筛选后追加搜索仍保持过滤生效`() = runTest(dispatcher) {
        val vm = AppListViewModel(FakeAppListRepository(listOf(appA, appB)))
        dispatcher.scheduler.advanceUntilIdle()

        vm.setFilter(AppFilter.System)
        assertEquals(listOf("com.xiaomi.system"), vm.uiState.value.filteredApps.map { it.packageName })

        // 保持 System 过滤下追加搜索，组合仍生效
        vm.setQuery("system")
        assertEquals(listOf("com.xiaomi.system"), vm.uiState.value.filteredApps.map { it.packageName })

        // 换一个与该筛选不匹配的关键词 → 空结果
        vm.setQuery("app")
        assertTrue(vm.uiState.value.filteredApps.isEmpty())
    }

    // ---------- 假实现 ----------

    private class FakeAppListRepository(
        private val installedApps: List<AppInfo> = emptyList(),
        private val cachedApps: List<AppInfo>? = null,
        private val throwOnLoad: RuntimeException? = null
    ) : AppListRepository {
        override suspend fun getInstalledApps(): List<AppInfo> {
            throwOnLoad?.let { throw it }
            return installedApps
        }

        override suspend fun getCachedInstalledApps(): List<AppInfo>? = cachedApps
    }
}
