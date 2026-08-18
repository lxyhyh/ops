package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.ModifyMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CommandExecutorRouter 单测：覆盖可用性探测（含并行化）、AUTO 路由决策、
 * 5s TTL 缓存与“双不可用回退 Root”行为。
 *
 * 并行化验证（isAnyAvailable 总耗时≈max 而非 sum）用 runBlocking + 真实 delay；
 * 其余用例用 runTest 虚拟时间。
 */
class CommandExecutorRouterTest {

    private class FakeExecutor(
        private val available: Boolean,
        private val probeDelayMs: Long = 0
    ) : CommandExecutor {
        var probeCount = 0
        var lastCommand: String? = null
        override suspend fun execute(command: String): ShellResult {
            lastCommand = command
            return ShellResult("uid=0", "", 0)
        }
        override suspend fun isAvailable(): Boolean {
            probeCount++
            if (probeDelayMs > 0) delay(probeDelayMs)
            return available
        }
    }

    private class FakeModifyModeRepository(initial: ModifyMode) : ModifyModeRepository {
        override val modifyMode = MutableStateFlow(initial)
        override fun setModifyMode(mode: ModifyMode) {
            modifyMode.value = mode
        }
    }

    private fun router(
        rootAvailable: Boolean,
        shizukuAvailable: Boolean,
        mode: ModifyMode,
        rootDelay: Long = 0,
        shizukuDelay: Long = 0
    ): Pair<CommandExecutorRouter, Pair<FakeExecutor, FakeExecutor>> {
        val root = FakeExecutor(rootAvailable, rootDelay)
        val shizuku = FakeExecutor(shizukuAvailable, shizukuDelay)
        val repo = FakeModifyModeRepository(mode)
        return CommandExecutorRouter(root, shizuku, repo) to (root to shizuku)
    }

    // ---- isAnyAvailable 语义 ----

    @Test
    fun `isAnyAvailable root 可用即返回 true`() = runTest {
        val (r, _) = router(rootAvailable = true, shizukuAvailable = false, mode = ModifyMode.AUTO)
        assertTrue(r.isAnyAvailable())
    }

    @Test
    fun `isAnyAvailable root 不可但 shizuku 可用返回 true`() = runTest {
        val (r, _) = router(rootAvailable = false, shizukuAvailable = true, mode = ModifyMode.AUTO)
        assertTrue(r.isAnyAvailable())
    }

    @Test
    fun `isAnyAvailable 两者都不可用返回 false`() = runTest {
        val (r, _) = router(rootAvailable = false, shizukuAvailable = false, mode = ModifyMode.AUTO)
        assertFalse(r.isAnyAvailable())
    }

    // ---- AUTO 路由决策 ----

    @Test
    fun `AUTO 模式 root 可用时命令走 root 执行器`() = runTest {
        val (r, pair) = router(rootAvailable = true, shizukuAvailable = false, mode = ModifyMode.AUTO)
        val (root, _) = pair
        r.execute("id")
        assertEquals("id", root.lastCommand)
    }

    @Test
    fun `AUTO 模式 root 不可用但 shizuku 可用时命令走 shizuku 执行器`() = runTest {
        val (r, pair) = router(rootAvailable = false, shizukuAvailable = true, mode = ModifyMode.AUTO)
        val (_, shizuku) = pair
        r.execute("id")
        assertEquals("id", shizuku.lastCommand)
    }

    @Test
    fun `AUTO 模式两者都不可用时回退 root 执行器且不抛异常`() = runTest {
        val (r, pair) = router(rootAvailable = false, shizukuAvailable = false, mode = ModifyMode.AUTO)
        val (root, _) = pair
        r.execute("id") // 不抛异常即通过
        assertEquals("id", root.lastCommand)
    }

    // ---- 5s TTL 缓存 ----

    @Test
    fun `可用性 5 秒 TTL 内重复查询不重复探测`() = runTest {
        val (r, pair) = router(rootAvailable = true, shizukuAvailable = true, mode = ModifyMode.AUTO)
        val (root, shizuku) = pair
        r.isAnyAvailable()
        r.isAnyAvailable()
        r.isAnyAvailable()
        assertEquals(1, root.probeCount)
        assertEquals(1, shizuku.probeCount)
    }

    // ---- 并行化：总耗时≈max 而非 sum（真实时钟） ----

    @Test
    fun `isAnyAvailable 探测并行化 总耗时显著小于串行之和`() = runBlocking {
        // 每个探测 100ms；串行=200ms、并行=100ms。断言 <190ms 证明并行生效。
        val (r, _) = router(
            rootAvailable = false, shizukuAvailable = false,
            mode = ModifyMode.AUTO, rootDelay = 100, shizukuDelay = 100
        )
        val start = System.currentTimeMillis()
        r.isAnyAvailable()
        val elapsed = System.currentTimeMillis() - start
        assertTrue("并行探测应 ≈100ms（串行≈200ms），实际 $elapsed ms", elapsed < 190)
    }
}
