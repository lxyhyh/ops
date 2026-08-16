package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.OpMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpsParserTest {

    @Test
    fun `parseGetOutput 解析标准输出`() {
        val raw = """
            Uid mode: default
              RUN_IN_BACKGROUND: allow
              READ_CLIPBOARD: deny
              POST_NOTIFICATION: ignore
              CAMERA: default
        """.trimIndent()

        val states = AppOpsParser.parseGetOutput(raw)

        assertEquals(4, states.size)
        assertEquals("RUN_IN_BACKGROUND", states[0].op.name)
        assertEquals(OpMode.ALLOW, states[0].mode)
        assertEquals(OpMode.DENY, states[1].mode)
        assertEquals(OpMode.IGNORE, states[2].mode)
        assertEquals(OpMode.DEFAULT, states[3].mode)
    }

    @Test
    fun `parseGetOutput 解析带 time 后缀的行`() {
        val raw = """
            Uid mode: default
              RUN_IN_BACKGROUND: allow; time=+1h2m3s400ms ago
        """.trimIndent()

        val states = AppOpsParser.parseGetOutput(raw)

        assertEquals(1, states.size)
        assertEquals(OpMode.ALLOW, states[0].mode)
    }

    @Test
    fun `parseGetOutput 跳过无法识别的行`() {
        val raw = """
            Uid mode: default
              UNKNOWN_OP: allow
              RUN_IN_BACKGROUND: allow
              garbage line
        """.trimIndent()

        val states = AppOpsParser.parseGetOutput(raw)

        // UNKNOWN_OP 不在目录中，garbage line 无法匹配，均被跳过
        assertEquals(1, states.size)
        assertEquals("RUN_IN_BACKGROUND", states[0].op.name)
    }

    @Test
    fun `parseGetOutput 空输入返回空列表`() {
        assertTrue(AppOpsParser.parseGetOutput("").isEmpty())
    }

    @Test
    fun `parseHistoryOutput 解析历史记录`() {
        val raw = """
            Uid 10001: com.example.app
              RUN_IN_BACKGROUND:
                allow: 2026-08-17 10:00:00.123 (recent)
              Historical AppOps (since boot):
                RUN_IN_BACKGROUND:
                  allow: 2026-08-17 08:00:00.000 (recent)
        """.trimIndent()

        val records = AppOpsParser.parseHistoryOutput(raw)

        // 只有 Historical AppOps 部分之后的记录被解析
        assertEquals(1, records.size)
        assertEquals("com.example.app", records[0].packageName)
        assertEquals("RUN_IN_BACKGROUND", records[0].opName)
        assertTrue(records[0].timestampMillis > 0)
    }

    @Test
    fun `parseHistoryOutput 空输入返回空列表`() {
        assertTrue(AppOpsParser.parseHistoryOutput("").isEmpty())
    }
}
