package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The recording the background holders share (roadmap D228): one recording for all started members, each frame to each
 * member as its own copy, the shortest read step, a new recording after a failure, and opening failures for the member
 * that started.
 * zh-CN: 后台持有者共用的录音 (路线图 D228): 所有已开始的成员共用一路录音, 每一帧以各自的副本交给每个成员, 取最短读取步长, 失败后另开新录音, 打开失败
 * 抛给开始的成员.
 */
class SharedCaptureTest {

    private class FakeRecording(val listener: MicrophoneSource.Listener, var step: Int, private val failStart: Boolean) : AudioCapture, SharedCapture.Steppable {
        var started = 0
        var stopped = 0

        override fun start() {
            if (failStart) throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "no microphone")
            started++
        }

        override fun stop() {
            stopped++
        }

        override fun step(frameSamples: Int) {
            step = frameSamples
        }
    }

    private class Heard : MicrophoneSource.Listener {
        val frames = ArrayList<FloatArray>()
        val failures = ArrayList<String>()

        override fun onAudio(samples: FloatArray) {
            frames += samples
        }

        override fun onSilenced(silenced: Boolean) = Unit

        override fun onFailure(error: SpeechException) {
            failures += error.code
        }
    }

    private val recordings = ArrayList<FakeRecording>()
    private var failStart = false
    private val shared = SharedCapture { listener, step -> FakeRecording(listener, step, failStart).also { recordings += it } }

    @Test
    fun `members share one recording that the last one stops`() {
        val resident = Heard()
        val script = Heard()
        val first = shared.member(resident, 3200)
        val second = shared.member(script, 320)
        first.start()
        second.start()
        assertEquals("one recording for both", 1, recordings.size)
        assertEquals("the shortest read step", 320, recordings[0].step)
        recordings[0].listener.onAudio(floatArrayOf(0.5f, -0.5f))
        assertEquals(1, resident.frames.size)
        assertEquals(1, script.frames.size)
        assertNotSame("each member gets its own copy", resident.frames[0], script.frames[0])
        assertEquals(listOf(0.5f, -0.5f), script.frames[0].toList())
        second.stop()
        assertEquals("the step follows the members left", 3200, recordings[0].step)
        assertEquals(0, recordings[0].stopped)
        first.stop()
        first.stop()
        assertEquals(1, recordings[0].stopped)
        assertEquals(0, shared.members)
        shared.member(Heard(), 320).start()
        assertEquals("a new recording after the last stop", 2, recordings.size)
    }

    @Test
    fun `a failure reaches every member and the next start opens a new recording`() {
        val a = Heard()
        val b = Heard()
        val first = shared.member(a)
        first.start()
        shared.member(b).start()
        recordings[0].listener.onFailure(SpeechException(SpeechErrorCodes.ENGINE_FAILED, "read failed"))
        assertEquals(listOf(SpeechErrorCodes.ENGINE_FAILED), a.failures)
        assertEquals(listOf(SpeechErrorCodes.ENGINE_FAILED), b.failures)
        val c = Heard()
        shared.member(c).start()
        assertEquals(2, recordings.size)
        recordings[1].listener.onAudio(floatArrayOf(1f))
        assertEquals(1, c.frames.size)
        assertEquals("the retired recording feeds nobody new", 0, a.frames.size)
        first.stop()
        assertEquals(0, recordings[1].stopped)
    }

    @Test
    fun `an opening failure is thrown to the member that started and leaves nothing behind`() {
        failStart = true
        try {
            shared.member(Heard()).start()
            fail("ENGINE_FAILED expected")
        } catch (e: SpeechException) {
            assertEquals(SpeechErrorCodes.ENGINE_FAILED, e.code)
        }
        assertEquals(0, shared.members)
        failStart = false
        shared.member(Heard()).start()
        assertTrue(recordings.last().started == 1)
        assertEquals(1, shared.members)
    }
}
