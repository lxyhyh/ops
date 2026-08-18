package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord
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

        // garbage line 无法匹配被跳过；UNKNOWN_OP 不在目录中但保留（显示原始名称）
        assertEquals(2, states.size)
        assertEquals("UNKNOWN_OP", states[0].op.name)
        assertEquals("UNKNOWN_OP", states[0].op.displayName)
        assertEquals("RUN_IN_BACKGROUND", states[1].op.name)
    }

    @Test
    fun `parseGetOutput 识别系统实际输出的定位权限名称`() {
        val raw = """
            Uid mode: default
              FINE_LOCATION: allow
              COARSE_LOCATION: deny
        """.trimIndent()

        val states = AppOpsParser.parseGetOutput(raw)

        assertEquals(2, states.size)
        assertEquals("FINE_LOCATION", states[0].op.name)
        assertEquals("精确定位", states[0].op.displayName)
        assertEquals("COARSE_LOCATION", states[1].op.name)
    }

    @Test
    fun `parseGetOutput 新增权限均能识别为中文`() {
        val raw = """
            Uid mode: default
              READ_CALL_LOG: allow
              WRITE_CALL_LOG: deny
              RECEIVE_EMERGENCY_BROADCAST: allow
              RECEIVE_SOUNDTRIGGER_AUDIO: ignore
              SYSTEM_EXEMPT_FROM_ACTIVITY_BG_START_RESTRICTION: allow
        """.trimIndent()

        val states = AppOpsParser.parseGetOutput(raw)

        assertEquals(5, states.size)
        assertEquals("读取通话记录", states[0].op.displayName)
        assertEquals("写入通话记录", states[1].op.displayName)
        assertEquals("接收紧急广播", states[2].op.displayName)
        assertEquals("接收声音触发音频", states[3].op.displayName)
        assertEquals("免于后台启动限制", states[4].op.displayName)
    }

    @Test
    fun `parseGetOutput 空输入返回空列表`() {
        assertTrue(AppOpsParser.parseGetOutput("").isEmpty())
    }

    @Test
    fun `parseHistoryOutput 解析历史记录`() {
        val raw = """
            Recent:
              Package com.example.app:
                RUN_IN_BACKGROUND (default):
                  Access: 2026-08-17 10:00:00.123
                  Reject: 2026-08-17 08:00:00.000
                READ_CLIPBOARD (allow):
                  Access: 2026-08-17 09:00:00.000
              Package com.other.app:
                CAMERA (deny):
                  Reject: 2026-08-17 06:00:00.000
            Uid 10001: com.example.app
        """.trimIndent()

        val records = AppOpsParser.parseHistoryOutput(raw)

        assertEquals(3, records.size)
        assertEquals("com.example.app", records[0].packageName)
        assertEquals("RUN_IN_BACKGROUND", records[0].opName)
        assertEquals(OpUsageRecord("com.example.app", "RUN_IN_BACKGROUND", 0).copy(timestampMillis = records[0].timestampMillis), records[0])
        assertEquals("com.example.app", records[1].packageName)
        assertEquals("READ_CLIPBOARD", records[1].opName)
        assertEquals("com.other.app", records[2].packageName)
        assertEquals("CAMERA", records[2].opName)
        assertTrue(records[0].timestampMillis > 0)
    }

    @Test
    fun `parseHistoryOutput 空输入返回空列表`() {
        assertTrue(AppOpsParser.parseHistoryOutput("").isEmpty())
    }
}
