package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.capture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.editable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.exists
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.text
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition.MacawRecognitionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant.AssistantActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechPluginBinder
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.ISpeechWakeCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap D57 on a device (opt-in `-e macawTakeover true`): a test client holds a wake session for the keyword of
 * `kws-zh-6.wav` (spotted in five of five plays of `docs/development/kws-baseline.md` and in 83 of 84 plays of a probe;
 * `LIGHT UP` of `en.wav` misses one in five), replayed at most three times until it is heard before and after. The
 * digital assistant, the system recognition service and the voice input sheet each take the microphone over while the
 * same recording and then `en.wav` play, and the keyword is not reported; when they end, the recording wakes the session
 * again. A dictation of a script is never taken over (the busy guidance of D47). Roadmap P11.4.3 (D190, D228 - D230): the
 * resident wake word and a script's background session pause for a listen and a foreground wake instead of failing them,
 * resume in queue order with `paused` / `resumed` and their reasons, a `thenListen` round holds the microphone, and a
 * background session hears as often as a foreground one.
 * zh-CN: 设备上的路线图 D57 (需显式开启 `-e macawTakeover true`): 测试客户端持有 `kws-zh-6.wav` 中关键词的唤醒会话
 * (`docs/development/kws-baseline.md` 中五次播放五次命中, 探测中 84 次命中 83 次; `en.wav` 中的 `LIGHT UP` 五次漏一次), 接管前后至多重放三次直到
 * 听到. 数字助理, 系统识别服务与语音输入弹层各自接管麦克风, 期间播放同一录音再播放 `en.wav`, 唤醒词不被报告; 它们结束后, 该录音再次唤醒会话. 脚本的
 * 听写从不被接管 (D47 的占用引导). 路线图 P11.4.3 (D190, D228 - D230): 常驻唤醒与脚本的后台会话为 listen 与前台 wake 暂停而不是让它们失败,
 * 以 `paused` / `resumed` 及其原因按队列顺序恢复, `thenListen` 轮次占用麦克风, 后台会话的命中率不低于前台会话.
 */
@RunWith(AndroidJUnit4::class)
class TakeoverDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefix: String get() = SpeechDevice.argument("macawPrefix") ?: "takeover"

    @Before
    fun prerequisites() {
        assumeTrue("opt-in: -e macawTakeover true", SpeechDevice.argument("macawTakeover") == "true")
        assumeTrue("RECORD_AUDIO is granted", context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        assumeTrue("the wake word model is installed", SpeechDevice.installed(SpeechDevice.KWS))
        assumeTrue("a recognition model is installed", SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).isNotEmpty())
        AssistantDevice.setUpRoom()
    }

    @After
    fun cleanUp() {
        AssistantDevice.resumed().filterIsInstance<AssistantActivity>().forEach { activity -> activity.runOnUiThread { activity.finish() } }
        AssistantDevice.tearDownRoom()
    }

    /** The keyword is heard before; [takeover] runs without it; the keyword is heard again after. zh-CN: 之前能听到唤醒词; 接管期间听不到; 之后又能听到. */
    private fun around(name: String, takeover: (AssistantDevice.Wake) -> Unit) {
        AssistantDevice.Wake(KEYWORD).use { wake ->
            val plays = hear(wake, 1)
            val before = wake.hits.size
            val busy = SpeechRuntime.get(context).sessions
            takeover(wake)
            Thread.sleep(HIT_WAIT_MILLIS)
            val during = wake.hits.size - before
            assertFalse("the wake session stays open", wake.ended)
            // The takeover ended: the wake session records again.
            val deadline = System.currentTimeMillis() + 5_000
            while (busy.takenOver && System.currentTimeMillis() < deadline) Thread.sleep(100)
            Thread.sleep(500)
            val replays = hear(wake, before + during + 1)
            val after = wake.hits.size - before - during
            SpeechDevice.log(
                "takeover $name hits before=$before during=$during after=$after plays=$plays/$replays events=${wake.events.filter { it != "wake" }.joinToString(",")}",
            )
            assertTrue("the keyword wakes the session before the takeover", before >= 1)
            assertEquals("no wake while taken over (D57)", 0, during)
            assertTrue("the keyword wakes the session again after the takeover", after >= 1)
            assertEquals("no other event: no error and no end", listOf("ready"), wake.events.filter { it != "wake" })
        }
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    /**
     * Plays the keyword until the session has [count] hits, at most [MAX_PLAYS] times: a plain wake session spots it in
     * about 97 % of the plays (83 of 84 in a probe, with or without takeovers), so one play may miss without any fault.
     * The keyword ends near the end of the recording, so a hit may come a moment after the playback. Answers the plays.
     * zh-CN: 播放关键词直到会话命中 [count] 次, 至多 [MAX_PLAYS] 次: 普通唤醒会话约 97 % 的播放能命中 (探测中 84 次命中 83 次, 有无接管均如此), 因此
     * 一次播放没有命中并不说明有故障. 关键词在录音末尾附近, 命中可能在播放结束后片刻才到. 返回播放次数.
     */
    private fun hear(wake: AssistantDevice.Wake, count: Int): Int {
        var plays = 0
        while (plays < MAX_PLAYS && wake.hits.size < count) {
            AssistantDevice.play(KEYWORD_AUDIO)
            plays++
            val deadline = System.currentTimeMillis() + HIT_WAIT_MILLIS
            while (wake.hits.size < count && System.currentTimeMillis() < deadline) Thread.sleep(100)
        }
        return plays
    }

    @Test
    fun theAssistantSilencesAWakeSessionThatResumesAfterwards() = around("assistant") {
        context.startActivity(AssistantActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        compose.waitUntil(30_000) { compose.text("macaw.voice.state") in AssistantDevice.listeningTexts() }
        assertTrue("taken over", SpeechRuntime.get(context).sessions.takenOver)
        // The keyword while the assistant listens, then English speech for its result.
        AssistantDevice.play(KEYWORD_AUDIO)
        if (!compose.exists("macaw.assistant.text")) AssistantDevice.play("test-audio/en.wav")
        compose.waitUntil(30_000) { compose.exists("macaw.assistant.text") || compose.exists("macaw.assistant.noMatch") }
        SpeechDevice.log("takeover assistant chars=${compose.editable("macaw.assistant.text")?.length} noMatch=${compose.exists("macaw.assistant.noMatch")}")
        capture(prefix, "assistant")
        compose.onNodeWithTag("macaw.assistant.close").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(AssistantDevice.awaitGone(AssistantActivity::class.java.name))
    }

    @Test
    fun theRecognitionServiceSilencesAWakeSessionToo() = around("recognition") {
        val ready = CountDownLatch(1)
        val done = CountDownLatch(1)
        var results: List<String> = emptyList()
        var recognizer: SpeechRecognizer? = null
        instrumentation.runOnMainSync {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context, ComponentName(context, MacawRecognitionService::class.java)).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = ready.countDown()
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onError(error: Int) = done.countDown()
                    override fun onResults(bundle: Bundle?) {
                        results = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        done.countDown()
                    }

                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"))
            }
        }
        try {
            assertTrue(ready.await(60, TimeUnit.SECONDS))
            AssistantDevice.play(KEYWORD_AUDIO)
            if (done.count > 0) AssistantDevice.play("test-audio/en.wav")
            assertTrue(done.await(30, TimeUnit.SECONDS))
            SpeechDevice.log("takeover recognition results=${results.size} chars=${results.firstOrNull()?.length}")
        } finally {
            instrumentation.runOnMainSync { recognizer?.destroy() }
        }
    }

    @Test
    fun theVoiceInputSheetSilencesAWakeSessionToo() = around("voice-input") {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).setClassName(context, VoiceInputActivity::class.java.name)
        val result = ActivityScenario.launchActivityForResult<VoiceInputActivity>(intent).use { scenario ->
                compose.waitUntil(30_000) { compose.text("macaw.voice.state") in AssistantDevice.listeningTexts() }
            AssistantDevice.play(KEYWORD_AUDIO)
            AssistantDevice.playInBackground("test-audio/en.wav")
            scenario.result
        }
        val text = result.resultData?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        SpeechDevice.log("takeover voice-input code=${result.resultCode} chars=${text.length}")
        assertTrue("a result or no match", result.resultCode == Activity.RESULT_OK || result.resultCode == RecognizerIntent.RESULT_NO_MATCH)
    }

    @Test
    fun aScriptDictationIsNeverTakenOver() {
        val ended = CountDownLatch(1)
        val script = AssistantDevice.dictation(ended)
        try {
            context.startActivity(AssistantActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            compose.waitUntil(30_000) { compose.exists("macaw.voice.problem.busy") }
            capture(prefix, "busy")
            assertTrue("the script dictation goes on", script.isActive)
            compose.onNodeWithTag("macaw.voice.problem.close").performSemanticsAction(SemanticsActions.OnClick)
            assertTrue(AssistantDevice.awaitGone(AssistantActivity::class.java.name))
            SpeechDevice.log("takeover dictation busy guidance shown, script active=${script.isActive}")
        } finally {
            script.cancel()
            assertTrue(ended.await(15, TimeUnit.SECONDS))
        }
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    /**
     * A background holder of the session table (roadmap D190, D228 - D230): a wake session opened with `background`, or
     * the resident wake word through the in-process entry of the resident service (its switch stays untouched), with
     * its events in [log] in arrival order as "<name> <event> [reason]".
     * zh-CN: 会话表的后台持有者 (路线图 D190, D228 - D230): 以 `background` 打开的唤醒会话, 或经常驻服务的进程内入口打开的常驻唤醒 (不动其开关),
     * 事件按到达顺序记入 [log], 形如 "<名称> <事件> [原因]".
     */
    private class Holder(
        val name: String,
        private val log: MutableList<String>,
        resident: Boolean = false,
        thenListenMillis: Long? = null,
    ) : AutoCloseable {
        val hits: MutableList<String> = Collections.synchronizedList(ArrayList())
        private val ready = CountDownLatch(1)
        private val endLatch = CountDownLatch(1)
        val session: ISpeechSession

        init {
            val options = Bundle().apply {
                putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CALL_VERSION_3)
                putParcelableArrayList(SpeechOptionKeys.KEYWORDS, arrayListOf(Bundle().apply { putString(SpeechOptionKeys.KEYWORD, KEYWORD) }))
                putString(SpeechOptionKeys.MODEL, SpeechDevice.KWS)
                if (!resident) putBoolean(SpeechOptionKeys.BACKGROUND, true)
                thenListenMillis?.let { millis -> putBundle(SpeechOptionKeys.THEN_LISTEN, Bundle().apply { putLong(SpeechOptionKeys.TIMEOUT_MILLIS, millis) }) }
            }
            val callback = object : ISpeechWakeCallback.Stub() {
                override fun onPaused(info: Bundle?) = record("paused ${info?.getString(SpeechResultKeys.REASON)}")
                override fun onResumed(info: Bundle?) = record("resumed ${info?.getString(SpeechResultKeys.REASON)}")
                override fun onReady(event: Bundle?) {
                    record("ready")
                    ready.countDown()
                }

                override fun onWake(event: Bundle?) {
                    hits += event?.getString(SpeechResultKeys.KEYWORD).orEmpty()
                    record("wake")
                }

                override fun onWarning(event: Bundle?) = record("warning ${event?.getString(SpeechContract.KEY_ERROR_CODE)}")
                override fun onError(event: Bundle?) = record("error ${event?.getString(SpeechContract.KEY_ERROR_CODE)}")
                override fun onEnd(event: Bundle?) {
                    record("end")
                    endLatch.countDown()
                }
            }
            val round = thenListenMillis?.let {
                object : ISpeechSessionCallback.Stub() {
                    override fun onReady(event: Bundle?) = record("round ready")
                    override fun onPartial(event: Bundle?) = Unit
                    override fun onFinal(event: Bundle?) = Unit
                    override fun onEndpoint() = Unit
                    override fun onVoiceActivity(event: Bundle?) = Unit
                    override fun onWarning(event: Bundle?) = Unit
                    override fun onError(event: Bundle?) = record("round error")
                    override fun onEnd(event: Bundle?) = record("round end ${event?.getString(SpeechResultKeys.REASON)}")
                }
            }
            session = if (resident) {
                (SpeechDevice.plugin as SpeechPluginBinder).openResidentWakeSession(options, callback)
            } else {
                SpeechDevice.plugin.openWakeSession(options, callback, round)
            }
            check(ready.await(60, TimeUnit.SECONDS)) { "$name did not become ready" }
        }

        private fun record(event: String) {
            log += "$name $event"
        }

        override fun close() {
            runCatching { session.cancel() }
            endLatch.await(15, TimeUnit.SECONDS)
        }
    }

    /** Plays the keyword until every holder has one hit more, at most [MAX_PLAYS] times. zh-CN: 播放关键词直到每个持有者都多一次命中. */
    private fun hearAll(holders: List<Holder>): Int {
        val wanted = holders.map { it.hits.size + 1 }
        var plays = 0
        fun heard() = holders.indices.all { holders[it].hits.size >= wanted[it] }
        while (plays < MAX_PLAYS && !heard()) {
            AssistantDevice.play(KEYWORD_AUDIO)
            plays++
            val deadline = System.currentTimeMillis() + HIT_WAIT_MILLIS
            while (!heard() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        }
        assertTrue("every background holder hears the keyword: ${holders.map { "${it.name}=${it.hits.size}" }}", heard())
        return plays
    }

    private fun await(log: List<String>, vararg events: String) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!log.containsAll(events.toList()) && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue("events ${events.toList()} in $log", log.containsAll(events.toList()))
    }

    @Test
    fun backgroundWakeSessionsPauseForAListenAndAWakeAndResumeInOrder() {
        // Roadmap P11.4.3 (D190, D228 - D230): the resident wake word and a script's background session hear together,
        // pause for a listen and a foreground wake instead of failing them, and resume in the order they were opened.
        val log: MutableList<String> = Collections.synchronizedList(ArrayList())
        Holder("resident", log, resident = true).use { resident ->
            Holder("background", log).use { background ->
                assertEquals(2, SpeechRuntime.get(context).sessions.backgroundHolders.size)
                val together = hearAll(listOf(resident, background))
                log.clear()
                val ended = CountDownLatch(1)
                val listen = AssistantDevice.dictation(ended)
                await(log, "resident paused listen", "background paused listen")
                val before = resident.hits.size + background.hits.size
                AssistantDevice.play(KEYWORD_AUDIO)
                Thread.sleep(HIT_WAIT_MILLIS)
                val during = resident.hits.size + background.hits.size - before
                listen.cancel()
                assertTrue(ended.await(15, TimeUnit.SECONDS))
                await(log, "resident resumed listen", "background resumed listen")
                val resumedOrder = log.filter { it.endsWith("resumed listen") }
                val after = hearAll(listOf(resident, background))
                log.clear()
                AssistantDevice.Wake(KEYWORD).use { wake ->
                    await(log, "resident paused wake", "background paused wake")
                    hear(wake, 1)
                }
                await(log, "resident resumed wake", "background resumed wake")
                SpeechDevice.log("arbitration listen together=$together during=$during after=$after resumed=$resumedOrder log=$log")
                assertEquals("no hit while the listen records", 0, during)
                assertEquals("queue order", listOf("resident resumed listen", "background resumed listen"), resumedOrder)
                assertTrue("no error or end: $log", log.none { it.contains(" error") || it.endsWith(" end") })
            }
        }
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    @Test
    fun aThenListenRoundOfABackgroundSessionHoldsTheMicrophone() {
        // Roadmap D229: the round after a hit is a foreground dictation; the resident wake word pauses for it, a listen
        // meanwhile is SESSION_BUSY, and both go on after it ends.
        val log: MutableList<String> = Collections.synchronizedList(ArrayList())
        Holder("resident", log, resident = true).use { resident ->
            Holder("background", log, thenListenMillis = ROUND_MILLIS).use { background ->
                hearAll(listOf(background))
                await(log, "background round ready", "resident paused listen")
                val busy = runCatching { AssistantDevice.dictation(CountDownLatch(1)) }
                busy.getOrNull()?.cancel()
                await(log, "resident resumed listen")
                val roundEnd = log.firstOrNull { it.startsWith("background round end") }
                // The same word would start the next round at once and pause the resident again before it handles that
                // audio (D229), so the resident's resumption is checked alone.
                // zh-CN: 同一个词会立即开始下一轮并在常驻会话处理该段音频之前再次让它暂停 (D229), 因此单独检查常驻会话的恢复.
                background.close()
                val after = hearAll(listOf(resident))
                SpeechDevice.log("arbitration round busy=${busy.exceptionOrNull()?.message?.substringBefore(':')} roundEnd=$roundEnd after=$after log=$log")
                assertTrue("a listen during the round is SESSION_BUSY", busy.exceptionOrNull()?.message.orEmpty().startsWith("SESSION_BUSY"))
                assertTrue("the round ended: $log", roundEnd != null)
                assertTrue("the background session itself never pauses for its round: $log", log.none { it.startsWith("background paused") })
            }
        }
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    @Test
    fun aBackgroundWakeSessionHearsAsOftenAsAForegroundOne() {
        // Roadmap P11.4.3: hits per play with the resident wake word on beside it (the shared recording, D228), then of
        // a plain foreground session alone, on the same recording.
        val plays = SpeechDevice.argument("macawPlays")?.toIntOrNull() ?: 5
        val log: MutableList<String> = Collections.synchronizedList(ArrayList())
        val backgroundHits = Holder("resident", log, resident = true).use { _ ->
            Holder("background", log).use { background -> count(plays) { background.hits.size } }
        }
        val foregroundHits = AssistantDevice.Wake(KEYWORD).use { wake -> count(plays) { wake.hits.size } }
        SpeechDevice.log("arbitration hit rate background=$backgroundHits/$plays foreground=$foregroundHits/$plays room=${if (AssistantDevice.virtual) "virtual" else "speaker"}")
        val tolerance = if (AssistantDevice.virtual) 0 else 1
        assertTrue("background $backgroundHits, foreground $foregroundHits of $plays", backgroundHits >= foregroundHits - tolerance)
    }

    /** Plays the keyword [plays] times and counts the plays that brought a hit. zh-CN: 播放 [plays] 次并统计带来命中的次数. */
    private fun count(plays: Int, hits: () -> Int): Int = (1..plays).count {
        val before = hits()
        AssistantDevice.play(KEYWORD_AUDIO)
        val deadline = System.currentTimeMillis() + HIT_WAIT_MILLIS
        while (hits() == before && System.currentTimeMillis() < deadline) Thread.sleep(100)
        Thread.sleep(500)
        hits() > before
    }

    private companion object {
        /** A thenListen round that hears no speech ends after this much audio. zh-CN: 听不到语音的 thenListen 轮次在这么长的音频后结束. */
        const val ROUND_MILLIS = 4_000L

        /** The keyword of `kws-zh-6.wav` (`KeywordSpottingDeviceTest`). zh-CN: `kws-zh-6.wav` 中的关键词. */
        const val KEYWORD = "\u6731\u4e3d\u6960"
        const val KEYWORD_AUDIO = "test-audio/kws-zh-6.wav"

        /** How long a late hit may take after the playback. zh-CN: 播放结束后迟到的命中最多等待多久. */
        const val HIT_WAIT_MILLIS = 3_000L
        const val MAX_PLAYS = 3
    }
}
