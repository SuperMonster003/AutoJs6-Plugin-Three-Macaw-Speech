package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dictation session state machine and its event order (roadmap D16, D17, P3.2; protocol "Session events, in
 * order"): `ready` first, recognition events in audio order, exactly one `end` last; VAD gating and pre-roll; endpoint
 * by silence; `finish`, `cancel`, `maxDuration`, `timeout`, failures and the pending state.
 * zh-CN: 听写会话状态机及其事件顺序 (路线图 D16, D17, P3.2; 协议 "Session events, in order"): 先 `ready`, 识别事件按音频顺序, 最后恰好一个
 * `end`; VAD 门控与预录; 静音端点; `finish`, `cancel`, `maxDuration`, `timeout`, 失败与 pending 状态.
 */
class DictationSessionTest {

    private val events = RecordingEvents()
    private val stream = FakeStream()
    private val vad = FakeVad()

    private fun session(options: ListenOptions = ListenOptions(), round: Int? = null, endOnFinal: Boolean = false) =
        DictationSession("s1", options, events, round, endOnFinal)

    private fun streaming(sink: DictationSink, options: ListenOptions = ListenOptions()): Dictation =
        StreamingDictation(stream, vad, sink, gate = options.vad, reportVad = options.vad, options.silenceEndMillis, options.punctuation) {
            stream.close()
            vad.close()
        }

    private val ten = List(10) { "w" }.joinToString(" ")

    @Test
    fun `gated streaming dictation reports ready, voice activity, final, endpoint and end in order`() {
        val session = session()
        assertEquals(SessionState.PENDING, session.state)
        session.start("asr.m") { streaming(it) }
        assertEquals(SessionState.ACTIVE, session.state)
        session.accept(Audio.silence(500))
        assertEquals("the gate keeps silence away from the recognizer", 0L, stream.fed)
        session.accept(Audio.speech(1000))
        // All of the 500 ms of silence fit in the pre-roll and are fed with the speech.
        assertTrue("pre-roll is fed with the speech", stream.fed >= 16_000 + 7680 - 512 && stream.fed <= 16_000 + AudioInput.samples(StreamingDictation.PREROLL_MILLIS) + 512)
        session.accept(Audio.silence(1500))
        session.finish()
        assertEquals(listOf("ready:asr.m", "vad:true", "vad:false", "final:0:$ten", "endpoint", "end:stopped:$ten"), events.milestones())
        assertEquals("partial:0:$ten", events.log.last { it.startsWith("partial:") })
        assertEquals(SessionState.ENDED, session.state)
        assertEquals("stopped", session.reason)
        assertEquals(ten, session.text)
        assertTrue(stream.closed && vad.closed)
        val utterance = events.finals.single()
        assertTrue(utterance.startMillis in 0..500 && utterance.endMillis in 1400..1600)
        session.accept(Audio.speech(500))
        session.finish()
        session.cancel()
        assertEquals("nothing follows the end", 6, events.milestones().size)
    }

    @Test
    fun `without the gate every sample is decoded and no vad events are sent`() {
        val options = ListenOptions(vad = false)
        val session = session(options)
        session.start("asr.m") { streaming(it, options) }
        session.accept(Audio.silence(500))
        session.accept(Audio.speech(1000))
        session.accept(Audio.silence(1500))
        assertTrue(stream.fed >= 48_000)
        session.finish()
        assertEquals(listOf("ready:asr.m", "final:0:$ten", "endpoint", "end:stopped:$ten"), events.milestones())
    }

    @Test
    fun `finish flushes the utterance in progress, cancel drops it`() {
        val first = session()
        first.start("asr.m") { streaming(it) }
        first.accept(Audio.speech(500))
        first.finish()
        assertEquals(listOf("ready:asr.m", "vad:true", "final:0:w w w w w", "endpoint", "end:stopped:w w w w w"), events.milestones())

        val other = RecordingEvents()
        val second = DictationSession("s2", ListenOptions(), other)
        second.start("asr.m") { StreamingDictation(FakeStream(), FakeVad(), it, true, true, 1200, true) }
        second.accept(Audio.speech(500))
        second.cancel()
        assertEquals(listOf("ready:asr.m", "vad:true", "end:cancelled:"), other.milestones())
        assertEquals("cancelled", second.reason)
    }

    @Test
    fun `maxDurationMillis and timeoutMillis end the session by audio time`() {
        val limited = session(ListenOptions(maxDurationMillis = 1000))
        limited.start("asr.m") { streaming(it) }
        limited.accept(Audio.speech(1200))
        assertEquals(listOf("ready:asr.m", "vad:true", "final:0:${List(12) { "w" }.joinToString(" ")}", "endpoint", "end:maxDuration:${List(12) { "w" }.joinToString(" ")}"), events.milestones())

        val quiet = RecordingEvents()
        val waiting = DictationSession("s3", ListenOptions(timeoutMillis = 1000), quiet)
        waiting.start("asr.m") { StreamingDictation(FakeStream(), FakeVad(), it, true, true, 1200, true) }
        waiting.accept(Audio.silence(900))
        assertFalse(waiting.isEnded)
        waiting.accept(Audio.silence(200))
        assertEquals(listOf("ready:asr.m", "end:timeout:"), quiet.milestones())

        val unlimited = RecordingEvents()
        val forever = DictationSession("s4", ListenOptions(timeoutMillis = 0), unlimited)
        forever.start("asr.m") { StreamingDictation(FakeStream(), FakeVad(), it, true, true, 1200, true) }
        repeat(30) { forever.accept(Audio.silence(1000)) }
        assertFalse("timeoutMillis 0 disables the timeout", forever.isEnded)
    }

    @Test
    fun `a session ended while pending never sends ready and closes a late dictation`() {
        val session = session()
        session.cancel()
        var built = false
        session.start("asr.m") {
            built = true
            streaming(it)
        }
        assertFalse(built)
        assertEquals(listOf("end:cancelled:"), events.log)

        val other = RecordingEvents()
        val racing = DictationSession("s5", ListenOptions(), other)
        val late = FakeStream()
        racing.start("asr.m") { sink ->
            racing.finish()
            StreamingDictation(late, FakeVad(), sink, true, true, 1200, true) { late.close() }
        }
        assertTrue("a dictation built after the end is closed", late.closed)
        assertEquals(listOf("end:stopped:"), other.log)
    }

    @Test
    fun `failures end the session with error then end`() {
        val session = session()
        session.start("asr.m") { streaming(it) }
        stream.failOnDecode = true
        session.accept(Audio.speech(300))
        assertEquals(listOf("ready:asr.m", "vad:true", "error:${SpeechErrorCodes.ENGINE_FAILED}", "end:error:"), events.milestones())
        assertTrue(stream.closed)

        val other = RecordingEvents()
        val failing = DictationSession("s6", ListenOptions(), other)
        failing.fail(SpeechErrorCodes.MODEL_MISSING, "gone")
        failing.fail(SpeechErrorCodes.ENGINE_FAILED, "again")
        assertEquals(listOf("error:${SpeechErrorCodes.MODEL_MISSING}", "end:error:"), other.log)

        val coded = RecordingEvents()
        val throwing = DictationSession("s7", ListenOptions(), coded)
        throwing.start("asr.m") { sink ->
            object : Dictation by StreamingDictation(FakeStream(), FakeVad(), sink, true, true, 1200, true) {
                override fun accept(samples: FloatArray) = throw SpeechException(SpeechErrorCodes.IO_FAILED, "disk")
            }
        }
        throwing.accept(Audio.speech(100))
        assertEquals(listOf("ready:asr.m", "error:${SpeechErrorCodes.IO_FAILED}", "end:error:"), coded.log)
    }

    @Test
    fun `utterances get increasing segment indexes and the session text joins them`() {
        val session = session()
        session.start("asr.m") { streaming(it) }
        session.accept(Audio.speech(300))
        session.accept(Audio.silence(1300))
        session.accept(Audio.speech(200))
        session.accept(Audio.silence(1300))
        session.finish()
        assertEquals(listOf("ready:asr.m", "vad:true", "vad:false", "final:0:w w w", "endpoint", "vad:true", "vad:false", "final:1:w w", "endpoint", "end:stopped:w w w w w"), events.milestones())
    }

    @Test
    fun `a wake round ends with stopped after its first final`() {
        val session = session(round = 2, endOnFinal = true)
        session.start("asr.m") { streaming(it) }
        session.accept(Audio.speech(300))
        session.accept(Audio.silence(1300) + Audio.speech(300))
        assertTrue(session.isEnded)
        session.accept(Audio.speech(300))
        assertEquals(listOf("ready:asr.m:r2", "vad:true", "vad:false", "final:0:w w w", "endpoint", "end:stopped:w w w:r2"), events.milestones())
    }

    @Test
    fun `offline dictation reports segment level partials and a tagged final`() {
        val recognizer = FakeRecognizer()
        val session = session()
        session.start("asr.sense") { OfflineDictation(recognizer, vad, it, reportVad = true, silenceEndMillis = 1200, punctuation = true) { vad.close() } }
        session.accept(Audio.speech(1000))
        session.accept(Audio.silence(300))
        session.accept(Audio.speech(500))
        session.accept(Audio.silence(1500))
        session.finish()
        val partials = events.log.filter { it.startsWith("partial:") }
        assertEquals(2, partials.size)
        assertTrue(partials[1].startsWith(partials[0] + " "))
        assertEquals(2, recognizer.calls.size)
        val final = events.finals.single()
        assertEquals(partials[1].removePrefix("partial:0:"), final.text)
        assertEquals("zh", final.language)
        assertEquals("neutral", final.emotion)
        assertEquals("speech", final.event)
        assertEquals(listOf("ready:asr.sense", "vad:true", "vad:false", "vad:true", "vad:false", "final:0:${final.text}", "endpoint", "end:stopped:${final.text}"), events.milestones())
        assertTrue(vad.closed)
    }

    @Test
    fun `offline dictation without punctuation strips it`() {
        val session = session(ListenOptions(punctuation = false))
        session.start("asr.sense") { OfflineDictation(FakeRecognizer(), vad, it, reportVad = false, silenceEndMillis = 1200, punctuation = false) }
        session.accept(Audio.speech(1000))
        session.finish()
        assertEquals(listOf("ready:asr.sense", "final:0:s10", "endpoint", "end:stopped:s10"), events.milestones())
    }
}
