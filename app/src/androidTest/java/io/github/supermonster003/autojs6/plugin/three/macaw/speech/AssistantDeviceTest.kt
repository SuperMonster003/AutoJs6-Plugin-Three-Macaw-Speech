package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.capture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.editable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.exists
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.text
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.MacawVoiceInteractionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant.AssistantActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant.AssistantPolicy
import org.autojs.plugin.speech.api.SpeechContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The digital assistant on a device (roadmap P5.7 E3 / E4, opt-in `-e macawAssistant true`, run by
 * `.python/device_assistant_run.py`): the assist key opens the sheet when 3-Macaw Speech is the assistant (set with
 * `cmd role` on emulators only; the maintainer picks it on phones), the sheet listens once (the virtual room on
 * emulators, the speaker on phones), shows the text, lists the two `VOICE_COMMAND` test targets by label and sends the
 * text with the language to the chosen one only; editing, "Speak again", silence, "Copy text" and the lock screen
 * (`-e macawKeyguard true`, emulators without a lock PIN only). The recognized text is compared with the reference
 * transcript (CER); screenshots go to `screens/`.
 * zh-CN: 设备上的数字助理 (路线图 P5.7 E3 / E4, 需显式开启 `-e macawAssistant true`, 由 `.python/device_assistant_run.py` 运行): 3-Macaw Speech 为
 * 助理时助理键打开弹层 (模拟器上用 `cmd role` 设置, 手机上由维护者选择), 弹层听写一次 (模拟器用虚拟房间, 手机用扬声器), 显示文字, 按 label 列出两个
 * `VOICE_COMMAND` 测试目标, 只把文字与语言发给所选目标; 另测编辑, "重说", 静音, "复制文本" 与锁屏 (`-e macawKeyguard true`, 仅限没有锁屏密码的模拟器).
 * 识别文字与参考转写比较 (CER); 截图保存到 `screens/`.
 */
@RunWith(AndroidJUnit4::class)
class AssistantDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = AssistantDevice.context
    private val prefix: String get() = SpeechDevice.argument("macawPrefix") ?: "assistant"

    @Before
    fun prerequisites() {
        assumeTrue("opt-in: -e macawAssistant true", SpeechDevice.argument("macawAssistant") == "true")
        assumeTrue("RECORD_AUDIO is granted", context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        assumeTrue("a recognition model is installed", SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).isNotEmpty())
        AssistantDevice.setUpRoom()
    }

    @After
    fun cleanUp() {
        AssistantDevice.resumed().filterIsInstance<AssistantActivity>().forEach { activity -> activity.runOnUiThread { activity.finish() } }
        AssistantDevice.tearDownRoom()
    }

    @Test
    fun theAssistKeyOpensTheSheetAndOnlyTheChosenTargetGetsTheText() {
        assumeTrue("3-Macaw Speech is the default digital assistant", MacawVoiceInteractionService.isActive(context))
        AssistantDevice.Targets().use { targets ->
            val started = System.currentTimeMillis()
            AssistantDevice.shell("input keyevent KEYCODE_ASSIST")
            awaitListening()
            SpeechDevice.log("assistant gesture listening after ${System.currentTimeMillis() - started} ms")
            capture(prefix, "listening")
            val text = speakAndRead()
            capture(prefix, "result")
            // D50 / D53: both test targets, ordered by label, never the plugin itself.
            val labels = listOf("Two", "One").map { compose.text("macaw.assistant.target.${PACKAGE}.VoiceCommandTarget$it.label") }
            assertEquals(listOf(AssistantDevice.LABEL_FIRST, AssistantDevice.LABEL_SECOND), labels)
            compose.onNodeWithTag("macaw.assistant.target.$PACKAGE.VoiceCommandTargetTwo").performSemanticsAction(SemanticsActions.OnClick)
            val received = targets.next()
            assertNotNull("the chosen target received the text", received)
            received!!
            SpeechDevice.log(
                "assistant sent target=${received.getString("target")?.substringAfterLast('.')} chars=${received.getString("text")?.length} " +
                    "language=${received.getString("language")} newTask=${received.getBoolean("newTask")} keys=${received.getString("keys")}",
            )
            assertEquals("$PACKAGE.VoiceCommandTargetTwo", received.getString("target"))
            assertEquals(AssistantPolicy.ACTION_VOICE_COMMAND, received.getString("action"))
            assertEquals(text, received.getString("text"))
            assertTrue("the language of the English recording", received.getString("language")?.startsWith("en") == true)
            assertTrue(received.getBoolean("newTask"))
            assertEquals("nothing but the text and the language (D54)", "android.intent.extra.TEXT,${AssistantPolicy.EXTRA_VOICE_LANGUAGE}", received.getString("keys"))
            assertTrue("the sheet closed after sending", AssistantDevice.awaitGone(AssistantActivity::class.java.name))
            assertEquals("the other target got nothing", null, targets.next(2))
        }
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    /**
     * E4 hand-over to 3-Stove Agent (opt-in `-e macawStove true`, `.python/device_assistant_run.py stove`): with 3-Macaw
     * Speech as the assistant, the assist key opens the sheet, the recording is dictated and the "3-Stove Agent" button
     * opens Stove's reviewable draft (its workbench, or its floating card when the ball is on screen; D51); the sheet
     * closes. The screens before and after the hand-over are captured for review; the text itself is never logged.
     * zh-CN: E4 交给 3-Stove Agent (需显式开启 `-e macawStove true`, 由 `.python/device_assistant_run.py stove` 运行): 3-Macaw Speech 为助理时,
     * 助理键打开弹层, 听写测试录音后点 "3-Stove Agent" 按钮打开 Stove 可审阅的草稿 (任务台, 或悬浮球在屏幕上时的悬浮卡片; D51), 弹层关闭.
     * 交接前后的界面截图供审阅; 不记录识别文字.
     */
    @Test
    fun theAssistKeyHandsTheTextToStoveAsADraft() {
        assumeTrue("opt-in: -e macawStove true", SpeechDevice.argument("macawStove") == "true")
        assumeTrue("3-Macaw Speech is the default digital assistant", MacawVoiceInteractionService.isActive(context))
        val query = Intent(AssistantPolicy.ACTION_VOICE_COMMAND).addCategory(Intent.CATEGORY_DEFAULT).setPackage(STOVE)
        val stove = context.packageManager.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY).firstOrNull()?.activityInfo
        assumeTrue("3-Stove Agent answers VOICE_COMMAND", stove != null)
        val started = System.currentTimeMillis()
        AssistantDevice.shell("input keyevent KEYCODE_ASSIST")
        awaitListening()
        SpeechDevice.log("assistant stove listening after ${System.currentTimeMillis() - started} ms")
        val text = speakAndRead()
        val tag = "macaw.assistant.target.${stove!!.name}"
        SpeechDevice.log("assistant stove target label=${compose.text("$tag.label")}")
        capture(prefix, "stove-result")
        val sent = System.currentTimeMillis()
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        assertTrue("the sheet closed after sending", AssistantDevice.awaitGone(AssistantActivity::class.java.name))
        var draft: String? = null
        while (draft == null && System.currentTimeMillis() < sent + 15_000) {
            draft = when {
                AssistantDevice.shell("dumpsys activity activities").lineSequence()
                    .any { "ResumedActivity" in it && "$STOVE/" in it && "VoiceCommandActivity" !in it } -> "workbench"
                AssistantDevice.shell("dumpsys window windows").contains("3-Stove Agent floating") -> "card"
                else -> null
            }
            if (draft == null) Thread.sleep(200)
        }
        val shownMillis = System.currentTimeMillis() - sent
        Thread.sleep(1500)
        capture(prefix, "stove-draft")
        SpeechDevice.log("assistant stove draft=$draft after $shownMillis ms chars=${text.length}")
        assertNotNull("3-Stove Agent showed the draft", draft)
    }

    @Test
    fun editingChangesWhatIsSentAndSpeakAgainListensAgain() {
        AssistantDevice.Targets().use { targets ->
            launch()
            speakAndRead()
            compose.onNodeWithTag("macaw.assistant.redo").performSemanticsAction(SemanticsActions.OnClick)
            awaitListening()
            SpeechDevice.log("assistant redo listening again")
            speakAndRead()
            compose.onNodeWithTag("macaw.assistant.text").performTextReplacement("  turn on the lights  ")
            compose.waitForIdle()
            capture(prefix, "edited")
            compose.onNodeWithTag("macaw.assistant.target.$PACKAGE.VoiceCommandTargetOne").performSemanticsAction(SemanticsActions.OnClick)
            val received = targets.next()
            SpeechDevice.log("assistant edited sent=${received?.getString("text")} target=${received?.getString("target")?.substringAfterLast('.')}")
            assertEquals("turn on the lights", received?.getString("text"))
            assertEquals("$PACKAGE.VoiceCommandTargetOne", received?.getString("target"))
        }
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    @Test
    fun silenceShowsNothingRecognizedAndCopyClosesTheSheet() {
        AssistantDevice.Targets().use { targets ->
            launch()
            val started = System.currentTimeMillis()
            compose.waitUntil(20_000) { compose.exists("macaw.assistant.noMatch") }
            SpeechDevice.log("assistant silence noMatch after ${System.currentTimeMillis() - started} ms")
            capture(prefix, "no-match")
            compose.onNodeWithTag("macaw.assistant.redo").performSemanticsAction(SemanticsActions.OnClick)
            awaitListening()
            speakAndRead()
            compose.onNodeWithTag("macaw.assistant.copy").performSemanticsAction(SemanticsActions.OnClick)
            assertTrue("Copy text closes the sheet", AssistantDevice.awaitGone(AssistantActivity::class.java.name))
            assertEquals("copying sends nothing", null, targets.next(2))
            SpeechDevice.log("assistant copy closed the sheet")
        }
        assertTrue(AssistantDevice.microphoneIsFree())
    }

    @Test
    fun theSheetShowsOverTheLockScreenAndUnlocksBeforeSending() {
        assumeTrue("opt-in: -e macawKeyguard true (an emulator without a lock PIN)", SpeechDevice.argument("macawKeyguard") == "true")
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        assumeTrue("no lock PIN", !keyguard.isDeviceSecure)
        AssistantDevice.shell("input keyevent KEYCODE_SLEEP")
        Thread.sleep(1_500)
        AssistantDevice.shell("input keyevent KEYCODE_WAKEUP")
        val deadline = System.currentTimeMillis() + 10_000
        while (!keyguard.isKeyguardLocked && System.currentTimeMillis() < deadline) Thread.sleep(200)
        assumeTrue("the lock screen shows (an emulator with the lock screen disabled skips this)", keyguard.isKeyguardLocked)
        AssistantDevice.Targets().use { targets ->
            if (MacawVoiceInteractionService.isActive(context)) AssistantDevice.shell("input keyevent KEYCODE_ASSIST") else context.startActivity(AssistantActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            awaitListening()
            capture(prefix, "keyguard-listening")
            val text = speakAndRead()
            assertTrue("D55: dictation works while locked", keyguard.isKeyguardLocked)
            capture(prefix, "keyguard-result")
            compose.onNodeWithTag("macaw.assistant.target.$PACKAGE.VoiceCommandTargetTwo").performSemanticsAction(SemanticsActions.OnClick)
            val received = targets.next(20)
            SpeechDevice.log("assistant keyguard sent=${received != null} locked=${keyguard.isKeyguardLocked}")
            assertEquals("sent only after the unlock", text, received?.getString("text"))
            assertFalse("the keyguard was dismissed first", keyguard.isKeyguardLocked)
        }
    }

    /**
     * Speech that starts as the sheet appears, while the model still loads (found on the phones on 2026-10-20: the first
     * syllable was lost and a short phrase brought nothing, because the recording began only after a load of 1.5 - 2 s):
     * the sheet records at once and keeps the audio until the dictation exists. Virtual room only, where the speech
     * starts at a known moment; the idle models are released first, so the model must load.
     * zh-CN: 弹层一出现就开始说话, 模型仍在加载 (2026-10-20 在手机上发现: 录音要等 1.5 - 2 秒的加载结束才开始, 第一个音节丢失, 短句没有结果): 弹层立即
     * 录音并保留音频直到听写建立. 只在虚拟房间运行, 语音开始的时刻确定; 先释放空闲模型, 使模型必须加载.
     */
    @Test
    fun speechThatStartsWhileTheModelLoadsIsKept() {
        assumeTrue("the virtual room (-e macawRoom virtual)", AssistantDevice.virtual)
        SpeechRuntime.get(context).engines.trim()
        val started = SystemClock.elapsedRealtime()
        context.startActivity(AssistantActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(SPEECH_DELAY_MILLIS)
        speakAndRead()
        SpeechDevice.log("assistant early speech ${SPEECH_DELAY_MILLIS + EN_SILENCE_MILLIS} ms after the sheet, result after ${SystemClock.elapsedRealtime() - started} ms")
    }

    private fun launch() {
        context.startActivity(AssistantActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        awaitListening()
    }

    private fun awaitListening() {
        try {
            compose.waitUntil(30_000) { compose.text("macaw.voice.state") in AssistantDevice.listeningTexts() }
        } catch (e: Throwable) {
            capture(prefix, "not-listening")
            SpeechDevice.log("assistant state=${compose.text("macaw.voice.state")} busy=${compose.exists("macaw.voice.problem.busy")}")
            throw e
        }
    }

    /** Plays `en.wav`, waits for the editable result and logs its CER. zh-CN: 播放 `en.wav`, 等待可编辑的结果并记录 CER. */
    private fun speakAndRead(): String {
        AssistantDevice.play("test-audio/en.wav")
        compose.waitUntil(30_000) { compose.exists("macaw.assistant.text") }
        val text = compose.editable("macaw.assistant.text").orEmpty()
        val cer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, text)
        SpeechDevice.log("assistant result chars=${text.length} cer=${"%.3f".format(cer)} room=${if (AssistantDevice.virtual) "virtual" else "speaker"}")
        assertTrue("CER $cer", cer <= MAX_CER)
        return text
    }

    private companion object {
        const val PACKAGE = AssistantDevice.PLUGIN
        const val STOVE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
        const val MAX_CER = 0.15

        /** The recording starts this long after the sheet; `en.wav` speaks after [EN_SILENCE_MILLIS]. zh-CN: 弹层之后多久开始播放. */
        const val SPEECH_DELAY_MILLIS = 300L
        const val EN_SILENCE_MILLIS = 400L
    }
}
