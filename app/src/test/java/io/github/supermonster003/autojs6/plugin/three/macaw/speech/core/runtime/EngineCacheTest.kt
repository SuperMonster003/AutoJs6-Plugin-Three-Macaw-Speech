package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shared engines of roadmap D30: one load per key, shared by every user, released after the idle time, never while in
 * use; loading one key does not block another; removed models are evicted.
 * zh-CN: 路线图 D30 的共享引擎: 每个键只加载一次, 由所有使用者共享, 空闲时间后释放, 使用中绝不释放; 加载一个键不阻塞其他键; 已删除模型被逐出.
 */
class EngineCacheTest {

    private class Engine(val name: String) : Closeable {
        var closed = false
        override fun close() {
            closed = true
        }
    }

    /** Runs scheduled tasks when the test advances the clock. zh-CN: 测试推进时钟时运行已排定的任务. */
    private class ManualScheduler : IdleScheduler {
        var now = 0L
        private val tasks = ArrayList<Triple<Long, () -> Unit, BooleanArray>>()

        override fun schedule(delayMillis: Long, task: () -> Unit): () -> Unit {
            val cancelled = BooleanArray(1)
            tasks += Triple(now + delayMillis, task, cancelled)
            return { cancelled[0] = true }
        }

        fun advance(millis: Long) {
            now += millis
            val due = tasks.filter { it.first <= now }
            tasks.removeAll(due.toSet())
            due.filterNot { it.third[0] }.forEach { it.second() }
        }
    }

    private val scheduler = ManualScheduler()
    private val cache = EngineCache(300_000, scheduler)
    private val key = EngineKey("asr.a", 1, "greedy")
    private val loads = AtomicInteger(0)

    private fun load(name: String = "a") = Engine(name).also { loads.incrementAndGet() }

    @Test
    fun `users share one instance that is released after the idle time`() {
        val first = cache.acquire(key) { load() }
        val second = cache.acquire(key) { load() }
        assertSame(first.engine, second.engine)
        assertEquals(1, loads.get())
        assertTrue(cache.inUse("asr.a"))
        first.close()
        first.close()
        assertTrue("one user is left", cache.inUse("asr.a"))
        second.close()
        assertFalse(cache.inUse("asr.a"))
        scheduler.advance(299_999)
        assertFalse(first.engine.closed)
        assertEquals(listOf(key), cache.loaded())
        scheduler.advance(1)
        assertTrue(first.engine.closed)
        assertEquals(emptyList<EngineKey>(), cache.loaded())
    }

    @Test
    fun `a new user within the idle time keeps the instance`() {
        val first = cache.acquire(key) { load() }
        first.close()
        scheduler.advance(200_000)
        val again = cache.acquire(key) { load() }
        assertSame(first.engine, again.engine)
        scheduler.advance(200_000)
        assertFalse(again.engine.closed)
        again.close()
        scheduler.advance(300_000)
        assertTrue(again.engine.closed)
        val fresh = cache.acquire(key) { load("b") }
        assertEquals("b", fresh.engine.name)
        assertEquals(2, loads.get())
    }

    @Test
    fun `variants of a model are separate and eviction only drops idle ones`() {
        val greedy = cache.acquire(key) { load("greedy") }
        val hotwords = cache.acquire(key.copy(variant = "hotwords")) { load("hotwords") }
        hotwords.close()
        cache.evict("asr.a")
        assertTrue(hotwords.engine.closed)
        assertFalse(greedy.engine.closed)
        greedy.close()
        cache.trim()
        assertTrue(greedy.engine.closed)
        val other = cache.acquire(EngineKey("kws.b", 1, "kws")) { load("kws") }
        other.close()
        cache.evict("asr.a")
        assertFalse(other.engine.closed)
    }

    @Test
    fun `a failed load leaves nothing behind and zero idle time releases at once`() {
        assertThrows(IllegalStateException::class.java) { cache.acquire(key) { error("bad model") } }
        assertFalse(cache.inUse("asr.a"))
        assertEquals(emptyList<EngineKey>(), cache.loaded())
        val immediate = EngineCache(0, scheduler)
        val lease = immediate.acquire(key) { load() }
        lease.close()
        assertTrue(lease.engine.closed)
    }

    @Test
    fun `the memory budget closes the idle instances released longest ago`() {
        val budget = EngineCache(300_000, scheduler, budgetBytes = 1000)
        val asr = budget.acquire(EngineKey("asr.a", 1, "greedy"), 400) { load("asr") }
        val kws = budget.acquire(EngineKey("kws.a", 1, "kws"), 300) { load("kws") }
        kws.close()
        asr.close()
        assertEquals(700L, budget.loadedBytes())
        val tts = budget.acquire(EngineKey("tts.a", 1, "tts"), 500) { load("tts") }
        assertTrue("the instance released first goes", kws.engine.closed)
        assertFalse(asr.engine.closed)
        assertEquals(900L, budget.loadedBytes())
        val big = budget.acquire(EngineKey("tts.b", 1, "tts"), 800) { load("big") }
        assertTrue(asr.engine.closed)
        assertFalse("an instance in use is never closed, the load still happens", tts.engine.closed)
        assertEquals(1300L, budget.loadedBytes())
        assertEquals(setOf("tts.a", "tts.b"), budget.loaded().map { it.modelId }.toSet())
        tts.close()
        big.close()
        val again = budget.acquire(EngineKey("tts.a", 1, "tts"), 500) { load("tts") }
        assertSame("a loaded key is reused without eviction", tts.engine, again.engine)
        again.close()
    }

    @Test
    fun `loading one key does not block another`() {
        val real = EngineCache(1000, IdleScheduler.of(Executors.newSingleThreadScheduledExecutor()))
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val slow = pool.submit<EngineCache.Lease<Engine>> {
            real.acquire(EngineKey("big", 1, "x")) {
                started.countDown()
                release.await(5, TimeUnit.SECONDS)
                Engine("big")
            }
        }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        val quick = pool.submit<EngineCache.Lease<Engine>> { real.acquire(EngineKey("small", 1, "x")) { Engine("small") } }
        assertEquals("small", quick.get(5, TimeUnit.SECONDS).engine.name)
        release.countDown()
        assertEquals("big", slow.get(5, TimeUnit.SECONDS).engine.name)
        pool.shutdown()
    }
}
