package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord

/**
 * 纯函数解析器：把 root 命令输出解析为领域模型。
 * 不依赖任何外部环境，是单元测试的核心 Seam。
 */
object AppOpsParser {

    private val GET_LINE_REGEX = Regex("""([A-Z_]+):\s*(\w+)(?:;.*)?""")

    /**
     * 解析 `cmd appops get <package>` 输出。
     * 每行格式：`  OP_NAME: MODE` 或 `  OP_NAME: MODE; time=...`
     * 无法识别的行跳过，不整体崩溃。
     */
    fun parseGetOutput(raw: String): List<AppOpState> {
        return raw.lineSequence()
            .mapNotNull { line ->
                val match = GET_LINE_REGEX.matchEntire(line.trim()) ?: return@mapNotNull null
                val opName = match.groupValues[1]
                val modeValue = match.groupValues[2]
                val mode = OpMode.fromCommandValue(modeValue) ?: return@mapNotNull null
                val op = AppOpCatalog.find(opName) ?: return@mapNotNull null
                AppOpState(op, mode)
            }
            .toList()
    }

    /**
     * 解析 `dumpsys appops` 历史输出。
     * 定位 "Historical AppOps" 部分，解析每个权限的使用记录。
     * 格式差异较大，采用宽松解析，无法识别的条目跳过。
     */
    fun parseHistoryOutput(raw: String): List<OpUsageRecord> {
        val records = mutableListOf<OpUsageRecord>()
        val lines = raw.lineSequence().toList()

        var inHistorical = false
        var currentPackage: String? = null
        var currentOp: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.contains("Historical AppOps") -> inHistorical = true

                // 包名行：  Uid 10001: com.example.app（始终记录，供历史块使用）
                trimmed.startsWith("Uid ") && trimmed.contains(": ") -> {
                    currentPackage = trimmed.substringAfter(": ").trim()
                    currentOp = null
                }
                !inHistorical -> continue

                // 权限名行：    RUN_IN_BACKGROUND:
                trimmed.endsWith(":") && trimmed.length <= 60 && !trimmed.contains(" ") -> {
                    currentOp = trimmed.dropLast(1).trim()
                }
                // 记录行：      allow: 2026-08-17 10:00:00.123 (recent)
                trimmed.contains(":") && currentPackage != null && currentOp != null -> {
                    val timestamp = parseTimestamp(trimmed.substringAfter(":").trim())
                    if (timestamp != null) {
                        records.add(
                            OpUsageRecord(
                                packageName = currentPackage,
                                opName = currentOp,
                                timestampMillis = timestamp
                            )
                        )
                    }
                }
            }
        }
        return records
    }

    /** 宽松解析时间戳：优先取日期时间部分，失败返回 null。 */
    private fun parseTimestamp(raw: String): Long? {
        val dateTime = Regex("""\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}(?:\.\d+)?""")
            .find(raw)
            ?.value
            ?: return null
        return try {
            java.time.LocalDateTime.parse(
                dateTime,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
            ).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
