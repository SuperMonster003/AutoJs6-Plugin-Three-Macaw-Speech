package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.content.Context
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerMatch
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeakerRecognition
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolCall
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.autojs.plugin.tool.api.PluginToolResult
import kotlin.math.roundToLong

/**
 * `speech_speaker_verify` and `speech_speaker_identify` (roadmap P8.3.5, D125, D133): the audio file the host hands over
 * as a descriptor is scored against the voiceprints of [SpeakerRecognition] on the tool's worker thread. The result has
 * names, scores, the threshold and the model, never a voiceprint (D125). Only the counts and times are logged, never a
 * name or the file name.
 * zh-CN: `speech_speaker_verify` 与 `speech_speaker_identify` (路线图 P8.3.5, D125, D133): 在工具工作线程上, 用 [SpeakerRecognition] 的声纹为
 * 宿主以描述符交来的音频文件打分. 结果只有名字, 分数, 阈值与模型, 从不含声纹 (D125). 日志只记数量与时间, 从不记名字或文件名.
 */
internal class SpeechSpeakerTool(context: Context) {
    private val speakers = SpeakerRecognition.get(context.applicationContext)

    fun verify(call: PluginToolCall, request: SpeakerVerifyRequest): PluginToolResult {
        val started = now()
        val match = scored(call) { speakers.verify(request.name, it, request.threshold) }
        Log.i(TAG, "Verified a speaker with ${match.modelId} in ${now() - started} ms: matched ${match.matched}")
        return PluginToolResult(SpeakerToolRules.verifyResult(match))
    }

    fun identify(call: PluginToolCall, request: SpeakerIdentifyRequest): PluginToolResult {
        val started = now()
        val match = scored(call) { speakers.identify(it, request.threshold, request.topK) }
        Log.i(TAG, "Identified among ${match.candidates.size} candidate(s) with ${match.modelId} in ${now() - started} ms: matched ${match.matched}")
        return PluginToolResult(SpeakerToolRules.identifyResult(match))
    }

    private fun scored(call: PluginToolCall, score: (SpeakerRecognition.Recording) -> SpeakerMatch): SpeakerMatch {
        val input = call.input(SpeechToolCatalog.PARAM_FILE) ?: throw SpeakerToolRules.missingFile()
        val match = try {
            score(SpeakerRecognition.Recording.Descriptor(input.descriptor, "The audio file"))
        } catch (e: SpeechException) {
            throw SpeechToolErrors.of(e, input = true)
        }
        if (call.isCancelled) throw SpeakerToolRules.cancelled()
        return match
    }

    private companion object {
        const val TAG = "MacawTools"

        fun now(): Long = SystemClock.elapsedRealtime()
    }
}

/**
 * The pure rules of the speaker tools (roadmap P8.3.5): the results of the catalog's output schemas, scores rounded to
 * four decimals; identify names the speaker only when the best score reaches the threshold and warns when no one is
 * enrolled for the default speaker model. Pure Kotlin.
 * zh-CN: 说话人工具的纯规则 (路线图 P8.3.5): 符合目录输出 schema 的结果, 分数保留四位小数; identify 只在最高分达到阈值时给出名字, 默认说话人模型
 * 没有已注册的人时给出警告. 纯 Kotlin.
 */
internal object SpeakerToolRules {

    /** The warning of an identify without voiceprints. zh-CN: 没有声纹时 identify 的警告. */
    const val NO_SPEAKERS = "No speaker is enrolled for the default speaker model; enroll one on the Speakers screen of 3-Macaw Speech or with speech.speaker.enroll"

    fun verifyResult(match: SpeakerMatch): String {
        val best = requireNotNull(match.best) { "verify always scores the named speaker" }
        val result = linkedMapOf<String, Any?>(
            "name" to best.name,
            "score" to round(best.score),
            "matched" to match.matched,
            "threshold" to round(match.threshold),
            "modelId" to match.modelId,
            "warnings" to emptyList<Any?>(),
        )
        return json(result)
    }

    fun identifyResult(match: SpeakerMatch): String {
        val result = linkedMapOf<String, Any?>()
        match.best?.let { best ->
            if (match.matched) result["name"] = best.name
            result["score"] = round(best.score)
        }
        result["matched"] = match.matched
        result["threshold"] = round(match.threshold)
        result["modelId"] = match.modelId
        result["candidates"] = match.candidates.map { linkedMapOf<String, Any?>("name" to it.name, "score" to round(it.score)) }
        result["warnings"] = if (match.candidates.isEmpty()) listOf(ToolWarning(SpeechErrorCodes.SPEAKER_NOT_FOUND, NO_SPEAKERS).toJson()) else emptyList()
        return json(result)
    }

    fun missingFile(): PluginToolException = PluginToolException(PluginToolErrorCodes.INVALID_ARGUMENTS, "${SpeechToolCatalog.PARAM_FILE} is required")

    fun cancelled(): PluginToolException = PluginToolException(PluginToolErrorCodes.CANCELLED, "The speaker check was cancelled")

    private fun round(value: Double): Double = (value * 10_000).roundToLong() / 10_000.0

    private fun json(result: Map<String, Any?>): String = String(StrictJson.write(result), Charsets.UTF_8).trimEnd()
}
