package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.content.Context
import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAnalysis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileProblem
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.TranscriptExport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeTexts
import java.text.NumberFormat

/**
 * The localized texts of the file tab shared by the tab, the notifications and the exports: the hints of a failed run
 * (roadmap D115), the actions of D153, the progress line, speaker labels and the copied text of each action.
 * zh-CN: 文件标签, 通知与导出共用的本地化文字: 失败提示 (路线图 D115), D153 的操作, 进度行, 说话人标签与各操作复制的文字.
 */
internal object FileTexts {

    @StringRes
    fun problem(problem: FileProblem): Int = when (problem) {
        FileProblem.UNSUPPORTED_FORMAT -> R.string.tryout_transcribe_error_format
        FileProblem.LIMIT_EXCEEDED -> R.string.tryout_transcribe_error_limit
        FileProblem.IO_FAILED -> R.string.tryout_transcribe_error_io
        FileProblem.MODEL_MISSING -> R.string.tryout_transcribe_error_model
        FileProblem.ENGINE_FAILED -> R.string.tryout_transcribe_error_engine
    }

    @StringRes
    fun action(action: FileAction): Int = when (action) {
        FileAction.TRANSCRIBE -> R.string.tryout_file_action_transcribe
        FileAction.DIARIZE -> R.string.tryout_file_action_diarize
        FileAction.DIARIZE_TRANSCRIBE -> R.string.tryout_file_action_diarize_transcribe
        FileAction.LANGUAGE -> R.string.tryout_file_action_language
        FileAction.TAG -> R.string.tryout_file_action_tag
    }

    @StringRes
    fun actionAbout(action: FileAction): Int = when (action) {
        FileAction.TRANSCRIBE -> R.string.tryout_file_action_transcribe_about
        FileAction.DIARIZE -> R.string.tryout_file_action_diarize_about
        FileAction.DIARIZE_TRANSCRIBE -> R.string.tryout_file_action_diarize_transcribe_about
        FileAction.LANGUAGE -> R.string.tryout_file_action_language_about
        FileAction.TAG -> R.string.tryout_file_action_tag_about
    }

    /**
     * The progress line of a run (D112, D153): recognized time for a transcription, the stage and its share for the
     * analyses and the turns of "diarize and transcribe".
     * zh-CN: 运行的进度行 (D112, D153): 转写显示已识别时长, 分析与 "分离 + 转写" 的轮次显示阶段与完成比例.
     */
    fun progress(context: Context, run: FileRun): String {
        val total = run.declaredMillis?.takeIf { it > 0 }
        val recognized = TranscriptExport.clock(run.recognizedMillis)
        return when {
            run.phase == FileRun.Phase.CANCELLING -> context.getString(R.string.tryout_transcribe_cancelling)
            run.phase == FileRun.Phase.PREPARING -> context.getString(R.string.tryout_transcribe_preparing)
            run.stage == FileRun.Stage.TRANSCRIBING && total != null -> context.getString(R.string.tryout_transcribe_progress, recognized, TranscriptExport.clock(total))
            run.stage == FileRun.Stage.TRANSCRIBING -> context.getString(R.string.tryout_transcribe_progress_unknown, recognized)
            total == null -> context.getString(stage(run.stage))
            else -> context.getString(R.string.tryout_file_pair, context.getString(stage(run.stage)), percent(context, run.recognizedMillis.toDouble() / total))
        }
    }

    fun percent(context: Context, share: Double): String = NumberFormat.getPercentInstance(context.resources.configuration.locales[0]).format(share.coerceIn(0.0, 1.0))

    /** The enrolled name identify found, else "Speaker n" counted from 1. zh-CN: 认人得到的已注册名字, 否则为从 1 起计数的 "说话人 n". */
    fun speaker(context: Context, speakers: FileAnalysis.Speakers?, speaker: Int): String =
        speakers?.name(speaker) ?: context.getString(R.string.tryout_file_speaker, speaker + 1)

    /** The segments of [record] as written to TXT, SRT and the clipboard. zh-CN: 写入 TXT, SRT 与剪贴板的 [record] 分段. */
    fun segments(context: Context, record: FileTranscriptRecord): List<Utterance> {
        if (record.action != FileAction.DIARIZE_TRANSCRIBE) return record.segments
        val speakers = record.analysis as? FileAnalysis.Speakers
        return TranscriptExport.labeled(record.segments, record.segmentSpeakers) { speaker, text ->
            context.getString(R.string.tryout_file_pair, speaker(context, speakers, speaker), text)
        }
    }

    /** What "Copy" puts on the clipboard for each action (D153). zh-CN: 各操作的 "复制" 放入剪贴板的内容 (D153). */
    fun copyText(context: Context, record: FileTranscriptRecord): String = when (val analysis = record.analysis) {
        is FileAnalysis.Speakers -> if (record.action.transcribes) {
            segments(context, record).mapNotNull { it.text.trim().takeIf(String::isNotEmpty) }.joinToString("\n")
        } else {
            TranscriptExport.turns(analysis.turns) { speaker(context, analysis, it) }
        }
        is FileAnalysis.Languages -> analysis.candidates.joinToString("\n") { (language, share) ->
            context.getString(R.string.tryout_file_pair, HomeTexts.languages(context, listOf(language)), percent(context, share.toDouble()))
        }
        is FileAnalysis.Tags -> analysis.labels.joinToString("\n") {
            val peak = context.getString(R.string.tryout_file_tag_peak, TranscriptExport.segmentClock(it.startMillis), TranscriptExport.segmentClock(it.endMillis))
            "${context.getString(R.string.tryout_file_pair, it.label.name, percent(context, it.score.toDouble()))} ($peak)"
        }
        null -> record.text
    }

    @StringRes
    private fun stage(stage: FileRun.Stage): Int = when (stage) {
        FileRun.Stage.DIARIZING -> R.string.tryout_file_stage_diarize
        FileRun.Stage.LANGUAGE -> R.string.tryout_file_stage_language
        FileRun.Stage.TAGGING -> R.string.tryout_file_stage_tag
        FileRun.Stage.TRANSCRIBING, FileRun.Stage.TURNS -> R.string.tryout_file_stage_turns
    }
}
