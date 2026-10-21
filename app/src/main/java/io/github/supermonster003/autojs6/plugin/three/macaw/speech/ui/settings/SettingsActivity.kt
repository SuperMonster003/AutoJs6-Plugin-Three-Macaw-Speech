package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.MacawVoiceInteractionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferencePolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.HitRoute
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ListeningDuration
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentAvailability
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentKeywords
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentListenerApp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentNotifications
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentSettings
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.WakeSensitivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.FloatingMicService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.HeadsetTriggerComponent
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.KeyGesture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.KeyTriggerService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TileOutcome
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerPolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.VoiceTileService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.VoiceWidgetProvider
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.about.NoticesActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.HostAppearanceReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.LauncherIconMode
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.LauncherIcons
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.ThemeColorValue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawScreen
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MultiChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SwitchRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.ThemeColorDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeTexts
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.ManagerBackend
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.OperationTracker
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.TrackedOperation
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.release.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.UpdateDialogs
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputComponent
import org.autojs.plugin.speech.api.SpeechContract
import java.util.Locale
import java.util.concurrent.Executors

/**
 * The settings screen (roadmap P5.2, standalone settings specification): appearance (language, night mode, theme
 * color, launcher icon; the first three follow AutoJs6 by default), recognition and synthesis defaults (tryout screen and
 * system TTS engine only, maintainer's decision of 2026-10-16), resources and about. Every choice dialog edits a draft
 * and saves once on confirm. It is also the `settingsActivity` of the system TTS engine (`res/xml/tts_engine.xml`): the
 * system settings start it with `ACTION_MAIN`, and it then opens at the synthesis group. Through `RecognitionSettingsAlias`
 * it is also the settings entry of the digital assistant and the recognition service (roadmap P5.7) and opens at the
 * recognition group, whose "Digital assistant" row tells whether 3-Macaw Speech is the default assistant and opens the
 * system settings to choose it (the plugin cannot choose it itself, D48). Its triggers group (roadmap P7.1, D58 - D65)
 * sets up the entries that open the assistant sheet; through `TriggerSettingsAlias` (a long press on the tile) the screen
 * opens there. Its resident wake word group (roadmap P7.4, D66 - D80) switches the resident wake word, whose switch is the
 * component state of `ResidentWakeService`; through `ResidentSettingsAlias` (the notification, other resident listener
 * apps) the screen opens there.
 * zh-CN:设置页 (路线图 P5.2, 独立设置规范): 外观 (语言, 夜间模式, 主题色, 启动器图标; 前三项默认跟随 AutoJs6), 识别与合成默认值 (按维护者 2026-10-16
 * 的决定只作用于试听试说页与系统 TTS 引擎), 资源与关于. 每个选择对话框编辑草稿, 确定时保存一次. 它也是系统 TTS 引擎的 `settingsActivity`
 * (`res/xml/tts_engine.xml`): 系统设置以 `ACTION_MAIN` 启动它, 此时定位到合成分组. 经 `RecognitionSettingsAlias` 它也是数字助理与识别服务的设置入口
 * (路线图 P5.7), 此时定位到识别分组; 该组的 "数字助理" 行显示 3-Macaw Speech 是否为默认助理, 并打开系统设置供用户选择 (插件无法自行选择, D48).
 * "触发方式" 分组 (路线图 P7.1, D58 - D65) 设置打开助理弹层的各个入口; 经 `TriggerSettingsAlias` (长按磁贴) 打开时定位到该分组. "常驻唤醒" 分组
 * (路线图 P7.4, D66 - D80) 开关常驻唤醒, 开关即 `ResidentWakeService` 的组件状态; 经 `ResidentSettingsAlias` (通知, 其他常驻监听应用) 打开时定位到该分组.
 */
class SettingsActivity : StandaloneActivity() {

    private enum class Dialog {
        LANGUAGE, DARK_MODE, THEME_COLOR, LAUNCHER_ICON, ASR_MODEL, ENDPOINT, TTS_MODEL, SPEAKER, SPEED, VOICE_LANGUAGE, VOICE,
        IDLE, RECYCLE, IGNORED, TILE_STEPS, WIDGET_STEPS, KEY_SERVICE, KEY_GESTURE,
        RESIDENT_KEYWORDS, RESIDENT_SENSITIVITY, RESIDENT_DURATION, RESIDENT_CONFLICT, RESIDENT_OVERLAY,
    }

    /**
     * What the resident wake word switch needs (roadmap D78), loaded off the main thread: [unspellable] is the first wake
     * word the model cannot spell, [others] the apps whose resident listener is on (D75).
     * zh-CN: 常驻唤醒开关所需的条件 (路线图 D78), 在主线程之外加载: [unspellable] 是模型无法拼写的第一个唤醒词, [others] 是常驻监听已开启的应用 (D75).
     */
    private data class ResidentState(
        val availability: ResidentAvailability? = null,
        val unspellable: String? = null,
        val others: List<ResidentListenerApp> = emptyList(),
    ) {
        val ready: Boolean get() = availability == ResidentAvailability.READY && unspellable == null
    }

    /** What the speech rows show, loaded off the main thread. zh-CN: 语音相关行显示的内容, 在主线程之外加载. */
    private data class SpeechState(
        val asr: List<ModelEntry> = emptyList(),
        val tts: List<ModelEntry> = emptyList(),
        val speakers: List<String> = emptyList(),
        val voices: TtsVoiceTable? = null,
        val modelNames: Map<String, String> = emptyMap(),
    )

    private lateinit var preferences: SpeechPreferences
    private lateinit var updates: AppUpdateController
    private var speech by mutableStateOf(SpeechState())
    private var revision by mutableIntStateOf(0)
    private val snackbar = SnackbarHostState()
    private var pendingMessage by mutableStateOf<String?>(null)
    private var dialog by mutableStateOf<Dialog?>(null)
    private var voiceLanguage by mutableStateOf<String?>(null)
    private var busy by mutableStateOf<TrackedOperation.Kind?>(null)
    private var assistantActive by mutableStateOf(false)
    private var floatingOn by mutableStateOf(false)
    private var keyEnabled by mutableStateOf(false)
    private var awaitingOverlay = false
    private var residentOn by mutableStateOf(false)
    private var resident by mutableStateOf(ResidentState())
    private var residentRival by mutableStateOf<ResidentListenerApp?>(null)
    private var awaitingResidentOverlay = false

    /** Roadmap D78: the microphone for the resident wake word; granted, the switch tries again. zh-CN: 常驻唤醒的麦克风权限; 授予后重试开启. */
    private val residentMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            setResident(true)
        } else {
            pendingMessage = getString(R.string.settings_resident_microphone)
            if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) openAppSettings()
        }
    }

    /** Roadmap D78: asked once; the switch turns on whatever the answer. zh-CN: 只问一次; 无论结果都继续开启. */
    private val residentNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { askResidentOverlay() }

    private val outcomes: (OperationTracker.Outcome) -> Unit = { outcome ->
        val kind = outcome.operation.kind
        if (kind == TrackedOperation.Kind.REFRESH || kind == TrackedOperation.Kind.RECYCLE) {
            busy = null
            pendingMessage = when (outcome) {
                is OperationTracker.Outcome.Completed -> when (val result = outcome.result) {
                    is OperationResult.Catalog -> getString(if (result.updated) R.string.settings_catalog_updated else R.string.settings_catalog_current, result.catalogVersion)
                    is OperationResult.Recycled -> getString(R.string.settings_recycle_done, HomeTexts.size(this, result.freedBytes), result.files)
                    else -> null
                }
                is OperationTracker.Outcome.Failed -> getString(if (kind == TrackedOperation.Kind.REFRESH) R.string.settings_catalog_failed else R.string.settings_recycle_failed)
            }
            loadSpeech()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = SpeechPreferences(this)
        updates = AppUpdateController(AppUpdateStore.of(this), BuildConfig.VERSION_NAME)
        super.onCreate(savedInstanceState)
        OperationTracker.shared.addListener(outcomes)
        busy = OperationTracker.shared.running(TrackedOperation.Kind.REFRESH)?.kind ?: OperationTracker.shared.running(TrackedOperation.Kind.RECYCLE)?.kind
    }

    override fun onResume() {
        super.onResume()
        loadSpeech()
        // Back from the system settings: the assistant may have been chosen there (roadmap P5.7).
        assistantActive = MacawVoiceInteractionService.isActive(this)
        resumeFloating()
        // Back from the accessibility settings: the key trigger may have been turned on there (roadmap D64).
        keyEnabled = KeyTriggerService.isEnabled(this)
        resumeResident()
    }

    /**
     * Roadmap D74 / D76 / D77: a visible settings screen resumes an enabled resident wake word (or turns it off when its
     * listening time ran out); back from the overlay permission page the switch turns on whatever the answer (D78).
     * zh-CN: 路线图 D74 / D76 / D77: 可见的设置页恢复开着的常驻唤醒 (监听时长已到则关闭); 从悬浮窗权限页返回时无论结果都开启开关 (D78).
     */
    private fun resumeResident() {
        if (awaitingResidentOverlay) {
            awaitingResidentOverlay = false
            turnResidentOn()
        } else {
            ResidentWake.resume(this)
        }
        residentOn = ResidentWake.enabled(this)
        loadResident()
    }

    private fun loadResident() {
        val app = applicationContext
        val keywords = ResidentPreferences(this).read().keywords
        io.execute {
            val availability = runCatching { ResidentWake.availability(app) }.getOrDefault(ResidentAvailability.NO_WAKE_MODEL)
            val unspellable = if (availability == ResidentAvailability.READY) ResidentWake.unspellable(app, keywords) else null
            val state = ResidentState(availability, unspellable, ResidentWake.others(app))
            main.post { if (!isDestroyed) resident = state }
        }
    }

    /**
     * Roadmap D78: off turns the service off; on checks the other resident listener apps (D75) and what the switch needs,
     * then asks for the notification permission once (API 33+) and for the overlay permission (D71) before it turns on.
     * zh-CN: 路线图 D78: 关闭即停止服务; 开启时检查其他常驻监听应用 (D75) 与开关所需的条件, 然后申请一次通知权限 (API 33+) 与悬浮窗权限 (D71), 再开启.
     */
    private fun setResident(on: Boolean) {
        if (!on) {
            ResidentWake.turnOff(this)
            residentOn = false
            return
        }
        val app = applicationContext
        val keywords = ResidentPreferences(this).read().keywords
        io.execute {
            val availability = runCatching { ResidentWake.availability(app) }.getOrDefault(ResidentAvailability.NO_WAKE_MODEL)
            val unspellable = if (availability == ResidentAvailability.READY) ResidentWake.unspellable(app, keywords) else null
            val state = ResidentState(availability, unspellable, ResidentWake.others(app))
            main.post {
                if (isDestroyed) return@post
                resident = state
                val rival = state.others.firstOrNull()
                when {
                    rival != null -> {
                        residentRival = rival
                        dialog = Dialog.RESIDENT_CONFLICT
                    }
                    state.availability == ResidentAvailability.MICROPHONE ->
                        runCatching { residentMicrophone.launch(Manifest.permission.RECORD_AUDIO) }.onFailure { openAppSettings() }
                    !state.ready -> pendingMessage = residentBlocker(state)
                    else -> askResidentNotifications()
                }
            }
        }
    }

    private fun askResidentNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !preferences.notificationsAsked &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            preferences.notificationsAsked = true
            if (runCatching { residentNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }.isSuccess) return
        }
        askResidentOverlay()
    }

    private fun askResidentOverlay() {
        if (HitRoute.askOverlay(MacawVoiceInteractionService.isActive(this), Settings.canDrawOverlays(this), Build.VERSION.SDK_INT)) {
            dialog = Dialog.RESIDENT_OVERLAY
        } else {
            turnResidentOn()
        }
    }

    private fun openResidentOverlay() {
        awaitingResidentOverlay = true
        val page = "package:$packageName".toUri()
        val opened = listOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, page), Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            .any { intent -> runCatching { startActivity(intent) }.isSuccess }
        if (!opened) {
            awaitingResidentOverlay = false
            turnResidentOn()
        }
    }

    private fun turnResidentOn() {
        residentOn = ResidentWake.turnOn(this) && ResidentWake.enabled(this)
        if (!residentOn) ResidentWake.turnOff(this)
        revision++
    }

    /** The first thing the switch misses (D78), or null when it can turn on. zh-CN: 开关缺少的第一项 (D78); 可开启时为 null. */
    private fun residentBlocker(state: ResidentState): String? = when {
        state.availability == ResidentAvailability.MICROPHONE -> getString(R.string.settings_resident_microphone)
        state.availability == ResidentAvailability.NO_WAKE_MODEL -> getString(R.string.settings_resident_no_wake_model)
        state.availability == ResidentAvailability.NO_RECOGNITION_MODEL -> getString(R.string.settings_resident_no_recognition_model)
        state.unspellable != null -> getString(R.string.settings_resident_keywords_unspellable, state.unspellable)
        else -> null
    }

    /** D78: a tap on the row that names the missing piece grants it or opens where it is set. zh-CN: 点按写明缺项的行即去授权或打开对应位置. */
    private fun fixResident() {
        when {
            resident.availability == ResidentAvailability.MICROPHONE ->
                runCatching { residentMicrophone.launch(Manifest.permission.RECORD_AUDIO) }.onFailure { openAppSettings() }
            resident.availability == ResidentAvailability.NO_WAKE_MODEL || resident.availability == ResidentAvailability.NO_RECOGNITION_MODEL ->
                startActivity(HomeActivity.intent(this))
            resident.unspellable != null -> dialog = Dialog.RESIDENT_KEYWORDS
        }
    }

    private fun openAppSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) }
    }

    /** Saves the resident preferences; [restart] reopens the wake session, else the service follows the rules. zh-CN: 保存常驻偏好. */
    private fun saveResident(restart: Boolean, transform: (ResidentSettings) -> ResidentSettings) {
        ResidentPreferences(this).update(transform)
        if (restart) ResidentWake.restart(this) else ResidentWake.refresh(this)
        saved()
        if (restart) loadResident()
    }

    /**
     * Roadmap D62: back from the overlay permission page, the floating microphone turns on when the permission was
     * granted; otherwise a switch that is on starts the service again if it is not running (a force stop ends it).
     * zh-CN: 路线图 D62: 从悬浮窗权限页返回时, 已授权则开启悬浮麦克风; 否则开关为开而服务未运行时重新启动 (强行停止会结束服务).
     */
    private fun resumeFloating() {
        if (awaitingOverlay) {
            awaitingOverlay = false
            if (Settings.canDrawOverlays(this)) {
                TriggerPreferences(this).floating = true
                FloatingMicService.start(this)
            } else {
                pendingMessage = getString(R.string.settings_floating_denied)
            }
        } else if (FloatingMicService.isOn(this) && FloatingMicService.running == null) {
            FloatingMicService.start(this)
        }
        floatingOn = FloatingMicService.isOn(this)
    }

    /** Roadmap D62: off by default; turning it on asks for the overlay permission first. zh-CN: 默认关; 开启时先申请悬浮窗权限. */
    private fun setFloating(on: Boolean) {
        val triggers = TriggerPreferences(this)
        if (!on) {
            triggers.floating = false
            FloatingMicService.stop(this)
            floatingOn = false
            return
        }
        if (Settings.canDrawOverlays(this)) {
            triggers.floating = true
            floatingOn = FloatingMicService.start(this)
            return
        }
        awaitingOverlay = true
        val page = "package:$packageName".toUri()
        val opened = listOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, page), Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            .any { intent -> runCatching { startActivity(intent) }.isSuccess }
        if (!opened) {
            awaitingOverlay = false
            pendingMessage = getString(R.string.settings_floating_denied)
        }
    }

    /**
     * Roadmap D48: the system settings where the user picks the digital assistant app; the default apps list and the
     * settings home are fallbacks for systems without that page.
     * zh-CN: 路线图 D48: 用户选择数字助理应用的系统设置页; 没有该页面的系统依次退到默认应用列表与设置首页.
     */
    private fun openAssistantSettings() {
        val opened = listOf(Settings.ACTION_VOICE_INPUT_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, Settings.ACTION_SETTINGS)
            .firstOrNull { action -> runCatching { startActivity(Intent(action)) }.isSuccess }
        Log.i(TAG, "Assistant settings opened with ${opened ?: "nothing"}")
    }

    override fun onDestroy() {
        OperationTracker.shared.removeListener(outcomes)
        if (isFinishing) updates.cancel()
        super.onDestroy()
    }

    override fun holdsAppearance(): Boolean = dialog != null || updates.state != null

    private fun loadSpeech() {
        val app = applicationContext
        io.execute {
            val state = runCatching {
                val runtime = SpeechRuntime.get(app)
                val models = SpeechModels.get(app).list(installed = true)
                val tts = models.filter { it.kind == SpeechContract.KIND_TTS }
                val defaultTts = tts.firstOrNull { it.isDefault }
                val speakers = defaultTts?.let { entry ->
                    runtime.models.store.installed(entry.id)?.let { model ->
                        val known = runtime.speakers(model)
                        List(known.count) { index -> known.names.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "#$index" }
                    }
                }.orEmpty()
                SpeechState(
                    asr = models.filter { it.kind == SpeechContract.KIND_ASR },
                    tts = tts,
                    speakers = speakers,
                    voices = runtime.voiceTable(),
                    modelNames = models.associate { it.id to it.name },
                )
            }.getOrDefault(SpeechState())
            main.post { if (!isDestroyed) speech = state }
        }
    }

    @Composable
    override fun Content() {
        val message = pendingMessage
        LaunchedEffect(message) {
            if (message != null) {
                snackbar.showSnackbar(message)
                pendingMessage = null
            }
        }
        // The recognition entry of the assistant and the recognition service first, then the trigger entry of the tile and
        // the key service (roadmap P7.1), then the resident entry (P7.4), then the TTS engine's (ACTION_MAIN).
        val alias = intent?.component?.className
        val fromRecognition = alias?.endsWith(RECOGNITION_ALIAS) == true
        val fromTriggers = !fromRecognition && alias?.endsWith(TRIGGER_ALIAS) == true
        val fromResident = !fromRecognition && !fromTriggers && alias?.endsWith(ResidentNotifications.RESIDENT_SETTINGS) == true
        val fromSystem = !fromRecognition && !fromTriggers && !fromResident && intent?.action == Intent.ACTION_MAIN
        val scroll = rememberScrollState()
        var synthesisTop by remember { mutableIntStateOf(-1) }
        var recognitionTop by remember { mutableIntStateOf(-1) }
        var triggersTop by remember { mutableIntStateOf(-1) }
        var residentTop by remember { mutableIntStateOf(-1) }
        var scrolled by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(synthesisTop, recognitionTop, triggersTop, residentTop) {
            val target = when {
                fromRecognition -> recognitionTop
                fromTriggers -> triggersTop
                fromResident -> residentTop
                fromSystem -> synthesisTop
                else -> -1
            }
            if (!scrolled && target >= 0) {
                scroll.scrollTo(target)
                scrolled = true
            }
        }
        // Saved choices bump the revision, so the summaries below read the stored values again.
        val appearancePreferences = remember(revision) { AppearancePreferences.read(this) }
        MacawScreen(title = stringResource(R.string.settings_title), onBack = ::finish, snackbarHostState = snackbar) { padding ->
            // A saved choice bumps the revision and rebuilds the rows from the stored values. zh-CN: 保存后修订号增加, 各行按存储值重建.
            key(revision, speech) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scroll).testTag("macaw.settings")) {
                SectionHeader(stringResource(R.string.settings_appearance))
                SettingsRow(stringResource(R.string.settings_language), summary = languageSummary(appearancePreferences.language), icon = R.drawable.ic_language_24,
                    onClick = { dialog = Dialog.LANGUAGE }, modifier = Modifier.testTag("macaw.settings.language"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_night_mode), summary = darkModeSummary(appearancePreferences.darkMode), icon = R.drawable.ic_dark_mode_24,
                    onClick = { dialog = Dialog.DARK_MODE }, modifier = Modifier.testTag("macaw.settings.night"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_theme_color), summary = colorSummary(appearancePreferences.color), icon = R.drawable.ic_palette_24,
                    onClick = { dialog = Dialog.THEME_COLOR }, modifier = Modifier.testTag("macaw.settings.color"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_launcher_icon), summary = stringResource(iconLabel(LauncherIcons.current(this@SettingsActivity))), icon = R.drawable.ic_apps_24,
                    onClick = { dialog = Dialog.LAUNCHER_ICON }, modifier = Modifier.testTag("macaw.settings.icon"))

                SectionHeader(stringResource(R.string.settings_recognition), Modifier.onGloballyPositioned { recognitionTop = it.positionInParent().y.toInt() })
                SettingsRow(stringResource(R.string.settings_asr_model), summary = defaultName(speech.asr), icon = R.drawable.ic_mic_24,
                    enabled = speech.asr.isNotEmpty(), onClick = { dialog = Dialog.ASR_MODEL }, modifier = Modifier.testTag("macaw.settings.asrModel"))
                Hairline()
                // Roadmap D43: the component state of the voice input sheet is the setting itself.
                var voiceInput by remember { mutableStateOf(VoiceInputComponent.isEnabled(this@SettingsActivity)) }
                SwitchRow(stringResource(R.string.settings_voice_input), voiceInput, { voiceInput = it; VoiceInputComponent.setEnabled(this@SettingsActivity, it) },
                    summary = stringResource(R.string.settings_voice_input_summary), icon = R.drawable.ic_mic_24, modifier = Modifier.testTag("macaw.settings.voiceInput"))
                Hairline()
                // Roadmap D48: whether 3-Macaw Speech is the default digital assistant; the system settings choose it.
                SettingsRow(stringResource(R.string.settings_assistant),
                    summary = stringResource(if (assistantActive) R.string.settings_assistant_on else R.string.settings_assistant_off),
                    icon = R.drawable.ic_record_voice_over_24, onClick = ::openAssistantSettings, modifier = Modifier.testTag("macaw.settings.assistant"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_endpoint), summary = stringResource(endpointLabel(preferences.endpointMillis)), icon = R.drawable.ic_timer_24,
                    onClick = { dialog = Dialog.ENDPOINT }, modifier = Modifier.testTag("macaw.settings.endpoint"))
                Hairline()
                var vad by remember { mutableStateOf(preferences.vad) }
                SwitchRow(stringResource(R.string.settings_vad), vad, { vad = it; preferences.vad = it }, summary = stringResource(R.string.settings_vad_summary),
                    icon = R.drawable.ic_tune_24, modifier = Modifier.testTag("macaw.settings.vad"))
                Text(
                    stringResource(R.string.settings_scope_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 64.dp, end = 24.dp, top = 4.dp),
                )

                // Roadmap P7.1 (D58 - D65): the entries that open the assistant sheet; none of them records until it listens.
                SectionHeader(stringResource(R.string.settings_triggers), Modifier.onGloballyPositioned { triggersTop = it.positionInParent().y.toInt() })
                SettingsRow(stringResource(R.string.settings_tile), summary = stringResource(R.string.settings_tile_summary), icon = R.drawable.ic_toggle_24,
                    onClick = ::addTile, modifier = Modifier.testTag("macaw.settings.tile"))
                Hairline()
                SwitchRow(stringResource(R.string.settings_floating), floatingOn, ::setFloating, summary = stringResource(R.string.settings_floating_summary),
                    icon = R.drawable.ic_mic_24, modifier = Modifier.testTag("macaw.settings.floating"))
                Hairline()
                // Roadmap D64: the key service is switched in the system accessibility settings; the gesture is set here.
                SettingsRow(stringResource(R.string.settings_key), summary = stringResource(if (keyEnabled) R.string.settings_key_on else R.string.settings_key_off),
                    icon = R.drawable.ic_volume_down_24, onClick = { dialog = Dialog.KEY_SERVICE }, modifier = Modifier.testTag("macaw.settings.key"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_key_gesture), summary = stringResource(gestureLabel(TriggerPreferences(this@SettingsActivity).keyGesture)),
                    icon = R.drawable.ic_tune_24, onClick = { dialog = Dialog.KEY_GESTURE }, modifier = Modifier.testTag("macaw.settings.keyGesture"))
                Hairline()
                // Roadmap D65: the component state of the headset entry is the setting itself.
                var headset by remember { mutableStateOf(HeadsetTriggerComponent.isEnabled(this@SettingsActivity)) }
                SwitchRow(stringResource(R.string.settings_headset), headset, { headset = it; HeadsetTriggerComponent.setEnabled(this@SettingsActivity, it) },
                    summary = stringResource(R.string.settings_headset_summary), icon = R.drawable.ic_headset_24, modifier = Modifier.testTag("macaw.settings.headset"))
                Hairline()
                // Roadmap D63: the launcher places the widget after its own confirmation; the shortcut needs no setup.
                SettingsRow(stringResource(R.string.settings_widget), summary = stringResource(R.string.settings_widget_summary), icon = R.drawable.ic_widgets_24,
                    onClick = { dialog = Dialog.WIDGET_STEPS },
                    modifier = Modifier.testTag("macaw.settings.widget"))
                Text(
                    stringResource(R.string.settings_triggers_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 64.dp, end = 24.dp, top = 4.dp),
                )

                // Roadmap P7.4 (D66 - D80): the resident wake word; the switch is the component state of its service (D75).
                SectionHeader(stringResource(R.string.settings_resident_group), Modifier.onGloballyPositioned { residentTop = it.positionInParent().y.toInt() })
                val residentSettings = remember { ResidentPreferences(this@SettingsActivity).read() }
                SwitchRow(stringResource(R.string.settings_resident), residentOn, ::setResident, summary = residentSummary(residentSettings),
                    icon = R.drawable.ic_record_voice_over_24, enabled = residentOn || resident.availability != null && (resident.ready || resident.availability == ResidentAvailability.MICROPHONE),
                    modifier = Modifier.testTag("macaw.settings.resident"))
                val blocker = if (residentOn || resident.availability == null) null else residentBlocker(resident)
                if (blocker != null) {
                    SettingsRow(blocker, icon = R.drawable.ic_warning_24, onClick = ::fixResident, modifier = Modifier.testTag("macaw.settings.residentMissing"))
                }
                Hairline()
                SettingsRow(stringResource(R.string.settings_resident_keywords), summary = residentSettings.keywords.joinToString(", "), icon = R.drawable.ic_tune_24,
                    onClick = { dialog = Dialog.RESIDENT_KEYWORDS }, modifier = Modifier.testTag("macaw.settings.residentKeywords"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_resident_sensitivity), summary = stringResource(sensitivityLabel(residentSettings.sensitivity)),
                    icon = R.drawable.ic_tune_24, onClick = { dialog = Dialog.RESIDENT_SENSITIVITY }, modifier = Modifier.testTag("macaw.settings.residentSensitivity"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_resident_duration), summary = stringResource(durationLabel(residentSettings.duration)),
                    icon = R.drawable.ic_timer_24, onClick = { dialog = Dialog.RESIDENT_DURATION }, modifier = Modifier.testTag("macaw.settings.residentDuration"))
                Hairline()
                SwitchRow(stringResource(R.string.settings_resident_charging), residentSettings.chargingOnly,
                    { on -> saveResident(restart = false) { it.copy(chargingOnly = on) } },
                    summary = stringResource(R.string.settings_resident_charging_note), icon = R.drawable.ic_pause_24, modifier = Modifier.testTag("macaw.settings.residentCharging"))
                Hairline()
                SwitchRow(stringResource(R.string.settings_resident_read_aloud), residentSettings.readAloud,
                    { on -> saveResident(restart = false) { it.copy(readAloud = on) } },
                    summary = stringResource(R.string.settings_resident_read_aloud_summary), icon = R.drawable.ic_volume_up_24, modifier = Modifier.testTag("macaw.settings.residentReadAloud"))
                Text(
                    stringResource(R.string.settings_resident_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 64.dp, end = 24.dp, top = 4.dp),
                )

                SectionHeader(stringResource(R.string.settings_synthesis), Modifier.onGloballyPositioned { synthesisTop = it.positionInParent().y.toInt() })
                SettingsRow(stringResource(R.string.settings_tts_model), summary = defaultName(speech.tts), icon = R.drawable.ic_volume_up_24,
                    enabled = speech.tts.isNotEmpty(), onClick = { dialog = Dialog.TTS_MODEL }, modifier = Modifier.testTag("macaw.settings.ttsModel"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_speaker), summary = speakerSummary(), icon = R.drawable.ic_person_24,
                    enabled = speech.speakers.size > 1, onClick = { dialog = Dialog.SPEAKER }, modifier = Modifier.testTag("macaw.settings.speaker"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_speed), summary = stringResource(R.string.settings_speed_value, preferences.speed), icon = R.drawable.ic_speed_24,
                    onClick = { dialog = Dialog.SPEED }, modifier = Modifier.testTag("macaw.settings.speed"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_voices), summary = voicesSummary(), icon = R.drawable.ic_language_24,
                    enabled = speech.voices?.isEmpty() == false, onClick = { dialog = Dialog.VOICE_LANGUAGE }, modifier = Modifier.testTag("macaw.settings.voices"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_install_voices), summary = stringResource(R.string.settings_install_voices_summary), icon = R.drawable.ic_download_24,
                    onClick = { startActivity(HomeActivity.intent(this@SettingsActivity, showTts = true)) }, modifier = Modifier.testTag("macaw.settings.installVoices"))

                SectionHeader(stringResource(R.string.settings_resources))
                SettingsRow(stringResource(R.string.settings_idle), summary = stringResource(R.string.settings_idle_value, preferences.idleMinutes), icon = R.drawable.ic_storage_24,
                    onClick = { dialog = Dialog.IDLE }, modifier = Modifier.testTag("macaw.settings.idle"))
                Hairline()
                var metered by remember { mutableStateOf(preferences.allowMeteredDownloads) }
                SwitchRow(stringResource(R.string.settings_metered), metered, { metered = it; preferences.allowMeteredDownloads = it },
                    summary = stringResource(R.string.settings_metered_summary), icon = R.drawable.ic_cloud_24, modifier = Modifier.testTag("macaw.settings.metered"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_catalog_refresh),
                    summary = stringResource(if (busy == TrackedOperation.Kind.REFRESH) R.string.settings_working else R.string.settings_catalog_refresh_summary, speechCatalogVersion()),
                    icon = R.drawable.ic_restart_24, enabled = busy == null, onClick = ::refreshCatalog, modifier = Modifier.testTag("macaw.settings.catalog"))
                Hairline()
                SettingsRow(stringResource(R.string.settings_recycle),
                    summary = stringResource(if (busy == TrackedOperation.Kind.RECYCLE) R.string.settings_working else R.string.settings_recycle_summary),
                    icon = R.drawable.ic_cleanup_24, enabled = busy == null, onClick = { dialog = Dialog.RECYCLE }, modifier = Modifier.testTag("macaw.settings.recycle"))

                SectionHeader(stringResource(R.string.settings_about))
                SettingsRow(stringResource(R.string.settings_version),
                    summary = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, stringResource(R.string.plugin_version_date)),
                    icon = R.drawable.ic_info_24, modifier = Modifier.testTag("macaw.settings.version"))
                Hairline()
                SettingsRow(stringResource(R.string.release_history_title), summary = stringResource(R.string.settings_release_history_summary), icon = R.drawable.ic_history_24,
                    onClick = { startActivity(Intent(this@SettingsActivity, ReleaseHistoryActivity::class.java)) }, modifier = Modifier.testTag("macaw.settings.releaseHistory"))
                Hairline()
                SettingsRow(stringResource(R.string.update_check), summary = stringResource(R.string.settings_update_summary), icon = R.drawable.ic_update_24,
                    onClick = updates::checkManually, modifier = Modifier.testTag("macaw.settings.update"))
                Hairline()
                val store = remember { AppUpdateStore.of(this@SettingsActivity) }
                var automatic by remember { mutableStateOf(store.automaticChecks) }
                SwitchRow(stringResource(R.string.update_automatic), automatic, { automatic = it; store.automaticChecks = it },
                    summary = stringResource(R.string.update_automatic_summary), icon = R.drawable.ic_notifications_24, modifier = Modifier.testTag("macaw.settings.autoUpdate"))
                Hairline()
                val ignored = store.ignoredTags.size
                SettingsRow(stringResource(R.string.update_ignored), summary = pluralStringOr(ignored), icon = R.drawable.ic_close_24,
                    enabled = ignored > 0, onClick = { dialog = Dialog.IGNORED }, modifier = Modifier.testTag("macaw.settings.ignored"))
                Hairline()
                SettingsRow(stringResource(R.string.notices_title), summary = stringResource(R.string.settings_notices_summary), icon = R.drawable.ic_license_24,
                    onClick = { startActivity(Intent(this@SettingsActivity, NoticesActivity::class.java)) }, modifier = Modifier.testTag("macaw.settings.notices"))
                Spacer(Modifier.height(24.dp))
            }
            }
        }
        Dialogs()
        UpdateDialogs(updates, onOpenRelease = ::open, onReleaseHistory = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) })
    }

    @Composable
    private fun pluralStringOr(count: Int): String =
        if (count == 0) stringResource(R.string.update_ignored_none) else androidx.compose.ui.res.pluralStringResource(R.plurals.update_ignored_count, count, count)

    @Composable
    private fun Dialogs() {
        val close = { dialog = null }
        when (dialog) {
            null -> Unit
            Dialog.LANGUAGE -> {
                val current = AppearancePreferences.read(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_language),
                    options = AppearancePreferences.LANGUAGES.map { Choice(languageLabel(it), followNote(it == AppearancePreferences.FOLLOW_HOST)) },
                    selectedIndex = AppearancePreferences.LANGUAGES.indexOf(current.language).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveAppearance(current.copy(language = AppearancePreferences.LANGUAGES[index])) },
                    tag = "macaw.dialog.language",
                )
            }
            Dialog.DARK_MODE -> {
                val current = AppearancePreferences.read(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_night_mode),
                    options = AppearancePreferences.DARK_MODES.map { Choice(darkModeLabel(it), followNote(it == AppearancePreferences.FOLLOW_HOST)) },
                    selectedIndex = AppearancePreferences.DARK_MODES.indexOf(current.darkMode).coerceAtLeast(0),
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveAppearance(current.copy(darkMode = AppearancePreferences.DARK_MODES[index])) },
                    tag = "macaw.dialog.night",
                )
            }
            Dialog.THEME_COLOR -> {
                val current = AppearancePreferences.read(this)
                ThemeColorDialog(
                    currentColor = current.color,
                    hostColor = HostAppearanceReader.cached()?.primary ?: AppearancePreferences.DEFAULT_COLOR,
                    onDismissRequest = close,
                    onConfirm = { color -> close(); saveAppearance(current.copy(color = color)) },
                )
            }
            Dialog.LAUNCHER_ICON -> ChoiceDialog(
                title = stringResource(R.string.settings_launcher_icon),
                options = LauncherIconMode.entries.map { mode ->
                    Choice(stringResource(iconLabel(mode)), when (mode) {
                        LauncherIconMode.AUTO -> stringResource(R.string.launcher_icon_auto_note)
                        LauncherIconMode.TRANSPARENT -> stringResource(R.string.launcher_icon_transparent_note)
                        else -> null
                    })
                },
                selectedIndex = LauncherIcons.current(this).ordinal,
                onDismissRequest = close,
                onConfirm = { index ->
                    close()
                    val result = runCatching { LauncherIcons.select(this, LauncherIconMode.entries[index]) }
                    revision++
                    pendingMessage = getString(if (result.isSuccess) R.string.launcher_icon_applied_note else R.string.launcher_icon_failed)
                },
                tag = "macaw.dialog.icon",
            )
            Dialog.ASR_MODEL -> ModelChoice(R.string.settings_asr_model, speech.asr, SpeechContract.KIND_ASR, "macaw.dialog.asrModel")
            Dialog.TTS_MODEL -> ModelChoice(R.string.settings_tts_model, speech.tts, SpeechContract.KIND_TTS, "macaw.dialog.ttsModel")
            Dialog.ENDPOINT -> ChoiceDialog(
                title = stringResource(R.string.settings_endpoint),
                message = stringResource(R.string.settings_endpoint_message),
                options = SpeechPreferencePolicy.ENDPOINT_MILLIS.map { Choice(stringResource(endpointLabel(it)), stringResource(R.string.settings_endpoint_note, it)) },
                selectedIndex = SpeechPreferencePolicy.ENDPOINT_MILLIS.indexOf(preferences.endpointMillis),
                onDismissRequest = close,
                onConfirm = { index -> close(); preferences.endpointMillis = SpeechPreferencePolicy.ENDPOINT_MILLIS[index]; saved() },
                tag = "macaw.dialog.endpoint",
            )
            Dialog.SPEAKER -> {
                val model = speech.tts.firstOrNull { it.isDefault }
                if (model == null) {
                    close()
                } else {
                    val chosen = preferences.speaker(model.id)
                    ChoiceDialog(
                        title = stringResource(R.string.settings_speaker),
                        options = listOf(Choice(stringResource(R.string.settings_speaker_model_default))) + speech.speakers.mapIndexed { index, name -> Choice(speakerLabel(index, name)) },
                        selectedIndex = chosen?.let { it + 1 } ?: 0,
                        onDismissRequest = close,
                        onConfirm = { index -> close(); preferences.setSpeaker(model.id, if (index == 0) null else index - 1); saved(); loadSpeech() },
                        tag = "macaw.dialog.speaker",
                    )
                }
            }
            Dialog.SPEED -> SpeedDialog(onDismiss = close) { value -> close(); preferences.speed = value; saved() }
            Dialog.VOICE_LANGUAGE -> {
                val languages = voiceLanguages()
                ChoiceDialog(
                    title = stringResource(R.string.settings_voices),
                    message = stringResource(R.string.settings_voices_message),
                    options = languages.map { (code, label) -> Choice(label, preferences.voices()[code]?.let(::voiceLabel) ?: stringResource(R.string.settings_voice_automatic)) },
                    selectedIndex = 0,
                    onDismissRequest = close,
                    onConfirm = { index ->
                        voiceLanguage = languages.getOrNull(index)?.first
                        dialog = Dialog.VOICE
                    },
                    tag = "macaw.dialog.voiceLanguage",
                )
            }
            Dialog.VOICE -> {
                val language = voiceLanguage
                val voices = language?.let(::voicesFor).orEmpty()
                val chosen = language?.let { preferences.voices()[it] }
                ChoiceDialog(
                    title = Locale.forLanguageTag(language ?: "").getDisplayName(resources.configuration.locales[0]),
                    options = listOf(Choice(stringResource(R.string.settings_voice_automatic), stringResource(R.string.settings_voice_automatic_note))) +
                        voices.map { Choice(voiceLabel(it.name), it.languageTag) },
                    selectedIndex = voices.indexOfFirst { it.name == chosen }.let { if (it < 0) 0 else it + 1 },
                    onDismissRequest = close,
                    onConfirm = { index ->
                        close()
                        if (language != null) preferences.setVoice(language, if (index == 0) null else voices[index - 1].name)
                        saved()
                    },
                    tag = "macaw.dialog.voice",
                )
            }
            Dialog.IDLE -> ChoiceDialog(
                title = stringResource(R.string.settings_idle),
                message = stringResource(R.string.settings_idle_message),
                options = SpeechPreferencePolicy.IDLE_MINUTES.map { Choice(stringResource(R.string.settings_idle_value, it)) },
                selectedIndex = SpeechPreferencePolicy.IDLE_MINUTES.indexOf(preferences.idleMinutes),
                onDismissRequest = close,
                onConfirm = { index ->
                    close()
                    preferences.idleMinutes = SpeechPreferencePolicy.IDLE_MINUTES[index]
                    SpeechRuntime.get(this).engines.idleMillis = preferences.idleMillis
                    saved()
                },
                tag = "macaw.dialog.idle",
            )
            Dialog.RECYCLE -> MessageDialog(
                title = stringResource(R.string.settings_recycle),
                message = stringResource(R.string.settings_recycle_message),
                onDismissRequest = close,
                onConfirm = { close(); recycle() },
                confirmText = stringResource(R.string.settings_recycle_confirm),
                tag = "macaw.dialog.recycle",
            )
            Dialog.TILE_STEPS -> MessageDialog(
                title = stringResource(R.string.settings_tile_steps_title),
                message = stringResource(R.string.settings_tile_steps),
                onDismissRequest = close,
                onConfirm = close,
                dismissText = null,
                tag = "macaw.dialog.tileSteps",
            )
            Dialog.WIDGET_STEPS -> {
                // Roadmap D63: the steps always show, because some launchers (ColorOS, HyperOS tablets) accept a pin request
                // without showing anything; where the launcher supports it, a button asks it to place the widget as well.
                val pinnable = remember { VoiceWidgetProvider.canRequestPin(this) }
                val steps = stringResource(R.string.settings_widget_steps)
                MessageDialog(
                    title = stringResource(R.string.settings_widget_steps_title),
                    message = if (pinnable) stringResource(R.string.settings_widget_pin_note) + PARAGRAPH + steps else steps,
                    onDismissRequest = close,
                    onConfirm = { close(); if (pinnable) VoiceWidgetProvider.requestPin(this) },
                    confirmText = stringResource(if (pinnable) R.string.settings_widget_place else R.string.ui_dialog_confirm),
                    dismissText = if (pinnable) stringResource(R.string.ui_dialog_cancel) else null,
                    tag = "macaw.dialog.widgetSteps",
                )
            }
            Dialog.KEY_SERVICE -> MessageDialog(
                title = stringResource(R.string.settings_key),
                message = stringResource(R.string.settings_key_message),
                onDismissRequest = close,
                onConfirm = {
                    close()
                    if (!KeyTriggerService.openSettings(this)) pendingMessage = getString(R.string.settings_key_unavailable)
                },
                confirmText = stringResource(R.string.settings_key_open),
                tag = "macaw.dialog.keyService",
            )
            Dialog.KEY_GESTURE -> {
                val triggers = TriggerPreferences(this)
                ChoiceDialog(
                    title = stringResource(R.string.settings_key_gesture),
                    options = KeyGesture.entries.map { Choice(stringResource(gestureLabel(it)), stringResource(if (it.double) R.string.key_gesture_double_note else R.string.key_gesture_long_note)) },
                    selectedIndex = triggers.keyGesture.ordinal,
                    onDismissRequest = close,
                    onConfirm = { index -> close(); triggers.keyGesture = KeyGesture.entries[index]; saved() },
                    tag = "macaw.dialog.keyGesture",
                )
            }
            Dialog.RESIDENT_KEYWORDS -> KeywordsDialog()
            Dialog.RESIDENT_SENSITIVITY -> {
                val current = ResidentPreferences(this).read()
                ChoiceDialog(
                    title = stringResource(R.string.settings_resident_sensitivity),
                    options = WakeSensitivity.entries.map { Choice(stringResource(sensitivityLabel(it))) },
                    selectedIndex = current.sensitivity.ordinal,
                    onDismissRequest = close,
                    onConfirm = { index -> close(); saveResident(restart = true) { it.copy(sensitivity = WakeSensitivity.entries[index]) } },
                    tag = "macaw.dialog.residentSensitivity",
                )
            }
            Dialog.RESIDENT_DURATION -> {
                val current = ResidentPreferences(this).read()
                ChoiceDialog(
                    title = stringResource(R.string.settings_resident_duration),
                    options = ListeningDuration.entries.map { Choice(stringResource(durationLabel(it))) },
                    selectedIndex = current.duration.ordinal,
                    onDismissRequest = close,
                    onConfirm = { index ->
                        close()
                        // Roadmap D77: an enabled switch counts the new duration from now, a disabled one only remembers it.
                        val enabled = ResidentWake.enabled(this)
                        saveResident(restart = false) { it.withDuration(ListeningDuration.entries[index], enabled, System.currentTimeMillis()) }
                    },
                    tag = "macaw.dialog.residentDuration",
                )
            }
            Dialog.RESIDENT_CONFLICT -> {
                // Roadmap D75 (3-Stone AI D31): only one resident listener; the other app's settings turn it off there.
                val rival = residentRival
                if (rival == null) {
                    close()
                } else {
                    MessageDialog(
                        title = stringResource(R.string.resident_conflict_title),
                        message = stringResource(R.string.resident_conflict_message, rival.label),
                        onDismissRequest = close,
                        onConfirm = { close(); ResidentWake.openSettings(this, rival) },
                        confirmText = stringResource(R.string.resident_conflict_open, rival.label),
                        tag = "macaw.dialog.residentConflict",
                    )
                }
            }
            Dialog.RESIDENT_OVERLAY -> MessageDialog(
                // Roadmap D71 / D78: asked when turning on; whatever the answer, the switch turns on.
                title = stringResource(R.string.resident_overlay_title),
                message = stringResource(R.string.resident_overlay_message),
                onDismissRequest = { close(); turnResidentOn() },
                onConfirm = { close(); openResidentOverlay() },
                confirmText = stringResource(R.string.resident_overlay_grant),
                dismissText = stringResource(R.string.resident_overlay_skip),
                tag = "macaw.dialog.residentOverlay",
            )
            Dialog.IGNORED -> {
                val store = AppUpdateStore.of(this)
                val tags = store.ignoredTags.sorted()
                MultiChoiceDialog(
                    title = stringResource(R.string.update_ignored),
                    message = stringResource(R.string.update_ignored_message),
                    options = tags.map { Choice(it) },
                    selected = emptySet(),
                    onDismissRequest = close,
                    onConfirm = { chosen -> close(); store.stopIgnoring(chosen.map(tags::get)); revision++ },
                    confirmText = stringResource(R.string.update_stop_ignoring),
                    requireOne = true,
                    tag = "macaw.dialog.ignored",
                )
            }
        }
    }

    @Composable
    private fun ModelChoice(@StringRes title: Int, models: List<ModelEntry>, kind: String, tag: String) {
        ChoiceDialog(
            title = stringResource(title),
            options = models.map { Choice(it.name, HomeTexts.languages(this, it.languages)) },
            selectedIndex = models.indexOfFirst { it.isDefault }.coerceAtLeast(0),
            onDismissRequest = { dialog = null },
            onConfirm = { index ->
                dialog = null
                val chosen = models.getOrNull(index) ?: return@ChoiceDialog
                val app = applicationContext
                io.execute {
                    val ok = runCatching { SpeechModels.get(app).setDefault(chosen.id, kind) }.isSuccess
                    main.post {
                        pendingMessage = getString(if (ok) R.string.settings_saved else R.string.settings_save_failed)
                        loadSpeech()
                    }
                }
            },
            tag = tag,
        )
    }

    @Composable
    private fun SpeedDialog(onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
        var draft by rememberSaveable { mutableFloatStateOf(preferences.speed) }
        MacawDialog(
            title = stringResource(R.string.settings_speed),
            onDismissRequest = onDismiss,
            confirmText = stringResource(R.string.ui_dialog_confirm),
            onConfirm = { onConfirm(SpeechPreferencePolicy.speed(draft)) },
            dismissText = stringResource(R.string.ui_dialog_cancel),
            tag = "macaw.dialog.speed",
        ) {
            Column(modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding)) {
                Text(stringResource(R.string.settings_speed_value, SpeechPreferencePolicy.speed(draft)), style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = draft,
                    onValueChange = { draft = SpeechPreferencePolicy.speed(it) },
                    valueRange = SpeechPreferencePolicy.MIN_SPEED..SpeechPreferencePolicy.MAX_SPEED,
                    steps = ((SpeechPreferencePolicy.MAX_SPEED - SpeechPreferencePolicy.MIN_SPEED) / SpeechPreferencePolicy.SPEED_STEP).toInt() - 1,
                    modifier = Modifier.fillMaxWidth().testTag("macaw.dialog.speed.slider"),
                )
                Text(
                    stringResource(R.string.settings_speed_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }

    /**
     * Roadmap D70: the wake words, one per line; a list that breaks the rules cannot be saved, and a word the wake word
     * model cannot spell (D18) is named before anything is saved.
     * zh-CN: 路线图 D70: 唤醒词, 每行一个; 不合规则的列表不能保存, 唤醒词模型无法拼写的词 (D18) 在保存前指出.
     */
    @Composable
    private fun KeywordsDialog() {
        val current = remember { ResidentPreferences(this).read().keywords }
        var text by rememberSaveable { mutableStateOf(current.joinToString(System.lineSeparator())) }
        var problem by remember { mutableStateOf<String?>(null) }
        var checking by remember { mutableStateOf(false) }
        val parsed = ResidentKeywords.parse(text)
        val invalid = stringResource(R.string.settings_resident_keywords_invalid)
        MacawDialog(
            title = stringResource(R.string.settings_resident_keywords),
            onDismissRequest = { dialog = null },
            confirmText = stringResource(R.string.ui_dialog_confirm),
            onConfirm = {
                val keywords = parsed ?: return@MacawDialog
                checking = true
                val app = applicationContext
                io.execute {
                    val unspellable = ResidentWake.unspellable(app, keywords)
                    main.post {
                        if (isDestroyed) return@post
                        checking = false
                        if (unspellable != null) {
                            problem = getString(R.string.settings_resident_keywords_unspellable, unspellable)
                        } else {
                            dialog = null
                            saveResident(restart = true) { it.copy(keywords = keywords) }
                        }
                    }
                }
            },
            dismissText = stringResource(R.string.ui_dialog_cancel),
            confirmEnabled = parsed != null && !checking,
            tag = "macaw.dialog.residentKeywords",
        ) {
            Column(modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding)) {
                Text(
                    stringResource(R.string.settings_resident_keywords_note, ResidentKeywords.MAX_KEYWORDS, ResidentKeywords.MAX_CHARACTERS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val error = problem ?: invalid.takeIf { parsed == null }
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input.take(KEYWORDS_INPUT_LIMIT)
                        problem = null
                    },
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp).testTag("macaw.dialog.residentKeywords.input"),
                )
            }
        }
    }

    // Actions -------------------------------------------------------------------------------------------------------

    private fun saveAppearance(next: AppearancePreferences) {
        if (next == AppearancePreferences.read(this)) return
        if (next.save(this)) {
            revision++
            applyAppearanceChange()
        } else {
            pendingMessage = getString(R.string.settings_save_failed)
        }
    }

    private fun saved() {
        revision++
        pendingMessage = getString(R.string.settings_saved)
    }

    private fun refreshCatalog() {
        busy = TrackedOperation.Kind.REFRESH
        runCatching { OperationTracker.shared.track(TrackedOperation.Kind.REFRESH, null) { listener -> ManagerBackend(this).refreshCatalog(listener) } }
            .onFailure { busy = null; pendingMessage = getString(R.string.settings_catalog_failed) }
    }

    private fun recycle() {
        busy = TrackedOperation.Kind.RECYCLE
        runCatching { OperationTracker.shared.track(TrackedOperation.Kind.RECYCLE, null) { listener -> ManagerBackend(this).recycle(listener) } }
            .onFailure { busy = null; pendingMessage = getString(R.string.settings_recycle_failed) }
    }

    private fun open(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    /**
     * Roadmap D61: Android 13+ asks the system to add the tile, older versions show the manual steps; nothing pops up
     * unasked.
     * zh-CN: 路线图 D61: Android 13+ 请系统添加磁贴, 更早的版本显示手动步骤; 不主动弹窗.
     */
    private fun addTile() {
        if (TriggerPolicy.tileOutcome(Build.VERSION.SDK_INT) != TileOutcome.REQUEST || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            dialog = Dialog.TILE_STEPS
            return
        }
        VoiceTileService.requestAdd(this) { code ->
            Log.i(TAG, "Tile request result $code")
            when (TriggerPolicy.tileOutcome(Build.VERSION.SDK_INT, code)) {
                TileOutcome.ADDED -> pendingMessage = getString(R.string.settings_tile_added)
                TileOutcome.ALREADY_ADDED -> pendingMessage = getString(R.string.settings_tile_present)
                TileOutcome.MANUAL -> dialog = Dialog.TILE_STEPS
                TileOutcome.REQUEST, TileOutcome.DECLINED -> Unit
            }
        }
    }

    // Summaries and labels --------------------------------------------------------------------------------------

    private fun speechCatalogVersion(): Int = runCatching { SpeechModels.get(this).catalogVersion() }.getOrDefault(0)

    @Composable
    private fun defaultName(models: List<ModelEntry>): String =
        models.firstOrNull { it.isDefault }?.name ?: stringResource(R.string.settings_no_model)

    @Composable
    private fun speakerSummary(): String {
        val model = speech.tts.firstOrNull { it.isDefault } ?: return stringResource(R.string.settings_no_model)
        val chosen = preferences.speaker(model.id)?.takeIf { it < speech.speakers.size }
        return if (chosen == null) stringResource(R.string.settings_speaker_model_default) else speakerLabel(chosen, speech.speakers[chosen])
    }

    @Composable
    private fun speakerLabel(index: Int, name: String): String =
        if (name == "#$index") stringResource(R.string.settings_speaker_number, index) else stringResource(R.string.settings_speaker_named, index, name)

    @Composable
    private fun voicesSummary(): String {
        val chosen = preferences.voices()
        if (speech.voices?.isEmpty() != false) return stringResource(R.string.settings_no_model)
        return if (chosen.isEmpty()) stringResource(R.string.settings_voice_automatic) else chosen.keys.sorted().joinToString(", ") { code ->
            Locale.forLanguageTag(code).getDisplayName(resources.configuration.locales[0]) + ": " + voiceLabel(chosen.getValue(code))
        }
    }

    /** (code, display name) of every language the installed voices speak. zh-CN: 已安装语音所说的每种语言. */
    private fun voiceLanguages(): List<Pair<String, String>> {
        val locale = resources.configuration.locales[0]
        return speech.voices?.voices.orEmpty().map { it.languageTag.substringBefore('-') }.distinct().sorted()
            .map { it to Locale.forLanguageTag(it).getDisplayName(locale) }
    }

    private fun voicesFor(language: String): List<TtsVoice> {
        val code = TtsVoiceTable.iso3Language(language)
        return speech.voices?.voices.orEmpty().filter { it.iso3Language == code }.sortedWith(compareByDescending<TtsVoice> { it.primary }.thenBy { it.modelId }.thenBy { it.speakerId })
    }

    private fun voiceLabel(name: String): String {
        val voice = speech.voices?.voice(name) ?: return name
        val model = speech.modelNames[voice.modelId] ?: voice.modelId
        return getString(R.string.settings_voice_label, model, voice.speakerId)
    }

    @Composable
    private fun followNote(isFollow: Boolean): String? =
        if (isFollow && HostAppearanceReader.cached() == null) stringResource(R.string.settings_follow_autojs6_unavailable) else null

    @Composable
    private fun languageSummary(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.settings_follow_autojs6_summary, languageLabel(appearance.language))
        else -> languageLabel(value)
    }

    @Composable
    private fun darkModeSummary(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(
            R.string.settings_follow_autojs6_summary,
            stringResource(if (appearance.dark) R.string.settings_dark_mode_dark else R.string.settings_dark_mode_light),
        )
        else -> darkModeLabel(value)
    }

    @Composable
    private fun colorSummary(color: Int?): String =
        if (color == null) stringResource(R.string.settings_follow_autojs6_summary, ThemeColorValue.hex(appearance.primary)) else ThemeColorValue.hex(color)

    @Composable
    private fun languageLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_HOST -> stringResource(R.string.settings_follow_autojs6)
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.settings_follow_system)
        "zh-Hans" -> stringResource(R.string.settings_language_zh_hans)
        "zh-Hant-HK" -> stringResource(R.string.settings_language_zh_hant_hk)
        "zh-Hant-TW" -> stringResource(R.string.settings_language_zh_hant_tw)
        "en" -> stringResource(R.string.settings_language_en)
        "fr" -> stringResource(R.string.settings_language_fr)
        "es" -> stringResource(R.string.settings_language_es)
        "ja" -> stringResource(R.string.settings_language_ja)
        "ko" -> stringResource(R.string.settings_language_ko)
        "ru" -> stringResource(R.string.settings_language_ru)
        "ar" -> stringResource(R.string.settings_language_ar)
        else -> Locale.forLanguageTag(value).getDisplayName(Locale.forLanguageTag(value)).ifBlank { value }
    }

    @Composable
    private fun darkModeLabel(value: String): String = when (value) {
        AppearancePreferences.FOLLOW_SYSTEM -> stringResource(R.string.settings_follow_system)
        AppearancePreferences.LIGHT -> stringResource(R.string.settings_dark_mode_light)
        AppearancePreferences.DARK -> stringResource(R.string.settings_dark_mode_dark)
        else -> stringResource(R.string.settings_follow_autojs6)
    }

    @StringRes
    private fun endpointLabel(millis: Long): Int = when (SpeechPreferencePolicy.ENDPOINT_MILLIS.indexOf(millis)) {
        0 -> R.string.settings_endpoint_sensitive
        2 -> R.string.settings_endpoint_relaxed
        else -> R.string.settings_endpoint_standard
    }

    /**
     * Roadmap D66 / D75 / D77: what the switch does, then the app that listens instead or when the listening time ends.
     * zh-CN: 路线图 D66 / D75 / D77: 开关的作用, 其后是正在监听的其他应用或监听时长的截止时刻.
     */
    @Composable
    private fun residentSummary(settings: ResidentSettings): String {
        val rival = resident.others.firstOrNull()
        val deadline = settings.deadline
        val extra = when {
            !residentOn && rival != null -> stringResource(R.string.settings_resident_conflict, rival.label)
            residentOn && deadline != null -> ResidentNotifications.until(this, deadline)
            else -> null
        }
        val summary = stringResource(R.string.settings_resident_summary)
        return if (extra == null) summary else summary + System.lineSeparator() + extra
    }

    @StringRes
    private fun sensitivityLabel(sensitivity: WakeSensitivity): Int = when (sensitivity) {
        WakeSensitivity.HIGH -> R.string.resident_sensitivity_high
        WakeSensitivity.MEDIUM -> R.string.resident_sensitivity_medium
        WakeSensitivity.LOW -> R.string.resident_sensitivity_low
    }

    @StringRes
    private fun durationLabel(duration: ListeningDuration): Int = when (duration) {
        ListeningDuration.MINUTES_10 -> R.string.resident_duration_10
        ListeningDuration.MINUTES_30 -> R.string.resident_duration_30
        ListeningDuration.MINUTES_60 -> R.string.resident_duration_60
        ListeningDuration.ALWAYS -> R.string.resident_duration_always
    }

    @StringRes
    private fun gestureLabel(gesture: KeyGesture): Int = when (gesture) {
        KeyGesture.LONG_VOLUME_DOWN -> R.string.key_gesture_long_volume_down
        KeyGesture.LONG_VOLUME_UP -> R.string.key_gesture_long_volume_up
        KeyGesture.DOUBLE_VOLUME_DOWN -> R.string.key_gesture_double_volume_down
    }

    @StringRes
    private fun iconLabel(mode: LauncherIconMode): Int = when (mode) {
        LauncherIconMode.LIGHT -> R.string.launcher_icon_light
        LauncherIconMode.DARK -> R.string.launcher_icon_dark
        LauncherIconMode.AUTO -> R.string.launcher_icon_auto
        LauncherIconMode.TRANSPARENT -> R.string.launcher_icon_transparent
    }

    private companion object {
        const val TAG = "MacawSettings"

        /** The settings entry of the assistant and the recognition service (Manifest alias). zh-CN: 助理与识别服务的设置入口 (Manifest 别名). */
        const val RECOGNITION_ALIAS = ".ui.settings.RecognitionSettingsAlias"

        /**
         * The settings entry of the tile (long press) and the key service (roadmap P7.1, Manifest alias).
         * zh-CN: 磁贴 (长按) 与按键服务的设置入口 (路线图 P7.1, Manifest 别名).
         */
        const val TRIGGER_ALIAS = ".ui.settings.TriggerSettingsAlias"

        /** A blank line between two paragraphs of a dialog message. zh-CN: 对话框消息两段之间的空行. */
        val PARAGRAPH: String = System.lineSeparator().repeat(2)

        /** Enough for five wake words of 32 characters. zh-CN: 足够容纳 5 个 32 字符的唤醒词. */
        const val KEYWORDS_INPUT_LIMIT = 400

        val io = Executors.newSingleThreadExecutor { Thread(it, "MacawSettings").apply { isDaemon = true } }
        val main = Handler(Looper.getMainLooper())
    }
}
