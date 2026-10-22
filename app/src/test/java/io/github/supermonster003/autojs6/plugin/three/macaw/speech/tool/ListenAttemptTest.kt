package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P7.6.2 (D89, D95, D100, D101 E2): the state machine of one `speech_listen` dictation, fed with the session
 * events a fake session would send: the first final finishes it, a refusal within the check time falls back to the next
 * route, a later one fails it unless text was heard, errors, cancellation and the tool's time limit.
 * zh-CN: 路线图 P7.6.2 (D89, D95, D100, D101 E2): `speech_listen` 一次听写的状态机, 以假会话会发出的事件驱动: 第一个 final 结束听写, 检查时间内的
 * 拒绝改走下一途径, 之后的拒绝在没听到文字时使其失败, 错误, 取消与工具时限.
 */
class ListenAttemptTest {

    private val silenced = "Android silences the microphone of 3-Macaw Speech"

    @Test
    fun `the first final finishes the dictation and the end text wins`() {
        val attempt = ListenAttempt()
        attempt.recording(1000)
        attempt.partial("open the")
        assertEquals(SessionAction.FINISH, attempt.final(" open the door ", "en"))
        assertEquals("a second final changes nothing", SessionAction.NONE, attempt.final("and close it", "fr"))
        assertNull(attempt.outcome)
        val outcome = attempt.end(SpeechContract.REASON_STOPPED, "open the door and close it", 3500)
        assertEquals(ListenOutcome.Heard("open the door and close it", "en", SpeechToolCatalog.END_ENDPOINT, 2500), outcome)
        assertSame("the end is settled once", outcome, attempt.end(SpeechContract.REASON_CANCELLED, null, 9000))
        assertSame(outcome, attempt.outcome)
    }

    @Test
    fun `without end text the first final, then the last partial, then nothing`() {
        ListenAttempt().apply {
            recording(0)
            final("hello", null)
            assertEquals("hello", (end(SpeechContract.REASON_STOPPED, "  ", 100) as ListenOutcome.Heard).text)
        }
        ListenAttempt().apply {
            recording(0)
            partial("half a sen")
            partial("half a sentence")
            val heard = end(SpeechContract.REASON_MAX_DURATION, null, 30_000) as ListenOutcome.Heard
            assertEquals("half a sentence", heard.text)
            assertEquals(SpeechToolCatalog.END_MAX_DURATION, heard.endReason)
        }
        ListenAttempt().apply {
            recording(200)
            partial(" ")
            assertEquals(SessionAction.NONE, final("", "en"))
            assertEquals(ListenOutcome.Heard("", null, SpeechToolCatalog.END_NO_SPEECH, 8000), end(SpeechContract.REASON_TIMEOUT, null, 8200))
        }
        assertEquals("never recorded", 0L, (ListenAttempt().end(SpeechContract.REASON_TIMEOUT, null, 5000) as ListenOutcome.Heard).durationMillis)
    }

    @Test
    fun `a refusal before the recording or within the check time falls back`() {
        ListenAttempt().apply {
            assertEquals("the foreground service is refused before the model loaded", SessionAction.CANCEL, warning(SpeechErrorCodes.PERMISSION_DENIED, "Android refused the microphone foreground service", 10))
            assertFalse(recording)
            assertEquals(SessionAction.NONE, warning(SpeechErrorCodes.PERMISSION_DENIED, silenced, 20))
            assertEquals(ListenOutcome.Fallback("Android refused the microphone foreground service"), end(SpeechContract.REASON_CANCELLED, null, 30))
        }
        ListenAttempt(checkMillis = 3000).apply {
            recording(1000)
            assertEquals(SessionAction.CANCEL, warning(SpeechErrorCodes.PERMISSION_DENIED, silenced, 3999))
            assertEquals("text after the decision is ignored", SessionAction.NONE, final("hi", "en"))
            assertEquals(ListenOutcome.Fallback(silenced), end(SpeechContract.REASON_CANCELLED, null, 4100))
        }
        ListenAttempt().apply {
            assertEquals(SessionAction.CANCEL, warning(SpeechErrorCodes.PERMISSION_DENIED, " ", 0))
            assertEquals("a blank message becomes the code", ListenOutcome.Fallback(SpeechErrorCodes.PERMISSION_DENIED), end(SpeechContract.REASON_CANCELLED, null, 1))
        }
    }

    @Test
    fun `a later refusal finishes the dictation and fails it only without text`() {
        ListenAttempt(checkMillis = 3000).apply {
            recording(1000)
            assertEquals(SessionAction.FINISH, warning(SpeechErrorCodes.PERMISSION_DENIED, silenced, 4000))
            assertEquals("finished once", SessionAction.NONE, warning(SpeechErrorCodes.PERMISSION_DENIED, silenced, 4500))
            assertEquals(ListenOutcome.Failed(SpeechErrorCodes.PERMISSION_DENIED, silenced), end(SpeechContract.REASON_STOPPED, null, 4600))
        }
        ListenAttempt(checkMillis = 3000).apply {
            recording(0)
            assertEquals(SessionAction.FINISH, final("turn on the light", "en"))
            assertEquals(SessionAction.FINISH, warning(SpeechErrorCodes.PERMISSION_DENIED, silenced, 5000))
            val heard = end(SpeechContract.REASON_STOPPED, null, 5100) as ListenOutcome.Heard
            assertEquals("turn on the light", heard.text)
            assertEquals("the refusal stays a warning", listOf(ToolWarning(SpeechErrorCodes.PERMISSION_DENIED, silenced)), heard.warnings)
        }
    }

    @Test
    fun `other warnings go into the result once each`() {
        val attempt = ListenAttempt()
        attempt.recording(0)
        val hotwords = "sherpa-onnx-en does not support hotwords; 2 hotwords were ignored"
        assertEquals(SessionAction.NONE, attempt.warning(SpeechErrorCodes.INVALID_OPTIONS, hotwords, 0))
        assertEquals(SessionAction.NONE, attempt.warning(SpeechErrorCodes.INVALID_OPTIONS, hotwords, 10))
        assertEquals(SessionAction.NONE, attempt.warning(null, null, 20))
        val expected = listOf(ToolWarning(SpeechErrorCodes.INVALID_OPTIONS, hotwords), ToolWarning(SpeechErrorCodes.INTERNAL, SpeechErrorCodes.INTERNAL))
        assertEquals(expected, attempt.warnings)
        assertEquals(expected, (attempt.end(SpeechContract.REASON_TIMEOUT, null, 8000) as ListenOutcome.Heard).warnings)
    }

    @Test
    fun `errors fail the call with their speech code`() {
        ListenAttempt().apply {
            recording(0)
            error(SpeechErrorCodes.ENGINE_FAILED, "The recognizer crashed")
            error(SpeechErrorCodes.INTERNAL, "second")
            assertEquals(ListenOutcome.Failed(SpeechErrorCodes.ENGINE_FAILED, "The recognizer crashed"), end(SpeechContract.REASON_ERROR, null, 10))
        }
        ListenAttempt().apply {
            error(null, "")
            assertEquals(ListenOutcome.Failed(SpeechErrorCodes.INTERNAL, "The dictation failed"), end(SpeechContract.REASON_ERROR, null, 10))
        }
        assertEquals(
            "an error end without an error event",
            ListenOutcome.Failed(SpeechErrorCodes.INTERNAL, "The dictation ended with an error"),
            ListenAttempt().end(SpeechContract.REASON_ERROR, "text", 10),
        )
    }

    @Test
    fun `cancellation and the time limit cancel the session once`() {
        ListenAttempt().apply {
            recording(0)
            final("hello", "en")
            assertEquals(SessionAction.CANCEL, cancel())
            assertEquals(SessionAction.NONE, cancel())
            assertEquals(SessionAction.NONE, expire())
            assertTrue(recording)
            assertEquals("a cancelled call returns no text", ListenOutcome.Cancelled, end(SpeechContract.REASON_STOPPED, "hello", 100))
        }
        ListenAttempt().apply {
            assertEquals(SessionAction.CANCEL, expire())
            assertEquals(SessionAction.NONE, cancel())
            assertEquals(ListenOutcome.Failed(SpeechErrorCodes.TIMEOUT, "speech_listen ran out of time before the dictation ended"), end(SpeechContract.REASON_CANCELLED, null, 100))
        }
        assertEquals("a session cancelled elsewhere", ListenOutcome.Cancelled, ListenAttempt().end(SpeechContract.REASON_CANCELLED, null, 5))
        ListenAttempt().apply {
            end(SpeechContract.REASON_STOPPED, "done", 5)
            assertEquals("nothing to cancel after the end", SessionAction.NONE, cancel())
            assertEquals(SessionAction.NONE, warning(SpeechErrorCodes.PERMISSION_DENIED, silenced, 6))
        }
    }

}
