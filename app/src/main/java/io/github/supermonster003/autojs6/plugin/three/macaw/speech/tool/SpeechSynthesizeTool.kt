package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.content.Context
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsSynthesis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.WavWriter
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.tool.api.PluginToolCall
import org.autojs.plugin.tool.api.PluginToolResult
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

/**
 * `speech_synthesize` (roadmap P7.6.7, D98, D100): synthesizes the text with a text-to-speech model of 3-Macaw Speech on
 * the tool's worker thread, sentence by sentence as `synthesizeToFile` does, into a file in the plugin's cache: `wav`
 * through `WavWriter`, `m4a` through [M4aWriter]. The file becomes the output `audio` and is deleted at once (the result
 * holds it open). Both writers stop at 8 MiB, the host's descriptor ceiling. A cancel or the end of the tool's time
 * stops within one sentence. Only counts, the model, the format and the times are logged, never the text.
 * zh-CN: `speech_synthesize` (路线图 P7.6.7, D98, D100): 在工具工作线程上用 3-Macaw Speech 的 TTS 模型合成文字, 与 `synthesizeToFile` 一样逐句进行,
 * 写入插件缓存中的文件: `wav` 经 `WavWriter`, `m4a` 经 [M4aWriter]. 该文件成为输出 `audio` 后立即删除 (结果持有其打开的描述符). 两种写入器都在
 * 宿主描述符上限 8 MiB 处停止. 取消或工具时限到达时在一句之内停止. 日志只记数量, 模型, 格式与时间, 从不记文字.
 */
internal class SpeechSynthesizeTool(context: Context) {
    private val app = context.applicationContext
    private val runtime = SpeechRuntime.get(app)

    fun call(call: PluginToolCall, request: SynthesizeRequest): PluginToolResult {
        val started = now()
        val deadline = started + (call.timeoutMs.takeIf { it > 0 } ?: SpeechToolCatalog.LONG_TIMEOUT_MS)
        val plan = SynthesizeRules.plan(request, runtime.voiceTable())
        val model = runtime.resolve(SpeechContract.KIND_TTS, plan.voice.modelId)
        val file = File.createTempFile("synthesize-", ".${request.format}", File(app.cacheDir, CACHE_DIRECTORY).apply { mkdirs() })
        try {
            runtime.acquireTts(model).use { lease ->
                val engine = lease.engine
                val loaded = now()
                var expired = false
                val (outcome, bytes) = writer(file, request.format, engine.sampleRate).use { writer ->
                    val outcome = TtsSynthesis.run(
                        generator = engine,
                        text = request.text,
                        speakerId = SynthesizeRules.speakerId(plan.voice, engine.speakerCount),
                        speed = request.speed,
                        isCancelled = { call.isCancelled || (now() >= deadline).also { if (it) expired = true } },
                        onProgress = { _, _ -> },
                        sink = { samples ->
                            writer.write(samples)
                            true
                        },
                    )
                    if (call.isCancelled) throw SynthesizeRules.cancelled()
                    if (outcome.cancelled) throw if (expired) SynthesizeRules.expired() else SynthesizeRules.cancelled()
                    if (outcome.samples == 0L) throw SynthesizeRules.silent(plan.voice.name)
                    outcome to writer.finish()
                }
                val durationMillis = outcome.samples * 1000 / engine.sampleRate
                val json = SynthesizeRules.result(request.format, model.modelId, plan.voice.name, engine.sampleRate, durationMillis, bytes, plan.warnings)
                Log.i(
                    TAG,
                    "Synthesized ${request.text.length} chars with ${model.modelId} to ${request.format}: $durationMillis ms audio, $bytes bytes in " +
                        "${now() - loaded} ms (load ${loaded - started} ms)",
                )
                return PluginToolResult(json).withOutput(SpeechToolCatalog.OUTPUT_AUDIO, SynthesizeRules.mimeType(request.format), file)
            }
        } finally {
            file.delete()
        }
    }

    private fun writer(file: File, format: String, sampleRate: Int): AudioFileWriter =
        if (format == SpeechToolCatalog.FORMAT_M4A) M4aWriter(file, sampleRate) else WavFileWriter(file, sampleRate)

    private companion object {
        const val TAG = "MacawTools"
        const val CACHE_DIRECTORY = "tool-synthesize"

        fun now(): Long = SystemClock.elapsedRealtime()
    }
}

/** A writer of synthesized samples into the output file. zh-CN: 把合成采样写入输出文件的写入器. */
internal interface AudioFileWriter : Closeable {
    fun write(samples: FloatArray)

    /** Completes the file; answers its size. zh-CN: 完成文件; 返回其大小. */
    fun finish(): Long
}

/** `WavWriter` on a file of its own, within the output ceiling. zh-CN: 写自己文件的 `WavWriter`, 不超过输出上限. */
private class WavFileWriter(file: File, sampleRate: Int) : AudioFileWriter {
    private val stream = FileOutputStream(file)
    private val wav = try {
        WavWriter(stream.channel, sampleRate, SynthesizeRules.MAX_OUTPUT_BYTES)
    } catch (e: Exception) {
        stream.close()
        throw e
    }

    override fun write(samples: FloatArray) = wav.write(samples)

    override fun finish(): Long = wav.finish()

    override fun close() = stream.close()
}
