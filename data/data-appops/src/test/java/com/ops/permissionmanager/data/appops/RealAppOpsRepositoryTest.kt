package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.AuditRecord
import com.ops.permissionmanager.core.model.ModifyMode
import com.ops.permissionmanager.core.model.OpGroup
import com.ops.permissionmanager.core.model.OpMode
import java.util.Collections
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealAppOpsRepositoryTest {

    private val parser = AppOpsParser()

    @Test
    fun `getAppOps 非法包名抛 InvalidPackage 且不执行命令`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "", 0))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        assertThrowsInvalidPackage {
            repo.getAppOps("com.evil; rm -rf /")
        }

        assertTrue("非法包名不应产生任何命令执行", executor.commands.isEmpty())
    }

    @Test
    fun `getAppOps 空包名抛 InvalidPackage`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "", 0))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        assertThrowsInvalidPackage {
            repo.getAppOps("")
        }
    }

    @Test
    fun `getAppOps 合法包名执行命令并解析输出`() = runTest {
        val raw = "Uid mode: default\n  RUN_IN_BACKGROUND: allow\n"
        val executor = RecordingExecutor(ShellResult(raw, "", 0))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        val state = repo.getAppOps("com.example.app")

        assertEquals(listOf("cmd appops get com.example.app"), executor.commands)
        assertEquals("com.example.app", state.packageName)
        assertEquals(1, state.states.size)
        assertEquals("RUN_IN_BACKGROUND", state.states[0].op.name)
        assertEquals(OpMode.ALLOW, state.states[0].mode)
    }

    @Test
    fun `getAppOps 命令失败抛 CommandFailed`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "permission denied", 13))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        val error = runCatching { repo.getAppOps("com.example.app") }
            .exceptionOrNull() as AppOpsError.CommandFailed

        assertEquals(13, error.exitCode)
        assertEquals("permission denied", error.stderr)
    }

    @Test
    fun `setAppOp 组装命令并返回成功`() = runTest {
        // 单查返回含 TEST_OP 的输出（旧值 allow），不触发回退全量查询
        val raw = "Uid mode: default\n  TEST_OP: allow\n"
        val executor = RecordingExecutor(ShellResult(raw, "", 0))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        val result = repo.setAppOp("com.example.app", testOp(), OpMode.DENY)

        assertTrue(result.isSuccess)
        // 先查询旧值（审计），再执行 set
        assertEquals(
            listOf(
                "cmd appops get com.example.app TEST_OP",
                "cmd appops set com.example.app TEST_OP deny"
            ),
            executor.commands
        )
    }

    @Test
    fun `setAppOp 命令失败返回 Result failure 而非裸崩`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "bad op", 1))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        val result = repo.setAppOp("com.example.app", testOp(), OpMode.DENY)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as AppOpsError.CommandFailed
        assertEquals(1, error.exitCode)
    }

    @Test
    fun `setAppOp 非法包名抛 InvalidPackage`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "", 0))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        assertThrowsInvalidPackage {
            repo.setAppOp("com.evil; id", testOp(), OpMode.DENY)
        }
    }

    @Test
    fun `setAppOp 取消异常被重新抛出而非吞掉或包装`() = runTest {
        val executor = ThrowingExecutor(CancellationException("cancelled"))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        val caught = runCatching {
            repo.setAppOp("com.example.app", testOp(), OpMode.DENY)
        }.exceptionOrNull()

        assertTrue(
            "CancellationException 必须被原样抛出（不包装为 CommandFailed）",
            caught is CancellationException
        )
    }

    @Test
    fun `setAppOp 旧值与新值不同时写入审计`() = runTest {
        // get 查询返回 allow（旧值），set 成功
        val raw = "Uid mode: default\n  TEST_OP: allow\n"
        val executor = RecordingExecutor(ShellResult(raw, "", 0))
        val audit = FakeAuditRepository()
        val repo = RealAppOpsRepository(
            executor, parser, audit, FakeModifyModeRepository(ModifyMode.ROOT)
        )

        val result = repo.setAppOp("com.example.app", testOp(), OpMode.DENY)

        assertTrue(result.isSuccess)
        assertEquals(1, audit.records.size)
        val record = audit.records[0]
        assertEquals("com.example.app", record.packageName)
        assertEquals("TEST_OP", record.opName)
        assertEquals(OpMode.ALLOW, record.oldMode)
        assertEquals(OpMode.DENY, record.newMode)
        assertEquals(ModifyMode.ROOT, record.channel)
    }

    @Test
    fun `setAppOp 旧值与新值相同不写审计`() = runTest {
        // get 查询返回 deny（旧值），set 同为 deny
        val raw = "Uid mode: default\n  TEST_OP: deny\n"
        val executor = RecordingExecutor(ShellResult(raw, "", 0))
        val audit = FakeAuditRepository()
        val repo = RealAppOpsRepository(
            executor, parser, audit, FakeModifyModeRepository(ModifyMode.AUTO)
        )

        val result = repo.setAppOp("com.example.app", testOp(), OpMode.DENY)

        assertTrue(result.isSuccess)
        assertTrue("同值修改不应产生审计记录", audit.records.isEmpty())
    }

    @Test
    fun `setAppOp 旧值查询失败回退全量查询并写入审计`() = runTest {
        // 单查 get 失败（exitCode=1），回退全量 getAppOps 成功（旧值 allow），set 成功
        val fullRaw = "Uid mode: default\n  TEST_OP: allow\n"
        val executor = SequenceExecutor(
            listOf(
                ShellResult("", "permission denied", 1), // 单查失败
                ShellResult(fullRaw, "", 0), // 全量查询成功
                ShellResult("", "", 0) // set 成功
            )
        )
        val audit = FakeAuditRepository()
        val repo = RealAppOpsRepository(
            executor, parser, audit, FakeModifyModeRepository(ModifyMode.AUTO)
        )

        val result = repo.setAppOp("com.example.app", testOp(), OpMode.DENY)

        assertTrue(result.isSuccess)
        assertEquals(1, audit.records.size)
        assertEquals(OpMode.ALLOW, audit.records[0].oldMode)
        assertEquals(false, audit.records[0].oldModeUnknown)
        assertEquals(OpMode.DENY, audit.records[0].newMode)
    }

    @Test
    fun `setAppOp 单查与回退全量均失败时写入审计且标记旧值未知`() = runTest {
        // 单查失败 + 全量查询失败，set 成功 → 审计 oldModeUnknown=true，不阻断修改
        val executor = SequenceExecutor(
            listOf(
                ShellResult("", "permission denied", 1), // 单查失败
                ShellResult("", "denied", 1), // 全量查询失败
                ShellResult("", "", 0) // set 成功
            )
        )
        val audit = FakeAuditRepository()
        val repo = RealAppOpsRepository(
            executor, parser, audit, FakeModifyModeRepository(ModifyMode.AUTO)
        )

        val result = repo.setAppOp("com.example.app", testOp(), OpMode.DENY)

        assertTrue(result.isSuccess)
        assertEquals(1, audit.records.size)
        assertTrue("旧值未知应标记", audit.records[0].oldModeUnknown)
        assertEquals(OpMode.DENY, audit.records[0].newMode)
    }

    @Test
    fun `getHistory 执行 dumpsys 并解析`() = runTest {
        val raw = """
            Recent:
              Package com.example.app:
                RUN_IN_BACKGROUND (default):
                  Access: 2026-08-17 10:00:00
        """.trimIndent()
        val executor = RecordingExecutor(ShellResult(raw, "", 0))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        val records = repo.getHistory()

        assertEquals(listOf("dumpsys appops"), executor.commands)
        assertEquals(1, records.size)
        assertEquals("com.example.app", records[0].packageName)
    }

    @Test
    fun `getHistory TTL 内二次调用走缓存不重复执行命令`() = runTest {
        val raw = """
            Recent:
              Package com.example.app:
                RUN_IN_BACKGROUND (default):
                  Access: 2026-08-17 10:00:00
        """.trimIndent()
        val executor = RecordingExecutor(ShellResult(raw, "", 0))
        val repo = RealAppOpsRepository(executor, parser, FakeAuditRepository(), FakeModifyModeRepository(ModifyMode.AUTO))

        val first = repo.getHistory()
        val second = repo.getHistory()

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        // 性能回归保护：TTL 内二次加载不重复执行慢速 dumpsys
        assertEquals(listOf("dumpsys appops"), executor.commands)
    }

    private suspend fun assertThrowsInvalidPackage(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: AppOpsError.InvalidPackage) {
            return
        } catch (e: Exception) {
            throw AssertionError("应抛出 AppOpsError.InvalidPackage，实际抛出：$e", e)
        }
        throw AssertionError("未抛出手 AppOpsError.InvalidPackage")
    }

    private fun testOp(): AppOp =
        AppOp(name = "TEST_OP", displayName = "测试操作", group = OpGroup.BACKGROUND)

    /** 记录每次被执行的命令，并按预设结果返回。 */
    private class RecordingExecutor(private val result: ShellResult) : CommandExecutor {
        val commands: MutableList<String> = Collections.synchronizedList(mutableListOf())

        override suspend fun execute(command: String): ShellResult {
            commands.add(command)
            return result
        }

        override suspend fun isAvailable(): Boolean = true
    }

    /** 按顺序依次返回预设结果的假执行器。 */
    private class SequenceExecutor(private val results: List<ShellResult>) : CommandExecutor {
        private val index = java.util.concurrent.atomic.AtomicInteger(0)

        override suspend fun execute(command: String): ShellResult {
            val i = index.getAndIncrement()
            return results[i.coerceAtMost(results.lastIndex)]
        }

        override suspend fun isAvailable(): Boolean = true
    }

    /** 固定抛出指定异常的假执行器。 */
    private class ThrowingExecutor(private val throwable: Throwable) : CommandExecutor {

        override suspend fun execute(command: String): ShellResult {
            throw throwable
        }

        override suspend fun isAvailable(): Boolean = throw throwable
    }

    /** 内存版审计仓库，供断言。 */
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

    private class FakeModifyModeRepository(initial: ModifyMode) : ModifyModeRepository {
        override val modifyMode: StateFlow<ModifyMode> = MutableStateFlow(initial)

        override fun setModifyMode(mode: ModifyMode) {
            (modifyMode as MutableStateFlow<ModifyMode>).value = mode
        }
    }
}