package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.speakers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerMatch
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.Voiceprint
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeakerRecognition
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeakerRecorder
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.EmptyAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawScreen
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The speaker screen (roadmap P8.3.4, D140): the enrolled speakers (name, recordings, last update), adding one by reading
 * [PROMPTS] aloud (about [PROMPT_MILLIS] each, recorded on this screen and enrolled at once, all or nothing), trying the
 * recognition on one sentence and deleting a speaker. Without the speaker model it leads to the model list first. Its
 * entry is the "Speakers" row of the resident wake word group, which "only enrolled voices" (D139) depends on.
 * zh-CN: 说话人页面 (路线图 P8.3.4, D140): 列出已注册的说话人 (名字, 录音段数, 更新时间), 跟读 [PROMPTS] 添加说话人 (每句约 [PROMPT_MILLIS],
 * 在本页录音, 一次性注册, 要么全部成功要么不变), 用一句话试验证, 以及删除说话人. 没有说话人模型时先引导到模型列表. 入口是常驻唤醒组的 "说话人" 行,
 * "只听主人" (D139) 依赖它.
 */
class SpeakersActivity : StandaloneActivity() {

    /** What the screen shows: [installed] null while loading. zh-CN: 页面显示的内容: 加载中时 [installed] 为 null. */
    private data class Listing(val installed: Boolean? = null, val speakers: List<Voiceprint> = emptyList())

    /** An enrollment in progress: [recordings] of [name] so far. zh-CN: 进行中的注册: [name] 已录的 [recordings]. */
    private data class Enrollment(val name: String, val recordings: List<FloatArray> = emptyList())

    private sealed interface Dialog {
        data object Name : Dialog
        data class Speaker(val voiceprint: Voiceprint) : Dialog
        data class Enroll(val enrollment: Enrollment) : Dialog
        data class Try(val result: String? = null) : Dialog
    }

    private val main = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "MacawSpeakerScreen").apply { isDaemon = true } }
    private val snackbar = SnackbarHostState()
    private var listing by mutableStateOf(Listing())
    private var dialog by mutableStateOf<Dialog?>(null)

    /** The share of the current recording, or -1 while the plugin works; null when idle. zh-CN: 当前录音的进度, 处理中为 -1; 空闲时为 null. */
    private var busy by mutableStateOf<Float?>(null)
    private var message by mutableStateOf<String?>(null)

    /** Ends the current recording early (the dialog closed, the screen left). zh-CN: 提前结束当前录音 (对话框关闭, 离开页面). */
    @Volatile
    private var cancelled = false
    private var afterPermission: (() -> Unit)? = null

    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val next = afterPermission
        afterPermission = null
        if (granted) next?.invoke() else message = getString(R.string.speakers_microphone)
    }

    private val speakers: SpeakerRecognition get() = SpeakerRecognition.get(applicationContext)

    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onStop() {
        // Leaving the screen ends a recording, so the microphone goes back to the resident wake word (D76).
        cancelled = true
        super.onStop()
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private fun load() = worker.execute {
        val installed = runCatching { SpeechRuntime.get(applicationContext).resolve(SpeechContract.KIND_SPEAKER, null) }.isSuccess
        val list = runCatching { speakers.list() }.getOrDefault(emptyList())
        main.post { if (!isDestroyed) listing = Listing(installed, list) }
    }

    /** Runs [action] once the microphone permission is granted. zh-CN: 获得麦克风权限后执行 [action]. */
    private fun withMicrophone(action: () -> Unit) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) return action()
        afterPermission = action
        microphone.launch(Manifest.permission.RECORD_AUDIO)
    }

    /** Records [millis] on the worker, then hands the samples to [then] there. zh-CN: 在工作线程上录音 [millis], 再在该线程上交给 [then]. */
    private fun record(millis: Long, then: (FloatArray) -> Unit) = withMicrophone {
        busy = 0f
        cancelled = false
        worker.execute {
            try {
                val samples = SpeakerRecorder.record(applicationContext, millis, progress = { fraction -> main.post { if (busy != null) busy = fraction } }) { cancelled }
                if (cancelled) {
                    main.post { busy = null; dialog = null }
                    return@execute
                }
                main.post { busy = -1f }
                then(samples)
            } catch (e: SpeechException) {
                failed(e)
            } catch (e: RuntimeException) {
                failed(SpeechException(SpeechErrorCodes.INTERNAL, e.message ?: e.javaClass.simpleName))
            }
        }
    }

    private fun failed(error: SpeechException) = main.post {
        busy = null
        message = if (error.code == SpeechErrorCodes.AUDIO_TOO_SHORT) getString(R.string.speakers_too_short) else getString(R.string.speakers_failed, error.message ?: error.code)
    }

    private fun recordPrompt(enrollment: Enrollment) = record(PROMPT_MILLIS) { samples ->
        val next = enrollment.copy(recordings = enrollment.recordings + samples)
        if (next.recordings.size < PROMPTS.size) {
            main.post { busy = null; if (!cancelled) dialog = Dialog.Enroll(next) }
            return@record
        }
        val recordings = next.recordings.mapIndexed { index, it -> SpeakerRecognition.Recording.Samples(it, "Sentence ${index + 1}") }
        val result = speakers.enroll(next.name, recordings, replace = false, model = null)
        main.post {
            busy = null
            dialog = null
            message = getString(R.string.speakers_saved, result.voiceprint.name, result.voiceprint.samples)
            load()
        }
    }

    private fun recordTry() = record(TRY_MILLIS) { samples ->
        val match = speakers.identify(SpeakerRecognition.Recording.Samples(samples, "The recording"), threshold = null, topK = 1)
        main.post {
            busy = null
            dialog = Dialog.Try(describe(match))
        }
    }

    private fun describe(match: SpeakerMatch): String {
        val best = match.best ?: return getString(R.string.speakers_empty)
        val score = String.format(Locale.ROOT, "%.2f", best.score)
        val threshold = String.format(Locale.ROOT, "%.2f", match.threshold)
        return if (match.matched) getString(R.string.speakers_matched, best.name, score, threshold) else getString(R.string.speakers_no_match, best.name, score, threshold)
    }

    private fun delete(name: String) = worker.execute {
        val removed = runCatching { speakers.remove(name) }.getOrDefault(false)
        main.post {
            if (removed) message = getString(R.string.speakers_deleted, name)
            load()
        }
    }

    private fun summary(voiceprint: Voiceprint): String = getString(
        R.string.speakers_item_summary,
        voiceprint.samples,
        DateUtils.formatDateTime(this, voiceprint.updatedAt, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH),
    )

    @Composable
    override fun Content() {
        val text = message
        LaunchedEffect(text) {
            if (text != null) {
                snackbar.showSnackbar(text)
                message = null
            }
        }
        MacawScreen(title = stringResource(R.string.speakers_title), onBack = ::finish, snackbarHostState = snackbar) { padding ->
            val current = listing
            when (current.installed) {
                null -> Unit
                false -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    EmptyState(
                        title = stringResource(R.string.speakers_model_missing),
                        body = stringResource(R.string.speakers_model_missing_body),
                        icon = R.drawable.ic_model_24,
                        primary = EmptyAction(stringResource(R.string.speakers_open_models), { startActivity(Intent(this@SpeakersActivity, HomeActivity::class.java)) }, "macaw.speakers.openModels"),
                    )
                }
                true -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("macaw.speakers.list")) {
                    item {
                        SettingsRow(stringResource(R.string.speakers_add), summary = stringResource(R.string.speakers_add_summary), icon = R.drawable.ic_person_24,
                            enabled = busy == null, onClick = { dialog = Dialog.Name }, modifier = Modifier.testTag("macaw.speakers.add"))
                        Hairline()
                        SettingsRow(stringResource(R.string.speakers_try), summary = stringResource(R.string.speakers_try_summary), icon = R.drawable.ic_record_voice_over_24,
                            enabled = busy == null && current.speakers.isNotEmpty(), onClick = { dialog = Dialog.Try() }, modifier = Modifier.testTag("macaw.speakers.try"))
                        SectionHeader(stringResource(R.string.speakers_enrolled))
                        if (current.speakers.isEmpty()) {
                            Text(
                                stringResource(R.string.speakers_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag("macaw.speakers.empty"),
                            )
                        }
                    }
                    items(current.speakers, key = { it.name }) { voiceprint ->
                        SettingsRow(voiceprint.name, summary = summary(voiceprint), icon = R.drawable.ic_person_24, showChevron = false,
                            onClick = { dialog = Dialog.Speaker(voiceprint) }, modifier = Modifier.testTag("macaw.speakers.item"))
                    }
                }
            }
        }
        when (val shown = dialog) {
            null -> Unit
            Dialog.Name -> NameDialog()
            is Dialog.Speaker -> MessageDialog(
                title = shown.voiceprint.name,
                message = summary(shown.voiceprint),
                onDismissRequest = { dialog = null },
                onConfirm = {
                    dialog = null
                    delete(shown.voiceprint.name)
                },
                confirmText = stringResource(R.string.speakers_delete),
                neutralText = stringResource(R.string.speakers_add_more),
                onNeutral = { dialog = Dialog.Enroll(Enrollment(shown.voiceprint.name)) },
                tag = "macaw.speakers.speaker",
            )
            is Dialog.Enroll -> RecordDialog(
                title = stringResource(R.string.speakers_read_title, shown.enrollment.recordings.size + 1, PROMPTS.size),
                text = stringResource(PROMPTS[shown.enrollment.recordings.size.coerceAtMost(PROMPTS.size - 1)]),
                emphasized = true,
                onRecord = { recordPrompt(shown.enrollment) },
                tag = "macaw.speakers.enroll",
            )
            is Dialog.Try -> RecordDialog(
                title = stringResource(R.string.speakers_try),
                text = shown.result ?: stringResource(R.string.speakers_try_prompt),
                emphasized = false,
                onRecord = ::recordTry,
                again = shown.result != null,
                tag = "macaw.speakers.tryDialog",
            )
        }
    }

    @Composable
    private fun NameDialog() {
        var name by remember { mutableStateOf("") }
        val valid = runCatching { SpeakerRules.name(name) }.isSuccess
        MacawDialog(
            title = stringResource(R.string.speakers_add),
            onDismissRequest = { dialog = null },
            confirmText = stringResource(R.string.ui_dialog_confirm),
            onConfirm = { dialog = Dialog.Enroll(Enrollment(SpeakerRules.name(name))) },
            dismissText = stringResource(R.string.ui_dialog_cancel),
            confirmEnabled = valid,
            tag = "macaw.speakers.name",
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(NAME_INPUT_LIMIT) },
                label = { Text(stringResource(R.string.speakers_name)) },
                singleLine = true,
                isError = name.isNotEmpty() && !valid,
                supportingText = { Text(stringResource(R.string.speakers_name_rule, SpeechLimits.MAX_SPEAKER_NAME_CHARS)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("macaw.speakers.nameInput"),
            )
        }
    }

    @Composable
    private fun RecordDialog(title: String, text: String, emphasized: Boolean, onRecord: () -> Unit, tag: String, again: Boolean = false) {
        val progress = busy
        MacawDialog(
            title = title,
            onDismissRequest = {
                // A recording ends with the dialog; work the plugin already started finishes in the background.
                cancelled = true
                busy = null
                dialog = null
            },
            confirmText = stringResource(if (again) R.string.speakers_record_again else R.string.speakers_record),
            onConfirm = onRecord,
            dismissText = stringResource(R.string.ui_dialog_cancel),
            confirmEnabled = progress == null,
            tag = tag,
        ) {
            Text(
                text,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp).testTag("$tag.text"),
            )
            when {
                progress == null -> Unit
                progress < 0f -> {
                    Text(stringResource(R.string.speakers_working), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp))
                }
                else -> {
                    Text(stringResource(R.string.speakers_recording), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).testTag("$tag.progress"))
                }
            }
        }
    }

    companion object {
        /** The sentences an enrollment reads, one recording each (roadmap D140). zh-CN: 注册时跟读的句子, 每句一段录音 (路线图 D140). */
        val PROMPTS = listOf(R.string.speakers_prompt_1, R.string.speakers_prompt_2, R.string.speakers_prompt_3)
        const val PROMPT_MILLIS = 4500L
        const val TRY_MILLIS = 3000L
        private const val NAME_INPUT_LIMIT = 80
    }
}
