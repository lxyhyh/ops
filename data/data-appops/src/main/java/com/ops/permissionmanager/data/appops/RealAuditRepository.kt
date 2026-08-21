package com.ops.permissionmanager.data.appops

import android.content.Context
import com.ops.permissionmanager.core.model.AuditRecord
import com.ops.permissionmanager.core.model.ModifyMode
import com.ops.permissionmanager.core.model.OpMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 审计记录持久化实现（JSON 文件，本地不联网）。
 *
 * 进程内缓存最新列表，写操作经 Mutex 串行化并全量落盘（记录量小，可接受）；
 * 超过 [MAX_RECORDS] 条时丢弃最旧记录，防止文件无限膨胀。
 */
@Singleton
class RealAuditRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AuditRepository {

    private val mutex = Mutex()

    @Volatile
    private var records: List<AuditRecord>? = null

    private fun auditFile(): File {
        val dir = File(context.filesDir, "ops_cache")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "audit.json")
    }

    private suspend fun load(): List<AuditRecord> = withContext(Dispatchers.IO) {
        records ?: runCatching {
            val file = auditFile()
            if (!file.exists()) return@runCatching emptyList()
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val oldMode = OpMode.fromCommandValue(o.optString("old"))
                    val newMode = OpMode.fromCommandValue(o.optString("new"))
                    val channel = runCatching {
                        ModifyMode.valueOf(o.optString("ch"))
                    }.getOrDefault(ModifyMode.AUTO)
                    if (oldMode != null && newMode != null) {
                        add(
                            AuditRecord(
                                timestampMillis = o.optLong("t"),
                                packageName = o.optString("p"),
                                opName = o.optString("op"),
                                opDisplayName = o.optString("d"),
                                oldMode = oldMode,
                                newMode = newMode,
                                channel = channel
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList()).also { records = it }
    }

    private suspend fun save(list: List<AuditRecord>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("t", r.timestampMillis)
                    .put("p", r.packageName)
                    .put("op", r.opName)
                    .put("d", r.opDisplayName)
                    .put("old", r.oldMode.commandValue)
                    .put("new", r.newMode.commandValue)
                    .put("ch", r.channel.name)
            )
        }
        runCatching { auditFile().writeText(arr.toString()) }
    }

    override suspend fun recordChange(record: AuditRecord) {
        mutex.withLock {
            val current = load().toMutableList()
            current.add(0, record) // 最新在前
            val trimmed = if (current.size > MAX_RECORDS) current.take(MAX_RECORDS) else current
            records = trimmed
            save(trimmed)
        }
    }

    override suspend fun latestFor(packageName: String, opName: String): AuditRecord? {
        mutex.withLock {
            return load().firstOrNull { it.packageName == packageName && it.opName == opName }
        }
    }

    override suspend fun all(): List<AuditRecord> {
        mutex.withLock {
            return load()
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            records = emptyList()
            save(emptyList())
        }
    }

    private companion object {
        const val MAX_RECORDS = 500
    }
}