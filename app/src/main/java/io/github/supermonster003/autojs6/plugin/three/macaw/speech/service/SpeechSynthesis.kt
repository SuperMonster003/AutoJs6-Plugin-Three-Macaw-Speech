package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SynthesizeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsSynthesis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.WavWriter
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException

/**
 * `synthesizeToFile` of contract version 1 (roadmap D20, P4.3). The Binder call checks the request (text, model,
 * speaker, speed, format; the ceilings of `SpeechLimits`), resolves the model and the speaker and answers the operation
 * id at once; the synthesis runs on the [SpeechRuntime.tts] thread: the model loads there (never on the Binder path,
 * roadmap D30), the text is synthesized sentence by sentence into a PCM16 WAV at the model's sample rate written to a
 * duplicate of the host's descriptor, progress counts the characters done, and the operation completes or fails exactly
 * once. A cancel (or the death of the host's callback) stops within one sentence and fails with `SESSION_CANCELLED`;
 * the host discards the half written file. A single speaker model ignores a requested speaker and says so in
 * `warnings`.
 * zh-CN: 契约版本 1 的 `synthesizeToFile` (路线图 D20, P4.3). Binder 调用检查请求 (文本, 模型, 说话人, 语速, 格式; `SpeechLimits` 的上限), 解析
 * 模型与说话人并立即返回操作 ID; 合成在 [SpeechRuntime.tts] 线程上进行: 模型在该线程加载 (从不在 Binder 路径上, 路线图 D30), 文本逐句合成为
 * 模型采样率的 PCM16 WAV, 写入宿主描述符的副本, 进度按已完成的字符计算, 操作恰好完成或失败一次. 取消 (或宿主回调死亡) 在一句之内停止并以
 * `SESSION_CANCELLED` 失败; 写了一半的文件由宿主丢弃. 单说话人模型忽略所请求的说话人, 并在 `warnings` 中说明.
 */
internal class SpeechSynthesis(private val runtime: SpeechRuntime) {

    private class Operation(val id: Int) {
        val cancel = CancelToken()
    }

    private val operations = ConcurrentHashMap<Int, Operation>()

    fun start(request: Bundle?, output: ParcelFileDescriptor?, callback: ISpeechProgressCallback?): Int {
        val options = SpeechOptions.synthesize(request?.let(::BundleOptionSource))
        val target = output ?: throw SpeechException(SpeechErrorCodes.IO_FAILED, "An output descriptor is required")
        val model = runtime.resolve(SpeechContract.KIND_TTS, options.model)
        // A speaker the model does not have fails the call itself; the loaded engine confirms the count later.
        runtime.speakers(model).resolve(model.modelId, options.speaker)
        val copy = try {
            target.dup()
        } catch (e: IOException) {
            throw SpeechException(SpeechErrorCodes.IO_FAILED, "The output descriptor cannot be used: ${e.message}", e)
        }
        val operation = Operation(runtime.models.nextOperationId())
        operations[operation.id] = operation
        val death = IBinder.DeathRecipient { operation.cancel.cancel() }
        val binder = callback?.asBinder()
        runCatching { binder?.linkToDeath(death, 0) }
        try {
            runtime.tts.execute {
                try {
                    run(operation, options, model, copy, callback)
                } finally {
                    operations.remove(operation.id)
                    runCatching { binder?.unlinkToDeath(death, 0) }
                }
            }
        } catch (e: RejectedExecutionException) {
            operations.remove(operation.id)
            runCatching { copy.close() }
            throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "The synthesis thread is not available", e)
        }
        return operation.id
    }

    /** Cancels a queued or running synthesis; other ids are ignored. zh-CN: 取消排队或运行中的合成; 忽略其他 ID. */
    fun cancel(opId: Int) {
        operations[opId]?.cancel?.cancel()
    }

    private fun run(operation: Operation, options: SynthesizeOptions, model: InstalledModel, output: ParcelFileDescriptor, callback: ISpeechProgressCallback?) {
        val id = operation.id
        val total = options.text.length.toLong()
        val result = try {
            ParcelFileDescriptor.AutoCloseOutputStream(output).use { stream ->
                if (operation.cancel.isCancelled) throw cancelled()
                runCatching { callback?.onProgress(id, 0, total) }
                val started = SystemClock.elapsedRealtime()
                runtime.acquireTts(model).use { lease ->
                    val engine = lease.engine
                    val loadedAt = SystemClock.elapsedRealtime()
                    val choice = engine.speakers.resolve(model.modelId, options.speaker)
                    val writer = WavWriter(stream.channel, engine.sampleRate)
                    val outcome = TtsSynthesis.run(
                        generator = engine,
                        text = options.text,
                        speakerId = choice.id,
                        speed = options.speed,
                        isCancelled = { operation.cancel.isCancelled },
                        onProgress = { done, all -> runCatching { callback?.onProgress(id, done.toLong(), all.toLong()) } },
                        sink = { samples ->
                            writer.write(samples)
                            true
                        },
                    )
                    if (outcome.cancelled) throw cancelled()
                    val size = writer.finish()
                    val finished = SystemClock.elapsedRealtime()
                    Log.i(TAG, "Synthesized ${options.text.length} chars with ${model.modelId}: ${writer.durationMillis} ms audio in ${finished - loadedAt} ms (load ${loadedAt - started} ms); ${SpeechRuntime.memory()}")
                    SpeechBundles.answer {
                        putInt(SpeechResultKeys.SAMPLE_RATE, engine.sampleRate)
                        putLong(SpeechResultKeys.DURATION_MILLIS, writer.durationMillis)
                        putString(SpeechResultKeys.MODEL_ID, model.modelId)
                        putInt(SpeechResultKeys.SPEAKER_ID, choice.id)
                        putLong(SpeechResultKeys.SIZE_BYTES, size)
                        putParcelableArrayList(SpeechResultKeys.WARNINGS, ArrayList(listOfNotNull(choice.warning).map { SpeechBundles.problem(it.code, it.message) }))
                    }
                }
            }
        } catch (e: SpeechException) {
            fail(callback, id, SpeechBundles.failure(e))
            return
        } catch (e: IOException) {
            fail(callback, id, SpeechBundles.failure(SpeechErrorCodes.IO_FAILED, "Writing the synthesized audio failed: ${e.message}"))
            return
        } catch (e: RuntimeException) {
            Log.w(TAG, "Synthesis $id failed", e)
            fail(callback, id, SpeechBundles.failure(SpeechErrorCodes.ENGINE_FAILED, e.message ?: e.javaClass.simpleName))
            return
        }
        runCatching { callback?.onCompleted(id, result) }
    }

    private fun cancelled() = SpeechException(SpeechErrorCodes.SESSION_CANCELLED, "The synthesis was cancelled")

    private fun fail(callback: ISpeechProgressCallback?, id: Int, error: Bundle) {
        runCatching { callback?.onFailed(id, error) }
    }

    private companion object {
        const val TAG = "MacawTts"
    }
}
