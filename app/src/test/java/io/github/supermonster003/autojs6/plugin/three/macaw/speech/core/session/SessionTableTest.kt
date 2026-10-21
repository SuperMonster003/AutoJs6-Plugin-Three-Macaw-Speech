package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Session admission (roadmap D16) and the takeover of a wake session by the plugin's own entries (D57): one microphone
 * holder, a takeover only from a holder that yields, at most one takeover, the holder resumed when the takeover ends,
 * and external sessions counted per uid.
 * zh-CN: 会话准入 (路线图 D16) 与插件自己入口对唤醒会话的接管 (D57): 只有一个麦克风持有者, 只能从愿意让出的持有者手中接管, 同时至多一个接管, 接管结束时
 * 恢复持有者, 外部会话按 uid 计数.
 */
class SessionTableTest {

    private class FakeWake(var allow: Boolean = true) : SessionTable.Yielding {
        var yields = 0
        var resumes = 0

        override fun yieldMicrophone(): Boolean = allow.also { if (it) yields++ }

        override fun resumeMicrophone() {
            resumes++
        }
    }

    private val table = SessionTable()

    private fun assertBusy(block: () -> Unit) {
        try {
            block()
            fail("SESSION_BUSY expected")
        } catch (e: SpeechException) {
            assertEquals(SpeechErrorCodes.SESSION_BUSY, e.code)
        }
    }

    @Test
    fun `one microphone session runs at a time`() {
        val first = table.reserve(10, microphone = true)
        assertTrue(table.microphoneInUse)
        assertBusy { table.reserve(11, microphone = true) }
        assertBusy { table.reserve(11, microphone = true, takeover = true) }
        table.release(first)
        table.release(first)
        assertFalse(table.microphoneInUse)
        table.release(table.reserve(11, microphone = true, takeover = true))
        assertFalse("a takeover with the microphone free is an ordinary holder", table.microphoneInUse)
    }

    @Test
    fun `a takeover silences a yielding holder and resumes it when it ends`() {
        val wake = FakeWake()
        val holder = table.reserve(10, microphone = true)
        table.yieldable(holder, wake)
        val taker = table.reserve(20, microphone = true, takeover = true)
        assertTrue(taker.takeover)
        assertTrue(table.takenOver)
        assertEquals(1, wake.yields)
        assertBusy { table.reserve(30, microphone = true) }
        assertBusy { table.reserve(30, microphone = true, takeover = true) }
        assertEquals("only one takeover at a time", 1, wake.yields)
        table.release(taker)
        assertEquals(1, wake.resumes)
        assertFalse(table.takenOver)
        assertTrue(table.microphoneInUse)
        val again = table.reserve(20, microphone = true, takeover = true)
        assertEquals(2, wake.yields)
        table.release(again)
        table.release(again)
        assertEquals("a release resumes once", 2, wake.resumes)
    }

    @Test
    fun `a dictation and a wake session in its round are never taken over`() {
        val dictation = table.reserve(10, microphone = true)
        assertBusy { table.reserve(20, microphone = true, takeover = true) }
        table.release(dictation)
        val wake = FakeWake(allow = false)
        val holder = table.reserve(10, microphone = true)
        table.yieldable(holder, wake)
        assertBusy { table.reserve(20, microphone = true, takeover = true) }
        wake.allow = true
        val taker = table.reserve(20, microphone = true, takeover = true)
        assertBusy("a takeover never yields to another takeover") {
            table.yieldable(taker, FakeWake())
            table.reserve(30, microphone = true, takeover = true)
        }
        table.release(taker)
        table.release(holder)
        assertFalse(table.microphoneInUse)
    }

    @Test
    fun `a holder that ends during the takeover is not resumed`() {
        val wake = FakeWake()
        val holder = table.reserve(10, microphone = true)
        table.yieldable(holder, wake)
        val taker = table.reserve(20, microphone = true, takeover = true)
        table.release(holder)
        assertTrue("the takeover still records", table.microphoneInUse)
        assertBusy { table.reserve(30, microphone = true) }
        table.release(taker)
        assertEquals(0, wake.resumes)
        assertFalse(table.microphoneInUse)
        table.yieldable(holder, wake)
        table.release(table.reserve(30, microphone = true))
    }

    @Test
    fun `the resident background holder records only while the microphone is free`() {
        // Roadmap D72 / D76: every microphone reservation silences it, without a takeover flag, and it records again after.
        val resident = FakeWake()
        val background = table.reserve(1, microphone = true, background = true)
        table.yieldable(background, resident)
        assertTrue(table.backgroundListening)
        assertFalse("the background holder never counts as the microphone holder", table.microphoneInUse)
        assertBusy("one background holder at a time") { table.reserve(1, microphone = true, background = true) }
        val script = table.reserve(10, microphone = true)
        assertEquals(1, resident.yields)
        assertFalse(table.backgroundListening)
        assertBusy { table.reserve(11, microphone = true) }
        table.release(script)
        assertEquals(1, resident.resumes)
        assertTrue(table.backgroundListening)
        table.release(background)
        assertFalse(table.backgroundListening)
        table.release(table.reserve(10, microphone = true))
        assertEquals("a released background holder is left alone", 1, resident.yields)
    }

    @Test
    fun `a background holder reserved while the microphone is in use starts silenced`() {
        val dictation = table.reserve(10, microphone = true)
        val resident = FakeWake()
        val background = table.reserve(1, microphone = true, background = true)
        assertFalse(table.backgroundListening)
        table.yieldable(background, resident)
        assertEquals("it stops before it records", 1, resident.yields)
        table.release(dictation)
        assertEquals(1, resident.resumes)
        assertTrue(table.backgroundListening)
        table.release(background)
    }

    @Test
    fun `the background holder waits for the takeover and the holder to end`() {
        // D57 stays intact above it: a sheet takes a script's wake session over, and the resident word waits for both.
        val resident = FakeWake()
        val background = table.reserve(1, microphone = true, background = true)
        table.yieldable(background, resident)
        val wake = FakeWake()
        val holder = table.reserve(10, microphone = true)
        table.yieldable(holder, wake)
        val taker = table.reserve(20, microphone = true, takeover = true)
        assertEquals(1, wake.yields)
        table.release(taker)
        assertEquals(1, wake.resumes)
        assertEquals("the script's session still records", 0, resident.resumes)
        table.release(holder)
        assertEquals(1, resident.resumes)
        assertEquals(1, resident.yields)
        table.release(background)
    }

    @Test
    fun `a background holder that cannot yield keeps the microphone and one that ends silenced is not resumed`() {
        val resident = FakeWake(allow = false)
        val background = table.reserve(1, microphone = true, background = true)
        table.yieldable(background, resident)
        assertBusy { table.reserve(10, microphone = true) }
        assertFalse(table.microphoneInUse)
        resident.allow = true
        val dictation = table.reserve(10, microphone = true)
        table.release(background)
        table.release(dictation)
        assertEquals(0, resident.resumes)
        assertFalse(table.backgroundListening)
    }

    @Test
    fun `external sessions are counted per uid beside the microphone`() {
        val slots = List(SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN) { table.reserve(10, microphone = false) }
        assertBusy { table.reserve(10, microphone = false) }
        table.release(table.reserve(11, microphone = false))
        val microphone = table.reserve(10, microphone = true)
        assertEquals(SpeechLimits.MAX_PCM_SESSIONS_PER_TOKEN, table.externalSessions(10))
        slots.forEach(table::release)
        table.release(microphone)
        assertEquals(0, table.externalSessions(10))
    }

    private fun assertBusy(message: String, block: () -> Unit) {
        try {
            block()
            fail("$message: SESSION_BUSY expected")
        } catch (e: SpeechException) {
            assertEquals(message, SpeechErrorCodes.SESSION_BUSY, e.code)
        }
    }
}
