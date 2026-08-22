package com.ops.permissionmanager.core.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 带 TTL 的进程内缓存（double-checked locking）。
 *
 * 统一抽象了项目内三处重复的「volatile + 时间戳 + TTL」缓存策略：
 * - `CommandExecutorRouter.AvailabilityCache`（Root/Shizuku 可用性探测，5s）
 * - `RealAppOpsRepository.cachedHistory`（历史记录，60s）
 * - `RealAppListRepository.cached`（应用列表，30s）
 *
 * 语义：
 * - [get]：无锁快速路径，TTL 内返回缓存，过期/缺失返回 null；
 * - [getOrRefresh]：TTL 内返回缓存；过期或缺失时在锁内执行 [refresh]
 *   并写缓存（并发调用只刷新一次，其余等待后复用新值）；
 *   refresh 抛异常时**不写缓存**、异常透传给调用方；
 * - [peek]：不做 TTL 检查直接返回当前缓存值（供「秒开」路径使用：
 *   过期数据先展示，随后由刷新路径覆盖）。
 *
 * @param clock 时钟源（测试可注入虚拟时钟；生产默认系统时钟）。
 */
class TtlCache<T>(
    private val ttlMs: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private val mutex = Mutex()

    @Volatile
    private var cached: T? = null

    @Volatile
    private var cachedAt: Long = 0L

    /** 快速路径读取：TTL 内的有效值；过期或未缓存返回 null。 */
    fun get(): T? {
        val now = clock()
        val value = cached
        return if (value != null && now - cachedAt < ttlMs) value else null
    }

    /** 无条件读取当前缓存值（不校验 TTL）。 */
    fun peek(): T? = cached

    /**
     * 读取缓存；过期或缺失时在锁内刷新。
     * 并发下仅一个调用方执行 [refresh]，其余等待后复用新值。
     */
    suspend fun getOrRefresh(refresh: suspend () -> T): T {
        get()?.let { return it }
        return mutex.withLock {
            // 锁内二次检查：等待期间可能已被其他协程刷新
            get()?.let { return@withLock it }
            val fresh = refresh()
            cached = fresh
            cachedAt = clock()
            fresh
        }
    }
}