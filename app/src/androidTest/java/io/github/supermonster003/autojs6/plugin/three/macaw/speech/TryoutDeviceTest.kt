package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.TryoutActivity
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.SpeechOptionKeys
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
 * The tryout screen on a real microphone (roadmap P5.3, opt-in `-e macawTryout true`, run by
 * `.python/device_ui_run.py tryout`): the test recordings are played through the device speaker while the screen
 * listens, so dictation, the wake word trial and the synthesis playback run exactly as a user sees them; screenshots go to
 * the external files folder `screens/`. Leaving the screen must release the microphone: a script session opens right
 * after. Needs RECORD_AUDIO (granted by the maintainer) and the default recognition, wake word and synthesis models.
 * zh-CN: 真机麦克风上的试听试说页 (路线图 P5.3, 需显式开启 `-e macawTryout true`, 由 `.python/device_ui_run.py tryout` 运行): 测试录音经设备扬声器播放,
 * 页面同时收音, 听写, 唤醒词试验与合成播放的运行方式与用户所见完全一致; 截图保存到外部文件目录 `screens/`. 离开页面必须释放麦克风: 随后能立即打开
 * 脚本会话. 需要 RECORD_AUDIO (由维护者授权) 以及默认的识别, 唤醒词与合成模型.
 */
@RunWith(AndroidJUnit4::class)
class TryoutDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefix: String get() = SpeechDevice.argument("macawPrefix") ?: "tryout"

    @Before
    fun prerequisites() {
        assumeTrue("opt-in: -e macawTryout true", SpeechDevice.argument("macawTryout") == "true")
        assumeTrue("RECORD_AUDIO is granted by the maintainer", context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    @Test
    fun dictationWakeWordAndSynthesisOnTheMicrophone() {
        ActivityScenario.launch<TryoutActivity>(Intent(context, TryoutActivity::class.java)).use {
            settle()
            // Dictation: the English recording through the speaker.
            compose.waitUntil(10_000) { exists("macaw.tryout.listen.start") }
            compose.onNodeWithTag("macaw.tryout.listen.start").performClick()
            compose.waitUntil(20_000) { text("macaw.tryout.listen.state") == context.getString(R.string.tryout_listening) }
            play("test-audio/en.wav")
            // An offline model answers after the endpoint silence and one decoding pass of the segment.
            runCatching { compose.waitUntil(15_000) { exists("macaw.tryout.final.0") } }
            Thread.sleep(1_000)
            compose.waitForIdle()
            val finals = texts("macaw.tryout.final.")
            capture("dictation")
            compose.onNodeWithTag("macaw.tryout.listen.stop").performClick()
            compose.waitUntil(10_000) { exists("macaw.tryout.listen.start") }
            val heard = finals.joinToString(" ")
            SpeechDevice.log("tryout dictation finals=${finals.size} cer=${"%.3f".format(SpeechDevice.cer(SpeechDevice.EN_REFERENCE, heard))}")
            assertTrue("the transcript shows finals", finals.isNotEmpty())

            // Wake word: the English keywords of the same recording.
            compose.onNodeWithTag("macaw.tryout.tab.1").performClick()
            settle()
            // The keyword of kws-zh-6 (the acoustic wake check of roadmap P3.1) and an English one of en.
            compose.onNodeWithTag("macaw.tryout.keywords").performTextReplacement("\u6731\u4e3d\u6960\nLIGHT UP")
            settle()
            // The keyboard of the keyword field moves the layout: click through the semantics action, not a position.
            compose.onNodeWithTag("macaw.tryout.wake.start").performSemanticsAction(SemanticsActions.OnClick)
            try {
                compose.waitUntil(20_000) { text("macaw.tryout.wake.state") == context.getString(R.string.tryout_listening) }
            } catch (e: Throwable) {
                capture("wake-failure")
                SpeechDevice.log("tryout wake state=${text("macaw.tryout.wake.state")} snackbar=${snackbar()}")
                throw e
            }
            play("test-audio/kws-zh-6.wav")
            Thread.sleep(1_000)
            play("test-audio/en.wav")
            runCatching { compose.waitUntil(10_000) { exists("macaw.tryout.hit.1") } }
            compose.waitForIdle()
            val hits = texts("macaw.tryout.hit.")
            capture("wake")
            compose.onNodeWithTag("macaw.tryout.wake.stop").performClick()
            compose.waitUntil(10_000) { exists("macaw.tryout.wake.start") }
            SpeechDevice.log("tryout wake hits=${hits.size} keywords=${hits.joinToString("|")}")
            assertTrue("the trial lists hits", hits.isNotEmpty())

            // Synthesis: the default sample with the default voice, played back.
            compose.onNodeWithTag("macaw.tryout.tab.2").performClick()
            settle()
            val started = System.currentTimeMillis()
            compose.onNodeWithTag("macaw.tryout.play").performClick()
            compose.waitUntil(300_000) { exists("macaw.tryout.stop") && !exists("macaw.tryout.synthesisProgress") }
            val synthesized = System.currentTimeMillis() - started
            Thread.sleep(800)
            capture("synthesis")
            compose.waitUntil(120_000) { exists("macaw.tryout.play") }
            SpeechDevice.log("tryout synthesis playedAfterMillis=$synthesized")
        }
        // Leaving the screen released the microphone: a script session can open now.
        assertTrue("the microphone is free after the tryout screen closed", openAndCancelMicrophoneSession())
        SpeechDevice.log("tryout microphone released=true")
    }

    private fun openAndCancelMicrophoneSession(): Boolean {
        val ended = CountDownLatch(1)
        val callback = object : ISpeechSessionCallback.Stub() {
            override fun onReady(event: Bundle?) = Unit
            override fun onPartial(event: Bundle?) = Unit
            override fun onFinal(event: Bundle?) = Unit
            override fun onEndpoint() = Unit
            override fun onVoiceActivity(event: Bundle?) = Unit
            override fun onWarning(event: Bundle?) = Unit
            override fun onError(event: Bundle?) = Unit
            override fun onEnd(event: Bundle?) = ended.countDown()
        }
        // The screen releases its slot on the ASR thread right after onStop; allow that a moment.
        val deadline = System.currentTimeMillis() + 5_000
        var session = runCatching { SpeechDevice.plugin.openSession(Bundle().apply { putLong(SpeechOptionKeys.TIMEOUT_MILLIS, 0) }, callback) }.getOrNull()
        while (session == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(200)
            session = runCatching { SpeechDevice.plugin.openSession(Bundle().apply { putLong(SpeechOptionKeys.TIMEOUT_MILLIS, 0) }, callback) }.getOrNull()
        }
        if (session == null) return false
        session.cancel()
        return ended.await(15, TimeUnit.SECONDS)
    }

    /** Plays an androidTest recording through the speaker and waits for its end. zh-CN: 经扬声器播放 androidTest 录音并等待结束. */
    private fun play(asset: String) {
        val file = File(context.cacheDir, "tryout-${asset.substringAfterLast('/')}")
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
        try {
            assertTrue(done.await(120, TimeUnit.SECONDS))
        } finally {
            instrumentation.runOnMainSync { player.release() }
            file.delete()
        }
    }

    private fun SemanticsNodeInteractionsProvider.nodes(tag: String) = onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()

    private fun exists(tag: String): Boolean = compose.nodes(tag).isNotEmpty()

    private fun text(tag: String): String? = compose.nodes(tag).firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }

    /** Texts of the nodes tagged `<prefix>0`, `<prefix>1`, ... zh-CN: 标签为 `<prefix>0`, `<prefix>1`, ... 的节点文字. */
    private fun texts(prefix: String): List<String> = generateSequence(0) { it + 1 }.map { index ->
        compose.nodes("$prefix$index").firstOrNull()?.let { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
                ?: node.children.flatMap { child -> child.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }.joinToString(" ")
        }
    }.takeWhile { it != null }.map { it!! }.toList()

    private fun snackbar(): String = compose.onAllNodes(hasTestTag("macaw.snackbar"), useUnmergedTree = true).fetchSemanticsNodes()
        .flatMap { node -> node.children.flatMap { child -> child.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } + child.children.flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty().map { t -> t.text } } } }
        .joinToString(" | ")

    private fun settle() {
        compose.waitForIdle()
        Thread.sleep(800)
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("no screenshot")
        val folder = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }
        val file = File(folder, "$prefix-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        SpeechDevice.log("screen ${file.name} ${bitmap.width}x${bitmap.height}")
    }
}
