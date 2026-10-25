package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.TranscriptExport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SwitchRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeTexts

/**
 * The file tab of the tryout screen (roadmap D110 - D115, P7.8.3): its own model, language and punctuation choice, the
 * picker button (cancel while a run goes on), the progress, the segments as they arrive and the latest result with its
 * actions or its failure card. The run itself lives in [FileTranscription], so the tab only shows its state.
 * zh-CN: 试听试说页的文件标签 (路线图 D110 - D115, P7.8.3): 自己的模型, 语言与标点选择, 选择按钮 (识别中为取消), 进度, 逐条到达的分段, 以及最近一次
 * 结果与其操作或失败卡片. 识别本身在 [FileTranscription] 中进行, 标签只显示其状态.
 */
@Composable
internal fun FileTab(
    controller: TryoutController,
    transcription: FileTranscription,
    languageLabel: @Composable (String?) -> String,
    onPick: () -> Unit,
    onCopy: (String) -> Unit,
    onSaveText: () -> Unit,
    onSaveSrt: () -> Unit,
) {
    val context = LocalContext.current
    var choosing by remember { mutableStateOf<String?>(null) }
    val run = transcription.run
    val record = transcription.record
    val idle = run == null
    val model = controller.asrModels.firstOrNull { it.id == controller.fileModel }
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
    Text(
        stringResource(R.string.tryout_transcribe_settings, stringResource(if (controller.vad) R.string.tryout_vad_on else R.string.tryout_vad_off)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        if (run == null) {
            Button(onClick = onPick, enabled = model != null, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.pick")) {
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
            stringResource(R.string.tryout_transcribe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
    }
    record?.takeIf { run == null }?.let { Outcome(it, controller, languageLabel) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 120.dp).testTag("macaw.tryout.file.segments"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        val segments = (run?.segments ?: record?.segments.orEmpty()).filter { it.text.isNotBlank() }
        Column(modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
            if (segments.isEmpty()) {
                val empty = record != null && run == null && record.state == FileTranscriptRecord.State.COMPLETED
                Text(
                    stringResource(if (empty) R.string.tryout_transcribe_empty else R.string.tryout_transcribe_waiting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("macaw.tryout.file.placeholder"),
                )
            }
            segments.forEachIndexed { index, segment -> Segment(segment, Modifier.testTag("macaw.tryout.file.segment.$index")) }
        }
    }
    if (record != null && run == null) {
        val hasText = record.text.isNotBlank()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSaveText, enabled = hasText, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.saveText")) {
                Text(stringResource(R.string.tryout_transcribe_save_text))
            }
            OutlinedButton(onClick = onSaveSrt, enabled = hasText, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.saveSrt")) {
                Text(stringResource(R.string.tryout_transcribe_save_srt))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onCopy(record.text) }, enabled = hasText, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.copy")) {
                Text(stringResource(R.string.tryout_transcribe_copy))
            }
            OutlinedButton(onClick = transcription::clear, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("macaw.tryout.file.clear")) {
                Text(stringResource(R.string.tryout_transcribe_clear))
            }
        }
    }
    when (choosing) {
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
    }
}

/** D112: a determinate bar while the length is known, else an indeterminate one. zh-CN: D112: 时长已知时为确定进度条, 否则为不确定进度条. */
@Composable
private fun Progress(run: FileRun) {
    val total = run.declaredMillis?.takeIf { it > 0 }
    val recognized = TranscriptExport.clock(run.recognizedMillis)
    val text = when {
        run.phase == FileRun.Phase.CANCELLING -> stringResource(R.string.tryout_transcribe_cancelling)
        run.phase == FileRun.Phase.PREPARING -> stringResource(R.string.tryout_transcribe_preparing)
        total != null -> stringResource(R.string.tryout_transcribe_progress, recognized, TranscriptExport.clock(total))
        else -> stringResource(R.string.tryout_transcribe_progress_unknown, recognized)
    }
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

/** The end of a run: the summary, the cancel note or the failure card (D112, D113, D115). zh-CN: 识别结束: 摘要, 取消说明或失败卡片. */
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
                Text(stringResource(R.string.tryout_transcribe_failed_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
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
        FileTranscriptRecord.State.CANCELLED -> Note(stringResource(R.string.tryout_transcribe_cancelled, recognized), "macaw.tryout.file.cancelled")
        FileTranscriptRecord.State.COMPLETED -> {
            val model = controller.asrModels.firstOrNull { it.id == record.modelId }?.name ?: record.modelId
            Note(stringResource(R.string.tryout_transcribe_summary, recognized, model), "macaw.tryout.file.summary")
        }
    }
    record.language?.takeIf { record.state != FileTranscriptRecord.State.FAILED }?.let { Note(stringResource(R.string.tryout_transcribe_language, languageLabel(it)), "macaw.tryout.file.language.result") }
    if (record.warnings.isNotEmpty()) {
        Note(stringResource(R.string.tryout_transcribe_warnings, record.warnings.joinToString("; ") { it.message }), "macaw.tryout.file.warnings")
    }
}

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
private fun Segment(segment: Utterance, modifier: Modifier) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            "${TranscriptExport.segmentClock(segment.startMillis)} - ${TranscriptExport.segmentClock(maxOf(segment.endMillis, segment.startMillis))}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(segment.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
