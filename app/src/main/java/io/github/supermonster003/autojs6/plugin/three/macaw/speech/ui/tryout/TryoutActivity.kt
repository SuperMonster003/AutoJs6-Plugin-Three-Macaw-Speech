package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.TranscriptExport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferencePolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.LocalMacawPalette
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.EmptyAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawScreen
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeTexts
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.util.Locale

/**
 * Tryout (roadmap D7, P5.3): live dictation (partials in gray, finals as body text) with model and language choice, a
 * wake word trial (keywords, hits and scores), synthesis (text, model, speaker, speed; play and save to a file) and,
 * since P7.8, the recognition of a picked audio file ([FileTab]). Leaving the screen releases the microphone
 * (`onStop`); a file run goes on in [FileTranscription].
 * zh-CN: 试听试说 (路线图 D7, P5.3): 实时听写 (中间结果为灰字, 整句为正文), 可切换模型与语言; 唤醒词试验 (关键词, 命中与分数); 合成 (文本, 模型, 说话人,
 * 语速; 播放与保存到文件); P7.8 起还有所选音频文件的识别 ([FileTab]). 离开界面即释放麦克风 (`onStop`); 文件识别在 [FileTranscription] 中继续.
 */
class TryoutActivity : StandaloneActivity() {

    private lateinit var tryout: TryoutController
    private lateinit var transcription: FileTranscription
    private var microphoneGranted by mutableStateOf(false)
    private var saveText: String? = null
    private val snackbar = SnackbarHostState()
    private var message by mutableStateOf<Pair<Int, String>?>(null)
    private var messages = 0
    private var tab by mutableIntStateOf(0)

    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> microphoneGranted = granted }

    private val saveDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        val text = saveText
        if (uri != null && text != null) tryout.save(text, uri) { ok -> show(getString(if (ok) R.string.tryout_saved else R.string.tryout_save_failed)) }
    }

    // File (roadmap D110, D111, D113) ----------------------------------------------------------------------------

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) startFile(uri) }

    private val saveTextDocument = registerForActivityResult(ActivityResultContracts.CreateDocument(TranscriptExport.TEXT_MIME)) { uri -> export(uri, TranscriptExport::text) }

    private val saveSrtDocument = registerForActivityResult(ActivityResultContracts.CreateDocument(TranscriptExport.SRT_MIME)) { uri -> export(uri, TranscriptExport::srt) }

    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        tryout = TryoutController(this)
        transcription = FileTranscription.get(this)
        tab = (savedInstanceState?.getInt(STATE_TAB) ?: intent.getIntExtra(EXTRA_TAB, 0)).coerceIn(0, TAB_FILE)
        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra(EXTRA_TAB)) selectTab(intent.getIntExtra(EXTRA_TAB, 0).coerceIn(0, TAB_FILE))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB, tab)
    }

    override fun onStart() {
        super.onStart()
        transcription.screenStarted()
        transcription.load()
    }

    override fun onResume() {
        super.onResume()
        microphoneGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        tryout.load()
    }

    override fun onStop() {
        tryout.release()
        transcription.screenStopped()
        super.onStop()
    }

    private fun selectTab(index: Int) {
        if (tab == index) return
        // Switching away from a microphone part ends its session. zh-CN: 离开麦克风部分即结束其会话.
        tryout.stopDictation()
        tryout.stopWake()
        tab = index
    }

    /** A recording or a synthesis is not interrupted by an appearance change. zh-CN: 录音或合成不因外观变化而中断. */
    override fun holdsAppearance(): Boolean = tryout.busy

    private fun show(text: String) {
        message = ++messages to text
    }

    @Composable
    override fun Content() {
        val problem = tryout.problem
        LaunchedEffect(problem?.serial) { problem?.let { show(getString(problemText(it.code))) } }
        LaunchedEffect(message?.first) { message?.second?.let { snackbar.showSnackbar(it) } }
        MacawScreen(title = stringResource(R.string.tryout_title), onBack = ::finish, snackbarHostState = snackbar) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Scrollable: four fixed tabs break "Synthesis" in the middle of the word on a 360 dp screen.
                // zh-CN: 可滚动: 四个固定标签在 360 dp 宽的屏幕上会把 "Synthesis" 从词中间断行.
                SecondaryScrollableTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background, edgePadding = 0.dp) {
                    listOf(R.string.tryout_tab_dictation, R.string.tryout_tab_wake, R.string.tryout_tab_synthesis, R.string.tryout_tab_file).forEachIndexed { index, title ->
                        Tab(selected = tab == index, onClick = { selectTab(index) }, text = { Text(stringResource(title)) }, modifier = Modifier.testTag("macaw.tryout.tab.$index"))
                    }
                }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                    when (tab) {
                        0 -> Dictation()
                        1 -> Wake()
                        2 -> Synthesis()
                        else -> Transcribe()
                    }
                }
            }
        }
    }

    @Composable
    private fun NeedsMicrophone(content: @Composable ColumnScope.() -> Unit) {
        if (!microphoneGranted) {
            EmptyState(
                title = stringResource(R.string.tryout_microphone_title),
                body = stringResource(R.string.tryout_microphone_body),
                icon = R.drawable.ic_mic_24,
                primary = EmptyAction(stringResource(R.string.home_status_grant), { microphone.launch(Manifest.permission.RECORD_AUDIO) }, "macaw.tryout.grant"),
            )
        } else {
            Column(content = content)
        }
    }

    @Composable
    private fun MissingModel(kind: String) {
        EmptyState(
            title = stringResource(R.string.tryout_no_model_title, stringResource(HomeTexts.kindTitle(kind))),
            body = stringResource(R.string.tryout_no_model_body),
            icon = R.drawable.ic_model_24,
            primary = EmptyAction(stringResource(R.string.tryout_open_models), { startActivity(HomeActivity.intent(this, showTts = kind == "tts")) }, "macaw.tryout.models"),
        )
    }

    // Dictation -------------------------------------------------------------------------------------------------

    @Composable
    private fun Dictation() = NeedsMicrophone {
        if (tryout.asrModels.isEmpty()) {
            MissingModel("asr")
            return@NeedsMicrophone
        }
        var choosing by remember { mutableStateOf<String?>(null) }
        val idle = tryout.dictationPhase == TryoutPhase.IDLE
        val model = tryout.asrModels.firstOrNull { it.id == tryout.asrModel }
        SettingsRow(stringResource(R.string.tryout_model), summary = model?.name, icon = R.drawable.ic_model_24, enabled = idle,
            onClick = { choosing = "model" }, modifier = Modifier.testTag("macaw.tryout.asrModel"))
        if (tryout.takesLanguage(tryout.asrModel)) {
            SettingsRow(stringResource(R.string.tryout_language), summary = languageLabel(tryout.language), icon = R.drawable.ic_language_24, enabled = idle,
                onClick = { choosing = "language" }, modifier = Modifier.testTag("macaw.tryout.language"))
        }
        Text(
            stringResource(R.string.tryout_dictation_settings, stringResource(endpointLabel(tryout.endpointMillis)), stringResource(if (tryout.vad) R.string.tryout_vad_on else R.string.tryout_vad_off)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        StartStop(
            running = !idle,
            phase = tryout.dictationPhase,
            startLabel = stringResource(R.string.tryout_start_listening),
            onStart = tryout::startDictation,
            onStop = tryout::stopDictation,
            tag = "macaw.tryout.listen",
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 160.dp).testTag("macaw.tryout.transcript"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                if (tryout.transcript.isEmpty() && tryout.partial.isEmpty()) {
                    Text(stringResource(if (idle) R.string.tryout_transcript_empty else R.string.tryout_transcript_speak), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                tryout.transcript.forEachIndexed { index, line ->
                    Text(line.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 6.dp).testTag("macaw.tryout.final.$index"))
                }
                if (tryout.partial.isNotEmpty()) {
                    Text(tryout.partial, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.testTag("macaw.tryout.partial"))
                }
            }
        }
        if (tryout.voiceActive) {
            Text(stringResource(R.string.tryout_voice_active), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }
        when (choosing) {
            "model" -> ChoiceDialog(
                title = stringResource(R.string.tryout_model),
                options = tryout.asrModels.map { Choice(it.name, HomeTexts.languages(this@TryoutActivity, it.languages)) },
                selectedIndex = tryout.asrModels.indexOfFirst { it.id == tryout.asrModel }.coerceAtLeast(0),
                onDismissRequest = { choosing = null },
                onConfirm = { index -> choosing = null; tryout.asrModel = tryout.asrModels[index].id },
                tag = "macaw.tryout.asrModelDialog",
            )
            "language" -> {
                val codes = listOf(null) + (model?.languages ?: emptyList())
                ChoiceDialog(
                    title = stringResource(R.string.tryout_language),
                    options = codes.map { Choice(languageLabel(it)) },
                    selectedIndex = codes.indexOf(tryout.language).coerceAtLeast(0),
                    onDismissRequest = { choosing = null },
                    onConfirm = { index -> choosing = null; tryout.language = codes[index] },
                    tag = "macaw.tryout.languageDialog",
                )
            }
        }
    }

    // Wake word -------------------------------------------------------------------------------------------------

    @Composable
    private fun Wake() = NeedsMicrophone {
        if (tryout.kwsModels.isEmpty()) {
            MissingModel("kws")
            return@NeedsMicrophone
        }
        val defaults = stringResource(R.string.tryout_wake_default_keywords)
        var keywords by rememberSaveable { mutableStateOf(defaults) }
        val idle = tryout.wakePhase == TryoutPhase.IDLE
        val list = keywords.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(50)
        OutlinedTextField(
            value = keywords,
            onValueChange = { keywords = it.take(1000) },
            label = { Text(stringResource(R.string.tryout_keywords)) },
            supportingText = { Text(stringResource(R.string.tryout_keywords_hint)) },
            enabled = idle,
            minLines = 2,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).testTag("macaw.tryout.keywords"),
        )
        val focus = LocalFocusManager.current
        StartStop(
            running = !idle,
            phase = tryout.wakePhase,
            startLabel = stringResource(R.string.tryout_start_wake),
            startEnabled = list.isNotEmpty(),
            onStart = {
                // The keyboard would cover the hits. zh-CN: 键盘会遮住命中列表.
                focus.clearFocus()
                tryout.startWake(list)
            },
            onStop = tryout::stopWake,
            tag = "macaw.tryout.wake",
        )
        Text(
            stringResource(R.string.tryout_wake_score_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 120.dp).testTag("macaw.tryout.hits"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                if (tryout.hits.isEmpty()) {
                    Text(stringResource(if (idle) R.string.tryout_hits_empty else R.string.tryout_hits_waiting), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                tryout.hits.forEachIndexed { index, hit ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("macaw.tryout.hit.$index"), verticalAlignment = Alignment.CenterVertically) {
                        Text(hit.keyword, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(
                            stringResource(R.string.tryout_hit_value, hit.score, hit.atMillis / 1000.0),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // Synthesis -------------------------------------------------------------------------------------------------

    @Composable
    private fun Synthesis() {
        if (tryout.ttsModels.isEmpty()) {
            MissingModel("tts")
            return
        }
        val focus = LocalFocusManager.current
        val sample = stringResource(R.string.tryout_tts_sample)
        var text by rememberSaveable { mutableStateOf(sample) }
        var choosing by remember { mutableStateOf<String?>(null) }
        val idle = tryout.synthesisPhase == TryoutPhase.IDLE
        val model = tryout.ttsModels.firstOrNull { it.id == tryout.ttsModel }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(MAX_TEXT) },
            label = { Text(stringResource(R.string.tryout_text)) },
            supportingText = { Text(stringResource(R.string.tryout_text_count, text.length, MAX_TEXT)) },
            enabled = idle,
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).testTag("macaw.tryout.text"),
        )
        SettingsRow(stringResource(R.string.tryout_model), summary = model?.name, icon = R.drawable.ic_model_24, enabled = idle,
            onClick = { choosing = "model" }, modifier = Modifier.testTag("macaw.tryout.ttsModel"))
        SettingsRow(stringResource(R.string.settings_speaker), summary = speakerLabel(tryout.speaker), icon = R.drawable.ic_person_24,
            enabled = idle && tryout.speakers.size > 1, onClick = { choosing = "speaker" }, modifier = Modifier.testTag("macaw.tryout.speaker"))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.settings_speed_value, tryout.speed), style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = tryout.speed,
                onValueChange = { tryout.speed = SpeechPreferencePolicy.speed(it) },
                enabled = idle,
                valueRange = SpeechPreferencePolicy.MIN_SPEED..SpeechPreferencePolicy.MAX_SPEED,
                steps = ((SpeechPreferencePolicy.MAX_SPEED - SpeechPreferencePolicy.MIN_SPEED) / SpeechPreferencePolicy.SPEED_STEP).toInt() - 1,
                modifier = Modifier.fillMaxWidth().testTag("macaw.tryout.speed"),
            )
        }
        if (!idle) {
            LinearProgressIndicator(progress = { tryout.synthesisProgress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).testTag("macaw.tryout.synthesisProgress"))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (idle && !tryout.playing) {
                Button(onClick = { focus.clearFocus(); tryout.play(text) }, enabled = text.isNotBlank(), modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.play")) {
                    Icon(painterResource(R.drawable.ic_play_arrow_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tryout_play))
                }
            } else {
                Button(onClick = tryout::stopSynthesis, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.stop")) {
                    Icon(painterResource(R.drawable.ic_stop_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tryout_stop))
                }
            }
            OutlinedButton(
                onClick = {
                    focus.clearFocus()
                    saveText = text
                    runCatching { saveDocument.launch(getString(R.string.tryout_file_name)) }
                },
                enabled = idle && text.isNotBlank(),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.save"),
            ) {
                Icon(painterResource(R.drawable.ic_save_24), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tryout_save))
            }
        }
        when (choosing) {
            "model" -> ChoiceDialog(
                title = stringResource(R.string.tryout_model),
                options = tryout.ttsModels.map { Choice(it.name, HomeTexts.languages(this@TryoutActivity, it.languages)) },
                selectedIndex = tryout.ttsModels.indexOfFirst { it.id == tryout.ttsModel }.coerceAtLeast(0),
                onDismissRequest = { choosing = null },
                onConfirm = { index -> choosing = null; tryout.selectTtsModel(tryout.ttsModels[index].id) },
                tag = "macaw.tryout.ttsModelDialog",
            )
            "speaker" -> ChoiceDialog(
                title = stringResource(R.string.settings_speaker),
                options = listOf(Choice(stringResource(R.string.settings_speaker_model_default))) + tryout.speakers.indices.map { Choice(speakerLabel(it)) },
                selectedIndex = tryout.speaker?.let { it + 1 } ?: 0,
                onDismissRequest = { choosing = null },
                onConfirm = { index -> choosing = null; tryout.speaker = if (index == 0) null else index - 1 },
                tag = "macaw.tryout.speakerDialog",
            )
        }
    }

    // File ------------------------------------------------------------------------------------------------------

    /**
     * No microphone needed (D110); the missing model card only while no model of any file action is installed and
     * there is nothing to show (D153).
     * zh-CN: 不需要麦克风 (D110); 只有任何文件操作的模型都未安装且没有内容可显示时才显示缺少模型的卡片 (D153).
     */
    @Composable
    private fun Transcribe() {
        if (tryout.asrModels.isEmpty() && tryout.analysisModels.isEmpty() && transcription.run == null && transcription.record == null) {
            MissingModel("asr")
            return
        }
        FileTab(
            controller = tryout,
            transcription = transcription,
            languageLabel = { languageLabel(it) },
            onPick = { runCatching { pickAudio.launch(AUDIO_TYPES) } },
            onCopy = { copy(FileTexts.copyText(this, it)) },
            onSaveText = { saveExport(TranscriptExport.TEXT_EXTENSION, saveTextDocument::launch) },
            onSaveSrt = { saveExport(TranscriptExport.SRT_EXTENSION, saveSrtDocument::launch) },
            onOpenModels = { startActivity(HomeActivity.intent(this)) },
        )
    }

    private fun startFile(uri: Uri) {
        val action = tryout.fileAction
        if (tryout.missingKinds(action).isNotEmpty()) return
        val model = tryout.fileModel?.takeIf { action.transcribes }
        val options = FileRunOptions(
            model, tryout.fileLanguage.takeIf { model != null && tryout.takesLanguage(model) }, tryout.filePunctuation, tryout.vad,
            action = action, speakers = tryout.fileSpeakers, identify = tryout.fileIdentify, languageModel = tryout.languageModel,
        )
        if (transcription.start(FileSource.of(this, uri), options)) askNotificationsOnce()
    }

    /** D111: once on Android 13+, shared with the first download; a denial changes nothing. zh-CN: D111: Android 13+ 询问一次, 与首次下载共用记录. */
    private fun askNotificationsOnce() {
        val preferences = SpeechPreferences(this)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || preferences.notificationsAsked) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        preferences.notificationsAsked = true
        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun saveExport(extension: String, launch: (String) -> Unit) {
        val record = transcription.record ?: return
        runCatching { launch(TranscriptExport.fileName(record.displayName, extension)) }
    }

    private fun export(uri: Uri?, bytes: (List<Utterance>) -> ByteArray) {
        val record = transcription.record
        if (uri == null || record == null) return
        tryout.writeDocument(uri, bytes(FileTexts.segments(this, record))) { ok -> show(getString(if (ok) R.string.tryout_transcribe_saved else R.string.tryout_transcribe_save_failed)) }
    }

    private fun copy(text: String) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
        // Android 13+ confirms a copy itself. zh-CN: Android 13+ 自行提示复制.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) show(getString(R.string.assistant_copied))
    }

    // Shared ----------------------------------------------------------------------------------------------------

    @Composable
    private fun StartStop(running: Boolean, phase: TryoutPhase, startLabel: String, onStart: () -> Unit, onStop: () -> Unit, tag: String, startEnabled: Boolean = true) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (running) {
                Button(onClick = onStop, enabled = phase != TryoutPhase.STOPPING, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("$tag.stop")) {
                    Icon(painterResource(R.drawable.ic_stop_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tryout_stop))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    stringResource(if (phase == TryoutPhase.LOADING) R.string.tryout_loading else R.string.tryout_listening),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(LocalMacawPalette.current.success),
                    modifier = Modifier.testTag("$tag.state"),
                )
            } else {
                Button(onClick = onStart, enabled = startEnabled, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("$tag.start")) {
                    Icon(painterResource(R.drawable.ic_mic_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(startLabel)
                }
            }
        }
    }

    @Composable
    private fun languageLabel(code: String?): String =
        if (code == null) stringResource(R.string.tryout_language_auto) else Locale.forLanguageTag(code).getDisplayName(resources.configuration.locales[0]).ifBlank { code }

    @Composable
    private fun speakerLabel(index: Int?): String {
        if (index == null) return stringResource(R.string.settings_speaker_model_default)
        val name = tryout.speakers.getOrNull(index).orEmpty()
        return if (name.isEmpty()) stringResource(R.string.settings_speaker_number, index) else stringResource(R.string.settings_speaker_named, index, name)
    }

    private fun endpointLabel(millis: Long): Int = when (SpeechPreferencePolicy.ENDPOINT_MILLIS.indexOf(millis)) {
        0 -> R.string.settings_endpoint_sensitive
        2 -> R.string.settings_endpoint_relaxed
        else -> R.string.settings_endpoint_standard
    }

    /** The microphone and engine failures read differently here than on the model screens. zh-CN: 麦克风与引擎错误在此另有说法. */
    private fun problemText(code: String): Int = when (code) {
        SpeechErrorCodes.SESSION_BUSY -> R.string.tryout_microphone_busy
        SpeechErrorCodes.ENGINE_FAILED, SpeechErrorCodes.INTERNAL -> R.string.tryout_engine_failed
        else -> HomeTexts.error(code, insufficientSpace = false)
    }

    companion object {
        private const val MAX_TEXT = 500
        private const val EXTRA_TAB = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.extra.TRYOUT_TAB"
        private const val STATE_TAB = "tab"
        private const val TAB_FILE = 3
        private val AUDIO_TYPES = arrayOf("audio/*", "video/*", "application/ogg")

        /** The file tab, for the notifications of [FileRecognitionService] (D111). zh-CN: 文件标签, 供 [FileRecognitionService] 的通知使用 (D111). */
        internal fun fileIntent(context: Context): Intent = Intent(context, TryoutActivity::class.java)
            .putExtra(EXTRA_TAB, TAB_FILE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
