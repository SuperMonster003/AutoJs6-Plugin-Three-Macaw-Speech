package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.MacawRecognitionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import org.autojs.plugin.speech.api.SpeechContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The system recognition service through `SpeechRecognizer` (roadmap P5.7 E3, D56, opt-in `-e macawRecognition true`):
 * the plugin's component named explicitly, one utterance with the callback order, partial results and one result
 * (CER), `stopListening` during speech, `cancel` (no callback afterwards), silence (`ERROR_SPEECH_TIMEOUT`) and the
 * support check of API 33. The sound comes from the virtual room on emulators and the speaker on phones.
 * zh-CN: 经 `SpeechRecognizer` 使用系统识别服务 (路线图 P5.7 E3, D56, 需显式开启 `-e macawRecognition true`): 显式指定插件组件, 一句话的回调顺序,
 * 部分结果与一条结果 (CER), 说话中 `stopListening`, `cancel` (之后不再回调), 静音 (`ERROR_SPEECH_TIMEOUT`) 以及 API 33 的支持查询. 声音在模拟器上
 * 来自虚拟房间, 在手机上来自扬声器.
 */
@RunWith(AndroidJUnit4::class)
class RecognitionServiceDeviceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val component = ComponentName(context, MacawRecognitionService::class.java)
    private var recognizer: SpeechRecognizer? = null

    @Before
    fun prerequisites() {
        assumeTrue("opt-in: -e macawRecognition true", SpeechDevice.argument("macawRecognition") == "true")
        assumeTrue("RECORD_AUDIO is granted", context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        assumeTrue("a recognition model is installed", SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).isNotEmpty())
        AssistantDevice.setUpRoom()
    }

    @After
    fun cleanUp() {
        recognizer?.let { current -> instrumentation.runOnMainSync { current.destroy() } }
        AssistantDevice.tearDownRoom()
    }

    /** Records the callbacks with their time. zh-CN: 记录回调及其时间. */
    private class Events : RecognitionListener {
        val log: MutableList<Pair<Long, String>> = Collections.synchronizedList(ArrayList())
        var results: List<String> = emptyList()
        var partials = 0
        var error: Int? = null
        val ready = CountDownLatch(1)
        val done = CountDownLatch(1)

        private fun add(type: String) {
            log += SystemClock.elapsedRealtime() to type
        }

        override fun onReadyForSpeech(params: Bundle?) {
            add("ready")
            ready.countDown()
        }

        override fun onBeginningOfSpeech() = add("begin")
        override fun onRmsChanged(rmsdB: Float) = add("rms")
        override fun onBufferReceived(buffer: ByteArray?) = add("buffer")
        override fun onEndOfSpeech() = add("end")

        override fun onError(error: Int) {
            add("error")
            this.error = error
            done.countDown()
        }

        override fun onResults(results: Bundle?) {
            add("results")
            this.results = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            done.countDown()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            add("partial")
            partials++
        }

        override fun onEvent(eventType: Int, params: Bundle?) = add("event")

        fun milestones(): List<String> = synchronized(log) { log.map { it.second }.filter { it != "rms" && it != "partial" } }
    }

    private fun intent(partial: Boolean = true) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partial)
        .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

    private fun start(events: Events, intent: Intent = intent()) {
        instrumentation.runOnMainSync {
            val created = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context, component).also { recognizer = it }
            created.setRecognitionListener(events)
            created.startListening(intent)
        }
        assertTrue("onReadyForSpeech", events.ready.await(60, TimeUnit.SECONDS))
    }

    @Test
    fun oneUtteranceArrivesInOrderWithPartialResultsAndOneResult() {
        val events = Events()
        val started = SystemClock.elapsedRealtime()
        start(events)
        val ready = SystemClock.elapsedRealtime() - started
        AssistantDevice.play("test-audio/en.wav")
        assertTrue(events.done.await(30, TimeUnit.SECONDS))
        val text = events.results.firstOrNull().orEmpty()
        val cer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, text)
        SpeechDevice.log(
            "recognition ready=${ready} ms order=${events.milestones().joinToString(">")} partials=${events.partials} results=${events.results.size} " +
                "chars=${text.length} cer=${"%.3f".format(cer)} error=${events.error}",
        )
        assertEquals(listOf("ready", "begin", "end", "results"), events.milestones())
        assertEquals("one result (D46)", 1, events.results.size)
        assertTrue("partial results were asked for", events.partials > 0)
        assertTrue("CER $cer", cer <= MAX_CER)
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    @Test
    fun stopListeningReturnsWhatWasHeardAndCancelReportsNothing() {
        val stopped = Events()
        start(stopped, intent(partial = false))
        val speech = AssistantDevice.playInBackground("test-audio/en.wav")
        Thread.sleep(3_500)
        instrumentation.runOnMainSync { recognizer!!.stopListening() }
        assertTrue(stopped.done.await(30, TimeUnit.SECONDS))
        speech.join()
        SpeechDevice.log("recognition stop order=${stopped.milestones().joinToString(">")} chars=${stopped.results.firstOrNull()?.length} partials=${stopped.partials}")
        assertTrue("the words before stopListening", stopped.results.firstOrNull()?.isNotBlank() == true)
        assertEquals("no partial results unless asked", 0, stopped.partials)

        val cancelled = Events()
        start(cancelled)
        instrumentation.runOnMainSync { recognizer!!.cancel() }
        Thread.sleep(3_000)
        SpeechDevice.log("recognition cancel order=${cancelled.milestones().joinToString(">")}")
        assertEquals("nothing after cancel", listOf("ready"), cancelled.milestones())
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    @Test
    fun silenceEndsWithASpeechTimeout() {
        val events = Events()
        val started = SystemClock.elapsedRealtime()
        start(events)
        assertTrue(events.done.await(30, TimeUnit.SECONDS))
        SpeechDevice.log("recognition silence error=${events.error} after ${SystemClock.elapsedRealtime() - started} ms order=${events.milestones().joinToString(">")}")
        assertEquals(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, events.error)
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    @Test
    fun theSupportCheckListsTheInstalledLanguages() {
        assumeTrue("API 33", Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val done = CountDownLatch(1)
        var installed: List<String> = emptyList()
        var error: Int? = null
        instrumentation.runOnMainSync {
            val created = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context, component).also { recognizer = it }
            created.checkRecognitionSupport(intent(), context.mainExecutor, object : RecognitionSupportCallback {
                override fun onSupportResult(support: RecognitionSupport) {
                    installed = support.installedOnDeviceLanguages
                    done.countDown()
                }

                override fun onError(code: Int) {
                    error = code
                    done.countDown()
                }
            })
        }
        assertTrue(done.await(20, TimeUnit.SECONDS))
        val expected = SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).flatMap { it.languages }.toSet()
        SpeechDevice.log("recognition support installed=${installed.joinToString(",")} error=$error")
        assertEquals(expected, installed.toSet())
    }

    private companion object {
        const val MAX_CER = 0.15
    }
}
