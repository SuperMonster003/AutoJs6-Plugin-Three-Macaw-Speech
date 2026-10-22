package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.SentenceSplitter
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.autojs.plugin.tool.api.PluginToolLimits

/** The voice of a synthesis, the language it reads in and the warnings of the choice. zh-CN: 合成所用的语音, 朗读语言及选择时的警告. */
internal data class SynthesizePlan(val language: String, val voice: TtsVoice, val warnings: List<ToolWarning> = emptyList())

/** The AAC parameters of an M4A output. zh-CN: M4A 输出的 AAC 参数. */
internal data class AacFormat(val sampleRate: Int, val bitRate: Int)

/**
 * Roadmap P7.6.7 (D98, D100): the pure rules of `speech_synthesize`: the voice (the choice of `speech_speak` among the
 * plugin's own voices; no voice is `NOT_READY`, since the system engine cannot write a file for the tool), the speaker
 * the loaded engine has, the AAC parameters of `m4a`, the output ceiling, the result of the catalog's output schema and
 * the errors of a stopped synthesis. Pure Kotlin.
 * zh-CN: 路线图 P7.6.7 (D98, D100): `speech_synthesize` 的纯规则: 语音 (与 `speech_speak` 一样在插件自己的语音中选择; 没有语音为 `NOT_READY`,
 * 因为系统引擎不能为工具写文件), 已加载引擎具有的说话人, `m4a` 的 AAC 参数, 输出上限, 符合目录输出 schema 的结果, 以及被打断的合成的错误.
 * 纯 Kotlin.
 */
internal object SynthesizeRules {

    /** The host's ceiling of one output descriptor (8 MiB). zh-CN: 宿主对单个输出描述符的上限 (8 MiB). */
    const val MAX_OUTPUT_BYTES = PluginToolLimits.MAX_DESCRIPTOR_BYTES.toLong()

    /** D98: the bit rate of `m4a`, mono. zh-CN: D98: `m4a` 的码率, 单声道. */
    const val AAC_BIT_RATE = 64_000

    /** AAC-LC carries at most 6 bits per sample and channel, so 8 kHz voices get 48 kbps. zh-CN: AAC-LC 每声道每采样至多 6 比特, 8 kHz 语音因此为 48 kbps. */
    const val AAC_MAX_BITS_PER_SAMPLE = 6

    /** The sampling frequencies of the AAC standard. zh-CN: AAC 标准的采样率. */
    val AAC_SAMPLE_RATES = setOf(96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350)

    fun plan(request: SynthesizeRequest, table: TtsVoiceTable): SynthesizePlan {
        if (SentenceSplitter.split(request.text).none { it.speakable }) {
            throw PluginToolException(PluginToolErrorCodes.INVALID_ARGUMENTS, "text has nothing to speak; it needs at least one letter or digit")
        }
        val plan = SpeakRules.plan(SpeakRequest(request.text, request.language, request.voice, request.speed), table)
        val voice = plan.voice ?: throw SpeechToolErrors.of(
            SpeechErrorCodes.MODEL_MISSING,
            if (table.isEmpty()) {
                "No text-to-speech model is installed; download one on the home screen of 3-Macaw Speech"
            } else {
                "No installed text-to-speech model of 3-Macaw Speech speaks '${plan.language}'; download one that speaks it on the home screen of 3-Macaw Speech"
            },
        )
        return SynthesizePlan(plan.language, voice, plan.warnings)
    }

    /** The speaker of [voice] when the loaded model has it, else its first. zh-CN: 已加载模型具有 [voice] 的说话人时取之, 否则取第一个. */
    fun speakerId(voice: TtsVoice, speakerCount: Int): Int = voice.speakerId.takeIf { it in 0 until speakerCount } ?: 0

    fun mimeType(format: String): String = if (format == SpeechToolCatalog.FORMAT_M4A) SpeechToolCatalog.MIME_M4A else SpeechToolCatalog.MIME_WAV

    fun aac(sampleRate: Int): AacFormat {
        if (sampleRate !in AAC_SAMPLE_RATES) {
            throw PluginToolException(PluginToolErrorCodes.FAILED, "AAC does not take the $sampleRate Hz audio of this voice; synthesize to ${SpeechToolCatalog.FORMAT_WAV} instead")
        }
        return AacFormat(sampleRate, minOf(AAC_BIT_RATE, sampleRate * AAC_MAX_BITS_PER_SAMPLE))
    }

    /** The presentation time of the sample at [samples] in microseconds. zh-CN: 第 [samples] 个采样的呈现时间, 单位微秒. */
    fun presentationTimeUs(samples: Long, sampleRate: Int): Long = samples * 1_000_000L / sampleRate

    /** Fails with the message of `WavWriter` once the output would pass [maxBytes]. zh-CN: 输出将超过 [maxBytes] 时以 `WavWriter` 的信息失败. */
    fun checkSize(bytes: Long, maxBytes: Long = MAX_OUTPUT_BYTES) {
        if (bytes > maxBytes) throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The synthesized audio exceeds $maxBytes bytes; synthesize less text at a time")
    }

    fun result(format: String, modelId: String, voice: String, sampleRate: Int, durationMillis: Long, bytes: Long, warnings: List<ToolWarning>): String {
        val result = linkedMapOf<String, Any?>(
            "format" to format,
            "mimeType" to mimeType(format),
            "modelId" to modelId,
            "voice" to voice,
            "sampleRate" to sampleRate,
            "durationMillis" to durationMillis,
            "bytes" to bytes,
            "warnings" to warnings.distinct().map { it.toJson() },
        )
        return String(StrictJson.write(result), Charsets.UTF_8).trimEnd()
    }

    fun cancelled(): PluginToolException = PluginToolException(PluginToolErrorCodes.CANCELLED, "The synthesis of speech_synthesize was cancelled")

    fun expired(): PluginToolException =
        SpeechToolErrors.of(SpeechErrorCodes.TIMEOUT, "speech_synthesize ran out of time before the end of the text; synthesize less text at a time")

    fun silent(voice: String): PluginToolException = PluginToolException(PluginToolErrorCodes.FAILED, "The voice $voice produced no audio for the text")

}
