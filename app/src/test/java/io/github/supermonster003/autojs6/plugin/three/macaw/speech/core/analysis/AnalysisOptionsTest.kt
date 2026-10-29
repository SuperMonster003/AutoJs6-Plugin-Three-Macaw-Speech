package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.MapOptionSource
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The request options of the offline analyses (roadmap D147 - D149): defaults, the ranges of the host's script layer,
 * shapes, and the contract version a host stamps on these calls.
 * zh-CN: 离线分析的请求选项 (路线图 D147 - D149): 默认值, 与宿主脚本层一致的取值范围, 形态, 以及宿主为这些调用标记的契约版本.
 */
class AnalysisOptionsTest {

    private fun source(vararg pairs: Pair<String, Any?>) = MapOptionSource(mapOf(SpeechContract.KEY_CONTRACT_VERSION to SpeechContract.CALL_VERSION_2, *pairs))

    private fun code(block: () -> Unit): String = assertThrows(SpeechException::class.java) { block() }.code

    @Test
    fun `defaults apply to an empty request and to the plugin's own requests without a version`() {
        assertEquals(DiarizeOptions(), AnalysisOptions.diarize(null))
        assertEquals(DiarizeOptions(), AnalysisOptions.diarize(source()))
        assertEquals(LanguageOptions(windows = SpeechLimits.DEFAULT_LANGUAGE_WINDOWS), AnalysisOptions.language(MapOptionSource(emptyMap())))
        assertEquals(TagOptions(topK = SpeechLimits.DEFAULT_TAG_TOP_K, timeline = false), AnalysisOptions.tag(source()))
    }

    @Test
    fun `given options are read with their keys`() {
        assertEquals(
            DiarizeOptions("segment.pyannote.3-0", "speaker.eres2net.3dspeaker.en-voxceleb", 3, 0.9f, true, "talk.mp3"),
            AnalysisOptions.diarize(
                source(
                    SpeechOptionKeys.MODEL to "segment.pyannote.3-0",
                    SpeechOptionKeys.SPEAKER_MODEL to "speaker.eres2net.3dspeaker.en-voxceleb",
                    SpeechOptionKeys.SPEAKER_COUNT to 3,
                    SpeechOptionKeys.THRESHOLD to 0.9,
                    SpeechOptionKeys.IDENTIFY to true,
                    SpeechOptionKeys.DISPLAY_NAME to "talk.mp3",
                ),
            ),
        )
        assertEquals(LanguageOptions("langid.whisper.base.int8", 60), AnalysisOptions.language(source(SpeechOptionKeys.MODEL to "langid.whisper.base.int8", SpeechOptionKeys.WINDOWS to 60L)))
        assertEquals(TagOptions("tagger.ced.mini.int8", 20, true), AnalysisOptions.tag(source(SpeechOptionKeys.MODEL to "tagger.ced.mini.int8", SpeechOptionKeys.TOP_K to 20, SpeechOptionKeys.TIMELINE to true)))
        assertEquals(2f, AnalysisOptions.diarize(source(SpeechOptionKeys.THRESHOLD to 2)).threshold)
        assertEquals(0, AnalysisOptions.diarize(source(SpeechOptionKeys.SPEAKER_COUNT to 0)).speakers)
    }

    @Test
    fun `out of range and wrongly shaped options are INVALID_OPTIONS`() {
        listOf(
            { AnalysisOptions.diarize(source(SpeechOptionKeys.SPEAKER_COUNT to SpeechLimits.MAX_DIARIZATION_SPEAKERS + 1)) },
            { AnalysisOptions.diarize(source(SpeechOptionKeys.SPEAKER_COUNT to -1)) },
            { AnalysisOptions.diarize(source(SpeechOptionKeys.SPEAKER_COUNT to 2.5)) },
            { AnalysisOptions.diarize(source(SpeechOptionKeys.THRESHOLD to 2.01f)) },
            { AnalysisOptions.diarize(source(SpeechOptionKeys.THRESHOLD to -0.1)) },
            { AnalysisOptions.diarize(source(SpeechOptionKeys.THRESHOLD to Double.NaN)) },
            { AnalysisOptions.diarize(source(SpeechOptionKeys.THRESHOLD to "0.5")) },
            { AnalysisOptions.diarize(source(SpeechOptionKeys.IDENTIFY to 1)) },
            { AnalysisOptions.diarize(source(SpeechOptionKeys.SPEAKER_MODEL to "")) },
            { AnalysisOptions.language(source(SpeechOptionKeys.WINDOWS to 0)) },
            { AnalysisOptions.language(source(SpeechOptionKeys.WINDOWS to SpeechLimits.MAX_LANGUAGE_WINDOWS + 1)) },
            { AnalysisOptions.language(source(SpeechOptionKeys.WINDOWS to Long.MAX_VALUE)) },
            { AnalysisOptions.language(source(SpeechOptionKeys.MODEL to 7)) },
            { AnalysisOptions.tag(source(SpeechOptionKeys.TOP_K to 0)) },
            { AnalysisOptions.tag(source(SpeechOptionKeys.TOP_K to SpeechLimits.MAX_TAG_TOP_K + 1)) },
            { AnalysisOptions.tag(source(SpeechOptionKeys.TIMELINE to "yes")) },
            { AnalysisOptions.tag(source(SpeechOptionKeys.MODEL to "x".repeat(SpeechLimits.MAX_TOKEN_CHARS + 1))) },
        ).forEachIndexed { index, call -> assertEquals("case $index", SpeechErrorCodes.INVALID_OPTIONS, code { call() }) }
    }

    @Test
    fun `a request of contract version 1 or of an unknown version is PLUGIN_INCOMPATIBLE`() {
        listOf<Any?>(1, SpeechContract.MAX_CONTRACT_VERSION + 1, "2", null).forEach { version ->
            val request = MapOptionSource(mapOf(SpeechContract.KEY_CONTRACT_VERSION to version))
            assertEquals("$version", SpeechErrorCodes.PLUGIN_INCOMPATIBLE, code { AnalysisOptions.diarize(request) })
            assertEquals("$version", SpeechErrorCodes.PLUGIN_INCOMPATIBLE, code { AnalysisOptions.language(request) })
            assertEquals("$version", SpeechErrorCodes.PLUGIN_INCOMPATIBLE, code { AnalysisOptions.tag(request) })
        }
    }
}
