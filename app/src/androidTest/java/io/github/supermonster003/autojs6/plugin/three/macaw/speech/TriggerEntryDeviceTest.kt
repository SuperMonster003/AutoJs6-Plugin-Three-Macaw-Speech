package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.capture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.text
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.MacawVoiceInteractionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.FloatingMicService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.KeyGesture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerPolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerRoute
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.VoiceTileService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.VoiceWidgetProvider
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant.AssistantActivity
import org.autojs.plugin.speech.api.SpeechContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The trigger entries on a device (roadmap P7.1, opt-in `-e macawTriggers true`, run by `.python/device_trigger_run.py`):
 * the widget, the launcher shortcut, the quick settings tile and the floating microphone each open the assistant sheet,
 * through the assistant session while 3-Macaw Speech is the default digital assistant and its service is bound, directly
 * otherwise (D60). An instrumentation run force-stops the plugin, after which the system leaves the assistant service
 * unbound in the test process, so on phones the expected route is the sheet itself; on emulators the script hands the role
 * over again (`-e macawRebindAssistant true`) and both routes run. Where the microphone permission and a recognition model are present, the sheet
 * must start listening, and the device must show no active recording before the entry and after the sheet closed (the
 * microphone indicator only during dictation, D58). The floating microphone hides while the sheet shows. The tile case
 * needs the tile in Quick Settings (`-e macawTile true`); `-e macawKeyguard true` also taps it on a lock screen without a
 * PIN. Nothing on the screen is tapped by coordinates; the key trigger is driven from the script (an instrumentation would
 * suppress accessibility services).
 * zh-CN: 设备上的触发式入口 (路线图 P7.1, 需显式开启 `-e macawTriggers true`, 由 `.python/device_trigger_run.py` 运行): 桌面小部件, 启动器快捷方式,
 * 快速设置磁贴与悬浮麦克风各自打开助理弹层, 3-Macaw Speech 是默认数字助理且其服务已绑定时经助理会话, 否则直接打开 (D60). instrumentation 会强行停止
 * 插件, 之后系统不再把助理服务绑定到测试进程, 因此手机上预期直接打开; 模拟器上脚本重新分配角色 (`-e macawRebindAssistant true`), 两种路由都运行. 有麦克风权限与
 * 识别模型时弹层必须开始收音, 且入口之前与弹层关闭之后设备上没有进行中的录音 (麦克风指示灯只在听写期间出现, D58). 弹层显示期间悬浮麦克风隐藏. 磁贴用例
 * 需要磁贴已在快速设置中 (`-e macawTile true`); `-e macawKeyguard true` 另在没有 PIN 的锁屏上点磁贴. 不按坐标点按屏幕; 按键触发由脚本驱动 (instrumentation
 * 会挂起无障碍服务).
 */
@RunWith(AndroidJUnit4::class)
class TriggerEntryDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = AssistantDevice.context
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val prefix: String get() = SpeechDevice.argument("macawPrefix") ?: "trigger"
    private val assistant: Boolean get() = MacawVoiceInteractionService.isActive(context)
    private val canListen: Boolean
        get() = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).isNotEmpty()

    @Before
    fun prerequisites() {
        // The real microphone even on emulators (silence there): the active recordings are what the indicator shows.
        assumeTrue("opt-in: -e macawTriggers true", SpeechDevice.argument("macawTriggers") == "true")
        if (assistant) {
            // An instrumentation run force-stops the plugin, after which the system leaves the assistant service unbound
            // (`dumpsys voiceinteraction`: mBound=true mService=null) and the entries fall back to the sheet itself (D60).
            // On emulators the script lets the test hand the role over again, which binds the service into this process.
            val started = System.currentTimeMillis()
            awaitAssistantService(SHORT_WAIT_MILLIS)
            if (MacawVoiceInteractionService.running == null && SpeechDevice.argument("macawRebindAssistant") == "true") {
                val role = "android.app.role.ASSISTANT"
                AssistantDevice.shell("cmd role remove-role-holder $role ${context.packageName}")
                AssistantDevice.shell("cmd role add-role-holder $role ${context.packageName}")
                awaitAssistantService(ASSISTANT_WAIT_MILLIS)
            }
            SpeechDevice.log("trigger assistant service bound=${MacawVoiceInteractionService.running != null} after ${System.currentTimeMillis() - started} ms")
        }
    }

    private fun awaitAssistantService(millis: Long) {
        val deadline = System.currentTimeMillis() + millis
        while (MacawVoiceInteractionService.running == null && System.currentTimeMillis() < deadline) Thread.sleep(200)
    }

    @After
    fun cleanUp() {
        closeSheets()
    }

    @Test
    fun theWidgetOpensTheSheet() {
        val before = recordings()
        val started = System.currentTimeMillis()
        VoiceWidgetProvider.pendingIntent(context).send()
        opened(TriggerSource.WIDGET, started, before)
    }

    @Test
    fun theLauncherShortcutOpensTheSheet() {
        assumeTrue("launcher shortcuts are API 25", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        // The launcher starts the manifest shortcut as the plugin itself; the test process is the plugin's.
        val shortcut = context.getSystemService(ShortcutManager::class.java).manifestShortcuts.singleOrNull { it.id == "voice_command" }
        assertNotNull("the manifest shortcut is published", shortcut)
        val intent = shortcut!!.intent
        assertNotNull(intent)
        assertEquals(context.packageName + ".trigger.TriggerActivity", intent!!.component?.className)
        SpeechDevice.log("trigger shortcut short=${shortcut.shortLabel} long=${shortcut.longLabel}")
        val before = recordings()
        val started = System.currentTimeMillis()
        context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        opened(TriggerSource.SHORTCUT, started, before)
    }

    @Test
    fun theQuickSettingsTileOpensTheSheet() {
        assumeTrue("opt-in: -e macawTile true (the tile is in Quick Settings)", SpeechDevice.argument("macawTile") == "true")
        val before = recordings()
        val started = System.currentTimeMillis()
        clickTile()
        opened(TriggerSource.TILE, started, before)
    }

    @Test
    fun theTileOpensTheSheetOverTheLockScreen() {
        assumeTrue("opt-in: -e macawTile true and -e macawKeyguard true", SpeechDevice.argument("macawTile") == "true" && SpeechDevice.argument("macawKeyguard") == "true")
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        assumeTrue("no lock PIN", !keyguard.isDeviceSecure)
        AssistantDevice.shell("input keyevent KEYCODE_SLEEP")
        Thread.sleep(1_500)
        AssistantDevice.shell("input keyevent KEYCODE_WAKEUP")
        val deadline = System.currentTimeMillis() + 10_000
        while (!keyguard.isKeyguardLocked && System.currentTimeMillis() < deadline) Thread.sleep(200)
        assumeTrue("the lock screen shows (an emulator with the lock screen disabled skips this)", keyguard.isKeyguardLocked)
        val started = System.currentTimeMillis()
        clickTile()
        opened(TriggerSource.TILE, started, before = null)
        SpeechDevice.log("trigger tile keyguard locked=${keyguard.isKeyguardLocked}")
        assertTrue("D55, D61: the sheet listens over the lock screen without an unlock", keyguard.isKeyguardLocked)
        capture(prefix, "tile-keyguard")
    }

    @Test
    fun theFloatingMicrophoneOpensTheSheetAndHidesMeanwhile() {
        assumeTrue("the overlay permission is granted", Settings.canDrawOverlays(context))
        val preferences = TriggerPreferences(context)
        val wasOn = preferences.floating
        preferences.floating = true
        try {
            assertTrue("the service starts", FloatingMicService.start(context))
            val button = awaitButton()
            SpeechDevice.log("trigger floating shown recordings=${recordings()}")
            assertEquals("the floating microphone records nothing", 0, recordings())
            capture(prefix, "floating")
            val started = System.currentTimeMillis()
            instrumentation.runOnMainSync { button.performClick() }
            opened(TriggerSource.FLOATING, started, before = 0) {
                var visibility = View.VISIBLE
                instrumentation.runOnMainSync { visibility = button.visibility }
                SpeechDevice.log("trigger floating hidden=${visibility == View.GONE}")
                assertEquals("the button hides while the sheet shows", View.GONE, visibility)
            }
            val deadline = System.currentTimeMillis() + 5_000
            var visibility = View.GONE
            while (visibility != View.VISIBLE && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync { visibility = button.visibility }
                Thread.sleep(100)
            }
            assertEquals("the button returns after the sheet closed", View.VISIBLE, visibility)
        } finally {
            preferences.floating = wasOn
            if (!wasOn) FloatingMicService.stop(context)
        }
    }

    /**
     * Waits for the sheet of [source], checks its route against the assistant state (D60), lets it listen where possible,
     * closes it and checks that the microphone is free again.
     * zh-CN: 等待 [source] 打开的弹层, 按助理状态核对其路由 (D60), 条件允许时让它收音, 关闭后核对麦克风已释放.
     */
    private fun opened(source: TriggerSource, started: Long, before: Int?, whileOpen: () -> Unit = {}) {
        val sheet = awaitSheet(source)
        val millis = System.currentTimeMillis() - started
        val bound = MacawVoiceInteractionService.running != null
        val expected = TriggerPolicy.route(assistant, bound) == TriggerRoute.SESSION
        SpeechDevice.log("trigger ${source.id} route=${if (sheet.viaSession) "session" else "direct"} assistant=$assistant bound=$bound after $millis ms recordingsBefore=$before")
        assertEquals("D60: through the session exactly when 3-Macaw Speech is the assistant and its service is bound", expected, sheet.viaSession)
        if (canListen) {
            compose.waitUntil(30_000) { compose.text("macaw.voice.state") in AssistantDevice.listeningTexts() }
            val during = recordings()
            SpeechDevice.log("trigger ${source.id} listening recordings=$during")
            capture(prefix, "${source.id}-listening")
            assertTrue("the sheet records while it listens", during >= 1)
        }
        whileOpen()
        closeSheets()
        assertTrue("the sheet closed", AssistantDevice.awaitGone(AssistantActivity::class.java.name))
        var after = recordings()
        val deadline = System.currentTimeMillis() + 5_000
        while (after > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(200)
            after = recordings()
        }
        SpeechDevice.log("trigger ${source.id} closed recordings=$after")
        if (before != null) assertEquals("no recording is left after the sheet closed", before, after)
        if (SpeechDevice.argument("macawIndicator") == "true") {
            // The privacy indicator fades a few seconds after the recording ends.
            Thread.sleep(INDICATOR_FADE_MILLIS)
            capture(prefix, "${source.id}-closed")
        }
        if (canListen) assertTrue(AssistantDevice.microphoneIsFree())
    }

    private fun awaitSheet(source: TriggerSource): AssistantActivity {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            AssistantDevice.resumed().filterIsInstance<AssistantActivity>().firstOrNull { it.source == source }?.let { return it }
            Thread.sleep(100)
        }
        capture(prefix, "${source.id}-missing")
        throw AssertionError("the ${source.id} entry opened no sheet")
    }

    private fun awaitButton(): View {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            var button: View? = null
            instrumentation.runOnMainSync { button = FloatingMicService.shownButton?.takeIf { it.isAttachedToWindow } }
            button?.let { return it }
            Thread.sleep(100)
        }
        throw AssertionError("the floating microphone did not show")
    }

    /** The system UI binds a tile while Quick Settings is open; the tile collapses the panel itself. zh-CN: 快速设置展开时系统界面才绑定磁贴; 磁贴自行收起面板. */
    private fun clickTile() {
        val component = VoiceTileService.component(context).flattenToString()
        AssistantDevice.shell("cmd statusbar expand-settings")
        Thread.sleep(TILE_BIND_MILLIS)
        AssistantDevice.shell("cmd statusbar click-tile $component")
    }

    private fun closeSheets() {
        AssistantDevice.resumed().filterIsInstance<AssistantActivity>().forEach { activity -> activity.runOnUiThread { activity.finish() } }
    }

    /** Active recordings on the device, the source of the microphone indicator. zh-CN: 设备上进行中的录音, 即麦克风指示灯的来源. */
    private fun recordings(): Int = context.getSystemService(AudioManager::class.java).activeRecordingConfigurations.size

    private companion object {
        const val INDICATOR_FADE_MILLIS = 6_000L
        const val SHORT_WAIT_MILLIS = 3_000L
        const val ASSISTANT_WAIT_MILLIS = 15_000L
        const val TILE_BIND_MILLIS = 1_500L
    }
}

/**
 * Sets the key gesture of the key trigger for a script run (`-e macawKeyGesture <id>`, roadmap D64). Note that every
 * instrumentation run force-stops the plugin, which also turns its accessibility service off on most systems.
 * zh-CN: 为脚本运行设置按键触发的手势 (`-e macawKeyGesture <id>`, 路线图 D64). 注意每次 instrumentation 都会强行停止插件, 多数系统因此也会关闭其无障碍服务.
 */
@RunWith(AndroidJUnit4::class)
class TriggerSetupDeviceTest {
    @Test
    fun keyGesture() {
        val id = SpeechDevice.argument("macawKeyGesture")
        assumeTrue("opt-in: -e macawKeyGesture <id>", id != null)
        val gesture = KeyGesture.entries.single { it.id == id }
        val preferences = TriggerPreferences(AssistantDevice.context)
        preferences.keyGesture = gesture
        assertEquals(gesture, preferences.keyGesture)
        // apply() writes in the background; the run force-stops the process right after this test.
        Thread.sleep(500)
        SpeechDevice.log("trigger key gesture=${gesture.id}")
    }
}
