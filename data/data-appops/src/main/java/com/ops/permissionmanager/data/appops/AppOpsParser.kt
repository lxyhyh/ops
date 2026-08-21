package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.AppOpCatalog
import com.ops.permissionmanager.core.model.AppOpState
import com.ops.permissionmanager.core.model.OpGroup
import com.ops.permissionmanager.core.model.OpMode
import com.ops.permissionmanager.core.model.OpUsageRecord
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * AppOps 命令输出解析器。
 *
 * 与原版反编译逐项对齐：
 * - parseGetOutput 不去重（去重由上层 RealAppOpsRepository 按 op.name 完成）；
 * - parseHistoryOutput 逐条保留 Access/Reject 记录（不去重、不合并）；
 * - 时间戳正则小数位不限（`\.\d+`）。
 */
class AppOpsParser @Inject constructor() {

    fun parseGetOutput(raw: String): List<AppOpState> =
        raw.lineSequence()
            .mapNotNull { line ->
                val match = GET_LINE_REGEX.matchEntire(line.trim()) ?: return@mapNotNull null
                val opName = match.groupValues[1]
                val modeValue = match.groupValues[2]
                val mode = OpMode.fromCommandValue(modeValue) ?: return@mapNotNull null
                val op = AppOpCatalog.find(opName) ?: AppOp(opName, opName, OpGroup.OTHER)
                AppOpState(op, mode)
            }
            .toList()

    fun parseHistoryOutput(raw: String): List<OpUsageRecord> {
        val records = mutableListOf<OpUsageRecord>()
        var currentPackage: String? = null
        var currentOp: String? = null
        var currentUid: Int? = null

        for (line in raw.lineSequence()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Uid ") -> {
                    currentPackage = null
                    currentOp = null
                    currentUid = trimmed.removePrefix("Uid").substringBefore(":").trim().toIntOrNull()
                }
                trimmed.startsWith("Package ") && trimmed.endsWith(":") -> {
                    currentPackage = trimmed.removePrefix("Package ").dropLast(1).trim()
                    currentOp = null
                }
                currentPackage != null && trimmed.endsWith("):") && trimmed.contains(" (") -> {
                    val opName = trimmed.substringBefore(" (").trim()
                    if (isOpName(opName)) currentOp = opName
                }
                else -> {
                    val pkg = currentPackage
                    val op = currentOp
                    val accessType = when {
                        trimmed.startsWith("Access:") -> "Access"
                        trimmed.startsWith("Reject:") -> "Reject"
                        else -> null
                    }
                    if (pkg != null && op != null && accessType != null) {
                        val timestamp = parseTimestamp(trimmed)
                        if (timestamp != null) {
                            // 与原版一致：每条记录独立保留，不去重
                            records.add(
                                OpUsageRecord(
                                    packageName = pkg,
                                    opName = op,
                                    timestampMillis = timestamp,
                                    count = parseCount(trimmed),
                                    accessType = accessType,
                                    uid = currentUid
                                )
                            )
                        }
                    }
                }
            }
        }
        return records
    }

    /** 解析 Access/Reject 行时间戳之后的次数（如 "[...] 3"），缺省为 1。 */
    private fun parseCount(trimmed: String): Int {
        val tsMatch = TIMESTAMP_REGEX.find(trimmed) ?: return 1
        val tail = trimmed.substring(tsMatch.range.last + 1)
        return COUNT_REGEX.find(tail)?.value?.toIntOrNull() ?: 1
    }

    private fun isOpName(name: String): Boolean {
        if (name.isEmpty()) return false
        return name.all { it.isUpperCase() || it.isDigit() || it == '_' }
    }

    private fun parseTimestamp(raw: String): Long? {
        val dateTime = TIMESTAMP_REGEX.find(raw)?.value ?: return null
        return try {
            LocalDateTime.parse(dateTime, TIMESTAMP_FORMATTER)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val GET_LINE_REGEX = Regex("""([A-Z_]+):\s*(\w+)(?:;.*)?""")
        // 与原版一致：时间戳小数位不限
        private val TIMESTAMP_REGEX = Regex("""\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}(?:\.\d+)?""")
        /** Access/Reject 行时间戳之后的次数（如 "[...] 3"）。 */
        private val COUNT_REGEX = Regex("""(\d+)""")
        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
        private val INSTANCE by lazy { AppOpsParser() }
        fun parseGetOutput(raw: String): List<AppOpState> = INSTANCE.parseGetOutput(raw)
        fun parseHistoryOutput(raw: String): List<OpUsageRecord> = INSTANCE.parseHistoryOutput(raw)
    }
}