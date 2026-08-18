package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpsError
import com.ops.permissionmanager.core.model.OpGroup
import com.ops.permissionmanager.core.model.OpMode
import java.util.Collections
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealAppOpsRepositoryTest {

    private val parser = AppOpsParser()

    @Test
    fun `getAppOps 非法包名抛 InvalidPackage 且不执行命令`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "", 0))
        val repo = RealAppOpsRepository(executor, parser)

        assertThrowsInvalidPackage {
            repo.getAppOps("com.evil; rm -rf /")
        }

        assertTrue("非法包名不应产生任何命令执行", executor.commands.isEmpty())
    }

    @Test
    fun `getAppOps 空包名抛 InvalidPackage`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "", 0))
        val repo = RealAppOpsRepository(executor, parser)

        assertThrowsInvalidPackage {
            repo.getAppOps("")
        }
    }

    @Test
    fun `getAppOps 合法包名执行命令并解析输出`() = runTest {
        val raw = "Uid mode: default\n  RUN_IN_BACKGROUND: allow\n"
        val executor = RecordingExecutor(ShellResult(raw, "", 0))
        val repo = RealAppOpsRepository(executor, parser)

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
        val repo = RealAppOpsRepository(executor, parser)

        val error = runCatching { repo.getAppOps("com.example.app") }
            .exceptionOrNull() as AppOpsError.CommandFailed

        assertEquals(13, error.exitCode)
        assertEquals("permission denied", error.stderr)
    }

    @Test
    fun `setAppOp 组装命令并返回成功`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "", 0))
        val repo = RealAppOpsRepository(executor, parser)

        val result = repo.setAppOp("com.example.app", testOp(), OpMode.DENY)

        assertTrue(result.isSuccess)
        assertEquals(
            listOf("cmd appops set com.example.app TEST_OP deny"),
            executor.commands
        )
    }

    @Test
    fun `setAppOp 命令失败返回 Result failure 而非裸崩`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "bad op", 1))
        val repo = RealAppOpsRepository(executor, parser)

        val result = repo.setAppOp("com.example.app", testOp(), OpMode.DENY)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as AppOpsError.CommandFailed
        assertEquals(1, error.exitCode)
    }

    @Test
    fun `setAppOp 非法包名抛 InvalidPackage`() = runTest {
        val executor = RecordingExecutor(ShellResult("", "", 0))
        val repo = RealAppOpsRepository(executor, parser)

        assertThrowsInvalidPackage {
            repo.setAppOp("com.evil; id", testOp(), OpMode.DENY)
        }
    }

    @Test
    fun `setAppOp 取消异常被重新抛出而非吞掉或包装`() = runTest {
        val executor = ThrowingExecutor(CancellationException("cancelled"))
        val repo = RealAppOpsRepository(executor, parser)

        val caught = runCatching {
            repo.setAppOp("com.example.app", testOp(), OpMode.DENY)
        }.exceptionOrNull()

        assertTrue(
            "CancellationException 必须被原样抛出（不包装为 CommandFailed）",
            caught is CancellationException
        )
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
        val repo = RealAppOpsRepository(executor, parser)

        val records = repo.getHistory()

        assertEquals(listOf("dumpsys appops"), executor.commands)
        assertEquals(1, records.size)
        assertEquals("com.example.app", records[0].packageName)
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

    /** 固定抛出指定异常的假执行器。 */
    private class ThrowingExecutor(private val throwable: Throwable) : CommandExecutor {

        override suspend fun execute(command: String): ShellResult {
            throw throwable
        }

        override suspend fun isAvailable(): Boolean = throw throwable
    }
}