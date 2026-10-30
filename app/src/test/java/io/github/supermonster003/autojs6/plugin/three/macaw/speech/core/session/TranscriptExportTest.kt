package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageSegment
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerTurn
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The text forms of the file tab (roadmap D112, D113, P7.8.1): page times, SRT cues with their times, CRLF and
 * numbering, plain text lines, blank segments and default file names; since D153 the speaker labels of "diarize and
 * transcribe" and the turn list of a diarization.
 * zh-CN: 文件标签的文本形式 (路线图 D112, D113, P7.8.1): 页面时间, SRT 字幕条的时间, CRLF 与序号, 纯文本行, 空段与默认文件名; D153 起还有
 * "分离 + 转写" 的说话人标签与说话人分离的轮次列表.
 */
class TranscriptExportTest {

    private fun segment(text: String, start: Long, end: Long, index: Int = 0) = Utterance(text, index, start, end)

    @Test
    fun `page times round to a tenth and progress to whole seconds`() {
        assertEquals("00:00.0", TranscriptExport.segmentClock(0))
        assertEquals("00:00.0", TranscriptExport.segmentClock(49))
        assertEquals("00:00.1", TranscriptExport.segmentClock(50))
        assertEquals("00:01.0", TranscriptExport.segmentClock(999))
        assertEquals("01:00.0", TranscriptExport.segmentClock(59_950))
        assertEquals("30:00.0", TranscriptExport.segmentClock(1_800_000))
        assertEquals("00:00.0", TranscriptExport.segmentClock(-5))
        assertEquals("00:59", TranscriptExport.clock(59_999))
        assertEquals("01:00", TranscriptExport.clock(60_000))
        assertEquals("30:00", TranscriptExport.clock(1_800_000))
    }

    @Test
    fun `SRT times carry milliseconds into seconds, minutes and hours`() {
        assertEquals("00:00:00,000", TranscriptExport.srtTime(0))
        assertEquals("00:00:00,999", TranscriptExport.srtTime(999))
        assertEquals("00:00:01,000", TranscriptExport.srtTime(1000))
        assertEquals("00:00:59,999", TranscriptExport.srtTime(59_999))
        assertEquals("00:01:00,000", TranscriptExport.srtTime(60_000))
        assertEquals("00:59:59,999", TranscriptExport.srtTime(3_599_999))
        assertEquals("01:00:00,000", TranscriptExport.srtTime(3_600_000))
    }

    @Test
    fun `SRT has one numbered cue per segment with CRLF and skips blank segments`() {
        val segments = listOf(
            segment(" hello there ", 120, 1830, 0),
            segment("   ", 1900, 2000, 1),
            segment("second\nline", 61_005, 62_500, 2),
            segment("late end", 70_000, 69_000, 3),
        )
        val expected = "1\r\n00:00:00,120 --> 00:00:01,830\r\nhello there\r\n\r\n" +
            "2\r\n00:01:01,005 --> 00:01:02,500\r\nsecond line\r\n\r\n" +
            "3\r\n00:01:10,000 --> 00:01:10,000\r\nlate end\r\n\r\n"
        assertEquals(expected, String(TranscriptExport.srt(segments), Charsets.UTF_8))
        assertEquals("", String(TranscriptExport.srt(listOf(segment("", 0, 10))), Charsets.UTF_8))
    }

    @Test
    fun `text has one line per segment without times and is UTF-8 without a byte order mark`() {
        val segments = listOf(segment("\u4f60\u597d, \u4e16\u754c", 0, 900), segment(" ", 900, 1000), segment("good night", 1000, 1800))
        val bytes = TranscriptExport.text(segments)
        assertEquals("\u4f60\u597d, \u4e16\u754c\ngood night\n", String(bytes, Charsets.UTF_8))
        assertArrayEquals("\u4f60".toByteArray(Charsets.UTF_8), bytes.copyOf(3))
        assertEquals(0, TranscriptExport.text(emptyList()).size)
    }

    @Test
    fun `saved files take the picked file's name with their own extension`() {
        assertEquals("talk.srt", TranscriptExport.fileName("talk.mp3", TranscriptExport.SRT_EXTENSION))
        assertEquals("talk.final.txt", TranscriptExport.fileName("talk.final.m4a", TranscriptExport.TEXT_EXTENSION))
        assertEquals("talk.txt", TranscriptExport.fileName("talk", TranscriptExport.TEXT_EXTENSION))
        assertEquals(".hidden.srt", TranscriptExport.fileName(".hidden", TranscriptExport.SRT_EXTENSION))
        assertEquals("transcript.srt", TranscriptExport.fileName("  ", TranscriptExport.SRT_EXTENSION))
        assertEquals("transcript.txt", TranscriptExport.fileName(null, TranscriptExport.TEXT_EXTENSION))
    }

    @Test
    fun `speaker labels go before the text of each segment and the turns list one per line`() {
        val segments = listOf(segment(" hello ", 0, 900), segment(" ", 900, 1000, 1), segment("bye", 1500, 2100, 2), segment("tail", 2200, 2400, 3))
        val labeled = TranscriptExport.labeled(segments, listOf(0, 1, 1)) { speaker, text -> "S${speaker + 1}: $text" }
        assertEquals(listOf("S1: hello", " ", "S2: bye", "tail"), labeled.map { it.text })
        assertEquals(segments.map { it.startMillis }, labeled.map { it.startMillis })
        assertEquals("S1: hello\nS2: bye\ntail\n", String(TranscriptExport.text(labeled), Charsets.UTF_8))
        val turns = listOf(SpeakerTurn(0, 1500, 0), SpeakerTurn(1700, 4150, 1), SpeakerTurn(5000, 4900, 0))
        assertEquals("00:00.0 - 00:01.5 A\n00:01.7 - 00:04.2 B\n00:05.0 - 00:05.0 A", TranscriptExport.turns(turns) { if (it == 0) "A" else "B" })
        assertEquals("", TranscriptExport.turns(emptyList()) { "x" })
        val sentences = listOf(LanguageSegment(0, 1_500, "zh"), LanguageSegment(1_700, 4_200, "en"))
        assertEquals("00:00.0 - 00:01.5 ZH\n00:01.7 - 00:04.2 EN", TranscriptExport.languages(sentences) { it.uppercase() })
    }
}
