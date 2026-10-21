package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.capture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.exists
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.AssistantDevice.text
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ListeningDuration
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentNotifications
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentSettings
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentStartActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWakeService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerResumeReceiver
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant.AssistantActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.settings.SettingsActivity
import org.autojs.plugin.speech.api.SpeechContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The resident wake word on a device (roadmap P7.4 E3, opt-in `-e macawResident true`): the settings switch starts the
 * microphone foreground service; the keyword of `kws-zh-6.wav` (the stored wake word for the run) opens the sheet or posts
 * "Tap to speak" (D71, D79); every other microphone session silences it until it ends (D72, D76); the listening time turns
 * the switch off (D77); "only while charging" pauses and resumes with `dumpsys battery` (D77, D76); another resident
 * listener turns it away (D75); a reboot reminds and the trampoline resumes (D74); with `-e macawKeyguard true` a hit
 * turns the screen on (D73). The sound comes from the [VirtualRoom] (`-e macawRoom virtual`) or the speaker. The stored
 * settings, the switch and the notification question are restored afterwards; nothing on the screen is tapped outside
 * the plugin's own screens.
 * zh-CN: 设备上的常驻唤醒 (路线图 P7.4 E3, 需显式开启 `-e macawResident true`): 设置开关启动麦克风前台服务; `kws-zh-6.wav` 中的关键词 (本次运行保存的
 * 唤醒词) 打开弹层或发出 "点此说话" (D71, D79); 其他麦克风会话让它静默直到结束 (D72, D76); 监听时长到点关闭开关 (D77); "只在充电时监听" 随
 * `dumpsys battery` 暂停与恢复 (D77, D76); 其他常驻监听让它关闭 (D75); 重启后提醒, 跳板恢复 (D74); 有 `-e macawKeyguard true` 时命中点亮屏幕 (D73).
 * 声音来自 [VirtualRoom] (`-e macawRoom virtual`) 或扬声器. 之后恢复保存的设置, 开关与通知询问标记; 只点按插件自己的界面.
 */
@RunWith(AndroidJUnit4::class)
class ResidentWakeDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = ResidentPreferences(context)
    private val sessions get() = SpeechRuntime.get(context).sessions
    private val prefix: String get() = SpeechDevice.argument("macawPrefix") ?: "resident"

    /** `-e macawStove true`: 3-Stove Agent's resident listener is on (the run script switched its component on). zh-CN: 3-Stove Agent 的常驻监听已开启. */
    private val stove: Boolean get() = SpeechDevice.argument("macawStove") == "true"

    /** The route the run expects (`open` or `notify`, D71), any when absent. zh-CN: 本次运行预期的路线, 缺省时不限. */
    private val expectedRoute: String? get() = SpeechDevice.argument("macawRoute")
    private lateinit var saved: ResidentSettings
    private var savedOn = false
    private var savedAsked = false

    @Before
    fun prerequisites() {
        assumeTrue("opt-in: -e macawResident true", SpeechDevice.argument("macawResident") == "true")
        assumeTrue("RECORD_AUDIO is granted", context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        assumeTrue("the wake word model is installed", SpeechDevice.installed(SpeechDevice.KWS))
        assumeTrue("a recognition model is installed", SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).isNotEmpty())
        rival(false)
        if (!stove) assumeTrue("no other resident listener is on", ResidentWake.others(context).isEmpty())
        saved = preferences.read()
        savedOn = ResidentWake.enabled(context)
        ResidentWake.turnOff(context)
        awaitStopped()
        // The switch asks for the notification permission once (D78): never a system dialog during the run.
        val speech = SpeechPreferences(context)
        savedAsked = speech.notificationsAsked
        speech.notificationsAsked = true
        preferences.write(ResidentSettings(keywords = listOf(KEYWORD), readAloud = true))
        AssistantDevice.setUpRoom()
    }

    @After
    fun cleanUp() {
        if (!::saved.isInitialized) return
        closeSheets()
        ResidentWake.turnOff(context)
        awaitStopped()
        rival(false)
        AssistantDevice.shell("dumpsys battery reset")
        AssistantDevice.tearDownRoom()
        SpeechPreferences(context).notificationsAsked = savedAsked
        preferences.write(saved)
        // The switch was on before the run: it stays enabled and resumes at the next visible screen (D74).
        if (savedOn) ResidentWake.setEnabled(context, true)
        notifications().forEach { if (it.id != ResidentNotifications.LISTENING_ID) manager().cancel(it.id) }
    }

    // Helpers ---------------------------------------------------------------------------------------------------------

    private fun manager(): NotificationManager = context.getSystemService(NotificationManager::class.java)

    private fun notifications() = manager().activeNotifications.filter { it.id in RESIDENT_IDS }

    private fun posted(id: Int): Boolean = manager().activeNotifications.any { it.id == id }

    private fun state(): ResidentNotifications.State? {
        var value: ResidentNotifications.State? = null
        instrumentation.runOnMainSync { value = ResidentWakeService.runningState }
        return value
    }

    private fun running(): Boolean {
        var value = false
        instrumentation.runOnMainSync { value = ResidentWakeService.running != null }
        return value
    }

    private fun await(millis: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    private fun awaitState(wanted: ResidentNotifications.State, millis: Long = 60_000): Boolean = await(millis) { state() == wanted }

    private fun awaitStopped(): Boolean = await(15_000) { !running() }

    /**
     * Turns the test APK's rival listener on or off through its switch, which runs in the test APK's own process (shell may
     * only change the components of a test-only package). zh-CN: 经测试 APK 自己进程中的开关启用或禁用对方 (shell 只能修改 test-only 包的组件).
     */
    private fun rival(on: Boolean) {
        context.sendBroadcast(
            Intent().setComponent(ComponentName(TEST_PACKAGE, RivalResidentListener.Switch::class.java.name))
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES).putExtra(RivalResidentListener.EXTRA_ENABLED, on),
        )
        await(15_000) { ResidentWake.others(context).any { it.packageName == TEST_PACKAGE } == on }
    }

    /** Runs [block] with the settings screen on top, the visible state the service starts from. zh-CN: 在设置页位于顶层时运行. */
    private fun <T> withSettings(block: (SettingsActivity) -> T): T = ActivityScenario.launch<SettingsActivity>(Intent(context, SettingsActivity::class.java)).use { scenario ->
        var result: Result<T>? = null
        scenario.onActivity { activity -> result = runCatching { block(activity) } }
        result!!.getOrThrow()
    }

    private fun turnOn() {
        withSettings { activity -> assertTrue("the service starts", ResidentWake.turnOn(activity)) }
        assertTrue("the wake session is ready, state=${state()}", awaitState(ResidentNotifications.State.WAITING))
    }

    private fun closeSheets() {
        AssistantDevice.resumed().filterIsInstance<AssistantActivity>().forEach { activity -> activity.runOnUiThread { activity.finish() } }
        AssistantDevice.awaitGone(AssistantActivity::class.java.name)
    }

    /**
     * Plays the keyword until the resident service counts one more hit, at most [MAX_PLAYS] times (a play may miss, see
     * TakeoverDeviceTest); answers the plays, or 0 when it was never heard.
     * zh-CN: 播放关键词直到常驻服务多计一次命中, 至多 [MAX_PLAYS] 次; 返回播放次数, 一直没听到时为 0.
     */
    private fun hit(): Int {
        val before = ResidentWakeService.hits
        for (play in 1..MAX_PLAYS) {
            AssistantDevice.play(KEYWORD_AUDIO)
            if (await(HIT_WAIT_MILLIS) { ResidentWakeService.hits > before }) return play
        }
        return 0
    }

    /** The sheet opened (OPEN) or "Tap to speak" was posted (NOTIFY); answers which. zh-CN: 弹层已打开或已发 "点此说话"; 返回哪一种. */
    private fun route(): String {
        assertTrue("the sheet opens or Tap to speak is posted", await(15_000) { sheetShown() || posted(ResidentNotifications.SPEAK_ID) })
        return if (sheetShown()) "open" else "notify"
    }

    private fun sheetShown(): Boolean = AssistantDevice.resumed().any { it is AssistantActivity }

    /** Clicks the switch row once it is enabled (the availability check runs off the main thread). zh-CN: 开关行可用后点按. */
    private fun clickSwitch() {
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasTestTag("macaw.settings.resident")).fetchSemanticsNodes().any { it.config.getOrNull(SemanticsProperties.Disabled) == null }
        }
        compose.onNodeWithTag("macaw.settings.resident", useUnmergedTree = true).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
    }

    // Tests -----------------------------------------------------------------------------------------------------------

    @Test
    fun theSwitchListensAndTheWakeWordOpensTheSheet() {
        // D78 through the settings screen: the row waits for the availability check, then the switch turns on.
        ActivityScenario.launch<SettingsActivity>(Intent(context, SettingsActivity::class.java)).use {
            clickSwitch()
            // D71: asked only when nothing else may open the sheet from the background; "Not now" still turns it on.
            if (await(3_000) { compose.exists("macaw.dialog.residentOverlay") }) {
                Thread.sleep(DIALOG_SETTLE_MILLIS)
                capture(prefix, "overlay")
                compose.onNodeWithTag("macaw.dialog.residentOverlay.cancel").performSemanticsAction(SemanticsActions.OnClick)
            }
            assertTrue("the wake session is ready, state=${state()}", awaitState(ResidentNotifications.State.WAITING))
            assertTrue(ResidentWake.enabled(context))
            assertTrue("D75: the ongoing notification", posted(ResidentNotifications.LISTENING_ID))
            assertTrue("D76: the background holder records", sessions.backgroundListening)
            assertFalse("it never counts as the microphone holder", sessions.microphoneInUse)
            capture(prefix, "settings-on")
        }
        val plays = hit()
        assertTrue("the wake word is heard", plays > 0)
        val route = route()
        expectedRoute?.let { assertEquals("D71: the route of this run", it, route) }
        var tapped = false
        if (route == "notify") {
            // D71: "Tap to speak" opens the sheet; the tap cancels it.
            val speak = manager().activeNotifications.first { it.id == ResidentNotifications.SPEAK_ID }
            speak.notification.contentIntent.send()
            tapped = await(15_000) { sheetShown() }
            capture(prefix, "speak-tapped")
            closeSheets()
            manager().cancel(ResidentNotifications.SPEAK_ID)
        }
        var silenced = false
        if (route == "open") {
            // The sheet dictates: the resident word steps aside (D76), and reads the result aloud (D69).
            silenced = await(15_000) { !sessions.backgroundListening }
            AssistantDevice.play("test-audio/en.wav")
            compose.waitUntil(30_000) { compose.exists("macaw.assistant.text") || compose.exists("macaw.assistant.noMatch") }
            capture(prefix, "sheet")
            Thread.sleep(READ_ALOUD_MILLIS)
            closeSheets()
        }
        val resumed = await(15_000) { sessions.backgroundListening }
        val again = hit()
        if (route == "open") closeSheets()
        SpeechDevice.log("resident switch on, hit route=$route plays=$plays tapped=$tapped silencedBySheet=$silenced resumed=$resumed again=$again")
        if (route == "notify") assertTrue("Tap to speak opens the sheet", tapped)
        assertTrue("listening again after the sheet", resumed)
        assertTrue("the wake word is heard again", again > 0)
        if (route == "open") assertTrue("the sheet silenced the resident word", silenced)
    }

    @Test
    fun everyOtherMicrophoneSessionSilencesItUntilItEnds() {
        turnOn()
        val before = ResidentWakeService.hits
        // D72: a script's wake session (a test client) records instead, and hears the keyword itself.
        val clientHits = AssistantDevice.Wake(KEYWORD).use { wake ->
            assertTrue("silenced by the client's wake session", await(5_000) { !sessions.backgroundListening })
            var plays = 0
            while (plays < MAX_PLAYS && wake.hits.isEmpty()) {
                AssistantDevice.play(KEYWORD_AUDIO)
                plays++
                await(HIT_WAIT_MILLIS) { wake.hits.isNotEmpty() }
            }
            wake.hits.size
        }
        val resident = ResidentWakeService.hits - before
        assertTrue("listening again after the client", await(10_000) { sessions.backgroundListening })
        // A script dictation is never refused because of the resident word (no SESSION_BUSY).
        val ended = CountDownLatch(1)
        val dictation = AssistantDevice.dictation(ended)
        val silenced = await(5_000) { !sessions.backgroundListening }
        dictation.cancel()
        assertTrue(ended.await(15, TimeUnit.SECONDS))
        val resumed = await(10_000) { sessions.backgroundListening }
        SpeechDevice.log("resident yields client hits=$clientHits resident hits meanwhile=$resident dictation silenced=$silenced resumed=$resumed")
        assertTrue("the client hears the keyword", clientHits > 0)
        assertEquals("the resident word stays silent meanwhile", 0, resident)
        assertTrue(silenced)
        assertTrue(resumed)
        assertEquals("still waiting", ResidentNotifications.State.WAITING, state())
    }

    @Test
    fun theListeningTimeTurnsTheSwitchOff() {
        preferences.write(preferences.read().copy(duration = ListeningDuration.MINUTES_10))
        turnOn()
        val deadline = preferences.read().deadline
        assertTrue("D77: ten minutes from now", deadline != null && deadline - System.currentTimeMillis() in 9 * 60_000L..10 * 60_000L)
        // The deadline moves forward instead of waiting ten minutes; the running service follows it.
        preferences.update { it.copy(deadline = System.currentTimeMillis() + 3_000) }
        withSettings { activity -> ResidentWake.refresh(activity) }
        val off = await(20_000) { !ResidentWake.enabled(context) && !running() }
        SpeechDevice.log("resident time limit off=$off notifications=${notifications().map { it.id }}")
        assertTrue("the switch turned off at the deadline", off)
        assertTrue("no notification stays, nothing reminds", await(5_000) { notifications().isEmpty() })
        assertTrue("the wake session is closed", await(5_000) { !sessions.backgroundListening })
    }

    @Test
    fun unpluggedItPausesAndPluggedItListensAgain() {
        AssistantDevice.shell("dumpsys battery set ac 1")
        preferences.write(preferences.read().copy(chargingOnly = true))
        turnOn()
        AssistantDevice.shell("dumpsys battery unplug")
        val paused = awaitState(ResidentNotifications.State.PAUSED, 15_000)
        val silent = await(5_000) { !sessions.backgroundListening }
        // Back in the background before the power returns: the session reopens there (D76).
        AssistantDevice.shell("input keyevent KEYCODE_HOME")
        Thread.sleep(2_000)
        AssistantDevice.shell("dumpsys battery set ac 1")
        await(30_000) { state() == ResidentNotifications.State.WAITING || state() == ResidentNotifications.State.BLOCKED }
        Thread.sleep(BLOCK_WAIT_MILLIS)
        val after = state()
        val plays = if (after == ResidentNotifications.State.WAITING) hit() else 0
        if (plays > 0) route()
        closeSheets()
        SpeechDevice.log("resident power paused=$paused silent=$silent plugged state=$after background hit plays=$plays")
        assertTrue("paused while unplugged", paused)
        assertTrue("no recording while paused", silent)
        assertNotEquals("listening or waiting for a tap after the power came back", ResidentNotifications.State.PAUSED, after)
        if (after == ResidentNotifications.State.WAITING) assertTrue("the wake word is heard after the power came back", plays > 0)
    }

    @Test
    fun anotherResidentListenerTurnsTheSwitchAway() {
        rival(true)
        val app = ResidentWake.others(context).singleOrNull()
        assertEquals(TEST_PACKAGE, app?.packageName)
        assertEquals(VoiceCommandTargetTwo::class.java.name, app?.settingsClass)
        // D75 through the settings screen: the conflict dialog names the other app and opens its settings.
        AssistantDevice.Targets().use { targets ->
            ActivityScenario.launch<SettingsActivity>(Intent(context, SettingsActivity::class.java)).use {
                clickSwitch()
                compose.waitUntil(15_000) { compose.exists("macaw.dialog.residentConflict") }
                val message = compose.text("macaw.dialog.residentConflict.message").orEmpty()
                Thread.sleep(DIALOG_SETTLE_MILLIS)
                capture(prefix, "conflict")
                compose.onNodeWithTag("macaw.dialog.residentConflict.confirm").performSemanticsAction(SemanticsActions.OnClick)
                val opened = targets.next()?.getString("target")
                val label = app!!.label
                SpeechDevice.log("resident conflict dialog names rival=${label in message} opened=${opened?.substringAfterLast('.')}")
                assertTrue("the dialog names the other app", label in message)
                assertEquals("its settings entry opens", VoiceCommandTargetTwo::class.java.name, opened)
            }
        }
        assertFalse("the switch stays off", ResidentWake.enabled(context))
        // A stale start while the other app listens (D75): the service turns the switch off and says why.
        withSettings { activity -> ResidentWake.turnOn(activity) }
        val away = await(15_000) { !running() && !ResidentWake.enabled(context) }
        SpeechDevice.log("resident conflict at start off=$away stopped notification=${posted(ResidentNotifications.STOPPED_ID)}")
        assertTrue(away)
        assertTrue(posted(ResidentNotifications.STOPPED_ID))
    }

    @Test
    fun theStoveListenerTurnsTheSwitchAway() {
        // E3 with the real 3-Stove Agent (D75): its resident listener is on, so the switch shows the conflict and opens it.
        assumeTrue("opt-in: -e macawStove true", stove)
        val app = ResidentWake.others(context).firstOrNull { it.packageName == STOVE }
        assumeTrue("3-Stove Agent's resident listener is on", app != null)
        ActivityScenario.launch<SettingsActivity>(Intent(context, SettingsActivity::class.java)).use {
            clickSwitch()
            compose.waitUntil(15_000) { compose.exists("macaw.dialog.residentConflict") }
            val message = compose.text("macaw.dialog.residentConflict.message").orEmpty()
            Thread.sleep(DIALOG_SETTLE_MILLIS)
            capture(prefix, "stove-conflict")
            compose.onNodeWithTag("macaw.dialog.residentConflict.confirm").performSemanticsAction(SemanticsActions.OnClick)
            val opened = await(10_000) { AssistantDevice.shell("dumpsys activity activities").lines().any { "ResumedActivity" in it && STOVE in it } }
            capture(prefix, "stove-settings")
            SpeechDevice.log("resident stove conflict names=${app!!.label in message} label=${app.label} settings opened=$opened")
            AssistantDevice.shell("input keyevent KEYCODE_HOME")
            assertTrue("the dialog names 3-Stove Agent", app.label in message)
            assertTrue("3-Stove Agent's resident settings open", opened)
        }
        assertFalse("the switch stays off", ResidentWake.enabled(context))
    }

    @Test
    fun aRebootRemindsAndTheTrampolineResumes() {
        // The receiver would also bring an enabled floating microphone back (D62), which this case must not do.
        assumeTrue("the floating microphone is off", !TriggerPreferences(context).floating)
        turnOn()
        // A reboot or an update ends the process; here the service stops while the switch stays on.
        context.stopService(Intent(context, ResidentWakeService::class.java))
        assertTrue(awaitStopped())
        assertTrue(ResidentWake.enabled(context))
        TriggerResumeReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertTrue("D74: tap to resume", await(5_000) { posted(ResidentNotifications.RESUME_ID) })
        // The notification's tap: the transparent trampoline starts the service while visible.
        context.startActivity(ResidentStartActivity.intent(context))
        val resumed = awaitState(ResidentNotifications.State.WAITING)
        val reminderGone = await(5_000) { !posted(ResidentNotifications.RESUME_ID) }
        // Past the deadline the reminder turns the switch off instead (D77).
        context.stopService(Intent(context, ResidentWakeService::class.java))
        assertTrue(awaitStopped())
        preferences.update { it.copy(duration = ListeningDuration.MINUTES_10, deadline = System.currentTimeMillis() - 1_000) }
        TriggerResumeReceiver().onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
        val off = !ResidentWake.enabled(context) && !posted(ResidentNotifications.RESUME_ID)
        SpeechDevice.log("resident reboot reminder resumed=$resumed reminderGone=$reminderGone expired off=$off")
        assertTrue(resumed)
        assertTrue(reminderGone)
        assertTrue(off)
    }

    @Test
    fun aHitWithTheScreenOffTurnsTheScreenOn() {
        assumeTrue("opt-in: -e macawKeyguard true (emulators)", SpeechDevice.argument("macawKeyguard") == "true")
        preferences.write(preferences.read().copy(readAloud = false))
        turnOn()
        val power = context.getSystemService(PowerManager::class.java)
        AssistantDevice.shell("input keyevent KEYCODE_SLEEP")
        assertTrue("the screen is off", await(10_000) { !power.isInteractive })
        Thread.sleep(2_000)
        val plays = hit()
        val awake = await(15_000) { power.isInteractive }
        val route = if (plays > 0) route() else "none"
        capture(prefix, "screen-off-hit")
        SpeechDevice.log("resident screen off hit plays=$plays route=$route awake=$awake")
        closeSheets()
        AssistantDevice.shell("input keyevent KEYCODE_WAKEUP")
        AssistantDevice.shell("wm dismiss-keyguard")
        assertTrue("D73: heard with the screen off", plays > 0)
        assertEquals("the sheet opens over the lock screen", "open", route)
        assertTrue("the screen turned on", awake)
    }

    private companion object {
        /** The keyword of `kws-zh-6.wav` (TakeoverDeviceTest). zh-CN: `kws-zh-6.wav` 中的关键词. */
        const val KEYWORD = "\u6731\u4e3d\u6960"
        const val KEYWORD_AUDIO = "test-audio/kws-zh-6.wav"
        const val HIT_WAIT_MILLIS = 3_000L
        const val MAX_PLAYS = 3

        /** Time for the sheet to start reading the result aloud before it closes. zh-CN: 弹层关闭前开始朗读结果的时间. */
        const val READ_ALOUD_MILLIS = 2_500L

        /** The dialog's enter animation, before a screenshot. zh-CN: 截图前等待对话框的进入动画. */
        const val DIALOG_SETTLE_MILLIS = 500L

        /** Longer than the background block check of D76 (15 s). zh-CN: 长于 D76 的后台阻断检查 (15 s). */
        const val BLOCK_WAIT_MILLIS = 16_000L

        const val STOVE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
        const val TEST_PACKAGE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.test"
        val RESIDENT_IDS = setOf(ResidentNotifications.LISTENING_ID, ResidentNotifications.SPEAK_ID, ResidentNotifications.STOPPED_ID, ResidentNotifications.RESUME_ID)
    }
}
