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

/**
 * AppOps 命令输出解析器（纯函数）。
 *
 * 把 `cmd appops get` / `dumpsys appops` 的文本输出解析为领域模型。
 * 不在 [AppOpCatalog] 中的操作名会保留（显示原始名称），
 * 无法识别的行跳过，不整体崩溃。
 */
object AppOpsParser {

    /** 匹配 `OP_NAME: MODE(; 其他)` 形式的行。 */
    private val GET_LINE_REGEX = Regex("""([A-Z_]+):\s*(\w+)(?:;.*)?""")

    /** 匹配时间戳：`yyyy-MM-dd HH:mm:ss[.SSS]`。 */
    private val TIMESTAMP_REGEX = Regex("""\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}(?:\.\d+)?""")

    private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")

    /**
     * 解析 `cmd appops get <package>` 输出。
     * 每行形如 `  OP_NAME: MODE` 或 `  OP_NAME: MODE; time=...`。
     */
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

    /**
     * 解析 `dumpsys appops` 历史输出。
     *
     * 跟踪"当前包名"与"当前操作名"，在出现 `Access:` / `Reject:` 记录行时解析时间戳。
     * - `Uid ...:` 重置上下文；
     * - `Package xxx:` 设置当前包名；
     * - `OP_NAME (...):`（操作头）设置当前操作名；
     * - `Access:` / `Reject:` 行为一次命中记录。
     */
    fun parseHistoryOutput(raw: String): List<OpUsageRecord> {
        // 按 (包名, 操作) 去重，保留第一次出现的顺序，时间戳取最新的一次。
        val byKey = LinkedHashMap<String, OpUsageRecord>()
        var currentPackage: String? = null
        var currentOp: String? = null

        for (line in raw.lineSequence()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Uid ") -> {
                    currentPackage = null
                    currentOp = null
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
                    if (pkg != null && op != null &&
                        (trimmed.startsWith("Access:") || trimmed.startsWith("Reject:"))
                    ) {
                        val timestamp = parseTimestamp(trimmed)
                        if (timestamp != null) {
                            updateRecord(byKey, pkg, op, timestamp)
                        }
                    }
                }
            }
        }
        return byKey.values.toList()
    }

    /** 更新/新增一条历史记录：同一 (包名, 操作) 只保留时间戳最新的一条。 */
    private fun updateRecord(
        byKey: LinkedHashMap<String, OpUsageRecord>,
        pkg: String,
        op: String,
        timestamp: Long
    ) {
        val key = "$pkg\u0000$op"
        val existing = byKey[key]
        if (existing == null) {
            byKey[key] = OpUsageRecord(pkg, op, timestamp)
        } else if (timestamp > existing.timestampMillis) {
            byKey[key] = existing.copy(timestampMillis = timestamp)
        }
    }

    /**
     * 判断是否为操作名：非空且仅由大写字母、数字、下划线组成。
     */
    private fun isOpName(name: String): Boolean {
        if (name.isEmpty()) return false
        return name.all { it.isUpperCase() || it.isDigit() || it == '_' }
    }

    /** 从文本中提取时间戳（毫秒），失败返回 null。 */
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
}