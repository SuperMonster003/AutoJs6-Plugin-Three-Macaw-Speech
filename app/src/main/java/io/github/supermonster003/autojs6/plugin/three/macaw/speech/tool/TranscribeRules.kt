package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscript
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.autojs.plugin.tool.api.PluginToolLimits

/** The recognition model of a transcription and the warnings of the choice. zh-CN: 转写所用的识别模型及选择时的警告. */
internal data class TranscribePlan(val modelId: String, val warnings: List<ToolWarning> = emptyList())

/**
 * Roadmap P7.6.7 (D97, D100): the pure rules of `speech_transcribe`: the model (the requested one, else the choice of
 * `speech_listen` by language), the result of the catalog's output schema within the result ceiling, and the errors of a
 * transcription stopped by a cancel or the time limit. Pure Kotlin.
 * zh-CN: 路线图 P7.6.7 (D97, D100): `speech_transcribe` 的纯规则: 模型 (请求指定的模型, 否则与 `speech_listen` 一样按语言选择), 在结果上限内符合
 * 目录输出 schema 的结果, 以及被取消或超时打断的转写的错误. 纯 Kotlin.
 */
internal object TranscribeRules {

    /** The warning of a result whose segments lost their text. zh-CN: 分段去掉文字时的警告. */
    const val TRIMMED = "The transcript is too large for a tool result with the text of every segment; the segments keep their times without text"

    fun plan(request: TranscribeRequest, models: List<ModelEntry>): TranscribePlan {
        // An explicit model goes to the runtime as it is, which answers MODEL_MISSING or INVALID_OPTIONS for it.
        request.model?.let { return TranscribePlan(it) }
        val modelId = ListenRules.model(models, request.language) ?: throw ListenRules.noModel()
        val languages = models.firstOrNull { it.id == modelId }?.languages.orEmpty()
        return TranscribePlan(modelId, listOfNotNull(ListenRules.languageWarning(request.language, modelId, languages, used = "transcribed")))
    }

    /**
     * The result of the catalog's output schema; when it would pass [maxChars] UTF-16 units or [maxBytes] UTF-8 bytes,
     * the segments keep their times without text and a `LIMIT_EXCEEDED` warning says so (D97). A result that is still
     * too large (the full text alone) fails with `LIMIT_EXCEEDED`. Both ceilings are the contract's: the result reaches
     * the host in a oneway Binder callback, whose Parcel holds strings as UTF-16 (appendix D, Q41), and the base class
     * would refuse a larger result as a whole instead of trimming it.
     * zh-CN: 符合目录输出 schema 的结果; 将超过 [maxChars] 个 UTF-16 单元或 [maxBytes] 个 UTF-8 字节时, 分段只保留时间不带文字, 并以
     * `LIMIT_EXCEEDED` 警告说明 (D97). 仍然过大 (仅全文就超限) 时以 `LIMIT_EXCEEDED` 失败. 两道上限都取自契约: 结果经 oneway Binder 回调交给宿主,
     * 其 Parcel 以 UTF-16 存放字符串 (附录 D, Q41), 更大的结果会被基类整体拒绝而不是裁剪.
     */
    fun result(
        transcript: FileTranscript,
        warnings: List<ToolWarning>,
        maxChars: Int = PluginToolLimits.MAX_RESULT_CHARS,
        maxBytes: Int = PluginToolLimits.MAX_RESULT_BYTES,
    ): String {
        val all = (warnings + transcript.warnings.map { ToolWarning(it.code, it.message) }).distinct()
        val full = json(transcript, all, withText = true)
        if (fits(full, maxChars, maxBytes)) return full
        val trimmed = json(transcript, all + ToolWarning(SpeechErrorCodes.LIMIT_EXCEEDED, TRIMMED), withText = false)
        if (fits(trimmed, maxChars, maxBytes)) return trimmed
        throw PluginToolException(
            PluginToolErrorCodes.LIMIT_EXCEEDED,
            "The transcript of ${transcript.text.length} characters is too large for a tool result; transcribe a shorter part of the audio",
        )
    }

    fun cancelled(): PluginToolException = PluginToolException(PluginToolErrorCodes.CANCELLED, "The transcription of speech_transcribe was cancelled")

    fun expired(): PluginToolException =
        SpeechToolErrors.of(SpeechErrorCodes.TIMEOUT, "speech_transcribe ran out of time before the end of the audio; transcribe a shorter part of the audio")

    fun missingFile(): PluginToolException = PluginToolException(PluginToolErrorCodes.INVALID_ARGUMENTS, "${SpeechToolCatalog.PARAM_FILE} is required")

    private fun json(transcript: FileTranscript, warnings: List<ToolWarning>, withText: Boolean): String {
        val result = linkedMapOf<String, Any?>()
        result["text"] = transcript.text
        result["segments"] = transcript.segments.map { segment ->
            linkedMapOf<String, Any?>("startMillis" to segment.startMillis, "endMillis" to segment.endMillis).apply { if (withText) put("text", segment.text) }
        }
        transcript.language?.let { result["language"] = it }
        result["modelId"] = transcript.modelId
        result["durationMillis"] = transcript.durationMillis
        result["warnings"] = warnings.map { it.toJson() }
        return String(StrictJson.write(result), Charsets.UTF_8).trimEnd()
    }

    private fun fits(json: String, maxChars: Int, maxBytes: Int): Boolean = json.length <= maxChars && json.toByteArray(Charsets.UTF_8).size <= maxBytes

}
