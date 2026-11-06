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
 * and external sessions counted per uid. Roadmap D190 / D229: background holders in one queue, all paused by any
 * microphone session and resumed in queue order, a `thenListen` round as the microphone holder, the pause reasons.
 * zh-CN: 会话准入 (路线图 D16) 与插件自己入口对唤醒会话的接管 (D57): 只有一个麦克风持有者, 只能从愿意让出的持有者手中接管, 同时至多一个接管, 接管结束时
 * 恢复持有者, 外部会话按 uid 计数. 路线图 D190 / D229: 后台持有者排成一个队列, 任何麦克风会话都让它们全部暂停并按队列顺序恢复, `thenListen` 轮次作为
 * 麦克风持有者, 以及暂停原因.
 */
class SessionTableTest {

    private class FakeWake(var allow: Boolean = true, val name: String = "", val log: MutableList<String>? = null) : SessionTable.Yielding {
        var yields = 0
        var resumes = 0
        val reasons = ArrayList<String>()

        override fun yieldMicrophone(reason: String): Boolean = allow.also {
            if (it) {
                yields++
                reasons += "paused $reason"
                log?.add("$name paused")
            }
        }

        override fun resumeMicrophone(reason: String) {
            resumes++
            reasons += "resumed $reason"
            log?.add("$name resumed")
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
    fun `background holders pause together and resume in queue order`() {
        // Roadmap D190: the resident wake word and script background sessions share one queue; a listen pauses all.
        val log = ArrayList<String>()
        val resident = FakeWake(name = "resident", log = log)
        val first = FakeWake(name = "first", log = log)
        val second = FakeWake(name = "second", log = log)
        val slots = listOf(
            table.reserve(1, microphone = true, background = true, resident = true) to resident,
            table.reserve(10, microphone = true, background = true) to first,
            table.reserve(11, microphone = true, background = true) to second,
        ).onEach { (slot, wake) -> table.yieldable(slot, wake) }
        assertEquals(listOf(1, 10, 11), table.backgroundHolders)
        assertTrue(table.backgroundListening)
        assertFalse("background holders never hold the microphone", table.microphoneInUse)
        val listen = table.reserve(12, microphone = true)
        assertEquals("no SESSION_BUSY for the listen; all pause in queue order", listOf("resident paused", "first paused", "second paused"), log)
        assertFalse(table.backgroundListening)
        log.clear()
        table.release(listen)
        assertEquals(listOf("resident resumed", "first resumed", "second resumed"), log)
        assertEquals(listOf("paused listen", "resumed listen"), first.reasons)
        assertTrue(table.backgroundListening)
        slots.forEach { (slot, _) -> table.release(slot) }
        assertEquals(emptyList<Int>(), table.backgroundHolders)
    }

    @Test
    fun `pauses carry the category of the session that caused them`() {
        val script = FakeWake()
        val background = table.reserve(10, microphone = true, background = true)
        table.yieldable(background, script)
        table.release(table.reserve(11, microphone = true, reason = SessionTable.REASON_WAKE))
        table.release(table.reserve(1000, microphone = true, takeover = true, reason = SessionTable.REASON_PLUGIN))
        assertEquals(listOf("paused wake", "resumed wake", "paused plugin", "resumed plugin"), script.reasons)
        // Reserved while a listen records: paused at once with the listen's category. zh-CN: 在听写录音时预留: 立即以听写的类别暂停.
        val listen = table.reserve(12, microphone = true, reason = SessionTable.REASON_LISTEN)
        val late = FakeWake()
        table.yieldable(table.reserve(13, microphone = true, background = true), late)
        assertEquals(listOf("paused listen"), late.reasons)
        table.release(listen)
        assertEquals(listOf("paused listen", "resumed listen"), late.reasons)
        table.release(background)
    }

    @Test
    fun `a foreground wake session and its takeover keep the background holders paused until both end`() {
        val log = ArrayList<String>()
        val background = FakeWake(name = "background", log = log)
        val slot = table.reserve(10, microphone = true, background = true)
        table.yieldable(slot, background)
        val wake = FakeWake(name = "wake", log = log)
        val holder = table.reserve(11, microphone = true, reason = SessionTable.REASON_WAKE)
        table.yieldable(holder, wake)
        val taker = table.reserve(1000, microphone = true, takeover = true, reason = SessionTable.REASON_PLUGIN)
        table.release(taker)
        table.release(holder)
        assertEquals(listOf("background paused", "wake paused", "wake resumed", "background resumed"), log)
        table.release(slot)
    }

    @Test
    fun `a thenListen round of a background holder holds the microphone until it ends`() {
        // Roadmap D229: the round is a dictation; others pause for it, and a listen meanwhile is SESSION_BUSY.
        val resident = FakeWake()
        val residentSlot = table.reserve(1, microphone = true, background = true, resident = true)
        table.yieldable(residentSlot, resident)
        val script = FakeWake()
        val scriptSlot = table.reserve(10, microphone = true, background = true)
        table.yieldable(scriptSlot, script)
        assertTrue(table.startRound(scriptSlot))
        assertTrue(table.startRound(scriptSlot))
        assertEquals(listOf("paused listen"), resident.reasons)
        assertEquals("the round holder itself never pauses", 0, script.yields)
        assertTrue(table.microphoneInUse)
        assertBusy("a listen during the round") { table.reserve(11, microphone = true) }
        script.allow = false
        assertBusy("a sheet never takes a round over") { table.reserve(1000, microphone = true, takeover = true) }
        assertFalse("no round starts while another holds the microphone", table.startRound(residentSlot))
        table.endRound(scriptSlot)
        table.endRound(scriptSlot)
        assertEquals(listOf("paused listen", "resumed listen"), resident.reasons)
        assertFalse(table.microphoneInUse)
        script.allow = true
        // A holder that ends in its round frees the microphone too. zh-CN: 在轮次中结束的持有者同样释放麦克风.
        assertTrue(table.startRound(scriptSlot))
        table.release(scriptSlot)
        assertFalse(table.microphoneInUse)
        assertEquals(2, resident.resumes)
        assertFalse("a released holder starts no round", table.startRound(scriptSlot))
        table.release(residentSlot)
    }

    @Test
    fun `a holder that cannot yield refuses the reservation and the ones paused before it record again`() {
        val first = FakeWake()
        val second = FakeWake(allow = false)
        val third = FakeWake()
        listOf(first, second, third).forEachIndexed { i, wake -> table.yieldable(table.reserve(10 + i, microphone = true, background = true), wake) }
        assertBusy { table.reserve(20, microphone = true) }
        assertEquals(listOf("paused listen", "resumed listen"), first.reasons)
        assertEquals(0, third.yields)
        assertTrue(table.backgroundListening)
        assertFalse(table.microphoneInUse)
    }

    @Test
    fun `script background sessions are limited and the resident wake word comes on top`() {
        val slots = List(SessionTable.MAX_BACKGROUND_WAKE_SESSIONS) { table.reserve(10 + it, microphone = true, background = true) }
        assertBusy { table.reserve(20, microphone = true, background = true) }
        val resident = table.reserve(1, microphone = true, background = true, resident = true)
        // A reopened resident session may overlap the old one while it ends. zh-CN: 重开的常驻会话可能与正在结束的旧会话短暂重叠.
        val reopened = table.reserve(1, microphone = true, background = true, resident = true)
        table.release(slots.first())
        table.release(table.reserve(20, microphone = true, background = true))
        (slots.drop(1) + resident + reopened).forEach(table::release)
        assertEquals(emptyList<Int>(), table.backgroundHolders)
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
