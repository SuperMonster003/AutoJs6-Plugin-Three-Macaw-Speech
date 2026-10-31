package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.text.format.Formatter
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAnalysis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PlaybackCue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PlaybackCues
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.TranscriptExport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SwitchRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeTexts
import kotlin.math.roundToInt

/**
 * The file tab of the tryout screen (roadmap D110 - D115, P7.8.3, D153): the action row (transcribe, find speakers,
 * both, identify the language, tag the sounds) with only the settings of the chosen action, the picker button (cancel
 * while a run goes on), the progress, the segments as they arrive and the latest result with its actions or its
 * failure card: speaker turns in their colors, the language with its share of the speech windows, the sounds with
 * score bars and the window where each is clearest. Since roadmap D164 a finished result has a player above it that
 * sticks to the top of the page, whose rows follow the sound and which turns into the lyrics view. Leaving the tab pauses
 * it, since the microphone tabs must not hear it (D171). The run itself lives in [FileTranscription], so the tab only
 * shows its state.
 * zh-CN: 试听试说页的文件标签 (路线图 D110 - D115, P7.8.3, D153): "操作" 行 (转写, 分离说话人, 分离 + 转写, 识别语种, 音频标签) 与所选操作自己的设置,
 * 选择按钮 (运行中为取消), 进度, 逐条到达的分段, 以及最近一次结果与其操作或失败卡片: 按说话人着色的轮次, 语种与其在语音窗中的占比, 声音及其分数条
 * 与最明显的窗口. 路线图 D164 起结束的结果上方有一个粘在页面顶部的播放器, 各行随声音同步, 也可切换为歌词式视图. 离开标签即暂停, 因为麦克风标签不能听到它
 * (D171). 运行本身在 [FileTranscription] 中进行, 标签只显示其状态.
 */
@Composable
internal fun FileTab(
    controller: TryoutController,
    transcription: FileTranscription,
    scroll: ScrollState,
    languageLabel: @Composable (String?) -> String,
    onPick: () -> Unit,
    onCopy: (FileTranscriptRecord) -> Unit,
    onSaveText: () -> Unit,
    onSaveSrt: () -> Unit,
    onOpenModels: () -> Unit,
) {
    val context = LocalContext.current
    var choosing by remember { mutableStateOf<String?>(null) }
    val run = transcription.run
    val record = transcription.record
    val idle = run == null
    val action = controller.fileAction
    val playable = record?.takeIf { run == null && it.state != FileTranscriptRecord.State.FAILED }
    val playback = controller.playback
    LaunchedEffect(playable) { if (playable == null) playback.close() else playback.open(playable, transcription.playbackSource(playable)) }
    DisposableEffect(Unit) { onDispose { playback.pause() } }
    val cues = remember(playable) { playable?.let(PlaybackCues::of) ?: PlaybackCues.NONE }
    val follow = remember(scroll) { PlaybackFollow(scroll) }
    LaunchedEffect(follow) { follow.watch() }
    val player = remember(playable, cues) { playable?.let { ResultPlayer(playback, follow, cues) } }
    SettingsRow(stringResource(R.string.tryout_file_action), summary = stringResource(FileTexts.action(action)), icon = R.drawable.ic_apps_24, enabled = idle,
        onClick = { choosing = "action" }, modifier = Modifier.testTag("macaw.tryout.file.action"))
    val model = controller.asrModels.firstOrNull { it.id == controller.fileModel }
    if (action.transcribes) {
        SettingsRow(stringResource(R.string.tryout_model), summary = model?.name, icon = R.drawable.ic_model_24, enabled = idle,
            onClick = { choosing = "model" }, modifier = Modifier.testTag("macaw.tryout.file.model"))
        if (controller.takesLanguage(controller.fileModel)) {
            SettingsRow(stringResource(R.string.tryout_language), summary = languageLabel(controller.fileLanguage), icon = R.drawable.ic_language_24, enabled = idle,
                onClick = { choosing = "language" }, modifier = Modifier.testTag("macaw.tryout.file.language"))
        }
        SwitchRow(
            stringResource(R.string.tryout_transcribe_punctuation),
            checked = controller.filePunctuation,
            onToggle = { controller.filePunctuation = it },
            summary = stringResource(R.string.tryout_transcribe_punctuation_summary),
            icon = R.drawable.ic_tune_24,
            enabled = idle,
            modifier = Modifier.testTag("macaw.tryout.file.punctuation"),
        )
    }
    if (action.diarizes) {
        SettingsRow(stringResource(R.string.tryout_file_speakers), summary = speakerCount(controller.fileSpeakers), icon = R.drawable.ic_person_24, enabled = idle,
            onClick = { choosing = "speakers" }, modifier = Modifier.testTag("macaw.tryout.file.speakers"))
        SwitchRow(
            stringResource(R.string.tryout_file_identify),
            checked = controller.fileIdentify,
            onToggle = { controller.fileIdentify = it },
            summary = stringResource(R.string.tryout_file_identify_summary),
            icon = R.drawable.ic_record_voice_over_24,
            enabled = idle,
            modifier = Modifier.testTag("macaw.tryout.file.identify"),
        )
    }
    val languageModel = controller.languageModels.firstOrNull { it.id == controller.languageModel }
    if (action == FileAction.LANGUAGE) {
        SettingsRow(stringResource(R.string.tryout_model), summary = languageModel?.name, icon = R.drawable.ic_model_24, enabled = idle,
            onClick = { choosing = "languageModel" }, modifier = Modifier.testTag("macaw.tryout.file.languageModel"))
    }
    Text(
        if (action.transcribes) {
            stringResource(R.string.tryout_transcribe_settings, stringResource(if (controller.vad) R.string.tryout_vad_on else R.string.tryout_vad_off))
        } else {
            stringResource(R.string.tryout_file_limits)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    val missing = controller.missingKinds(action)
    if (missing.isNotEmpty() && idle) {
        Missing(missing.map { stringResource(HomeTexts.kindTitle(it)) }, onOpenModels)
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        if (run == null) {
            Button(onClick = onPick, enabled = missing.isEmpty(), modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.pick")) {
                Icon(painterResource(R.drawable.ic_audio_file_24), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tryout_transcribe_pick))
            }
        } else {
            Button(onClick = transcription::cancel, enabled = run.phase != FileRun.Phase.CANCELLING, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.cancel")) {
                Icon(painterResource(R.drawable.ic_stop_24), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_dialog_cancel))
            }
        }
    }
    val name = run?.displayName ?: record?.displayName
    val size = run?.sizeBytes ?: record?.sizeBytes
    if (run != null || record != null) {
        Text(
            stringResource(R.string.tryout_transcribe_file, name ?: stringResource(R.string.tryout_transcribe_unnamed), size?.let { Formatter.formatShortFileSize(context, it) } ?: "?"),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).testTag("macaw.tryout.file.name"),
        )
    }
    if (run != null) {
        Progress(run)
    } else if (record == null) {
        Text(
            stringResource(if (action == FileAction.TRANSCRIBE) R.string.tryout_transcribe_hint else R.string.tryout_file_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
    }
    record?.takeIf { run == null }?.let { Outcome(it, controller, languageLabel) }
    val canLyric = cues.items.isNotEmpty() && playback.state != FilePlayback.State.UNAVAILABLE
    var lyricsShown by remember { mutableIntStateOf(0) }
    if (playable != null) {
        // Sticks to the top of the page, or a long result would scroll the controls away. zh-CN: 粘在页面顶部, 否则长结果会把控件滚走.
        var top by remember { mutableIntStateOf(0) }
        PlayerBar(
            playback,
            lyrics = controller.fileLyrics,
            lyricsEnabled = canLyric,
            onLyrics = {
                controller.fileLyrics = it
                lyricsShown += 1
            },
            modifier = Modifier
                .onPlaced {
                    top = it.positionInParent().y.roundToInt()
                    follow.barHeight = it.size.height
                }
                .zIndex(1f)
                .offset { IntOffset(0, (scroll.value - top).coerceAtLeast(0)) },
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 120.dp).testTag("macaw.tryout.file.segments"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        // Only a run announces what arrives; a played result would talk over its own sound. zh-CN: 只有运行中才播报新内容; 播放中的结果不能盖过声音.
        Column(modifier = Modifier.padding(16.dp).semantics { if (run != null) liveRegion = LiveRegionMode.Polite }) {
            if (controller.fileLyrics && canLyric && playable != null) {
                Lyrics(cues, playback, follow, lyricsShown, playable.analysis as? FileAnalysis.Speakers) { cueText(it, playable, languageLabel) }
            } else {
                Result(run, record?.takeIf { run == null }, player, languageLabel)
            }
        }
    }
    if (record != null && run == null) {
        val transcribed = record.action.transcribes
        val hasResult = if (transcribed) record.text.isNotBlank() else record.analysis != null
        if (transcribed) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSaveText, enabled = hasResult, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.saveText")) {
                    Text(stringResource(R.string.tryout_transcribe_save_text))
                }
                OutlinedButton(onClick = onSaveSrt, enabled = hasResult, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.saveSrt")) {
                    Text(stringResource(R.string.tryout_transcribe_save_srt))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onCopy(record) }, enabled = hasResult, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.copy")) {
                Text(stringResource(R.string.tryout_transcribe_copy))
            }
            OutlinedButton(onClick = transcription::clear, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.clear")) {
                Text(stringResource(R.string.tryout_transcribe_clear))
            }
        }
    }
    when (choosing) {
        "action" -> ChoiceDialog(
            title = stringResource(R.string.tryout_file_action),
            options = FileAction.entries.map { Choice(stringResource(FileTexts.action(it)), stringResource(FileTexts.actionAbout(it))) },
            selectedIndex = action.ordinal,
            onDismissRequest = { choosing = null },
            onConfirm = { index -> choosing = null; controller.fileAction = FileAction.entries[index] },
            tag = "macaw.tryout.file.actionDialog",
        )
        "model" -> ChoiceDialog(
            title = stringResource(R.string.tryout_model),
            options = controller.asrModels.map { Choice(it.name, HomeTexts.languages(context, it.languages)) },
            selectedIndex = controller.asrModels.indexOfFirst { it.id == controller.fileModel }.coerceAtLeast(0),
            onDismissRequest = { choosing = null },
            onConfirm = { index -> choosing = null; controller.fileModel = controller.asrModels[index].id },
            tag = "macaw.tryout.file.modelDialog",
        )
        "language" -> {
            val codes = listOf(null) + (model?.languages ?: emptyList())
            ChoiceDialog(
                title = stringResource(R.string.tryout_language),
                options = codes.map { Choice(languageLabel(it)) },
                selectedIndex = codes.indexOf(controller.fileLanguage).coerceAtLeast(0),
                onDismissRequest = { choosing = null },
                onConfirm = { index -> choosing = null; controller.fileLanguage = codes[index] },
                tag = "macaw.tryout.file.languageDialog",
            )
        }
        "speakers" -> ChoiceDialog(
            title = stringResource(R.string.tryout_file_speakers),
            options = SPEAKER_COUNTS.map { Choice(speakerCount(it)) },
            selectedIndex = SPEAKER_COUNTS.indexOf(controller.fileSpeakers).coerceAtLeast(0),
            onDismissRequest = { choosing = null },
            onConfirm = { index -> choosing = null; controller.fileSpeakers = SPEAKER_COUNTS[index] },
            tag = "macaw.tryout.file.speakersDialog",
        )
        "languageModel" -> ChoiceDialog(
            title = stringResource(R.string.tryout_model),
            options = controller.languageModels.map { Choice(it.name, HomeTexts.size(context, it.installedBytes ?: it.sizeBytes)) },
            selectedIndex = controller.languageModels.indexOfFirst { it.id == controller.languageModel }.coerceAtLeast(0),
            onDismissRequest = { choosing = null },
            onConfirm = { index -> choosing = null; controller.languageModel = controller.languageModels[index].id },
            tag = "macaw.tryout.file.languageModelDialog",
        )
    }
}

/** 0 is automatic; the contract takes up to 20, the dialog offers the common counts. zh-CN: 0 为自动; 契约至多 20, 对话框列出常见人数. */
private val SPEAKER_COUNTS = listOf(0, 2, 3, 4, 5, 6, 7, 8, 9, 10)

/** Turn colors, readable on light and dark surfaces next to the speaker's label. zh-CN: 轮次颜色, 在浅色与深色背景上都可辨认, 旁边另有说话人标签. */
private val SPEAKER_COLORS = listOf(
    Color(0xFF1E88E5), Color(0xFFD81B60), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFF6D4C41), Color(0xFF7CB342),
)

internal fun speakerColor(speaker: Int): Color = SPEAKER_COLORS[speaker % SPEAKER_COLORS.size]

@Composable
private fun speakerCount(count: Int): String = if (count == 0) stringResource(R.string.tryout_file_speakers_auto) else count.toString()

/** The kinds the action needs and that are not installed (D153). zh-CN: 操作需要但未安装的模型类别 (D153). */
@Composable
private fun Missing(kinds: List<String>, onOpenModels: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("macaw.tryout.file.missing"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.tryout_no_model_title, kinds.joinToString(", ")),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                stringResource(R.string.tryout_no_model_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedButton(onClick = onOpenModels, modifier = Modifier.padding(top = 8.dp).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.openModels")) {
                Text(stringResource(R.string.tryout_open_models))
            }
        }
    }
}

/** D112: a determinate bar while the length is known, else an indeterminate one. zh-CN: D112: 时长已知时为确定进度条, 否则为不确定进度条. */
@Composable
private fun Progress(run: FileRun) {
    val total = run.declaredMillis?.takeIf { it > 0 }
    val text = FileTexts.progress(LocalContext.current, run)
    val bar = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).testTag("macaw.tryout.file.progressBar")
    if (total != null && run.phase == FileRun.Phase.RUNNING) {
        LinearProgressIndicator(progress = { (run.recognizedMillis.toFloat() / total).coerceIn(0f, 1f) }, modifier = bar)
    } else {
        LinearProgressIndicator(modifier = bar)
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("macaw.tryout.file.progress"),
    )
}

/** The end of a run: the summary, the cancel note or the failure card (D112, D113, D115). zh-CN: 运行结束: 摘要, 取消说明或失败卡片. */
@Composable
private fun Outcome(record: FileTranscriptRecord, controller: TryoutController, languageLabel: @Composable (String?) -> String) {
    val recognized = TranscriptExport.clock(record.recognizedMillis)
    when (record.state) {
        FileTranscriptRecord.State.FAILED -> Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("macaw.tryout.file.error"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            val failure = record.failure
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(if (record.action.transcribes) R.string.tryout_transcribe_failed_title else R.string.tryout_file_analysis_failed),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    stringResource(failure?.let { FileTexts.problem(it.problem) } ?: R.string.tryout_transcribe_error_engine),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 4.dp).testTag("macaw.tryout.file.errorHint"),
                )
                if (failure != null) {
                    // D115: the English reason of the contract, never the file name. zh-CN: D115: 契约的英文原因, 不含文件名.
                    Text(
                        "${failure.code}: ${failure.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp).testTag("macaw.tryout.file.errorDetail"),
                    )
                }
            }
        }
        FileTranscriptRecord.State.CANCELLED -> {
            // An analysis stops without a position; "diarize and transcribe" got as far as its last segment.
            // zh-CN: 分析停止时没有位置; "分离 + 转写" 进行到最后一个分段.
            val reached = if (record.action == FileAction.TRANSCRIBE) record.recognizedMillis else record.segments.lastOrNull()?.endMillis
            Note(reached?.let { stringResource(R.string.tryout_transcribe_cancelled, TranscriptExport.clock(it)) } ?: stringResource(R.string.tryout_file_cancelled), "macaw.tryout.file.cancelled")
        }
        FileTranscriptRecord.State.COMPLETED -> Note(stringResource(R.string.tryout_transcribe_summary, recognized, controller.modelName(record.modelId)), "macaw.tryout.file.summary")
    }
    val speakers = record.analysis as? FileAnalysis.Speakers
    if (speakers != null && record.state == FileTranscriptRecord.State.COMPLETED) {
        Note(stringResource(R.string.tryout_file_speakers_found, speakers.speakers, speakers.turns.size), "macaw.tryout.file.speakersFound")
    }
    record.language?.takeIf { record.state != FileTranscriptRecord.State.FAILED && record.action.transcribes }?.let {
        Note(stringResource(R.string.tryout_transcribe_language, languageLabel(it)), "macaw.tryout.file.language.result")
    }
    if (record.warnings.isNotEmpty()) {
        Note(stringResource(R.string.tryout_transcribe_warnings, record.warnings.joinToString("; ") { it.message }), "macaw.tryout.file.warnings")
    }
}

/** The result card of a run or a record (D153). zh-CN: 运行或记录的结果卡片 (D153). */
@Composable
private fun Result(run: FileRun?, record: FileTranscriptRecord?, player: ResultPlayer?, languageLabel: @Composable (String?) -> String) {
    val action = run?.action ?: record?.action ?: FileAction.TRANSCRIBE
    val segments = run?.segments ?: record?.segments.orEmpty()
    val owners = run?.segmentSpeakers ?: record?.segmentSpeakers.orEmpty()
    val speakers = run?.speakers ?: record?.analysis as? FileAnalysis.Speakers
    val analysis = record?.analysis
    val completed = record?.state == FileTranscriptRecord.State.COMPLETED
    when {
        action.transcribes && segments.any { it.text.isNotBlank() } -> segments.forEachIndexed { index, segment ->
            if (segment.text.isNotBlank()) Segment(segment, index, owners.getOrNull(index), speakers, player, Modifier.testTag("macaw.tryout.file.segment.$index"))
        }
        action == FileAction.DIARIZE && analysis is FileAnalysis.Speakers && analysis.turns.isNotEmpty() -> analysis.turns.forEachIndexed { index, turn ->
            Turn(turn.startMillis, turn.endMillis, turn.speaker, analysis, index, player, Modifier.testTag("macaw.tryout.file.turn.$index"))
        }
        analysis is FileAnalysis.Languages -> Languages(analysis, player, languageLabel)
        analysis is FileAnalysis.Tags -> Tags(analysis, player)
        else -> {
            val waiting = when {
                completed -> R.string.tryout_transcribe_empty
                action == FileAction.TRANSCRIBE || run?.stage == FileRun.Stage.TURNS -> R.string.tryout_transcribe_waiting
                else -> R.string.tryout_file_waiting
            }
            Text(
                stringResource(waiting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("macaw.tryout.file.placeholder"),
            )
        }
    }
}

/** The text of one line in the lyrics view (D164). zh-CN: 歌词式视图中一行的文字 (D164). */
@Composable
private fun cueText(cue: PlaybackCue, record: FileTranscriptRecord, languageLabel: @Composable (String?) -> String): String = when (val analysis = record.analysis.takeUnless { record.action.transcribes }) {
    null -> cue.text
    is FileAnalysis.Speakers -> FileTexts.speaker(LocalContext.current, analysis, cue.speaker ?: 0)
    is FileAnalysis.Languages -> languageLabel(cue.text)
    is FileAnalysis.Tags -> tagText(cue)
}

@Composable
private fun tagText(cue: PlaybackCue): String = stringResource(R.string.tryout_file_pair, cue.text, FileTexts.percent(LocalContext.current, cue.score.toDouble()))

@Composable
private fun Note(text: String, tag: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp).testTag(tag),
    )
}

@Composable
internal fun SpeakerLabel(speaker: Int, speakers: FileAnalysis.Speakers?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(speakerColor(speaker), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            FileTexts.speaker(LocalContext.current, speakers, speaker),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Segment(segment: Utterance, index: Int, speaker: Int?, speakers: FileAnalysis.Speakers?, player: ResultPlayer?, modifier: Modifier) {
    CueRow(player, index, segment.startMillis, speaker?.let(::speakerColor) ?: MaterialTheme.colorScheme.primary, modifier) {
        if (speaker != null) SpeakerLabel(speaker, speakers)
        Text(
            "${TranscriptExport.segmentClock(segment.startMillis)} - ${TranscriptExport.segmentClock(maxOf(segment.endMillis, segment.startMillis))}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(segment.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Turn(startMillis: Long, endMillis: Long, speaker: Int, speakers: FileAnalysis.Speakers, index: Int, player: ResultPlayer?, modifier: Modifier) {
    CueRow(player, index, startMillis, speakerColor(speaker), modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${TranscriptExport.segmentClock(startMillis)} - ${TranscriptExport.segmentClock(maxOf(endMillis, startMillis))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(132.dp),
            )
            SpeakerLabel(speaker, speakers)
        }
    }
}

@Composable
private fun Languages(analysis: FileAnalysis.Languages, player: ResultPlayer?, languageLabel: @Composable (String?) -> String) {
    val context = LocalContext.current
    Text(
        stringResource(R.string.tryout_file_language_result, languageLabel(analysis.language)),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("macaw.tryout.file.languageResult"),
    )
    Text(
        stringResource(R.string.tryout_file_language_candidates),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    analysis.candidates.forEachIndexed { index, (language, share) ->
        Score(stringResource(R.string.tryout_file_pair, languageLabel(language), FileTexts.percent(context, share.toDouble())), share, null, Modifier.testTag("macaw.tryout.file.candidate.$index"))
    }
    Text(
        stringResource(R.string.tryout_file_language_sentences),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    analysis.segments.forEachIndexed { index, segment ->
        CueRow(player, index, segment.startMillis, MaterialTheme.colorScheme.primary, Modifier.testTag("macaw.tryout.file.sentence.$index")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${TranscriptExport.segmentClock(segment.startMillis)} - ${TranscriptExport.segmentClock(maxOf(segment.endMillis, segment.startMillis))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(132.dp),
                )
                Text(languageLabel(segment.language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun Tags(analysis: FileAnalysis.Tags, player: ResultPlayer?) {
    val context = LocalContext.current
    if (player?.ready == true) {
        // Empty in a gap, so the list does not jump. zh-CN: 空白处为空串, 列表不跳动.
        Text(
            player.current?.let { stringResource(R.string.tryout_file_tag_now, tagText(it)) }.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp).testTag("macaw.tryout.file.tag.now"),
        )
    }
    Text(
        stringResource(R.string.tryout_file_tags_heard),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    analysis.labels.forEachIndexed { index, tag ->
        CueRow(player, index, tag.startMillis, MaterialTheme.colorScheme.primary, Modifier.testTag("macaw.tryout.file.tag.$index")) {
            Score(
                stringResource(R.string.tryout_file_pair, tag.label.name, FileTexts.percent(context, tag.score.toDouble())),
                tag.score,
                stringResource(R.string.tryout_file_tag_peak, TranscriptExport.segmentClock(tag.startMillis), TranscriptExport.segmentClock(tag.endMillis)),
                Modifier,
            )
        }
    }
}

@Composable
private fun Score(title: String, share: Float, note: String?, modifier: Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        LinearProgressIndicator(progress = { share.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        if (note != null) Text(note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
