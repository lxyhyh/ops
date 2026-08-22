package com.ops.permissionmanager.feature.settings

import com.ops.permissionmanager.core.model.ModifyMode
import com.ops.permissionmanager.data.appops.ExecutionAvailability
import com.ops.permissionmanager.data.appops.ModifyModeRepository
import com.ops.permissionmanager.data.appops.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * SettingsViewModel 状态流转测试（feature-settings 核心逻辑）。
 *
 * 覆盖：初始状态（主题/修改模式/版本号）、setThemeMode/setModifyMode 更新、
 * Root 可用性检测、Shizuku 监听流驱动状态更新。
 * 原则：ViewModel 只依赖接口，测试注入假实现，穿透与生产相同的 seam。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSettingsRepository(initial: ThemeMode = ThemeMode.SYSTEM) : SettingsRepository {
        private val _themeMode = MutableStateFlow(initial)
        override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
        override fun setThemeMode(mode: ThemeMode) {
            _themeMode.value = mode
        }
    }

    private class FakeModifyModeRepository(initial: ModifyMode = ModifyMode.AUTO) :
        ModifyModeRepository {
        private val _modifyMode = MutableStateFlow(initial)
        override val modifyMode: StateFlow<ModifyMode> = _modifyMode.asStateFlow()
        override fun setModifyMode(mode: ModifyMode) {
            _modifyMode.value = mode
        }
    }

    private class FakeShizukuManager(
        binder: Boolean = false,
        granted: Boolean = false
    ) : ShizukuManager {
        private val _binder = MutableStateFlow(binder)
        private val _granted = MutableStateFlow(granted)
        override val isBinderAvailable: StateFlow<Boolean> = _binder.asStateFlow()
        override val isPermissionGranted: StateFlow<Boolean> = _granted.asStateFlow()
        var requestPermissionCalls = 0
            private set
        override fun isAvailable(): Boolean = _binder.value && _granted.value
        override fun requestPermission() {
            requestPermissionCalls++
        }

        /** 测试专用：模拟授权成功（等价于 Binder requestPermission 回调 grantResult==0）。 */
        fun grantForTest() {
            _granted.value = true
        }
    }

    private fun fakeAvailability(root: Boolean = false) = object : ExecutionAvailability {
        override suspend fun isAnyAvailable(): Boolean = root
        override suspend fun isRootAvailable(): Boolean = root
        override suspend fun isShizukuAvailable(): Boolean = false
    }

    private fun viewModel(
        settingsRepository: SettingsRepository = FakeSettingsRepository(),
        availability: ExecutionAvailability = fakeAvailability(),
        modifyModeRepository: ModifyModeRepository = FakeModifyModeRepository(),
        shizukuManager: ShizukuManager = FakeShizukuManager(),
        versionName: String = "0.2.0"
    ) = SettingsViewModel(
        settingsRepository = settingsRepository,
        executionAvailability = availability,
        modifyModeRepository = modifyModeRepository,
        shizukuManager = shizukuManager,
        versionNameProvider = VersionNameProvider { versionName }
    )

    @Test
    fun `初始状态反映仓库当前值与版本号`() = runTest(dispatcher) {
        val vm = viewModel(
            settingsRepository = FakeSettingsRepository(ThemeMode.DARK),
            modifyModeRepository = FakeModifyModeRepository(ModifyMode.ROOT),
            shizukuManager = FakeShizukuManager(binder = true, granted = true),
            versionName = "9.9.9"
        )

        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(ModifyMode.ROOT, state.modifyMode)
        assertEquals("9.9.9", state.versionName)
        assertTrue(state.isShizukuBinderAvailable)
        assertTrue(state.isShizukuPermissionGranted)
    }

    @Test
    fun `setThemeMode 更新状态`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository(ThemeMode.SYSTEM)
        val vm = viewModel(settingsRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setThemeMode(ThemeMode.LIGHT)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)
        assertEquals(ThemeMode.LIGHT, repo.themeMode.value)
    }

    @Test
    fun `setModifyMode 更新状态`() = runTest(dispatcher) {
        val repo = FakeModifyModeRepository(ModifyMode.AUTO)
        val vm = viewModel(modifyModeRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setModifyMode(ModifyMode.SHIZUKU)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ModifyMode.SHIZUKU, vm.uiState.value.modifyMode)
        assertEquals(ModifyMode.SHIZUKU, repo.modifyMode.value)
    }

    @Test
    fun `checkAvailability Root 可用时更新 isRootAvailable`() = runTest(dispatcher) {
        val availability = fakeAvailability(root = true)
        val vm = viewModel(availability = availability)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isRootAvailable)
    }

    @Test
    fun `checkAvailability Root 不可用时 isRootAvailable 为 false`() = runTest(dispatcher) {
        val vm = viewModel(availability = fakeAvailability(root = false))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isRootAvailable)
    }

    @Test
    fun `requestShizukuPermission 转发到 manager`() = runTest(dispatcher) {
        val shizuku = FakeShizukuManager()
        val vm = viewModel(shizukuManager = shizuku)
        dispatcher.scheduler.advanceUntilIdle()

        vm.requestShizukuPermission()

        assertEquals(1, shizuku.requestPermissionCalls)
    }

    @Test
    fun `Shizuku 授权状态流变化驱动 uiState 更新`() = runTest(dispatcher) {
        val shizuku = FakeShizukuManager(binder = true, granted = false)
        val vm = viewModel(shizukuManager = shizuku)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isShizukuPermissionGranted)

        // 模拟授权成功：直接改底层 flow（等价于 Binder 监听回调触发）
        // 通过反射不可行，这里用 Fake 暴露的更新入口
        shizuku.grantForTest()

        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isShizukuPermissionGranted)
    }

    @Test
    fun `主题模式流变化驱动 uiState 更新`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository(ThemeMode.SYSTEM)
        val vm = viewModel(settingsRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)

        repo.setThemeMode(ThemeMode.DARK)

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }
}