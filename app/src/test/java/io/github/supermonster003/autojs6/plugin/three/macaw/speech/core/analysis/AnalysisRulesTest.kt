package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VoiceSegment
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure rules of the offline analyses (roadmap D146 - D151, D155, D165): speaker turns, the audio `identify` embeds, the
 * sentences and vote of language identification, the windows and scores of audio tagging and the AudioSet label table.
 * zh-CN: 离线分析的纯规则 (路线图 D146 - D151, D155, D165): 说话人轮次, `identify` 提取的音频, 语种识别的语句与投票, 音频标签的窗口与分数, 以及
 * AudioSet 标签表.
 */
class AnalysisRulesTest {

    private fun code(block: () -> Unit): String = assertThrows(SpeechException::class.java) { block() }.code

    private fun speech(startMillis: Long, millis: Long, value: Float = 1f) =
        VoiceSegment(AudioInput.samples(startMillis), FloatArray(AudioInput.samples(millis).toInt()) { value })

    private val labels = listOf(TagLabel(0, "/m/09x0r", "Speech"), TagLabel(1, "/m/04rlf", "Music"), TagLabel(2, "/m/0jbk", "Animal"))

    @Test
    fun `turns are sorted, rounded and renumbered in order of first appearance`() {
        val turns = AnalysisRules.turns(
            listOf(
                Triple(4.2f, 6.0004f, 0),
                Triple(0.0f, 1.5f, 3),
                Triple(1.6f, 1.6f, 0),
                Triple(1.7f, 4.1f, 0),
                Triple(6.1f, 7f, 3),
            ),
        )
        assertEquals(
            listOf(SpeakerTurn(0, 1500, 0), SpeakerTurn(1700, 4100, 1), SpeakerTurn(4200, 6000, 1), SpeakerTurn(6100, 7000, 0)),
            turns,
        )
        assertEquals(emptyList<SpeakerTurn>(), AnalysisRules.turns(emptyList()))
    }

    @Test
    fun `more turns than the limit are LIMIT_EXCEEDED`() {
        val many = (0..SpeechLimits.MAX_DIARIZATION_SEGMENTS).map { Triple(it.toFloat(), it + 0.5f, it % 2) }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, code { AnalysisRules.turns(many) })
        assertEquals(SpeechLimits.MAX_DIARIZATION_SEGMENTS, AnalysisRules.turns(many.drop(1)).size)
    }

    @Test
    fun `identify takes the longest turns of a speaker up to ten seconds, in time order`() {
        val turns = listOf(
            SpeakerTurn(0, 3_000, 0),
            SpeakerTurn(3_000, 9_000, 1),
            SpeakerTurn(9_000, 16_000, 0),
            SpeakerTurn(16_000, 17_000, 0),
            SpeakerTurn(20_000, 25_000, 0),
        )
        // 7 s, then 5 s cut to the 3 s left. zh-CN: 先取 7 秒, 再把 5 秒截为剩余的 3 秒.
        assertEquals(listOf(9_000L until 16_000L, 20_000L until 23_000L), AnalysisRules.identifySpans(turns, 0))
        assertEquals(listOf(3_000L until 9_000L), AnalysisRules.identifySpans(turns, 1))
        assertEquals(emptyList<LongRange>(), AnalysisRules.identifySpans(turns, 2))
    }

    @Test
    fun `cut joins the samples of the spans and stays within the audio`() {
        val samples = FloatArray(AudioInput.samples(1_000).toInt()) { it.toFloat() }
        val out = AnalysisRules.cut(samples, listOf(0L until 1L, 999L until 2_000L))
        assertEquals(16 + 16, out.size)
        assertArrayEquals(FloatArray(16) { it.toFloat() }, out.copyOfRange(0, 16), 0f)
        assertArrayEquals(FloatArray(16) { (samples.size - 16 + it).toFloat() }, out.copyOfRange(16, 32), 0f)
    }

    private fun ranges(sentences: List<Pair<LongRange, FloatArray>>) = sentences.map { (range, _) -> AudioInput.millis(range.first) to AudioInput.millis(range.last + 1) }

    private fun speechMillis(sentences: List<Pair<LongRange, FloatArray>>) = sentences.map { (_, speech) -> AudioInput.millis(speech.size.toLong()) }

    @Test
    fun `every segment of a window is a sentence and only a segment longer than a window is cut`() {
        val windows = AnalysisRules.SpeechWindows(10)
        windows.accept(speech(0, 10_000))
        windows.accept(speech(12_000, 15_000))
        // 25 s so far: 8 s more do not fit, so they start the next window. zh-CN: 已有 25 秒, 再来 8 秒放不下, 另起新窗.
        windows.accept(speech(30_000, 8_000))
        // 70 s of speech do not fit either: the 8 s window closes and the segment is cut into 30, 30 and 10 s.
        // zh-CN: 70 秒语音同样放不下: 8 秒的窗结束, 该语音段被切成 30, 30 与 10 秒.
        windows.accept(speech(40_000, 70_000))
        val out = windows.finish()
        assertEquals(listOf(0L to 10_000L, 12_000L to 27_000L, 30_000L to 38_000L, 40_000L to 70_000L, 70_000L to 100_000L, 100_000L to 110_000L), ranges(out))
        assertEquals(listOf(10_000L, 15_000L, 8_000L, 30_000L, 30_000L, 10_000L), speechMillis(out))
    }

    @Test
    fun `speech windows stop at their limit`() {
        val windows = AnalysisRules.SpeechWindows(2)
        windows.accept(speech(0, 75_000))
        assertTrue(windows.isFull)
        windows.accept(speech(80_000, 5_000))
        assertEquals(listOf(30_000L, 30_000L), speechMillis(windows.finish()))
        assertEquals(emptyList<Pair<LongRange, FloatArray>>(), AnalysisRules.SpeechWindows(3).finish())
    }

    @Test
    fun `sentences stay whole when the limit stops the file`() {
        val windows = AnalysisRules.SpeechWindows(1)
        windows.accept(speech(0, 20_000))
        windows.accept(speech(21_000, 9_000))
        windows.accept(speech(31_000, 10_000))
        // The first window holds 29 s, the third segment would be the second window. zh-CN: 第一窗 29 秒, 第三段属于第二窗.
        assertTrue(windows.isFull)
        windows.accept(speech(42_000, 5_000))
        assertEquals(listOf(0L to 20_000L, 21_000L to 30_000L), ranges(windows.finish()))
    }

    @Test
    fun `a short sentence joins the neighbor it is closer to, without the silence between them`() {
        val windows = AnalysisRules.SpeechWindows(3)
        windows.accept(speech(0, 3_000, 1f))
        windows.accept(speech(3_500, 600, 2f))
        windows.accept(speech(4_300, 4_000, 3f))
        windows.accept(speech(10_000, 500, 4f))
        windows.accept(speech(12_000, 2_000, 5f))
        val out = windows.finish()
        // 600 ms: 200 ms to what follows, 500 ms to what comes before; 500 ms: 1.5 s against 1.7 s, so it goes with the next one too.
        // zh-CN: 600 毫秒的句距后句 200 毫秒, 距前句 500 毫秒; 500 毫秒的句距后句 1.5 秒, 距前句 1.7 秒, 也并入后句.
        assertEquals(listOf(0L to 3_000L, 3_500L to 8_300L, 10_000L to 14_000L), ranges(out))
        assertEquals(listOf(3_000L, 4_600L, 2_500L), speechMillis(out))
        assertEquals(2f, out[1].second.first(), 0f)
        assertEquals(3f, out[1].second.last(), 0f)
    }

    @Test
    fun `a tie goes to the sentence before and the first and last short sentences have one neighbor`() {
        val windows = AnalysisRules.SpeechWindows(3)
        windows.accept(speech(0, 500))
        windows.accept(speech(1_000, 2_000))
        windows.accept(speech(3_500, 500))
        windows.accept(speech(4_500, 2_000))
        windows.accept(speech(7_000, 300))
        assertEquals(listOf(0L to 4_000L, 4_500L to 7_300L), ranges(windows.finish()))
    }

    @Test
    fun `short sentences join each other until they reach the shortest sentence`() {
        val windows = AnalysisRules.SpeechWindows(3)
        listOf(0L, 600L, 1_200L).forEach { windows.accept(speech(it, 400)) }
        assertEquals(listOf(0L to 1_600L), ranges(windows.finish()))
        val alone = AnalysisRules.SpeechWindows(3)
        alone.accept(speech(0, AnalysisRules.MIN_SENTENCE_MILLIS - 100))
        assertEquals(listOf(0L to AnalysisRules.MIN_SENTENCE_MILLIS - 100), ranges(alone.finish()))
        val exact = AnalysisRules.SpeechWindows(3)
        exact.accept(speech(0, AnalysisRules.MIN_SENTENCE_MILLIS))
        exact.accept(speech(1_500, AnalysisRules.MIN_SENTENCE_MILLIS))
        assertEquals(2, exact.finish().size)
    }

    @Test
    fun `a short sentence stays inside its window`() {
        val windows = AnalysisRules.SpeechWindows(3)
        windows.accept(speech(0, 20_000))
        windows.accept(speech(20_500, 9_800))
        // 500 ms do not fit the 200 ms left, so they start the next window and join what follows there, however close
        // the previous sentence is. zh-CN: 500 毫秒放不下, 另起一窗并入窗内的后句, 无论前句多近.
        windows.accept(speech(30_500, 500))
        windows.accept(speech(32_000, 20_000))
        windows.accept(speech(53_000, 300))
        assertEquals(listOf(0L to 20_000L, 20_500L to 30_300L, 30_500L to 53_300L), ranges(windows.finish()))
        val last = AnalysisRules.SpeechWindows(3)
        last.accept(speech(0, 20_000))
        last.accept(speech(20_500, 9_800))
        last.accept(speech(30_500, 500))
        assertEquals(listOf(0L to 20_000L, 20_500L to 30_300L, 30_500L to 31_000L), ranges(last.finish()))
    }

    @Test
    fun `a sentence that does not fit the window moves whole to the next one`() {
        val windows = AnalysisRules.SpeechWindows(2)
        windows.accept(speech(0, 20_000))
        windows.accept(speech(21_000, 20_000))
        windows.accept(speech(42_000, 500))
        val out = windows.finish()
        assertEquals(listOf(0L to 20_000L, 21_000L to 42_500L), ranges(out))
        assertEquals(listOf(20_000L, 20_500L), speechMillis(out))
    }

    @Test
    fun `the vote goes by speaking time, a tie going to the language heard first`() {
        fun segment(start: Long, millis: Long, language: String) = LanguageSegment(start, start + millis, language)
        // Two short sentences of Chinese against one long one of English. zh-CN: 两句短中文对一句长英文.
        val mixed = listOf(segment(0, 1_000, "zh"), segment(1_500, 6_000, "en"), segment(8_000, 1_000, "zh"))
        assertEquals(listOf("en" to 0.75f, "zh" to 0.25f), AnalysisRules.vote(mixed))
        assertEquals(listOf("ja" to 0.5f, "ko" to 0.5f), AnalysisRules.vote(listOf(segment(0, 2_000, "ja"), segment(3_000, 2_000, "ko"))))
        assertEquals(listOf("zh" to 1f / 3, "en" to 1f / 3, "ru" to 1f / 3), AnalysisRules.vote(listOf("zh", "en", "ru").mapIndexed { index, language -> segment(index * 4_000L, 3_000, language) }))
        assertEquals(listOf("fr" to 1f), AnalysisRules.vote(listOf(segment(0, 900, "fr"))))
        assertEquals(SpeechErrorCodes.AUDIO_TOO_SHORT, code { AnalysisRules.vote(emptyList()) })
    }

    @Test
    fun `tag windows are ten seconds and a tail under one second is dropped unless it is the whole file`() {
        fun millis(windows: List<LongRange>) = windows.map { AudioInput.millis(it.first) to AudioInput.millis(it.last + 1) }
        assertEquals(listOf(0L to 10_000L, 10_000L to 20_000L, 20_000L to 25_000L), millis(AnalysisRules.tagWindows(AudioInput.samples(25_000))))
        assertEquals(listOf(0L to 10_000L, 10_000L to 20_000L), millis(AnalysisRules.tagWindows(AudioInput.samples(20_999))))
        assertEquals(listOf(0L to 10_000L, 10_000L to 11_000L), millis(AnalysisRules.tagWindows(AudioInput.samples(11_000))))
        assertEquals(listOf(0L to 500L), millis(AnalysisRules.tagWindows(AudioInput.samples(500))))
        assertEquals(emptyList<LongRange>(), AnalysisRules.tagWindows(0))
    }

    @Test
    fun `tag scores keep each label's best window and the timeline each window's best labels`() {
        val (speech, music, animal) = labels
        val windows = listOf(
            AudioInput.samples(0) until AudioInput.samples(10_000) to listOf(speech to 0.9f, music to 0.2f, animal to 0.1f),
            AudioInput.samples(10_000) until AudioInput.samples(14_000) to listOf(speech to 0.3f, music to 0.8f, animal to 0.8f),
        )
        val (best, none) = AnalysisRules.aggregate(windows, 2, timeline = false)
        assertEquals(listOf(TagScore(speech, 0.9f, 0, 10_000), TagScore(music, 0.8f, 10_000, 14_000)), best)
        assertNull(none)
        val (all, timeline) = AnalysisRules.aggregate(windows, 5, timeline = true)
        assertEquals(listOf(speech, music, animal), all.map { it.label })
        assertEquals(TagScore(animal, 0.8f, 10_000, 14_000), all[2])
        assertEquals(
            listOf(
                TagWindow(0, 10_000, listOf(speech to 0.9f, music to 0.2f, animal to 0.1f)),
                TagWindow(10_000, 14_000, listOf(music to 0.8f, animal to 0.8f, speech to 0.3f)),
            ),
            timeline,
        )
    }

    @Test
    fun `the label table reads quoted names and refuses a broken table`() {
        val csv = "index,mid,display_name\r\n0,/m/09x0r,\"Speech\"\r\n1,/m/0ytgt,\"Child speech, kid speaking\"\n2,/m/0x,\"Say \"\"hi\"\"\"\n\n"
        assertEquals(
            listOf(TagLabel(0, "/m/09x0r", "Speech"), TagLabel(1, "/m/0ytgt", "Child speech, kid speaking"), TagLabel(2, "/m/0x", "Say \"hi\"")),
            AnalysisRules.labels(csv),
        )
        listOf("", "0,/m/09x0r,Speech", "index,mid,display_name\n1,/m/09x0r,Speech", "index,mid,display_name\n0,/m/09x0r").forEach { broken ->
            assertEquals(broken, SpeechErrorCodes.MODEL_MISSING, code { AnalysisRules.labels(broken) })
        }
    }
}
