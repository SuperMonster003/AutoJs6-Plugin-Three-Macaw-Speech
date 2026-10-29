package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.content.Context
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.DiarizeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MonoAudio
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechAnalysis
import org.autojs.plugin.tool.api.PluginToolCall
import org.autojs.plugin.tool.api.PluginToolResult
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * `speech_diarize`, `speech_detect_language` and `speech_tag` (roadmap P8.4.4, D152): the audio file the host hands over
 * as a descriptor is analyzed by [SpeechAnalysis] on the tool's worker thread, queued behind any other analysis of the
 * process. A cancel or the end of the tool's time stops the decoding and the windowed analyses within one chunk (a
 * diarization only after its segmentation, which sherpa-onnx cannot stop); the call then fails instead of answering a
 * partial result. Results never hold a voiceprint. Only the counts and times are logged, never a name or the file name.
 * zh-CN: `speech_diarize`, `speech_detect_language` 与 `speech_tag` (路线图 P8.4.4, D152): 在工具工作线程上用 [SpeechAnalysis] 分析宿主以描述符
 * 交来的音频文件, 排在进程内其他分析之后. 取消或工具时限到达时, 解码与按窗分析在一块之内停止 (说话人分离要等 sherpa-onnx 无法打断的分割阶段结束);
 * 此时调用失败, 不返回部分结果. 结果从不含声纹. 日志只记数量与时间, 从不记名字或文件名.
 */
internal class SpeechAnalysisTool(context: Context) {
    private val analysis = SpeechAnalysis.get(context.applicationContext)

    fun diarize(call: PluginToolCall, request: DiarizeToolRequest): PluginToolResult = run(call, SpeechToolCatalog.TOOL_DIARIZE) { reader, control ->
        AnalysisToolRules.checkDiarizeLength(reader.durationMillis)
        val result = analysis.diarize(reader, DiarizeOptions(speakers = request.speakers, threshold = request.threshold?.toFloat(), identify = request.identify), control)
        Log.i(TAG, "Diarized ${result.durationMillis} ms into ${result.turns.size} turns of ${result.speakers} speaker(s)")
        AnalysisToolRules.diarizeResult(result)
    }

    fun detectLanguage(call: PluginToolCall, request: LanguageToolRequest): PluginToolResult = run(call, SpeechToolCatalog.TOOL_DETECT_LANGUAGE) { reader, control ->
        val result = analysis.detectLanguage(reader, LanguageOptions(windows = request.windows), control)
        Log.i(TAG, "Identified the language of ${result.windows.size} window(s) of ${result.durationMillis} ms")
        AnalysisToolRules.languageResult(result)
    }

    fun tag(call: PluginToolCall, request: TagToolRequest): PluginToolResult = run(call, SpeechToolCatalog.TOOL_TAG) { reader, control ->
        val result = analysis.tag(reader, TagOptions(topK = request.topK), control)
        Log.i(TAG, "Tagged ${result.durationMillis} ms with ${result.labels.size} label(s)")
        AnalysisToolRules.tagResult(result)
    }

    private fun run(call: PluginToolCall, tool: String, work: (MonoAudio, SpeechAnalysis.Control) -> String): PluginToolResult {
        val input = call.input(SpeechToolCatalog.PARAM_FILE) ?: throw AnalysisToolRules.missingFile()
        val started = now()
        val deadline = started + (call.timeoutMs.takeIf { it > 0 } ?: SpeechToolCatalog.LONG_TIMEOUT_MS)
        val cancel = CancelToken()
        val expired = AtomicBoolean(false)
        // The analysis reports no progress while it is queued or segments: a watcher turns a cancel or the deadline into
        // the cancel token. zh-CN: 排队或分割时分析不报告进度, 由监视线程把取消或时限转成取消标记.
        val watcher = Thread({
            while (!cancel.isCancelled) {
                when {
                    call.isCancelled -> cancel.cancel()
                    now() >= deadline -> expired.set(true).also { cancel.cancel() }
                    else -> SystemClock.sleep(WATCH_MILLIS)
                }
            }
        }, "MacawToolWatch").apply { isDaemon = true; start() }
        val control = SpeechAnalysis.Control(cancel)
        val json = try {
            AudioFiles.open(input.descriptor).use { reader -> work(reader, control) }
        } catch (e: SpeechException) {
            if (call.isCancelled) throw AnalysisToolRules.cancelled(tool)
            if (expired.get()) throw AnalysisToolRules.expired(tool)
            throw SpeechToolErrors.of(e, input = true)
        } catch (e: IOException) {
            throw AnalysisToolRules.unreadable(e)
        } finally {
            cancel.cancel()
            watcher.interrupt()
        }
        if (call.isCancelled) throw AnalysisToolRules.cancelled(tool)
        if (expired.get()) throw AnalysisToolRules.expired(tool)
        Log.i(TAG, "speech_$tool answered in ${now() - started} ms")
        return PluginToolResult(json)
    }

    private companion object {
        const val TAG = "MacawTools"
        const val WATCH_MILLIS = 250L

        fun now(): Long = SystemClock.elapsedRealtime()
    }
}
