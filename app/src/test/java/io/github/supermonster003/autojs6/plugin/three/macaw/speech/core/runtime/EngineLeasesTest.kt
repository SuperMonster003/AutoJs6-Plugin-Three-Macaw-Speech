package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Closeable

/**
 * The leases of contract version 3 (roadmap P11.2, D184, D197, D203): counted per holder up to the limit with the loads
 * in flight, released by their holder only, dropped when the holder dies, expiring `keepLoaded` minutes after their
 * session, and holding engines of [EngineCache] past its idle time.
 * zh-CN: 契约版本 3 的租约 (路线图 P11.2, D184, D197, D203): 按持有者计数 (含仍在加载的) 直到上限, 只能由持有者归还, 持有者死亡时丢弃, 在会话结束
 * `keepLoaded` 分钟后到期, 并让 [EngineCache] 的引擎越过空闲时间保持加载.
 */
class EngineLeasesTest {

    private class Use : Closeable {
        var closes = 0
        override fun close() {
            closes++
        }
    }

    private class Engine : Closeable {
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

        fun pending(): Int = tasks.count { !it.third[0] }
    }

    private val scheduler = ManualScheduler()
    private var next = 0
    private val leases = EngineLeases(scheduler, maxPerHolder = 3) { "lease-${++next}" }

    private fun expect(code: String, block: () -> Unit) {
        val error = assertThrows(SpeechException::class.java) { block() }
        assertEquals(error.message, code, error.code)
    }

    @Test
    fun `a lease is released by its holder only and closes its uses once`() {
        val use = Use()
        var ended = 0
        val id = leases.reserve(10).grant("asr.a", listOf(use)) { ended++ }
        assertEquals("lease-1", id)
        assertEquals(1, leases.count(10))
        assertEquals("asr.a", leases.modelOf(id))
        assertFalse("another holder keeps its hands off", leases.release(id, 11))
        assertEquals(0, use.closes)
        assertTrue(leases.release(id, 10))
        assertEquals(1, use.closes)
        assertEquals(1, ended)
        assertFalse("released already", leases.release(id, 10))
        assertFalse(leases.drop(id))
        assertEquals(1, use.closes)
        assertEquals(0, leases.count(10))
        assertNull(leases.modelOf(id))
    }

    @Test
    fun `the limit counts the leases and the loads in flight of each holder`() {
        leases.reserve(10).grant("asr.a", listOf(Use()))
        val loading = leases.reserve(10)
        leases.reserve(10)
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { leases.reserve(10) }
        leases.reserve(11).cancel()
        loading.cancel()
        loading.cancel()
        val again = leases.reserve(10)
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { leases.reserve(10) }
        again.grant("asr.b", listOf(Use()))
        again.cancel()
        assertEquals("a cancel after the grant changes nothing", 2, leases.count(10))
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { leases.reserve(10) }
        assertEquals("other holders are counted apart", 0, leases.count(11))
    }

    @Test
    fun `the default limit is the contract's leases per token`() {
        val defaults = EngineLeases(scheduler)
        repeat(SpeechLimits.MAX_LEASES_PER_TOKEN) { defaults.reserve(1).grant("asr.a", emptyList()) }
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { defaults.reserve(1) }
        assertTrue(defaults.ids().all { it.startsWith("lease-") })
        assertEquals(SpeechLimits.MAX_LEASES_PER_TOKEN, defaults.ids().toSet().size)
    }

    @Test
    fun `a ticket is used once`() {
        val ticket = leases.reserve(10)
        ticket.grant("asr.a", listOf(Use()))
        val late = Use()
        assertThrows(IllegalStateException::class.java) { ticket.grant("asr.a", listOf(late)) }
        assertEquals("the uses of a refused grant are closed", 1, late.closes)
        assertEquals(1, leases.count(10))
    }

    @Test
    fun `a lease of a dead holder is dropped whatever holder asks`() {
        val use = Use()
        var ended = 0
        val id = leases.reserve(10).grant("kws.a", listOf(use)) { ended++ }
        assertTrue(leases.drop(id))
        assertFalse(leases.drop(id))
        assertEquals(1, use.closes)
        assertEquals(1, ended)
        assertEquals(emptyList<String>(), leases.ids())
    }

    @Test
    fun `a keepLoaded lease expires its minutes after the session`() {
        val use = Use()
        var ended = 0
        val id = leases.reserve(10).grant("asr.a", listOf(use)) { ended++ }
        leases.expireAfter(id, 5)
        scheduler.advance(5 * 60_000L - 1)
        assertEquals(0, use.closes)
        scheduler.advance(1)
        assertEquals(1, use.closes)
        assertEquals(1, ended)
        assertFalse("expired", leases.release(id, 10))
        assertEquals(0, leases.count(10))
    }

    @Test
    fun `a release before the expiry cancels the timer`() {
        val use = Use()
        val id = leases.reserve(10).grant("asr.a", listOf(use))
        leases.expireAfter(id, 1)
        assertEquals(1, scheduler.pending())
        assertTrue(leases.release(id, 10))
        assertEquals(0, scheduler.pending())
        scheduler.advance(60_000L)
        assertEquals(1, use.closes)
    }

    @Test
    fun `until released means no timer and the minutes are checked`() {
        val use = Use()
        val id = leases.reserve(10).grant("asr.a", listOf(use))
        leases.expireAfter(id, SpeechLimits.KEEP_LOADED_UNTIL_RELEASED)
        assertEquals(0, scheduler.pending())
        scheduler.advance(SpeechLimits.MAX_KEEP_LOADED_MINUTES * 60_000L * 2)
        assertEquals(0, use.closes)
        assertThrows(IllegalArgumentException::class.java) { leases.expireAfter(id, 0) }
        assertThrows(IllegalArgumentException::class.java) { leases.expireAfter(id, SpeechLimits.MAX_KEEP_LOADED_MINUTES + 1) }
        leases.expireAfter("lease-unknown", 5)
        assertEquals(0, scheduler.pending())
    }

    @Test
    fun `leases keep engines loaded past the idle time and count like users`() {
        val cache = EngineCache(300_000, scheduler)
        val key = EngineKey("asr.a", 1, "greedy")
        val engine = Engine()
        val loaded = cache.acquire(key) { engine }
        assertTrue(loaded.loadedNow)
        val first = leases.reserve(10).grant("asr.a", listOf(loaded))
        val reused = cache.acquire(key) { Engine() }
        assertFalse("a second prepare reuses the instance", reused.loadedNow)
        val second = leases.reserve(10).grant("asr.a", listOf(reused))
        scheduler.advance(3_600_000L)
        assertFalse(engine.closed)
        assertTrue(leases.release(first, 10))
        scheduler.advance(3_600_000L)
        assertFalse("the second lease still holds it", engine.closed)
        assertTrue(leases.release(second, 10))
        assertFalse(cache.inUse("asr.a"))
        scheduler.advance(299_999)
        assertFalse("the idle time starts when the last lease ends", engine.closed)
        scheduler.advance(1)
        assertTrue(engine.closed)
    }

    @Test
    fun `a keepLoaded lease shares the engines of its session`() {
        val cache = EngineCache(300_000, scheduler)
        val key = EngineKey("kws.a", 1, "kws")
        val engine = Engine()
        val session = cache.acquire(key) { engine }
        val id = leases.reserve(10).grant("kws.a", listOf(cache.share(session)))
        session.close()
        assertTrue("the lease holds the instance after the session", cache.inUse("kws.a"))
        leases.expireAfter(id, 2)
        scheduler.advance(2 * 60_000L)
        assertFalse(cache.inUse("kws.a"))
        assertFalse(engine.closed)
        scheduler.advance(300_000)
        assertTrue(engine.closed)
    }
}
