package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReaderTest
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wake sessions (roadmap D18, P3.4): hits with the script's keyword and the threshold as score, the minimum interval,
 * `thenListen` rounds on the same audio with their own `ready` .. `end`, `finishRound`, and the end of the whole
 * session; plus file recognition and voice activity on fakes (P3.3).
 * zh-CN: 唤醒会话 (路线图 D18, P3.4): 命中携带脚本的关键词并以阈值为分数, 最小间隔, 在同一音频上进行并有各自 `ready` .. `end` 的 `thenListen`
 * 轮次, `finishRound`, 以及整个会话的结束; 另以假实现测试文件识别与语音活动检测 (P3.3).
 */
class WakeSessionTest {

    private val wakeEvents = RecordingEvents()
    private val listenEvents = RecordingEvents()

    private fun wake(listen: ListenOptions? = null, hits: MutableList<Pair<Long, Int>>, interval: Long = 1500): Pair<WakeSession, FakeKeywords> {
        val keywords = FakeKeywords(hits)
        val options = WakeOptions(listOf(Keyword("alpha"), Keyword("beta", threshold = 0.6f)), "kws.m", interval, listen)
        val session = WakeSession("w1", options, wakeEvents, if (listen != null) listenEvents else null, 0.25f) { 1234L }
        val round: ((DictationSink) -> Dictation)? = if (listen == null) null else { sink -> StreamingDictation(FakeStream(), FakeVad(), sink, true, true, listen.silenceEndMillis, true) }
        session.start("kws.m", keywords, if (listen != null) "asr.m" else null, round)
        return session to keywords
    }

    @Test
    fun `hits report the keyword, its threshold and the minimum interval drops close hits`() {
        val (session, keywords) = wake(hits = mutableListOf(16_000L to 0, 24_000L to 1, 48_000L to 1))
        repeat(40) { session.accept(Audio.silence(100)) }
        session.finish()
        assertEquals(listOf("wake-ready:kws.m", "wake:alpha:0.25", "wake:beta:0.6", "wake-end:stopped"), wakeEvents.log)
        assertTrue(keywords.closed)
        session.accept(Audio.silence(100))
        assertEquals(4, wakeEvents.log.size)
    }

    @Test
    fun `thenListen runs one dictation round per hit and resumes spotting`() {
        val (session, keywords) = wake(ListenOptions(), mutableListOf(8000L to 0, 24_000L to 1))
        session.accept(Audio.silence(500))
        assertEquals(listOf("wake-ready:kws.m", "wake:alpha:0.25:r0"), wakeEvents.log)
        assertEquals(listOf("ready:asr.m:r0"), listenEvents.log)
        session.accept(Audio.speech(300))
        session.accept(Audio.silence(1500))
        assertEquals(1, keywords.restarts)
        // The keyword stream did not hear the round: its second hit needs 1 s more of its own audio.
        repeat(10) { session.accept(Audio.silence(100)) }
        session.accept(Audio.silence(100))
        assertEquals(listOf("wake-ready:kws.m", "wake:alpha:0.25:r0", "wake:beta:0.6:r1"), wakeEvents.log)
        session.finishRound(discard = true)
        session.cancel()
        assertEquals(
            listOf("ready:asr.m:r0", "vad:true", "vad:false", "final:0:w w w", "endpoint", "end:stopped:w w w:r0", "ready:asr.m:r1", "end:cancelled::r1"),
            listenEvents.milestones(),
        )
        assertEquals("wake-end:cancelled", wakeEvents.log.last())
        assertEquals("w w w", session.text)
    }

    @Test
    fun `finish ends the round in progress first`() {
        val (session, _) = wake(ListenOptions(), mutableListOf(1600L to 0))
        session.accept(Audio.silence(100))
        session.accept(Audio.speech(500))
        session.finish()
        assertEquals(listOf("ready:asr.m:r0", "vad:true", "final:0:w w w w w", "endpoint", "end:stopped:w w w w w:r0"), listenEvents.milestones())
        assertEquals("wake-end:stopped", wakeEvents.log.last())
    }

    @Test
    fun `a failure ends the round and the wake session with errors`() {
        val (session, _) = wake(ListenOptions(), mutableListOf(1600L to 0))
        session.accept(Audio.silence(100))
        session.fail(SpeechErrorCodes.ENGINE_FAILED, "boom")
        assertEquals(listOf("ready:asr.m:r0", "error:${SpeechErrorCodes.ENGINE_FAILED}", "end:error::r0"), listenEvents.log)
        assertEquals(listOf("wake-ready:kws.m", "wake:alpha:0.25:r0", "error:${SpeechErrorCodes.ENGINE_FAILED}", "wake-end:error"), wakeEvents.log)
    }

    @Test
    fun `a wake session ended while pending sends only its end`() {
        val options = WakeOptions(listOf(Keyword("alpha")))
        val session = WakeSession("w2", options, wakeEvents, null, 0.25f)
        session.finish()
        val late = FakeKeywords(mutableListOf())
        session.start("kws.m", late)
        assertTrue(late.closed)
        assertEquals(listOf("wake-end:stopped"), wakeEvents.log)
    }

    @Test
    fun `file recognition collects the finals as timed segments`() {
        val pcm = Audio.silence(500) + Audio.speech(1000) + Audio.silence(1000) + Audio.speech(500) + Audio.silence(800)
        val result = FileRecognition.recognize(wavReader(pcm), "asr.m", "zh", listOf(SpeechWarning(SpeechErrorCodes.INVALID_OPTIONS, "hotwords ignored"))) { sink ->
            StreamingDictation(FakeStream(), FakeVad(), sink, gate = true, reportVad = false, silenceEndMillis = 600, punctuation = true)
        }
        assertEquals(listOf("w w w w w w w w w w", "w w w w w"), result.segments.map { it.text })
        assertEquals("w w w w w w w w w w w w w w w", result.text)
        assertEquals(3800L, result.durationMillis)
        assertEquals("zh", result.language)
        assertEquals("asr.m", result.modelId)
        assertEquals(1, result.warnings.size)
        val (first, second) = result.segments
        assertTrue(first.startMillis in 0..500 && first.endMillis in 1450..1600)
        assertTrue(second.startMillis in 2000..2500 && second.endMillis in 2950..3100)
    }

    @Test
    fun `offline file recognition takes the language most segments report`() {
        val pcm = Audio.speech(1000) + Audio.silence(300) + Audio.speech(500) + Audio.silence(300)
        val result = FileRecognition.recognize(wavReader(pcm), "asr.sense", null, emptyList()) { sink ->
            OfflineDictation(FakeRecognizer(), FakeVad(), sink, reportVad = false, silenceEndMillis = 0, punctuation = true)
        }
        assertEquals(2, result.segments.size)
        assertEquals("zh", result.language)
        assertEquals("speech", result.segments[0].event)
    }

    @Test
    fun `fixed segments split audio without VAD`() {
        val pcm = Audio.speech(45_000)
        val recognizer = FakeRecognizer()
        val result = FileRecognition.recognize(wavReader(pcm), "asr.sense", null, emptyList()) { sink ->
            OfflineDictation(recognizer, FixedSegments(), sink, reportVad = false, silenceEndMillis = 0, punctuation = true)
        }
        // The last window is padded with silence to 512 samples (24 ms).
        assertEquals(listOf(AudioInput.samples(20_000).toInt(), AudioInput.samples(20_000).toInt(), AudioInput.samples(5024).toInt()), recognizer.calls)
        assertEquals(listOf(0L to 20_000L, 20_000L to 40_000L, 40_000L to 45_024L), result.segments.map { it.startMillis to it.endMillis })
    }

    @Test
    fun `voice activity of a file lists speech segments in milliseconds`() {
        val pcm = Audio.silence(320) + Audio.speech(640) + Audio.silence(960) + Audio.speech(320)
        val result = FileRecognition.detectVoice(wavReader(pcm), FakeVad())
        assertEquals(listOf(320L to 960L, 1920L to 2240L), result.segments)
        assertEquals(2240L, result.durationMillis)
    }

    private fun wavReader(samples: FloatArray): WavReader {
        val pcm = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { pcm.putShort((it * 32767).toInt().toShort()) }
        val bytes = WavReaderTest.wav(1, 1, 16000, 16, pcm.array())
        return WavReader(ByteArrayInputStream(bytes), bytes.size.toLong())
    }
}
