package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
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
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.MacawVoiceInteractionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferencePolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
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
 * system settings to choose it (the plugin cannot choose it itself, D48).
 * zh-CN: 设置页 (路线图 P5.2, 独立设置规范): 外观 (语言, 夜间模式, 主题色, 启动器图标; 前三项默认跟随 AutoJs6), 识别与合成默认值 (按维护者 2026-10-16
 * 的决定只作用于试听试说页与系统 TTS 引擎), 资源与关于. 每个选择对话框编辑草稿, 确定时保存一次. 它也是系统 TTS 引擎的 `settingsActivity`
 * (`res/xml/tts_engine.xml`): 系统设置以 `ACTION_MAIN` 启动它, 此时定位到合成分组. 经 `RecognitionSettingsAlias` 它也是数字助理与识别服务的设置入口
 * (路线图 P5.7), 此时定位到识别分组; 该组的 "数字助理" 行显示 3-Macaw Speech 是否为默认助理, 并打开系统设置供用户选择 (插件无法自行选择, D48).
 */
class SettingsActivity : StandaloneActivity() {

    private enum class Dialog {
        LANGUAGE, DARK_MODE, THEME_COLOR, LAUNCHER_ICON, ASR_MODEL, ENDPOINT, TTS_MODEL, SPEAKER, SPEED, VOICE_LANGUAGE, VOICE,
        IDLE, RECYCLE, IGNORED,
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
        // The recognition entry of the assistant and the recognition service first, then the TTS engine's (ACTION_MAIN).
        val fromRecognition = intent?.component?.className?.endsWith(RECOGNITION_ALIAS) == true
        val fromSystem = !fromRecognition && intent?.action == Intent.ACTION_MAIN
        val scroll = rememberScrollState()
        var synthesisTop by remember { mutableIntStateOf(-1) }
        var recognitionTop by remember { mutableIntStateOf(-1) }
        var scrolled by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(synthesisTop, recognitionTop) {
            val target = if (fromRecognition) recognitionTop else if (fromSystem) synthesisTop else -1
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

        val io = Executors.newSingleThreadExecutor { Thread(it, "MacawSettings").apply { isDaemon = true } }
        val main = Handler(Looper.getMainLooper())
    }
}
