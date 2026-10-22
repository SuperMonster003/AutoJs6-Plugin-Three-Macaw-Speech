package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsModelVoices
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.WavWriter
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * Roadmap P7.6.7 (D98, D100, D101 E2): the pure rules of `speech_synthesize`: the voice among the plugin's own voices,
 * the speaker of the loaded model, the AAC parameters of `m4a`, the 8 MiB output guard, the result of the catalog's
 * output schema and the errors of a stopped synthesis.
 * zh-CN: 路线图 P7.6.7 (D98, D100, D101 E2): `speech_synthesize` 的纯规则: 在插件自己的语音中选择的语音, 已加载模型的说话人, `m4a` 的 AAC 参数,
 * 8 MiB 输出守卫, 符合目录输出 schema 的结果, 以及被打断的合成的错误.
 */
class SynthesizeRulesTest {

    private val melo = TtsModelVoices("tts.vits.melo.zh-en", listOf("zh", "en"), 1, 44100, isDefault = false)
    private val aishell = TtsModelVoices("tts.vits.icefall.zh-aishell3", listOf("zh"), 3, 8000, isDefault = true, speakerNames = listOf("SSB0005", "SSB0009", "SSB0011"))
    private val piper = TtsModelVoices("tts.vits.piper.en-us-lessac-medium", listOf("en"), 1, 22050, isDefault = false)
    private val table = TtsVoiceTable(listOf(melo, aishell, piper))

    @Test
    fun `the voice is chosen like speech_speak, but only among the plugin's voices`() {
        assertEquals(SynthesizePlan("zh", table.voice("tts.vits.icefall.zh-aishell3/0")!!), SynthesizeRules.plan(SynthesizeRequest("打开设置"), table))
        assertEquals("tts.vits.piper.en-us-lessac-medium/0", SynthesizeRules.plan(SynthesizeRequest("hello", format = SpeechToolCatalog.FORMAT_M4A), table).voice.name)
        val named = SynthesizeRules.plan(SynthesizeRequest("你好", voice = VoiceChoice("tts.vits.icefall.zh-aishell3", "SSB0011")), table)
        assertEquals("tts.vits.icefall.zh-aishell3/2", named.voice.name)
        val ignored = SynthesizeRules.plan(SynthesizeRequest("你好", language = "zh", voice = VoiceChoice("tts.vits.piper.en-us-lessac-medium", "0")), table)
        assertEquals("tts.vits.icefall.zh-aishell3/0", ignored.voice.name)
        assertEquals(listOf(ToolWarning(SpeechErrorCodes.INVALID_OPTIONS, "The voice tts.vits.piper.en-us-lessac-medium/0 does not speak 'zh' and was ignored")), ignored.warnings)
    }

    @Test
    fun `no voice is NOT_READY and text without words is INVALID_ARGUMENTS`() {
        val french = assertThrows(PluginToolException::class.java) { SynthesizeRules.plan(SynthesizeRequest("bonjour", language = "fr"), table) }
        assertEquals(PluginToolErrorCodes.NOT_READY, french.code)
        assertEquals(
            "MODEL_MISSING: No installed text-to-speech model of 3-Macaw Speech speaks 'fr'; download one that speaks it on the home screen of 3-Macaw Speech",
            french.message,
        )
        val none = assertThrows(PluginToolException::class.java) { SynthesizeRules.plan(SynthesizeRequest("hello"), TtsVoiceTable(emptyList())) }
        assertEquals(PluginToolErrorCodes.NOT_READY, none.code)
        assertEquals("MODEL_MISSING: No text-to-speech model is installed; download one on the home screen of 3-Macaw Speech", none.message)
        val silent = assertThrows(PluginToolException::class.java) { SynthesizeRules.plan(SynthesizeRequest("... !? --"), table) }
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, silent.code)
        assertEquals("text has nothing to speak; it needs at least one letter or digit", silent.message)
        val unknown = assertThrows(PluginToolException::class.java) { SynthesizeRules.plan(SynthesizeRequest("hi", voice = VoiceChoice("tts.none", "0")), table) }
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, unknown.code)
    }

    @Test
    fun `the speaker is the voice's when the loaded model has it`() {
        val voice = table.voice("tts.vits.icefall.zh-aishell3/2")!!
        assertEquals(2, SynthesizeRules.speakerId(voice, 3))
        assertEquals("a model with fewer speakers than its catalog entry", 0, SynthesizeRules.speakerId(voice, 2))
    }

    @Test
    fun `m4a is AAC-LC at the voice's rate, 64 kbps or 6 bits per sample`() {
        assertEquals(AacFormat(44100, 64_000), SynthesizeRules.aac(44100))
        assertEquals(AacFormat(22050, 64_000), SynthesizeRules.aac(22050))
        assertEquals(AacFormat(24000, 64_000), SynthesizeRules.aac(24000))
        assertEquals(AacFormat(16000, 64_000), SynthesizeRules.aac(16000))
        assertEquals("AAC-LC carries 48 kbps at 8 kHz", AacFormat(8000, 48_000), SynthesizeRules.aac(8000))
        val odd = assertThrows(PluginToolException::class.java) { SynthesizeRules.aac(11000) }
        assertEquals(PluginToolErrorCodes.FAILED, odd.code)
        assertEquals("AAC does not take the 11000 Hz audio of this voice; synthesize to wav instead", odd.message)
        assertEquals(1_000_000L, SynthesizeRules.presentationTimeUs(22050, 22050))
        assertEquals(23_219L, SynthesizeRules.presentationTimeUs(1024, 44100))
        assertEquals("30 minutes at 48 kHz stay exact", 1_800_000_000L, SynthesizeRules.presentationTimeUs(86_400_000, 48000))
        assertEquals(SpeechToolCatalog.MIME_M4A, SynthesizeRules.mimeType(SpeechToolCatalog.FORMAT_M4A))
        assertEquals(SpeechToolCatalog.MIME_WAV, SynthesizeRules.mimeType(SpeechToolCatalog.FORMAT_WAV))
    }

    @Test
    fun `the output stops at 8 MiB`() {
        assertEquals(8L * 1024 * 1024, SynthesizeRules.MAX_OUTPUT_BYTES)
        SynthesizeRules.checkSize(SynthesizeRules.MAX_OUTPUT_BYTES)
        val over = assertThrows(SpeechException::class.java) { SynthesizeRules.checkSize(SynthesizeRules.MAX_OUTPUT_BYTES + 1) }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, over.code)
        val error = SpeechToolErrors.of(over)
        assertEquals(PluginToolErrorCodes.LIMIT_EXCEEDED, error.code)
        assertEquals("LIMIT_EXCEEDED: The synthesized audio exceeds 8388608 bytes; synthesize less text at a time", error.message)

        val file = File.createTempFile("synthesize-", ".wav")
        try {
            RandomAccessFile(file, "rw").use { output ->
                val wav = WavWriter(output.channel, 22050, SynthesizeRules.MAX_OUTPUT_BYTES)
                val second = FloatArray(1 shl 20)
                repeat(3) { wav.write(second) }
                val full = assertThrows(SpeechException::class.java) { wav.write(second) }
                assertEquals("the same message as the m4a guard", over.message, full.message)
                assertEquals("nothing past the guard is written", WavWriter.HEADER_BYTES + 6L * 1024 * 1024, wav.finish())
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `the result has the catalog's keys in order`() {
        val warning = ToolWarning(SpeechErrorCodes.INVALID_OPTIONS, "ignored")
        assertEquals(
            """{"format":"m4a","mimeType":"audio/mp4","modelId":"tts.vits.melo.zh-en","voice":"tts.vits.melo.zh-en/0","sampleRate":44100,""" +
                """"durationMillis":2500,"bytes":20480,"warnings":[{"code":"INVALID_OPTIONS","message":"ignored"}]}""",
            SynthesizeRules.result(SpeechToolCatalog.FORMAT_M4A, "tts.vits.melo.zh-en", "tts.vits.melo.zh-en/0", 44100, 2500, 20480, listOf(warning, warning)),
        )
    }

    @Test
    fun `stopped syntheses have their codes`() {
        assertEquals(PluginToolErrorCodes.CANCELLED, SynthesizeRules.cancelled().code)
        val expired = SynthesizeRules.expired()
        assertEquals(PluginToolErrorCodes.FAILED, expired.code)
        assertEquals("TIMEOUT: speech_synthesize ran out of time before the end of the text; synthesize less text at a time", expired.message)
        val silent = SynthesizeRules.silent("tts.vits.melo.zh-en/0")
        assertEquals(PluginToolErrorCodes.FAILED, silent.code)
        assertEquals("The voice tts.vits.melo.zh-en/0 produced no audio for the text", silent.message)
    }

}
