package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.content.Context
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.RecognizeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.tool.api.PluginToolCall
import org.autojs.plugin.tool.api.PluginToolResult

/**
 * `speech_transcribe` (roadmap P7.6.7, D97, D100): transcribes the audio file the host hands over as a descriptor with
 * `recognizeFile` of the speech service, on the tool's worker thread. A cancel or the end of the tool's time stops the
 * decoding within one chunk; the call then fails instead of answering a partial transcript. A file the decoder cannot
 * read is `INPUT_UNREADABLE`. Only the model, the counts and the times are logged, never the text or the file name.
 * zh-CN: `speech_transcribe` (路线图 P7.6.7, D97, D100): 在工具工作线程上, 用语音服务的 `recognizeFile` 转写宿主以描述符交来的音频文件. 取消或
 * 工具时限到达时解码在一块之内停止, 此时调用失败, 不返回部分转写. 解码器读不了的文件为 `INPUT_UNREADABLE`. 日志只记模型, 数量与时间, 从不记文字
 * 或文件名.
 */
internal class SpeechTranscribeTool(context: Context) {
    private val runtime = SpeechRuntime.get(context.applicationContext)

    fun call(call: PluginToolCall, request: TranscribeRequest): PluginToolResult {
        val input = call.input(SpeechToolCatalog.PARAM_FILE) ?: throw TranscribeRules.missingFile()
        val started = now()
        val deadline = started + (call.timeoutMs.takeIf { it > 0 } ?: SpeechToolCatalog.LONG_TIMEOUT_MS)
        val plan = TranscribeRules.plan(request, runtime.models.list(kind = SpeechContract.KIND_ASR, installed = true))
        var expired = false
        val transcript = try {
            runtime.recognizeFile(input.descriptor, RecognizeOptions(model = plan.modelId, language = request.language)) {
                call.isCancelled || (now() >= deadline).also { if (it) expired = true }
            }
        } catch (e: SpeechException) {
            throw SpeechToolErrors.of(e, input = true)
        }
        if (call.isCancelled) throw TranscribeRules.cancelled()
        if (expired) throw TranscribeRules.expired()
        val result = TranscribeRules.result(transcript, plan.warnings)
        Log.i(TAG, "Transcribed ${transcript.durationMillis} ms of audio with ${transcript.modelId} in ${now() - started} ms: ${transcript.segments.size} segments, ${transcript.text.length} chars")
        return PluginToolResult(result)
    }

    private companion object {
        const val TAG = "MacawTools"

        fun now(): Long = SystemClock.elapsedRealtime()
    }
}
