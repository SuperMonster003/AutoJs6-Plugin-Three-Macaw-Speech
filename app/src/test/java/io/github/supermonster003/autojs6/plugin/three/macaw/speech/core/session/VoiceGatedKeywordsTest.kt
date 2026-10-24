package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * The power saving mode of the resident wake word (roadmap P8.1, D123, D128): silence never reaches the keyword spotter,
 * speech reaches it with a lookback of the silence before it, the gate closes after the hangover with a tail of silence
 * and a fresh spotter, a hit found while closing is not lost, and a restart forgets everything.
 * zh-CN: 常驻唤醒的省电模式 (路线图 P8.1, D123, D128): 静音从不进入唤醒词检测器, 语音连同之前的一段静音回溯进入, 余留时间过后关门并送入尾部静音,
 * 检测器重新开始, 关门时发现的命中不会丢失, 重启会忘掉一切.
 */
class VoiceGatedKeywordsTest {

    /** A spotter that keeps whether each fed sample was loud; [hitOnTail] fires on the tail of silence. zh-CN: 记录每个送入采样是否响亮的检测器. */
    private class TapeKeywords(private val hitOnTail: Int? = null, private val hitAt: Long? = null) : KeywordStream {
        val tape = ArrayList<Boolean>()
        var polls = 0
        var restarts = 0
        var closed = false
        private var fired: Int? = null
        private var hitDone = false

        override fun accept(samples: FloatArray) {
            samples.forEach { tape += abs(it) >= 0.1f }
            if (hitOnTail != null && samples.size == TAIL && samples.all { it == 0f }) fired = hitOnTail
            if (hitAt != null && !hitDone && tape.size >= hitAt) {
                hitDone = true
                fired = 1
            }
        }

        override fun poll(): Int? {
            polls++
            return fired.also { fired = null }
        }

        override fun restart() {
            restarts++
            fired = null
        }

        override fun close() {
            closed = true
        }

        /** Silent samples before the first loud one. zh-CN: 首个响亮采样之前的静音采样数. */
        fun silentPrefix(): Int = tape.indexOfFirst { it }.let { if (it < 0) tape.size else it }
    }

    private fun feed(gate: KeywordStream, audio: FloatArray, chunkMillis: Long = 20): List<Int> {
        val hits = ArrayList<Int>()
        val chunk = AudioInput.samples(chunkMillis).toInt()
        var offset = 0
        while (offset < audio.size) {
            val end = minOf(audio.size, offset + chunk)
            gate.accept(audio.copyOfRange(offset, end))
            gate.poll()?.let { hits += it }
            offset = end
        }
        return hits
    }

    @Test
    fun `silence never reaches the spotter`() {
        val spotter = TapeKeywords()
        val gate = VoiceGatedKeywords(spotter, FakeVad())
        feed(gate, Audio.silence(10_000), chunkMillis = 200)
        assertTrue(spotter.tape.isEmpty())
        assertEquals("nothing to decode, so no native poll", 0, spotter.polls)
        assertEquals(0L, gate.passed)
        assertEquals(0, gate.openings)
        assertFalse(gate.open)
        assertTrue("whole VAD windows of 10 s", gate.heard in AudioInput.samples(9_960)..AudioInput.samples(10_000))
    }

    @Test
    fun `speech opens the gate with a lookback of the silence before it`() {
        val spotter = TapeKeywords()
        val gate = VoiceGatedKeywords(spotter, FakeVad())
        feed(gate, Audio.silence(3_000))
        feed(gate, Audio.speech(500))
        assertTrue(gate.open)
        assertEquals(1, gate.openings)
        val prefix = spotter.silentPrefix().toLong()
        assertTrue("at least the lookback: $prefix", prefix >= AudioInput.samples(VoiceGatedKeywords.LOOKBACK_MILLIS))
        assertTrue("not more than two windows beyond it: $prefix", prefix <= AudioInput.samples(VoiceGatedKeywords.LOOKBACK_MILLIS) + 2 * 512)
        val loud = spotter.tape.count { it }
        assertTrue("every speech sample but the last partial window: $loud", loud > AudioInput.samples(500) - 512 && loud <= AudioInput.samples(500))
    }

    @Test
    fun `the gate closes after the hangover with a tail of silence and starts the spotter over`() {
        val spotter = TapeKeywords()
        val gate = VoiceGatedKeywords(spotter, FakeVad())
        feed(gate, Audio.silence(2_000))
        feed(gate, Audio.speech(600))
        feed(gate, Audio.silence(VoiceGatedKeywords.HANGOVER_MILLIS - 100))
        assertTrue("still open within the hangover", gate.open)
        assertEquals(0, spotter.restarts)
        val beforeClose = spotter.tape.size
        feed(gate, Audio.silence(200))
        assertFalse(gate.open)
        assertEquals(1, spotter.restarts)
        assertTrue("the tail of silence was fed", spotter.tape.size - beforeClose >= AudioInput.samples(400))
        val closed = spotter.tape.size
        feed(gate, Audio.silence(5_000))
        assertEquals("closed again: silence stays away", closed, spotter.tape.size)
        // A second speech opens it again with a fresh lookback.
        feed(gate, Audio.speech(300))
        assertTrue(gate.open)
        assertEquals(2, gate.openings)
    }

    @Test
    fun `a hit found while closing comes with the next poll and a hit while open with its own`() {
        val closing = TapeKeywords(hitOnTail = 3)
        val gate = VoiceGatedKeywords(closing, FakeVad())
        feed(gate, Audio.silence(1_500))
        val hits = feed(gate, Audio.speech(800)) + feed(gate, Audio.silence(2_000))
        assertEquals("the keyword at the end of the speech is decoded on the tail", listOf(3), hits)
        assertEquals(1, closing.restarts)
        assertNull(gate.poll())

        val open = TapeKeywords(hitAt = AudioInput.samples(VoiceGatedKeywords.LOOKBACK_MILLIS + 300))
        val second = VoiceGatedKeywords(open, FakeVad())
        feed(second, Audio.silence(3_000))
        assertEquals(listOf(1), feed(second, Audio.speech(800)))
    }

    @Test
    fun `the VAD hears quiet speech louder while the spotter hears it as it is`() {
        val quiet = FloatArray(AudioInput.samples(500).toInt()) { 0.02f }
        val plain = TapeKeywords()
        val unamplified = VoiceGatedKeywords(plain, FakeVad(), vadGain = 1f, riseDb = 0f)
        feed(unamplified, Audio.silence(1_000))
        feed(unamplified, quiet)
        assertFalse("the fake VAD needs 0.1: 0.02 stays shut without gain", unamplified.open)
        val spotter = TapeKeywords()
        val gate = VoiceGatedKeywords(spotter, FakeVad(), riseDb = 0f)
        feed(gate, Audio.silence(1_000))
        feed(gate, quiet)
        assertTrue("0.02 x ${VoiceGatedKeywords.VAD_GAIN} opens it", gate.open)
        assertEquals("the spotter gets the samples unchanged, so nothing it hears is loud", 0, spotter.tape.count { it })
    }

    @Test
    fun `a rise over the room opens the gate the VAD missed, and a louder room stops opening it`() {
        val random = Random(7)
        fun room(millis: Long, level: Float) = FloatArray(AudioInput.samples(millis).toInt()) { (random.nextFloat() * 2 - 1) * level }
        // VAD gain 0: the VAD hears nothing, only the rise over the floor opens the gate.
        val spotter = TapeKeywords()
        val gate = VoiceGatedKeywords(spotter, FakeVad(), vadGain = 0f)
        feed(gate, room(5_000, 0.001f))
        feed(gate, room(1_000, 0.0013f))
        assertEquals("2 dB over the room stays shut", 0, gate.openings)
        feed(gate, room(500, 0.004f))
        assertTrue("12 dB over the room opens it", gate.open)
        feed(gate, room(3_000, 0.001f))
        assertFalse("closed after the hangover", gate.open)
        // A room 12 dB louder for good: the floor follows it, so the gate closes and stays shut.
        feed(gate, room(30_000, 0.004f))
        val openings = gate.openings
        feed(gate, room(10_000, 0.004f))
        assertFalse(gate.open)
        assertEquals("no opening once the floor caught up", openings, gate.openings)
        // Off: no rise opens it.
        val off = VoiceGatedKeywords(TapeKeywords(), FakeVad(), vadGain = 0f, riseDb = 0f)
        feed(off, room(5_000, 0.001f))
        feed(off, room(500, 0.01f))
        assertEquals(0, off.openings)
    }

    @Test
    fun `a restart forgets the lookback, the open gate and the VAD state`() {
        val spotter = TapeKeywords()
        val vad = FakeVad()
        val gate = VoiceGatedKeywords(spotter, vad)
        feed(gate, Audio.silence(3_000))
        feed(gate, Audio.speech(200))
        assertTrue(gate.open)
        gate.restart()
        assertFalse(gate.open)
        assertEquals(1, spotter.restarts)
        assertEquals(1, vad.resets)
        val before = spotter.tape.size
        feed(gate, Audio.silence(200))
        feed(gate, Audio.speech(300))
        val prefix = spotter.tape.subList(before, spotter.tape.size).indexOfFirst { it }.toLong()
        assertTrue("only the silence heard after the restart: $prefix", prefix <= AudioInput.samples(200) + 512)
        gate.close()
        assertTrue(spotter.closed)
        assertTrue(vad.closed)
    }

    @Test
    fun `in a wake session only speech is spotted`() {
        val events = RecordingEvents()
        val keywords = FakeKeywords(mutableListOf(AudioInput.samples(VoiceGatedKeywords.LOOKBACK_MILLIS + 200) to 0))
        val options = WakeOptions(listOf(Keyword("alpha")), "kws.m", 1500, null)
        val session = WakeSession("w1", options, events, null, 0.25f) { 1L }
        session.start("kws.m", VoiceGatedKeywords(keywords, FakeVad()))
        repeat(100) { session.accept(Audio.silence(100)) }
        assertEquals("10 s of silence never reached the spotter", 0L, keywords.position)
        assertEquals(listOf("wake-ready:kws.m"), events.log)
        repeat(4) { session.accept(Audio.speech(100)) }
        assertEquals(listOf("wake-ready:kws.m", "wake:alpha:0.25"), events.log)
        session.finish()
        assertTrue(keywords.closed)
    }

    @Test
    fun `the power modes check their step`() {
        assertEquals(WakePower(voiceGate = false, frameMillis = 20), WakePower.OFF)
        assertEquals(320, WakePower.OFF.frameSamples)
        assertEquals(3200, WakePower(voiceGate = true, frameMillis = 200).frameSamples)
        assertThrows(IllegalArgumentException::class.java) { WakePower(voiceGate = true, frameMillis = 5) }
        assertThrows(IllegalArgumentException::class.java) { WakePower(voiceGate = true, frameMillis = 1000) }
    }

    private companion object {
        val TAIL = AudioInput.samples(400).toInt()
    }
}
