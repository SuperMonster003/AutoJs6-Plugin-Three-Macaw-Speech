package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.Diarization
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.autojs.plugin.tool.api.PluginToolLimits
import java.io.IOException
import kotlin.math.roundToLong

/**
 * The pure rules of the analysis tools (roadmap P8.4.4, D147 - D149, D152): the results of the catalog's output schemas
 * (the shapes of `speech.diarize`, `speech.detectLanguage` and `speech.tag`, scores rounded to four decimals, plus
 * `warnings`), the length limit of `speech_diarize` and the errors of a call stopped by a cancel or the time limit. A
 * result over the contract's ceilings fails with `LIMIT_EXCEEDED`. Pure Kotlin.
 * zh-CN: 分析工具的纯规则 (路线图 P8.4.4, D147 - D149, D152): 符合目录输出 schema 的结果 (与 `speech.diarize`, `speech.detectLanguage`,
 * `speech.tag` 同形, 分数保留四位小数, 另加 `warnings`), `speech_diarize` 的时长上限, 以及被取消或超时打断的调用的错误. 超出契约上限的结果以
 * `LIMIT_EXCEEDED` 失败. 纯 Kotlin.
 */
internal object AnalysisToolRules {

    /** D152: `speech_diarize` takes at most 10 minutes of audio (about 3 minutes of work on a slow phone). zh-CN: 分离工具至多 10 分钟音频. */
    const val MAX_DIARIZE_MILLIS = 600_000L

    fun checkDiarizeLength(durationMillis: Long?) {
        if (durationMillis != null && durationMillis > MAX_DIARIZE_MILLIS) {
            throw PluginToolException(
                PluginToolErrorCodes.INVALID_ARGUMENTS,
                "speech_diarize takes at most 10 minutes of audio, the file has ${durationMillis / 1000} s; diarize longer files with speech.diarize in an AutoJs6 script or on the Try it out screen of 3-Macaw Speech",
            )
        }
    }

    fun diarizeResult(result: Diarization, maxChars: Int = PluginToolLimits.MAX_RESULT_CHARS, maxBytes: Int = PluginToolLimits.MAX_RESULT_BYTES): String {
        val json = linkedMapOf<String, Any?>()
        json["segments"] = result.turns.map { linkedMapOf<String, Any?>("startMillis" to it.startMillis, "endMillis" to it.endMillis, "speaker" to it.speaker) }
        json["speakers"] = result.speakers
        json["durationMillis"] = result.durationMillis
        json["modelId"] = result.modelId
        json["speakerModelId"] = result.speakerModelId
        result.names?.let { names ->
            json["names"] = names.map { name ->
                linkedMapOf<String, Any?>("speaker" to name.speaker).apply {
                    name.name?.let { put("name", it) }
                    name.score?.let { put("score", round(it)) }
                }
            }
        }
        json["warnings"] = emptyList<Any?>()
        return bounded(write(json), maxChars, maxBytes, "${result.turns.size} speaker turns")
    }

    fun languageResult(result: LanguageResult): String {
        val json = linkedMapOf<String, Any?>()
        json["language"] = result.language
        json["candidates"] = result.candidates.map { (language, score) -> linkedMapOf<String, Any?>("language" to language, "score" to round(score.toDouble())) }
        json["segments"] = result.windows.map { linkedMapOf<String, Any?>("startMillis" to it.startMillis, "endMillis" to it.endMillis, "language" to it.language) }
        json["durationMillis"] = result.durationMillis
        json["modelId"] = result.modelId
        json["warnings"] = emptyList<Any?>()
        return write(json)
    }

    fun tagResult(result: TagResult): String {
        val json = linkedMapOf<String, Any?>()
        json["labels"] = result.labels.map {
            linkedMapOf<String, Any?>(
                "label" to it.label.name,
                "mid" to it.label.mid,
                "score" to round(it.score.toDouble()),
                "startMillis" to it.startMillis,
                "endMillis" to it.endMillis,
            )
        }
        json["durationMillis"] = result.durationMillis
        json["modelId"] = result.modelId
        json["warnings"] = emptyList<Any?>()
        return write(json)
    }

    fun missingFile(): PluginToolException = PluginToolException(PluginToolErrorCodes.INVALID_ARGUMENTS, "${SpeechToolCatalog.PARAM_FILE} is required")

    fun cancelled(tool: String): PluginToolException = PluginToolException(PluginToolErrorCodes.CANCELLED, "speech_$tool was cancelled")

    fun expired(tool: String): PluginToolException =
        SpeechToolErrors.of(SpeechErrorCodes.TIMEOUT, "speech_$tool ran out of time before the end of the audio; analyze a shorter part of the audio")

    fun unreadable(e: IOException): PluginToolException =
        PluginToolException(PluginToolErrorCodes.INPUT_UNREADABLE, "${SpeechErrorCodes.IO_FAILED}: reading the audio failed: ${e.message ?: "no detail"}", e)

    private fun bounded(json: String, maxChars: Int, maxBytes: Int, what: String): String {
        if (json.length <= maxChars && json.toByteArray(Charsets.UTF_8).size <= maxBytes) return json
        throw PluginToolException(PluginToolErrorCodes.LIMIT_EXCEEDED, "The result with $what is too large for a tool result; analyze a shorter part of the audio")
    }

    private fun round(value: Double): Double = (value * 10_000).roundToLong() / 10_000.0

    private fun write(json: Map<String, Any?>): String = String(StrictJson.write(json), Charsets.UTF_8).trimEnd()
}
