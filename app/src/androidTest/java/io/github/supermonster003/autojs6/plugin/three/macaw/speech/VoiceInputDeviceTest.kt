package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputComponent
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The system voice input on a real microphone (roadmap P5.6, opt-in `-e macawVoice true`, run by
 * `.python/device_ui_run.py voice`): `ACTION_RECOGNIZE_SPEECH` is sent with `startActivityForResult` as any app would,
 * the test recording is played through the device speaker while the sheet listens, and the activity result is checked:
 * `EXTRA_RESULTS` with one text and its CER, "Stop" during speech, "Cancel", silence (`RESULT_NO_MATCH`) and a microphone
 * held by a script session (the busy guidance, `RESULT_AUDIO_ERROR`). Each run releases the microphone. Needs
 * RECORD_AUDIO (granted by the maintainer) and an installed recognition model; screenshots go to `screens/`.
 * zh-CN: 真机麦克风上的系统语音输入 (路线图 P5.6, 需显式开启 `-e macawVoice true`, 由 `.python/device_ui_run.py voice` 运行): 像任何应用一样以
 * `startActivityForResult` 发出 `ACTION_RECOGNIZE_SPEECH`, 弹层收音时经设备扬声器播放测试录音, 再核对 Activity 结果: `EXTRA_RESULTS` 中的一条文字及其
 * CER, 说话中点 "停止", "取消", 静音 (`RESULT_NO_MATCH`), 以及麦克风被脚本会话占用 (占用引导, `RESULT_AUDIO_ERROR`). 每次运行后麦克风都被释放. 需要
 * RECORD_AUDIO (由维护者授权) 与已安装的识别模型; 截图保存到 `screens/`.
 */
@RunWith(AndroidJUnit4::class)
class VoiceInputDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefix: String get() = SpeechDevice.argument("macawPrefix") ?: "voice"

    @Before
    fun prerequisites() {
        assumeTrue("opt-in: -e macawVoice true", SpeechDevice.argument("macawVoice") == "true")
        assumeTrue("RECORD_AUDIO is granted by the maintainer", context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        assumeTrue("a recognition model is installed", SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).isNotEmpty())
        VoiceInputComponent.setEnabled(context, true)
    }

    /** As 3-Stove Agent asks: free form, the UI language; plus the extras the sheet accepts or ignores (D46). */
    private fun recognize(language: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .setClassName(context, VoiceInputActivity::class.java.name)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        .putExtra(RecognizerIntent.EXTRA_PROMPT, "3-Macaw Speech voice input test")
        .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        // RecognizerIntent.EXTRA_BIASING_STRINGS (API 33): hotwords on a transducer with bpe.vocab, dropped elsewhere.
        .putStringArrayListExtra("android.speech.extra.BIASING_STRINGS", arrayListOf("NIGHTFALL", "SQUALID QUARTER"))

    @Test
    fun oneUtteranceComesBackInExtraResults() {
        val result = ActivityScenario.launchActivityForResult<VoiceInputActivity>(recognize("zh-CN")).use { scenario ->
            awaitListening()
            capture("listening")
            val playback = play("test-audio/en.wav")
            // D42: the microphone pulses and the state reads "hearing" while the VAD hears the recording.
            // The sheet follows the language of AutoJs6, which may differ from the device language of the test context.
            val hearing = AssistantDevice.sheetText(R.string.voice_input_hearing)
            val heard = runCatching { compose.waitUntil(4_000) { text("macaw.voice.state") == hearing } }.isSuccess
            capture("speaking")
            SpeechDevice.log("voice hearing shown=$heard")
            awaitPlayback(playback)
            val ended = System.currentTimeMillis()
            scenario.result.also { SpeechDevice.log("voice result after playback ${System.currentTimeMillis() - ended} ms") }
        }
        assertEquals(Activity.RESULT_OK, result.resultCode)
        val results = result.resultData?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        val cer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, results.firstOrNull().orEmpty())
        SpeechDevice.log("voice utterance results=${results.size} chars=${results.firstOrNull()?.length ?: 0} cer=${"%.3f".format(cer)}")
        assertEquals("one result whatever EXTRA_MAX_RESULTS asks (D46)", 1, results.size)
        assertTrue("CER $cer", cer <= MAX_CER)
        assertTrue("the microphone is free after the sheet closed", microphoneIsFree())
    }

    @Test
    fun stopReturnsWhatWasHeard() {
        val result = ActivityScenario.launchActivityForResult<VoiceInputActivity>(recognize("en-US")).use { scenario ->
            awaitListening()
            val playback = play("test-audio/en.wav")
            Thread.sleep(3_500)
            compose.onNodeWithTag("macaw.voice.stop").performSemanticsAction(SemanticsActions.OnClick)
            val result = scenario.result
            awaitPlayback(playback)
            result
        }
        val text = result.resultData?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        SpeechDevice.log("voice stop code=${result.resultCode} chars=${text.length} cer=${"%.3f".format(SpeechDevice.cer(SpeechDevice.EN_REFERENCE, text))}")
        assertEquals(Activity.RESULT_OK, result.resultCode)
        assertTrue("the words before Stop", text.isNotBlank())
        assertTrue(microphoneIsFree())
    }

    @Test
    fun cancelAndSilenceReturnNoText() {
        val cancelled = ActivityScenario.launchActivityForResult<VoiceInputActivity>(recognize("en-US")).use { scenario ->
            awaitListening()
            compose.onNodeWithTag("macaw.voice.cancel").performSemanticsAction(SemanticsActions.OnClick)
            scenario.result
        }
        SpeechDevice.log("voice cancel code=${cancelled.resultCode}")
        assertEquals(Activity.RESULT_CANCELED, cancelled.resultCode)
        assertTrue(microphoneIsFree())

        val started = System.currentTimeMillis()
        val silent = ActivityScenario.launchActivityForResult<VoiceInputActivity>(recognize("en-US")).use { scenario ->
            awaitListening()
            scenario.result
        }
        SpeechDevice.log("voice silence code=${silent.resultCode} after ${System.currentTimeMillis() - started} ms")
        assertEquals("8 s without speech (D44)", RecognizerIntent.RESULT_NO_MATCH, silent.resultCode)
        assertTrue(microphoneIsFree())
    }

    @Test
    fun aScriptSessionHoldingTheMicrophoneShowsTheBusyGuidance() {
        val ended = CountDownLatch(1)
        val script = SpeechDevice.plugin.openSession(Bundle().apply { putLong(SpeechOptionKeys.TIMEOUT_MILLIS, 0) }, endCallback(ended))
        try {
            val result = ActivityScenario.launchActivityForResult<VoiceInputActivity>(recognize("en-US")).use { scenario ->
                compose.waitUntil(20_000) { exists("macaw.voice.problem.busy") }
                capture("busy")
                compose.onNodeWithTag("macaw.voice.problem.close").performSemanticsAction(SemanticsActions.OnClick)
                scenario.result
            }
            SpeechDevice.log("voice busy code=${result.resultCode}")
            assertEquals(RecognizerIntent.RESULT_AUDIO_ERROR, result.resultCode)
        } finally {
            script.cancel()
            assertTrue(ended.await(15, TimeUnit.SECONDS))
        }
        assertTrue(microphoneIsFree())
    }

    private fun awaitListening() {
        try {
            compose.waitUntil(30_000) { text("macaw.voice.state") in AssistantDevice.listeningTexts() }
        } catch (e: Throwable) {
            capture("not-listening")
            SpeechDevice.log("voice state=${text("macaw.voice.state")} problem=${problemShown()}")
            throw e
        }
    }

    private fun problemShown(): String? = listOf("microphone", "no_model", "busy", "failed").firstOrNull { exists("macaw.voice.problem.$it") }

    /** A script session can open, so the sheet released the microphone. zh-CN: 脚本会话能够打开, 说明弹层已释放麦克风. */
    private fun microphoneIsFree(): Boolean {
        val ended = CountDownLatch(1)
        val deadline = System.currentTimeMillis() + 5_000
        var session: ISpeechSession? = null
        while (session == null && System.currentTimeMillis() < deadline) {
            session = runCatching { SpeechDevice.plugin.openSession(Bundle().apply { putLong(SpeechOptionKeys.TIMEOUT_MILLIS, 0) }, endCallback(ended)) }.getOrNull()
            if (session == null) Thread.sleep(200)
        }
        session ?: return false
        session.cancel()
        return ended.await(15, TimeUnit.SECONDS)
    }

    private fun endCallback(ended: CountDownLatch) = object : ISpeechSessionCallback.Stub() {
        override fun onReady(event: Bundle?) = Unit
        override fun onPartial(event: Bundle?) = Unit
        override fun onFinal(event: Bundle?) = Unit
        override fun onEndpoint() = Unit
        override fun onVoiceActivity(event: Bundle?) = Unit
        override fun onWarning(event: Bundle?) = Unit
        override fun onError(event: Bundle?) = Unit
        override fun onEnd(event: Bundle?) = ended.countDown()
    }

    /** Starts an androidTest recording on the speaker. zh-CN: 开始经扬声器播放 androidTest 录音. */
    private fun play(asset: String): Pair<MediaPlayer, CountDownLatch> {
        val file = File(context.cacheDir, "voice-${asset.substringAfterLast('/')}")
        instrumentation.context.assets.open(asset).use { input -> file.outputStream().use { input.copyTo(it) } }
        val done = CountDownLatch(1)
        val player = MediaPlayer()
        instrumentation.runOnMainSync {
            player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener { done.countDown() }
            player.prepare()
            player.start()
        }
        return player to done
    }

    private fun awaitPlayback(playback: Pair<MediaPlayer, CountDownLatch>) {
        try {
            assertTrue(playback.second.await(120, TimeUnit.SECONDS))
        } finally {
            instrumentation.runOnMainSync { playback.first.release() }
        }
    }

    private fun SemanticsNodeInteractionsProvider.nodes(tag: String) = onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()

    private fun exists(tag: String): Boolean = compose.nodes(tag).isNotEmpty()

    private fun text(tag: String): String? = compose.nodes(tag).firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }

    private fun capture(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val folder = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }
        val file = File(folder, "$prefix-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        SpeechDevice.log("screen ${file.name} ${bitmap.width}x${bitmap.height}")
    }

    private companion object {
        /** The tryout runs read en.wav with CER 0.000 (roadmap P5.3); the speaker and the room add a margin. */
        const val MAX_CER = 0.15
    }
}
