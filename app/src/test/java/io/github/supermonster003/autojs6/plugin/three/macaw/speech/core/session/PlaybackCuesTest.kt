package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageSegment
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerTurn
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagLabel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lines that follow the playback (roadmap D164): each action's result, the silence between two lines, overlapping
 * turns and tags, the lyrics line and the speed labels.
 * zh-CN: 随播放同步的行 (路线图 D164): 各操作的结果, 两行之间的空白, 重叠的轮次与标签, 歌词行与倍速标签.
 */
class PlaybackCuesTest {

    private fun record(action: FileAction, segments: List<Utterance> = emptyList(), analysis: FileAnalysis? = null, speakers: List<Int> = emptyList()) = FileTranscriptRecord(
        state = FileTranscriptRecord.State.COMPLETED,
        displayName = "a.wav",
        sizeBytes = 1,
        modelId = "m",
        language = null,
        punctuation = false,
        declaredMillis = 10_000,
        recognizedMillis = 10_000,
        segments = segments,
        action = action,
        analysis = analysis,
        segmentSpeakers = speakers,
    )

    private val segments = listOf(
        Utterance("one", 0, 500, 2000),
        Utterance("  ", 1, 2000, 2500),
        Utterance(" two ", 2, 3000, 5000),
    )

    @Test
    fun `segments without text are no lines and the rest keep their rows`() {
        val cues = PlaybackCues.of(record(FileAction.TRANSCRIBE, segments))
        assertEquals(listOf(0, 2), cues.items.map { it.index })
        assertEquals(listOf("one", "two"), cues.items.map { it.text })
    }

    @Test
    fun `the silence between two lines has none and a line ends before its end time`() {
        val cues = PlaybackCues.of(record(FileAction.TRANSCRIBE, segments))
        assertNull(cues.at(0))
        assertEquals("one", cues.at(500)?.text)
        assertEquals("one", cues.at(1999)?.text)
        assertNull(cues.at(2000))
        assertNull(cues.at(2999))
        assertEquals("two", cues.at(3000)?.text)
        assertNull(cues.at(5000))
    }

    @Test
    fun `the lyrics line stays on the last line in a gap and starts on the first`() {
        val cues = PlaybackCues.of(record(FileAction.TRANSCRIBE, segments))
        assertEquals("one", cues.lyric(0)?.text)
        assertEquals("one", cues.lyric(2500)?.text)
        assertEquals("two", cues.lyric(9000)?.text)
        val first = cues.items.first()
        val last = cues.items.last()
        assertNull(cues.before(first))
        assertEquals(last, cues.after(first))
        assertEquals(first, cues.before(last))
        assertNull(cues.after(last))
        assertNull(PlaybackCues.NONE.lyric(0))
    }

    @Test
    fun `the speakers of diarize and transcribe stay with their segments`() {
        val cues = PlaybackCues.of(
            record(
                FileAction.DIARIZE_TRANSCRIBE,
                segments,
                FileAnalysis.Speakers(listOf(SpeakerTurn(0, 2500, 1), SpeakerTurn(3000, 5000, 0)), 2, "s"),
                listOf(1, 1, 0),
            ),
        )
        assertEquals(listOf(1, 0), cues.items.map { it.speaker })
        assertEquals(listOf("one", "two"), cues.items.map { it.text })
    }

    @Test
    fun `overlapping turns show the one that started last and rows keep their order`() {
        val turns = listOf(SpeakerTurn(5000, 7000, 0), SpeakerTurn(0, 6000, 1), SpeakerTurn(5500, 5800, 2))
        val cues = PlaybackCues.of(record(FileAction.DIARIZE, analysis = FileAnalysis.Speakers(turns, 3, "s")))
        assertEquals("in order of start", listOf(1, 0, 2), cues.items.map { it.index })
        assertEquals(1, cues.at(4000)?.speaker)
        assertEquals(0, cues.at(5200)?.speaker)
        assertEquals(2, cues.at(5600)?.speaker)
        assertEquals(0, cues.at(5900)?.speaker)
        assertTrue(cues.items.all { it.text.isEmpty() })
    }

    @Test
    fun `languages give a line per sentence`() {
        val analysis = FileAnalysis.Languages("zh", listOf("zh" to 1f), listOf(LanguageSegment(0, 1000, "zh"), LanguageSegment(1500, 2000, "en")))
        val cues = PlaybackCues.of(record(FileAction.LANGUAGE, analysis = analysis))
        assertEquals("zh", cues.at(500)?.text)
        assertNull(cues.at(1200))
        assertEquals("en", cues.at(1600)?.text)
    }

    @Test
    fun `the peak tag at a moment is the highest score of the windows around it`() {
        fun tag(index: Int, name: String, score: Float, start: Long, end: Long) = TagScore(TagLabel(index, "/m/$index", name), score, start, end)
        val labels = listOf(tag(0, "Speech", 0.9f, 0, 10_000), tag(1, "Music", 0.4f, 0, 10_000), tag(2, "Dog", 0.95f, 10_000, 20_000), tag(3, "Wind", 0.2f, 5000, 15_000))
        val cues = PlaybackCues.of(record(FileAction.TAG, analysis = FileAnalysis.Tags(labels)))
        assertEquals("Speech", cues.at(1000)?.text)
        assertEquals("Speech", cues.at(7000)?.text)
        assertEquals("Dog", cues.at(12_000)?.text)
        assertEquals(0.95f, cues.at(12_000)?.score)
        assertNull(cues.at(25_000))
    }

    @Test
    fun `a record without a result has no lines`() {
        assertTrue(PlaybackCues.of(record(FileAction.TRANSCRIBE)).items.isEmpty())
        assertTrue(PlaybackCues.of(record(FileAction.LANGUAGE)).items.isEmpty())
    }

    @Test
    fun `the speeds run from half to double and read without trailing zeros`() {
        assertEquals(0.5f, PlaybackRules.SPEEDS.first())
        assertEquals(2f, PlaybackRules.SPEEDS.last())
        assertEquals(listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"), PlaybackRules.SPEEDS.map(PlaybackRules::speedLabel))
    }
}
