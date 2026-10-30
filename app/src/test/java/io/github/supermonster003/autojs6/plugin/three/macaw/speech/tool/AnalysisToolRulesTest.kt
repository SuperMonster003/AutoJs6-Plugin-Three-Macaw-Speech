package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.Diarization
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageSegment
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerName
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerTurn
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagLabel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagScore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.JsonNumber
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Roadmap P8.4.4 (D152): the arguments, results and limits of `speech_diarize`, `speech_detect_language` and `speech_tag`.
 * Results have the shapes of the script API with rounded scores and `warnings`, never a voiceprint; the diarization tool
 * refuses more than 10 minutes and a result over the contract's ceiling.
 * zh-CN: 路线图 P8.4.4 (D152): `speech_diarize`, `speech_detect_language` 与 `speech_tag` 的参数, 结果与上限. 结果与脚本 API 同形, 分数舍入并带
 * `warnings`, 从不含声纹; 分离工具拒绝超过 10 分钟的音频与超出契约上限的结果.
 */
class AnalysisToolRulesTest {

    private fun parse(json: String): Map<String, Any?> = StrictJson.parseObject(json.toByteArray(Charsets.UTF_8))

    private fun code(block: () -> Unit): String = assertThrows(PluginToolException::class.java) { block() }.code

    private fun long(value: Any?): Long? = (value as JsonNumber).longOrNull()

    @Test
    fun `arguments are read with the catalog's ranges`() {
        assertEquals(DiarizeToolRequest(), SpeechToolArguments.diarize(mapOf("threshold" to null)))
        assertEquals(DiarizeToolRequest(3, 0.9, true), SpeechToolArguments.diarize(mapOf("speakers" to 3, "threshold" to 0.9, "identify" to true)))
        assertEquals(DiarizeToolRequest(0, 2.0), SpeechToolArguments.diarize(mapOf("speakers" to 0.0, "threshold" to 2)))
        assertEquals(LanguageToolRequest(60), SpeechToolArguments.detectLanguage(mapOf("windows" to 60)))
        assertEquals(TagToolRequest(1), SpeechToolArguments.tag(mapOf("topK" to 1)))
        listOf(
            { SpeechToolArguments.diarize(mapOf("speakers" to 21)) },
            { SpeechToolArguments.diarize(mapOf("speakers" to -1)) },
            { SpeechToolArguments.diarize(mapOf("speakers" to 1.5)) },
            { SpeechToolArguments.diarize(mapOf("threshold" to 2.5)) },
            { SpeechToolArguments.diarize(mapOf("threshold" to "0.8")) },
            { SpeechToolArguments.diarize(mapOf("identify" to "yes")) },
            { SpeechToolArguments.detectLanguage(mapOf("windows" to 0)) },
            { SpeechToolArguments.detectLanguage(mapOf("windows" to 61)) },
            { SpeechToolArguments.tag(mapOf("topK" to 0)) },
            { SpeechToolArguments.tag(mapOf("topK" to 21)) },
        ).forEachIndexed { index, call -> assertEquals("case $index", PluginToolErrorCodes.INVALID_ARGUMENTS, code { call() }) }
    }

    @Test
    fun `the diarization result lists the turns and the names found`() {
        val diarization = Diarization(
            listOf(SpeakerTurn(0, 1500, 0), SpeakerTurn(1700, 4100, 1)), 2, 4200, "segment.pyannote.3-0", "speaker.eres2net.3dspeaker.en-voxceleb",
            listOf(SpeakerName(0, "Alice", 0.712345), SpeakerName(1, null, 0.31)),
        )
        val json = parse(AnalysisToolRules.diarizeResult(diarization))
        assertEquals(listOf("segments", "speakers", "durationMillis", "modelId", "speakerModelId", "names", "warnings"), json.keys.toList())
        @Suppress("UNCHECKED_CAST")
        val segments = json["segments"] as List<Map<String, Any?>>
        assertEquals(listOf(listOf(0L, 1500L, 0L), listOf(1700L, 4100L, 1L)), segments.map { listOf(long(it["startMillis"]), long(it["endMillis"]), long(it["speaker"])) })
        assertEquals(2L, long(json["speakers"]))
        @Suppress("UNCHECKED_CAST")
        val names = json["names"] as List<Map<String, Any?>>
        assertEquals("Alice", names[0]["name"])
        assertEquals(0.7123, (names[0]["score"] as JsonNumber).toDouble(), 0.0)
        assertFalse("no name below the threshold", names[1].containsKey("name"))
        assertFalse("identify off: no names", parse(AnalysisToolRules.diarizeResult(diarization.copy(names = null))).containsKey("names"))
        assertFalse("no voiceprint", AnalysisToolRules.diarizeResult(diarization).contains("embedding"))
    }

    @Test
    fun `a diarization over 10 minutes or over the result ceiling is refused`() {
        AnalysisToolRules.checkDiarizeLength(null)
        AnalysisToolRules.checkDiarizeLength(AnalysisToolRules.MAX_DIARIZE_MILLIS)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, code { AnalysisToolRules.checkDiarizeLength(AnalysisToolRules.MAX_DIARIZE_MILLIS + 1) })
        val many = Diarization((0 until 200).map { SpeakerTurn(it * 1000L, it * 1000L + 900, it % 2) }, 2, 200_000, "s", "e")
        assertEquals(PluginToolErrorCodes.LIMIT_EXCEEDED, code { AnalysisToolRules.diarizeResult(many, maxChars = 1000) })
    }

    @Test
    fun `the language and tag results have the shapes of the catalog`() {
        val language = parse(
            AnalysisToolRules.languageResult(
                LanguageResult("en", listOf("en" to 2f / 3, "zh" to 1f / 3), listOf(LanguageSegment(0, 30_000, "en")), 95_000, "langid.whisper.tiny.int8"),
            ),
        )
        assertEquals(listOf("language", "candidates", "segments", "durationMillis", "modelId", "warnings"), language.keys.toList())
        @Suppress("UNCHECKED_CAST")
        val candidates = language["candidates"] as List<Map<String, Any?>>
        assertEquals(listOf(0.6667, 0.3333), candidates.map { (it["score"] as JsonNumber).toDouble() })
        val many = LanguageResult("en", listOf("en" to 1f), (0 until 200).map { LanguageSegment(it * 1000L, it * 1000L + 900, "en") }, 200_000, "l")
        assertEquals(PluginToolErrorCodes.LIMIT_EXCEEDED, code { AnalysisToolRules.languageResult(many, maxChars = 1000) })
        val speech = TagLabel(0, "/m/09x0r", "Speech")
        val tags = parse(AnalysisToolRules.tagResult(TagResult(listOf(TagScore(speech, 0.912345f, 10_000, 20_000)), null, 5, 25_000, "tagger.ced.mini.int8")))
        assertEquals(listOf("labels", "durationMillis", "modelId", "warnings"), tags.keys.toList())
        @Suppress("UNCHECKED_CAST")
        val label = (tags["labels"] as List<Map<String, Any?>>).single()
        assertEquals(listOf("Speech", "/m/09x0r"), listOf(label["label"], label["mid"]))
        assertEquals(0.9123, (label["score"] as JsonNumber).toDouble(), 0.0)
        assertEquals(listOf(10_000L, 20_000L), listOf(long(label["startMillis"]), long(label["endMillis"])))
    }
}
