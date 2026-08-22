package com.ops.permissionmanager.core.common

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TtlCache 行为测试：TTL 判过、刷新时机、并发去重、异常不写缓存、peek 语义。
 */
class TtlCacheTest {

    /** 虚拟时钟：测试内手动推进，避免依赖真实时间。 */
    private class VirtualClock {
        var now: Long = 0L
        fun advance(ms: Long) {
            now += ms
        }
    }

    private fun cacheWith(clock: VirtualClock, ttlMs: Long = 1000) =
        TtlCache<String>(ttlMs = ttlMs, clock = { clock.now })

    @Test
    fun `get 在 TTL 内返回缓存值`() = runTest {
        val clock = VirtualClock()
        val cache = cacheWith(clock)
        cache.getOrRefresh { "v1" }
        clock.advance(999)
        assertEquals("v1", cache.get())
    }

    @Test
    fun `get 过 TTL 后返回 null`() = runTest {
        val clock = VirtualClock()
        val cache = cacheWith(clock)
        cache.getOrRefresh { "v1" }
        clock.advance(1000)
        assertNull(cache.get())
    }

    @Test
    fun `peek 不过期检查 始终返回当前缓存值`() = runTest {
        val clock = VirtualClock()
        val cache = cacheWith(clock)
        cache.getOrRefresh { "v1" }
        clock.advance(5000)
        assertEquals("v1", cache.peek())
    }

    @Test
    fun `getOrRefresh 首次调用执行 refresh 并缓存`() = runTest {
        val clock = VirtualClock()
        val cache = cacheWith(clock)
        val refreshes = AtomicInteger(0)
        val value = cache.getOrRefresh {
            refreshes.incrementAndGet()
            "fresh"
        }
        assertEquals("fresh", value)
        assertEquals(1, refreshes.get())
        assertEquals("fresh", cache.get())
    }

    @Test
    fun `getOrRefresh 在 TTL 内不重复执行 refresh`() = runTest {
        val clock = VirtualClock()
        val cache = cacheWith(clock)
        val refreshes = AtomicInteger(0)
        cache.getOrRefresh { refreshes.incrementAndGet(); "v1" }
        val second = cache.getOrRefresh { refreshes.incrementAndGet(); "v2" }
        assertEquals("v1", second)
        assertEquals(1, refreshes.get())
    }

    @Test
    fun `getOrRefresh 过 TTL 后重新执行 refresh`() = runTest {
        val clock = VirtualClock()
        val cache = cacheWith(clock)
        val refreshes = AtomicInteger(0)
        cache.getOrRefresh { refreshes.incrementAndGet(); "v1" }
        clock.advance(1000)
        val second = cache.getOrRefresh { refreshes.incrementAndGet(); "v2" }
        assertEquals("v2", second)
        assertEquals(2, refreshes.get())
    }

    @Test
    fun `getOrRefresh 并发调用只执行一次 refresh`() = runTest {
        val clock = VirtualClock()
        val cache = cacheWith(clock)
        val refreshes = AtomicInteger(0)
        coroutineScope {
            val jobs = (1..10).map {
                async {
                    cache.getOrRefresh {
                        refreshes.incrementAndGet()
                        // 模拟慢刷新，扩大并发窗口
                        delay(10)
                        "v$it"
                    }
                }
            }
            jobs.forEach { it.await() }
        }
        assertEquals(1, refreshes.get())
        assertEquals("v1", cache.get())
    }

    @Test
    fun `getOrRefresh refresh 抛异常时透传且不写缓存`() = runTest {
        val clock = VirtualClock()
        val cache = cacheWith(clock)
        val refreshes = AtomicInteger(0)
        val error = runCatching {
            cache.getOrRefresh {
                refreshes.incrementAndGet()
                throw IllegalStateException("boom")
            }
        }
        assertTrue(error.isFailure)
        assertEquals("boom", error.exceptionOrNull()?.message)
        assertNull(cache.get())
        // 下次调用仍会尝试刷新（缓存未被污染）
        val second = cache.getOrRefresh { refreshes.incrementAndGet(); "ok" }
        assertEquals("ok", second)
        assertEquals(2, refreshes.get())
    }

    @Test
    fun `getOrRefresh 返回引用与缓存引用一致`() = runTest {
        val clock = VirtualClock()
        val cache = TtlCache<List<String>>(ttlMs = 1000, clock = { clock.now })
        val value = cache.getOrRefresh { listOf("a") }
        assertSame(value, cache.peek())
    }
}