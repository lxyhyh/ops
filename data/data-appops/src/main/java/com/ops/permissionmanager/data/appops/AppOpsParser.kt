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
            .distinctBy { it.op.name }
            .toList()

    fun parseHistoryOutput(raw: String): List<OpUsageRecord> {
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
        private val TIMESTAMP_REGEX = Regex("""\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?""")
        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
        private val INSTANCE by lazy { AppOpsParser() }
        fun parseGetOutput(raw: String): List<AppOpState> = INSTANCE.parseGetOutput(raw)
        fun parseHistoryOutput(raw: String): List<OpUsageRecord> = INSTANCE.parseHistoryOutput(raw)
    }
}
