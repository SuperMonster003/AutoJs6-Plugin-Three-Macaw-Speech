package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VoiceSegment
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import kotlin.math.roundToLong

/** One speaker turn in milliseconds, speakers numbered from 0 in order of first appearance. zh-CN: 一个说话人轮次 (毫秒). */
data class SpeakerTurn(val startMillis: Long, val endMillis: Long, val speaker: Int)

/** A diarization: [turns] ascending, [speakers] found, [names] with `identify` (roadmap D147). zh-CN: 一次说话人分离的结果. */
data class Diarization(
    val turns: List<SpeakerTurn>,
    val speakers: Int,
    val durationMillis: Long,
    val modelId: String,
    val speakerModelId: String,
    val names: List<SpeakerName>? = null,
)

/** The enrolled speaker closest to one diarized speaker; [name] null below the threshold. zh-CN: 与某位分离出的说话人最接近的已注册说话人. */
data class SpeakerName(val speaker: Int, val name: String?, val score: Double?)

/** One sentence of `detectLanguage` and the language whisper heard in it. zh-CN: `detectLanguage` 的一句及其语种. */
data class LanguageSegment(val startMillis: Long, val endMillis: Long, val language: String)

/** A language identification (roadmap D148, D165): [candidates] are languages with their share of the speaking time, best first. zh-CN: 一次语种识别的结果. */
data class LanguageResult(
    val language: String,
    val candidates: List<Pair<String, Float>>,
    val segments: List<LanguageSegment>,
    val durationMillis: Long,
    val modelId: String,
)

/** One AudioSet label of the label table. zh-CN: 标签表中的一个 AudioSet 标签. */
data class TagLabel(val index: Int, val mid: String, val name: String)

/** A label with its score; [startMillis] / [endMillis] locate its best window. zh-CN: 带分数的标签; 起止毫秒为其得分最高的窗. */
data class TagScore(val label: TagLabel, val score: Float, val startMillis: Long, val endMillis: Long)

/** The labels of one window of the timeline. zh-CN: 时间线上一个窗的标签. */
data class TagWindow(val startMillis: Long, val endMillis: Long, val labels: List<Pair<TagLabel, Float>>)

/** An audio tagging (roadmap D149). zh-CN: 一次音频标签的结果. */
data class TagResult(
    val labels: List<TagScore>,
    val timeline: List<TagWindow>?,
    val topK: Int,
    val durationMillis: Long,
    val modelId: String,
)

/**
 * The pure rules of the offline analyses (roadmap D146 - D151, D165): speaker turns renumbered and rounded, the sentences
 * of language identification and their vote, the windows of audio tagging and the aggregation of their scores, and the
 * AudioSet label table of a tagger. Pure Kotlin.
 * zh-CN: 离线分析的纯规则 (路线图 D146 - D151, D165): 说话人轮次的重新编号与取整, 语种识别的语句及其投票, 音频标签的窗口与分数汇总, 以及标签模型的
 * AudioSet 标签表. 纯 Kotlin.
 */
object AnalysisRules {

    /** Shortest last window of audio tagging; a shorter tail joins nothing and is dropped (unless it is the whole file). zh-CN: 音频标签最后一个窗的最短长度. */
    const val MIN_TAG_WINDOW_MILLIS = 1000L

    /** A sentence of language identification with less speech joins a neighbor (roadmap D168, measured). zh-CN: 语音少于此值的语句并入相邻句 (路线图 D168, 实测). */
    const val MIN_SENTENCE_MILLIS = 1250L

    /** Speech of each diarized speaker that `identify` embeds at most (its longest turns first). zh-CN: `identify` 为每位说话人至多提取的语音. */
    const val IDENTIFY_SPEECH_MILLIS = 10_000L

    /**
     * Speaker turns from the engine's segments (seconds, speakers numbered by the clustering): sorted by start,
     * milliseconds rounded, empty turns dropped, speakers renumbered in order of first appearance. More than
     * `MAX_DIARIZATION_SEGMENTS` turns are `LIMIT_EXCEEDED`.
     * zh-CN: 由引擎的分段 (秒, 说话人按聚类编号) 得到说话人轮次: 按开始排序, 毫秒取整, 去掉空轮次, 说话人按首次出现重新编号. 超过
     * `MAX_DIARIZATION_SEGMENTS` 个轮次为 `LIMIT_EXCEEDED`.
     */
    fun turns(segments: List<Triple<Float, Float, Int>>): List<SpeakerTurn> {
        val sorted = segments.map { (start, end, speaker) -> Triple(millis(start), millis(end), speaker) }
            .filter { it.second > it.first }
            .sortedWith(compareBy({ it.first }, { it.second }))
        if (sorted.size > SpeechLimits.MAX_DIARIZATION_SEGMENTS) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The diarization found ${sorted.size} speaker turns; at most ${SpeechLimits.MAX_DIARIZATION_SEGMENTS} are answered")
        }
        val numbers = LinkedHashMap<Int, Int>()
        return sorted.map { (start, end, speaker) -> SpeakerTurn(start, end, numbers.getOrPut(speaker) { numbers.size }) }
    }

    /**
     * The audio `identify` embeds for [speaker]: its longest turns until [IDENTIFY_SPEECH_MILLIS], each cut to what is
     * left, in time order. Empty when the speaker has no turn.
     * zh-CN: `identify` 为 [speaker] 提取的音频: 其最长的轮次, 直到 [IDENTIFY_SPEECH_MILLIS], 每段截到剩余长度, 按时间顺序.
     */
    fun identifySpans(turns: List<SpeakerTurn>, speaker: Int): List<LongRange> {
        var left = IDENTIFY_SPEECH_MILLIS
        val picked = ArrayList<SpeakerTurn>()
        for (turn in turns.filter { it.speaker == speaker }.sortedByDescending { it.endMillis - it.startMillis }) {
            if (left <= 0) break
            val length = minOf(turn.endMillis - turn.startMillis, left)
            picked += turn.copy(endMillis = turn.startMillis + length)
            left -= length
        }
        return picked.sortedBy { it.startMillis }.map { it.startMillis until it.endMillis }
    }

    /** The samples of [spans] (milliseconds) of [samples]. zh-CN: [samples] 中 [spans] (毫秒) 的采样. */
    fun cut(samples: FloatArray, spans: List<LongRange>): FloatArray {
        val pieces = spans.map { span ->
            val from = AudioInput.samples(span.first).toInt().coerceIn(0, samples.size)
            val to = AudioInput.samples(span.last + 1).toInt().coerceIn(from, samples.size)
            from to to
        }
        val out = FloatArray(pieces.sumOf { it.second - it.first })
        var offset = 0
        pieces.forEach { (from, to) ->
            System.arraycopy(samples, from, out, offset, to - from)
            offset += to - from
        }
        return out
    }

    /**
     * The sentences of language identification (roadmap D148, D165). The windows stay the budget of D148: at most
     * `LANGUAGE_WINDOW_MILLIS` of speech each and at most [limit] of them, a VAD segment that does not fit starts the next
     * window and only one longer than a window is cut. Inside a window every VAD segment is a sentence, and one with less
     * than [MIN_SENTENCE_MILLIS] of speech joins the neighbor it is closer to in time. Windows never share a sentence, so
     * a sentence holds at most one window of speech, and what is alone in its window stays as it is. A sentence keeps the
     * time of its first and last sample; its speech leaves out the silence inside it.
     * zh-CN: 语种识别的语句 (路线图 D148, D165). 窗仍是 D148 的预算: 每窗至多 `LANGUAGE_WINDOW_MILLIS` 语音, 至多 [limit] 个窗, 放不下的 VAD 语音段
     * 另起一窗, 只有比窗长的才被切开. 窗内每个 VAD 语音段是一句, 语音不足 [MIN_SENTENCE_MILLIS] 的句并入时间上更近的相邻句. 句不跨窗, 所以一句至多含一窗
     * 语音, 窗内只有一句时保持原样. 句保留其首尾采样的时间; 其语音不含句内的静音.
     */
    class SpeechWindows(private val limit: Int) {
        private val maxSamples = AudioInput.samples(SpeechLimits.LANGUAGE_WINDOW_MILLIS).toInt()
        private val minSamples = AudioInput.samples(MIN_SENTENCE_MILLIS).toInt()
        private val windows = ArrayList<List<Pair<LongRange, FloatArray>>>()
        private var sentences = ArrayList<Pair<LongRange, FloatArray>>()
        private var size = 0

        val isFull: Boolean get() = windows.size >= limit

        fun accept(segment: VoiceSegment) {
            if (isFull) return
            // A segment that does not fit the rest of the window starts a new one; only a segment longer than a window is cut.
            if (size > 0 && size + segment.samples.size > maxSamples) close()
            var offset = 0
            while (offset < segment.samples.size && !isFull) {
                val take = minOf(segment.samples.size - offset, maxSamples - size)
                sentences += (segment.start + offset until segment.start + offset + take) to segment.samples.copyOfRange(offset, offset + take)
                size += take
                offset += take
                if (size == maxSamples) close()
            }
        }

        /** The sentences in time order, as sample ranges and their speech. zh-CN: 按时间顺序的语句, 采样范围及其语音. */
        fun finish(): List<Pair<LongRange, FloatArray>> {
            if (size > 0 && !isFull) close()
            return windows.flatMap(::joined)
        }

        private fun close() {
            if (size == 0) return
            windows += sentences
            sentences = ArrayList()
            size = 0
        }

        private fun joined(window: List<Pair<LongRange, FloatArray>>): List<Pair<LongRange, FloatArray>> {
            val out = ArrayList(window)
            while (out.size > 1) {
                val short = out.indexOfFirst { it.second.size < minSamples }
                if (short < 0) break
                val before = if (short > 0) out[short].first.first - out[short - 1].first.last else Long.MAX_VALUE
                val after = if (short < out.lastIndex) out[short + 1].first.first - out[short].first.last else Long.MAX_VALUE
                val left = if (before <= after) short - 1 else short
                out[left] = out[left].first.first..out[left + 1].first.last to out[left].second + out[left + 1].second
                out.removeAt(left + 1)
            }
            return out
        }
    }

    /**
     * The vote of roadmap D165: the language spoken longest over [segments] (their spans, silence inside a joined sentence
     * included, so a script can repeat the sums), a tie going to the language heard first; candidates by that time (then
     * first appearance) with their share of it. Empty [segments] are `AUDIO_TOO_SHORT`.
     * zh-CN: 路线图 D165 的投票: [segments] 中说话时长最长的语种 (按其时间跨度计, 含合并句内的静音, 脚本可以复算), 平票时取最先听到的语种; 候选按时长
     * (再按首次出现) 排序, 附时长占比. 没有语句时为 `AUDIO_TOO_SHORT`.
     */
    fun vote(segments: List<LanguageSegment>): List<Pair<String, Float>> {
        if (segments.isEmpty()) throw SpeechException(SpeechErrorCodes.AUDIO_TOO_SHORT, "The audio has no speech to identify the language of")
        val first = LinkedHashMap<String, Int>()
        segments.forEachIndexed { index, segment -> first.putIfAbsent(segment.language, index) }
        val spoken = segments.groupingBy { it.language }.fold(0L) { sum, segment -> sum + segment.endMillis - segment.startMillis }
        val total = spoken.values.sum().coerceAtLeast(1).toFloat()
        return first.keys.sortedWith(compareByDescending<String> { spoken.getValue(it) }.thenBy { first.getValue(it) })
            .map { it to spoken.getValue(it) / total }
    }

    /**
     * The windows of audio tagging over [totalSamples] (roadmap D149): `TAG_WINDOW_MILLIS` each, the last one kept when
     * it holds [MIN_TAG_WINDOW_MILLIS] or is the whole file.
     * zh-CN: 音频标签在 [totalSamples] 上的窗 (路线图 D149): 每个 `TAG_WINDOW_MILLIS`, 最后一个窗满 [MIN_TAG_WINDOW_MILLIS] 或为整个文件时保留.
     */
    fun tagWindows(totalSamples: Long): List<LongRange> {
        val window = AudioInput.samples(SpeechLimits.TAG_WINDOW_MILLIS)
        val minimum = AudioInput.samples(MIN_TAG_WINDOW_MILLIS)
        val out = ArrayList<LongRange>()
        var start = 0L
        while (start < totalSamples) {
            val end = minOf(totalSamples, start + window)
            if (end - start >= minimum || start == 0L) out += start until end
            start = end
        }
        return out
    }

    /**
     * Each label's best window and score over [windows] (the scores of every label per window), the [topK] best
     * labels by that score; the timeline keeps each window's [topK] labels.
     * zh-CN: 每个标签在 [windows] (每窗所有标签的分数) 中得分最高的窗与分数, 取得分最高的 [topK] 个标签; 时间线保留每窗的前 [topK] 个标签.
     */
    fun aggregate(windows: List<Pair<LongRange, List<Pair<TagLabel, Float>>>>, topK: Int, timeline: Boolean): Pair<List<TagScore>, List<TagWindow>?> {
        val best = LinkedHashMap<Int, TagScore>()
        windows.forEach { (range, scores) ->
            scores.forEach { (label, score) ->
                val current = best[label.index]
                if (current == null || score > current.score) {
                    best[label.index] = TagScore(label, score, AudioInput.millis(range.first), AudioInput.millis(range.last + 1))
                }
            }
        }
        val labels = best.values.sortedWith(compareByDescending<TagScore> { it.score }.thenBy { it.label.index }).take(topK)
        val windowsOut = if (timeline) {
            windows.map { (range, scores) ->
                TagWindow(
                    AudioInput.millis(range.first),
                    AudioInput.millis(range.last + 1),
                    scores.sortedWith(compareByDescending<Pair<TagLabel, Float>> { it.second }.thenBy { it.first.index }).take(topK),
                )
            }
        } else {
            null
        }
        return labels to windowsOut
    }

    /**
     * The AudioSet label table of a tagger (`class_labels_indices.csv`: `index,mid,display_name`, quoted names with
     * commas); an unreadable table is `MODEL_MISSING`, since the model's `labels` file is broken.
     * zh-CN: 标签模型的 AudioSet 标签表 (`class_labels_indices.csv`: `index,mid,display_name`, 名称带逗号时加引号); 无法读取时为 `MODEL_MISSING`.
     */
    fun labels(csv: String): List<TagLabel> {
        val lines = csv.lineSequence().map { it.trimEnd('\r') }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty() || !lines.first().startsWith("index")) throw SpeechException(SpeechErrorCodes.MODEL_MISSING, "The label table of the tagger is not readable")
        return lines.drop(1).mapIndexed { row, line ->
            val fields = fields(line)
            val index = fields.getOrNull(0)?.toIntOrNull()
            if (fields.size < 3 || index != row) throw SpeechException(SpeechErrorCodes.MODEL_MISSING, "Line ${row + 2} of the tagger's label table is not readable")
            TagLabel(index, fields[1], fields[2])
        }
    }

    private fun fields(line: String): List<String> {
        val out = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoted && c == '"' && line.getOrNull(i + 1) == '"' -> {
                    field.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> {
                    out += field.toString()
                    field.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        out += field.toString()
        return out
    }

    private fun millis(seconds: Float): Long = (seconds.toDouble() * 1000).roundToLong().coerceAtLeast(0)
}
