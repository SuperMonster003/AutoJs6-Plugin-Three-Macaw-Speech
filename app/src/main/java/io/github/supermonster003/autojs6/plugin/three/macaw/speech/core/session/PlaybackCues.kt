package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import java.util.Locale

/**
 * One line of a result that follows the playback of the file tab (roadmap D164): [index] is its row in the result list,
 * [text] the recognized text, the language code or the sound's name (empty for a speaker turn, which only has its
 * [speaker]), [score] the rank among lines sounding at the same time (the tags).
 * zh-CN: 结果中随文件标签播放同步的一行 (路线图 D164): [index] 为其在结果列表中的行号, [text] 为识别文字, 语言代码或声音名称 (说话人轮次为空, 只有
 * [speaker]), [score] 为同时发声的行之间的排序依据 (标签).
 */
data class PlaybackCue(
    val index: Int,
    val startMillis: Long,
    val endMillis: Long,
    val text: String = "",
    val speaker: Int? = null,
    val score: Float = 0f,
)

/**
 * The lines of a [FileTranscriptRecord] in the order they start: the segments with text of the transcribing actions,
 * the speaker turns, the sentences of a language identification or the peak windows of the tags. Pure JVM.
 * zh-CN: [FileTranscriptRecord] 中按开始时间排列的各行: 转写类操作中有文字的分段, 说话人轮次, 语种识别的语句或标签的峰值窗口. 纯 JVM.
 */
class PlaybackCues(items: List<PlaybackCue>) {

    val items: List<PlaybackCue> = items.sortedBy { it.startMillis }

    /**
     * The line sounding at [millis], null in the silence between two: the highest score, then the latest start.
     * zh-CN: [millis] 时正在发声的行, 两行之间的空白为 null: 取分数最高者, 其次取开始最晚者.
     */
    fun at(millis: Long): PlaybackCue? {
        var best: PlaybackCue? = null
        for (cue in items) {
            if (cue.startMillis > millis) break
            if (millis < cue.endMillis && (best == null || cue.score >= best.score)) best = cue
        }
        return best
    }

    /**
     * The middle line of the lyrics view at [millis]: the sounding line, else the last one that started, else the first.
     * zh-CN: [millis] 时歌词式视图的中间一行: 正在发声的行, 否则为最后开始的行, 再否则为第一行.
     */
    fun lyric(millis: Long): PlaybackCue? = at(millis) ?: items.lastOrNull { it.startMillis <= millis } ?: items.firstOrNull()

    /** The line before [cue]. zh-CN: [cue] 的上一行. */
    fun before(cue: PlaybackCue): PlaybackCue? = items.getOrNull(items.indexOf(cue) - 1)

    /** The line after [cue]. zh-CN: [cue] 的下一行. */
    fun after(cue: PlaybackCue): PlaybackCue? = items.indexOf(cue).takeIf { it >= 0 }?.let { items.getOrNull(it + 1) }

    companion object {
        val NONE = PlaybackCues(emptyList())

        fun of(record: FileTranscriptRecord): PlaybackCues = PlaybackCues(
            when (val analysis = record.analysis.takeUnless { record.action.transcribes }) {
                null -> record.segments.mapIndexedNotNull { index, segment ->
                    segment.text.trim().takeIf { it.isNotEmpty() }?.let { PlaybackCue(index, segment.startMillis, segment.endMillis, it, record.segmentSpeakers.getOrNull(index)) }
                }
                is FileAnalysis.Speakers -> analysis.turns.mapIndexed { index, turn -> PlaybackCue(index, turn.startMillis, turn.endMillis, speaker = turn.speaker) }
                is FileAnalysis.Languages -> analysis.segments.mapIndexed { index, segment -> PlaybackCue(index, segment.startMillis, segment.endMillis, segment.language) }
                is FileAnalysis.Tags -> analysis.labels.mapIndexed { index, tag -> PlaybackCue(index, tag.startMillis, tag.endMillis, tag.label.name, score = tag.score) }
            },
        )
    }
}

/**
 * The fixed values of the file tab's player (roadmap D164). Pure JVM.
 * zh-CN: 文件标签播放器的固定取值 (路线图 D164). 纯 JVM.
 */
object PlaybackRules {
    const val SKIP_MILLIS = 5000L
    const val NORMAL_SPEED = 1f

    val SPEEDS = listOf(0.5f, 0.75f, NORMAL_SPEED, 1.25f, 1.5f, 2f)

    /** "0.75x", "1.5x", "2x". zh-CN: "0.75x", "1.5x", "2x". */
    fun speedLabel(speed: Float): String = "${String.format(Locale.ROOT, "%.2f", speed).trimEnd('0').trimEnd('.')}x"
}
