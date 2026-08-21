package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpsParserTest {

    private val parser = AppOpsParser()

    @Test
    fun `parseGetOutput 解析标准输出`() {
        val raw = """
            Uid mode: default
              RUN_IN_BACKGROUND: allow
              READ_CLIPBOARD: deny
              POST_NOTIFICATION: ignore
              CAMERA: default
        """.trimIndent()

        val states = parser.parseGetOutput(raw)

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

        val states = parser.parseGetOutput(raw)

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

        val states = parser.parseGetOutput(raw)

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

        val states = parser.parseGetOutput(raw)

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

        val states = parser.parseGetOutput(raw)

        assertEquals(5, states.size)
        assertEquals("读取通话记录", states[0].op.displayName)
        assertEquals("写入通话记录", states[1].op.displayName)
        assertEquals("接收紧急广播", states[2].op.displayName)
        assertEquals("接收声音触发音频", states[3].op.displayName)
        assertEquals("免于后台启动限制", states[4].op.displayName)
    }

    @Test
    fun `parseGetOutput 不去重（去重由仓库层按 op name 完成）`() {
        val raw = """
            Uid mode: default
              RUN_IN_BACKGROUND: allow
              CAMERA: deny
              RUN_IN_BACKGROUND: ignore
        """.trimIndent()

        val states = parser.parseGetOutput(raw)

        // 与原版一致：Parser 原样输出，重复行保留，去重交由 RealAppOpsRepository
        assertEquals(3, states.size)
        assertEquals("RUN_IN_BACKGROUND", states[0].op.name)
        assertEquals(OpMode.ALLOW, states[0].mode)
        assertEquals("CAMERA", states[1].op.name)
        assertEquals("RUN_IN_BACKGROUND", states[2].op.name)
        assertEquals(OpMode.IGNORE, states[2].mode)
    }

    @Test
    fun `parseGetOutput 空输入返回空列表`() {
        assertTrue(parser.parseGetOutput("").isEmpty())
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

        val records = parser.parseHistoryOutput(raw)

        // 与原版一致：每条 Access/Reject 独立保留，不去重、不合并
        assertEquals(4, records.size)
        assertEquals("com.example.app", records[0].packageName)
        assertEquals("RUN_IN_BACKGROUND", records[0].opName)
        // 解析器增强后携带 accessType（原断言不含新字段，此处显式匹配）
        assertEquals(
            OpUsageRecord(
                "com.example.app",
                "RUN_IN_BACKGROUND",
                records[0].timestampMillis,
                accessType = "Access"
            ),
            records[0]
        )
        assertEquals("com.example.app", records[1].packageName)
        assertEquals("RUN_IN_BACKGROUND", records[1].opName)
        assertEquals("com.example.app", records[2].packageName)
        assertEquals("READ_CLIPBOARD", records[2].opName)
        assertEquals("com.other.app", records[3].packageName)
        assertEquals("CAMERA", records[3].opName)
        assertTrue(records[0].timestampMillis > 0)
        assertTrue(records[0].timestampMillis >= records[1].timestampMillis)
    }

    @Test
    fun `parseHistoryOutput 空输入返回空列表`() {
        assertTrue(parser.parseHistoryOutput("").isEmpty())
    }

    @Test
    fun `parseHistoryOutput 无毫秒时间戳可解析`() {
        val raw = """
            Recent:
              Package com.example.app:
                RUN_IN_BACKGROUND (default):
                  Access: 2026-08-17 10:00:00
        """.trimIndent()

        val records = parser.parseHistoryOutput(raw)

        assertEquals(1, records.size)
        assertEquals(epochMillis("2026-08-17 10:00:00"), records[0].timestampMillis)
    }

    @Test
    fun `parseHistoryOutput 一位或两位毫秒不满足严格格式时优雅跳过不崩溃`() {
        val raw = """
            Recent:
              Package com.example.app:
                OP_A (default):
                  Access: 2026-08-17 10:00:00.1
                OP_B (default):
                  Access: 2026-08-17 10:00:00
                OP_C (default):
                  Access: 2026-08-17 10:00:00.12
        """.trimIndent()

        val records = parser.parseHistoryOutput(raw)

        assertEquals(1, records.size)
        assertEquals("OP_B", records[0].opName)
        assertEquals(epochMillis("2026-08-17 10:00:00"), records[0].timestampMillis)
    }

    @Test
    fun `parseHistoryOutput 超过三位毫秒无法解析则跳过该条`() {
        val raw = """
            Recent:
              Package com.example.app:
                RUN_IN_BACKGROUND (default):
                  Access: 2026-08-17 10:00:00.123456
        """.trimIndent()

        val records = parser.parseHistoryOutput(raw)

        // 与原版一致：正则 \d+ 捕获全部小数位，但格式器 [.SSS] 无法解析 >3 位，
        // 解析失败即跳过该条（不崩溃、不静默截断）
        assertEquals(0, records.size)
    }

    @Test
    fun `parseHistoryOutput 带时区不合法时间戳被跳过不崩溃`() {
        val raw = """
            Recent:
              Package com.example.app:
                RUN_IN_BACKGROUND (default):
                  Access: not-a-timestamp
                READ_CLIPBOARD (allow):
                  Access: 2026-08-17 09:00:00
        """.trimIndent()

        val records = parser.parseHistoryOutput(raw)

        assertEquals(1, records.size)
        assertEquals("READ_CLIPBOARD", records[0].opName)
    }

    @Test
    fun `parseHistoryOutput 提取 UID 与访问类型和次数`() {
        // Android 13+ 典型格式：Uid 段头 + 括号时间戳 + 次数
        val raw = """
            Uid 10123:
              Package com.example.app:
                RUN_IN_BACKGROUND (default):
                  Access: [2026-08-17 10:00:00.123]3
                  Reject: [2026-08-17 11:00:00]1
        """.trimIndent()

        val records = parser.parseHistoryOutput(raw)

        assertEquals(2, records.size)
        val access = records[0]
        assertEquals("Access", access.accessType)
        assertEquals(3, access.count)
        assertEquals(10123, access.uid)
        assertEquals("com.example.app", access.packageName)
        assertEquals("RUN_IN_BACKGROUND", access.opName)
        val reject = records[1]
        assertEquals("Reject", reject.accessType)
        assertEquals(1, reject.count)
        assertEquals(10123, reject.uid)
    }

    @Test
    fun `parseHistoryOutput 无次数无 UID 时降级默认值`() {
        // 旧格式：无括号、无次数、无 Uid 段头
        val raw = """
            Package com.old.app:
              READ_PHONE_STATE (default):
                Access: 2026-08-17 10:00:00
        """.trimIndent()

        val records = parser.parseHistoryOutput(raw)

        assertEquals(1, records.size)
        assertEquals(1, records[0].count)
        assertEquals("Access", records[0].accessType)
        assertEquals(null, records[0].uid)
    }

    private fun epochMillis(dateTime: String): Long =
        LocalDateTime.parse(
            dateTime,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
        )
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}
