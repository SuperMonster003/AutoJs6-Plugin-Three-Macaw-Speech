package io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition

import android.speech.SpeechRecognizer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.RecognitionStep.BeginningOfSpeech
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.RecognitionStep.EndOfSpeech
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.RecognitionStep.Error
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.RecognitionStep.Partial
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.RecognitionStep.ReadyForSpeech
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.RecognitionStep.Results
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.RecognitionStep.Rms
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The system recognition service (roadmap P5.7, D56): the callback order of one recognition, the error codes and the
 * permission and attribution rules.
 * zh-CN: 系统识别服务 (路线图 P5.7, D56): 一次识别的回调顺序, 错误码, 以及权限与归属规则.
 */
class RecognitionPolicyTest {

    private val quiet = Rms(RecognitionPolicy.RMS_QUIET)
    private val speech = Rms(RecognitionPolicy.RMS_SPEECH)

    @Test
    fun `one utterance reports ready, the speech, partials and one result`() {
        val flow = RecognitionFlow(partialResults = true)
        val steps = flow.onVoiceActivity(true) + flow.onReady() + flow.onReady() + flow.onVoiceActivity(true) + flow.onPartial("hello") +
            flow.onPartial("hello ") + flow.onPartial("hello world") + flow.onVoiceActivity(false) + flow.onFinal("  ", null) +
            flow.onFinal("Hello world.", "en") + flow.onEnd(SpeechContract.REASON_STOPPED, "Hello world.", "en") + flow.onError(SpeechErrorCodes.ENGINE_FAILED, true)
        assertEquals(
            listOf(ReadyForSpeech, quiet, BeginningOfSpeech, speech, Partial("hello"), Partial("hello world"), quiet, EndOfSpeech, Results("Hello world.", "en")),
            steps,
        )
        assertTrue(flow.done)
    }

    @Test
    fun `partial results only when the caller asked for them`() {
        val flow = RecognitionFlow(partialResults = false)
        assertEquals(listOf(ReadyForSpeech, quiet), flow.onReady())
        assertEquals(emptyList<RecognitionStep>(), flow.onPartial("hello"))
        assertEquals(
            "a final without voice activity still begins and ends the speech",
            listOf(BeginningOfSpeech, EndOfSpeech, Results("hello", null)),
            flow.onFinal("hello", null),
        )
    }

    @Test
    fun `silence ends with a speech timeout and unrecognized speech with no match`() {
        val silent = RecognitionFlow(partialResults = true)
        assertEquals(listOf(Error(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)), silent.onReady().let { silent.onEnd(SpeechContract.REASON_TIMEOUT, "", null) })
        val noise = RecognitionFlow(partialResults = true)
        noise.onReady()
        noise.onVoiceActivity(true)
        assertEquals(listOf(EndOfSpeech, Error(SpeechRecognizer.ERROR_NO_MATCH)), noise.onEnd(SpeechContract.REASON_TIMEOUT, "", null))
        val stopped = RecognitionFlow(partialResults = true)
        stopped.onReady()
        assertEquals("stopListening before any speech", listOf(Error(SpeechRecognizer.ERROR_NO_MATCH)), stopped.onEnd(SpeechContract.REASON_STOPPED, null, null))
    }

    @Test
    fun `the end text or the last partial is the result when no final came`() {
        val flow = RecognitionFlow(partialResults = true)
        flow.onReady()
        flow.onVoiceActivity(true)
        flow.onPartial("turn on")
        assertEquals(listOf(EndOfSpeech, Results("turn on", "zh")), flow.onEnd(SpeechContract.REASON_MAX_DURATION, null, "zh"))
        val ended = RecognitionFlow(partialResults = false)
        ended.onReady()
        assertEquals(listOf(BeginningOfSpeech, EndOfSpeech, Results("lights", null)), ended.onEnd(SpeechContract.REASON_STOPPED, " lights ", null))
    }

    @Test
    fun `a cancellation reports nothing and errors map to SpeechRecognizer codes`() {
        val cancelled = RecognitionFlow(partialResults = true)
        cancelled.onReady()
        cancelled.cancel()
        assertEquals(emptyList<RecognitionStep>(), cancelled.onFinal("late", null) + cancelled.onEnd(SpeechContract.REASON_CANCELLED, "", null))
        val ended = RecognitionFlow(partialResults = true)
        assertEquals(emptyList<RecognitionStep>(), ended.onEnd(SpeechContract.REASON_CANCELLED, "", null))
        assertTrue(ended.done)

        val failed = RecognitionFlow(partialResults = true)
        failed.onReady()
        failed.onVoiceActivity(true)
        assertEquals(listOf(EndOfSpeech, Error(SpeechRecognizer.ERROR_AUDIO)), failed.onError(SpeechErrorCodes.ENGINE_FAILED, recording = true))

        assertEquals(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, RecognitionPolicy.error(SpeechErrorCodes.PERMISSION_DENIED, recording = false))
        assertEquals(SpeechRecognizer.ERROR_RECOGNIZER_BUSY, RecognitionPolicy.error(SpeechErrorCodes.SESSION_BUSY, recording = false))
        assertEquals(SpeechRecognizer.ERROR_CLIENT, RecognitionPolicy.error(SpeechErrorCodes.INVALID_OPTIONS, recording = false))
        assertEquals(SpeechRecognizer.ERROR_SERVER, RecognitionPolicy.error(SpeechErrorCodes.MODEL_MISSING, recording = false))
        assertEquals(SpeechRecognizer.ERROR_SERVER, RecognitionPolicy.error(SpeechErrorCodes.ENGINE_FAILED, recording = false))
        assertEquals(SpeechRecognizer.ERROR_AUDIO, RecognitionPolicy.error(SpeechErrorCodes.ENGINE_FAILED, recording = true))
        assertEquals(SpeechRecognizer.ERROR_SERVER, RecognitionPolicy.error(SpeechErrorCodes.INTERNAL, recording = true))
        assertEquals(SpeechRecognizer.ERROR_SERVER, RecognitionPolicy.error(null, recording = false))
    }

    @Test
    fun `both the plugin and the caller need the microphone and API 31 attributes to the caller`() {
        assertEquals(RecognitionAccess.CHECKED, RecognitionPolicy.access(sdk = 30, selfGranted = true, callerGranted = true, callerAudio = false))
        assertEquals(RecognitionAccess.ATTRIBUTED, RecognitionPolicy.access(sdk = 31, selfGranted = true, callerGranted = true, callerAudio = false))
        assertEquals(RecognitionAccess.DENIED, RecognitionPolicy.access(sdk = 24, selfGranted = true, callerGranted = false, callerAudio = false))
        assertEquals(RecognitionAccess.DENIED, RecognitionPolicy.access(sdk = 36, selfGranted = false, callerGranted = true, callerAudio = false))
        assertEquals(RecognitionAccess.UNSUPPORTED, RecognitionPolicy.access(sdk = 33, selfGranted = true, callerGranted = false, callerAudio = true))
        assertEquals(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, RecognitionPolicy.refusal(RecognitionAccess.DENIED))
        assertEquals(SpeechRecognizer.ERROR_CLIENT, RecognitionPolicy.refusal(RecognitionAccess.UNSUPPORTED))
        assertNull(RecognitionPolicy.refusal(RecognitionAccess.ATTRIBUTED))
        assertNull(RecognitionPolicy.refusal(RecognitionAccess.CHECKED))
    }
}
