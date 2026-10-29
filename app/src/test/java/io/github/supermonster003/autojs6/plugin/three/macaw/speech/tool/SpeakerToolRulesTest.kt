package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerMatch
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerScore
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P8.3.5: the arguments, results and errors of `speech_speaker_verify` and `speech_speaker_identify`. A result
 * has names and rounded scores only (D125), identify names the speaker only on a match, and an unknown name or too
 * little speech become the tool codes the catalog promises.
 * zh-CN: 路线图 P8.3.5: `speech_speaker_verify` 与 `speech_speaker_identify` 的参数, 结果与错误. 结果只含名字与舍入后的分数 (D125), identify
 * 只在相符时给出名字, 未知名字与语音太少换成目录承诺的工具错误码.
 */
class SpeakerToolRulesTest {

    private fun parse(json: String): Map<String, Any?> = StrictJson.parseObject(json.toByteArray(Charsets.UTF_8))

    private fun invalid(block: () -> Unit) {
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, assertThrows(PluginToolException::class.java) { block() }.code)
    }

    @Test
    fun `arguments are read with the catalog's ranges`() {
        assertEquals(SpeakerVerifyRequest("Alice", null), SpeechToolArguments.speakerVerify(mapOf("name" to " Alice ")))
        assertEquals(SpeakerVerifyRequest("Bob", 0.6), SpeechToolArguments.speakerVerify(mapOf("name" to "Bob", "threshold" to 0.6)))
        assertEquals(SpeakerIdentifyRequest(0.0, 10), SpeechToolArguments.speakerIdentify(mapOf("threshold" to 0, "topK" to 10)))
        invalid { SpeechToolArguments.speakerVerify(emptyMap()) }
        invalid { SpeechToolArguments.speakerVerify(mapOf("name" to "  ")) }
        invalid { SpeechToolArguments.speakerVerify(mapOf("name" to "a\nb")) }
        invalid { SpeechToolArguments.speakerVerify(mapOf("name" to "x".repeat(65))) }
        invalid { SpeechToolArguments.speakerVerify(mapOf("name" to "a", "threshold" to 1.5)) }
        invalid { SpeechToolArguments.speakerVerify(mapOf("name" to "a", "threshold" to "0.5")) }
        invalid { SpeechToolArguments.speakerIdentify(mapOf("topK" to 0)) }
        invalid { SpeechToolArguments.speakerIdentify(mapOf("topK" to 11)) }
        invalid { SpeechToolArguments.speakerIdentify(mapOf("topK" to 2.5)) }
    }

    @Test
    fun `verify answers the named speaker with rounded scores`() {
        val best = SpeakerScore("Alice", 0.612345678)
        val result = parse(SpeakerToolRules.verifyResult(SpeakerMatch(best, 0.5, listOf(best), "speaker.m")))
        assertEquals(listOf("name", "score", "matched", "threshold", "modelId", "warnings"), result.keys.toList())
        assertEquals("Alice", result["name"])
        assertEquals("0.6123", result["score"].toString())
        assertEquals(true, result["matched"])
        assertEquals(emptyList<Any?>(), result["warnings"])
        val low = SpeakerScore("Alice", 0.2)
        assertEquals(false, parse(SpeakerToolRules.verifyResult(SpeakerMatch(low, 0.5, listOf(low), "speaker.m")))["matched"])
    }

    @Test
    fun `identify names the speaker only on a match and warns without voiceprints`() {
        val candidates = listOf(SpeakerScore("Alice", 0.71), SpeakerScore("Bob", 0.33))
        val matched = parse(SpeakerToolRules.identifyResult(SpeakerMatch(candidates[0], 0.5, candidates, "speaker.m")))
        assertEquals("Alice", matched["name"])
        assertEquals(2, (matched["candidates"] as List<*>).size)
        val missed = parse(SpeakerToolRules.identifyResult(SpeakerMatch(candidates[0], 0.8, candidates, "speaker.m")))
        assertFalse("no name below the threshold", missed.containsKey("name"))
        assertEquals("0.71", missed["score"].toString())
        assertEquals(false, missed["matched"])
        val empty = parse(SpeakerToolRules.identifyResult(SpeakerMatch(null, 0.5, emptyList(), "speaker.m")))
        assertEquals(setOf("matched", "threshold", "modelId", "candidates", "warnings"), empty.keys)
        val warning = (empty["warnings"] as List<*>).single() as Map<*, *>
        assertEquals(SpeechErrorCodes.SPEAKER_NOT_FOUND, warning["code"])
        assertTrue(SpeakerToolRules.NO_SPEAKERS.all { it.code in 0x20..0x7e })
    }

    @Test
    fun `speaker failures become the codes the catalog names`() {
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, SpeechToolErrors.code(SpeechErrorCodes.SPEAKER_NOT_FOUND))
        assertEquals(PluginToolErrorCodes.INPUT_UNREADABLE, SpeechToolErrors.code(SpeechErrorCodes.AUDIO_TOO_SHORT, input = true))
        assertEquals(PluginToolErrorCodes.FAILED, SpeechToolErrors.code(SpeechErrorCodes.AUDIO_TOO_SHORT))
        assertEquals(PluginToolErrorCodes.NOT_READY, SpeechToolErrors.code(SpeechErrorCodes.MODEL_MISSING, input = true))
    }
}
