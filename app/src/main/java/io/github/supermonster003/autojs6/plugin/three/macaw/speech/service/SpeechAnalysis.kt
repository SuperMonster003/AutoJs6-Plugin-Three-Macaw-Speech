package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.AnalysisOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.AnalysisRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.Diarization
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.DiarizeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageWindow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerName
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerTurn
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MonoAudio
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.AnalysisEngines
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.DiarizationEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineCache
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineKey
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaEngines
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WindowSplitter
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * The offline analyses of contract version 2 (roadmap P8.4, D146 - D151): diarization, spoken language identification
 * and audio tagging of one file. The Binder calls check the request and resolve the models, then answer an operation id
 * at once; the work runs one analysis at a time on the `MacawAnalysis` thread (later requests queue, D150), decodes the
 * file into 16 kHz memory (largeHeap) and leases its engines from [SpeechRuntime.engines]. Progress counts milliseconds
 * of the file's audio as the share of the work done and is repeated every [HEARTBEAT_MILLIS] (also while queued), so
 * the host's renewing window never runs out during the segmentation, which reports nothing. A cancel answers
 * `SESSION_CANCELLED` at once: decoding and the windowed analyses stop at the next chunk, a diarization runs to its end in
 * the background and is dropped. The tools and the screens call [diarize], [detectLanguage] and [tag] directly, which
 * queue behind any running analysis of the process.
 * zh-CN: 契约版本 2 的离线分析 (路线图 P8.4, D146 - D151): 对一个文件做说话人分离, 语种识别与音频标签. Binder 调用检查请求并解析模型, 然后立即返回
 * 操作 ID; 工作在 `MacawAnalysis` 线程上一次一个地运行 (后来的请求排队, D150), 把文件解码到 16 kHz 内存 (largeHeap) 并从 [SpeechRuntime.engines]
 * 租用引擎. 进度以文件音频的毫秒按已完成的工作比例计算, 每 [HEARTBEAT_MILLIS] 重复一次 (排队时也是), 因此在不报告进度的分割阶段宿主的续期窗口也不会
 * 耗尽. 取消立即应答 `SESSION_CANCELLED`: 解码与按窗分析在下一块停止, 说话人分离在后台运行到结束后丢弃结果. 工具与界面直接调用 [diarize],
 * [detectLanguage] 与 [tag], 它们排在进程内正在运行的分析之后.
 */
internal class SpeechAnalysis(private val runtime: SpeechRuntime, private val speakers: () -> SpeakerRecognition) {

    /** How one analysis reports and stops: [progress] takes (done, total) milliseconds. zh-CN: 一次分析的进度报告与停止方式. */
    class Control(val cancel: CancelToken = CancelToken(), val progress: (Long, Long) -> Unit = { _, _ -> })

    private val worker = Executors.newSingleThreadExecutor { Thread(it, "MacawAnalysis").apply { isDaemon = true } }
    private val beat = Executors.newSingleThreadScheduledExecutor { Thread(it, "MacawAnalysisBeat").apply { isDaemon = true } }
    private val operations = ConcurrentHashMap<Int, Operation>()
    private val busy = ReentrantLock()

    // ------------------------------------------------------------------ Binder operations

    fun startDiarize(audio: ParcelFileDescriptor?, request: Bundle?, callback: ISpeechProgressCallback?): Int {
        val options = AnalysisOptions.diarize(request?.let(::BundleOptionSource))
        val models = diarizationModels(options)
        return start(audio, callback, "diarize") { reader, control -> SpeechBundles.diarization(diarize(reader, options, control, models)) }
    }

    fun startLanguage(audio: ParcelFileDescriptor?, request: Bundle?, callback: ISpeechProgressCallback?): Int {
        val options = AnalysisOptions.language(request?.let(::BundleOptionSource))
        val model = runtime.resolve(SpeechContract.KIND_LANGID, options.model)
        return start(audio, callback, "detectLanguage") { reader, control -> SpeechBundles.language(detectLanguage(reader, options, control, model)) }
    }

    fun startTag(audio: ParcelFileDescriptor?, request: Bundle?, callback: ISpeechProgressCallback?): Int {
        val options = AnalysisOptions.tag(request?.let(::BundleOptionSource))
        val model = runtime.resolve(SpeechContract.KIND_TAGGER, options.model)
        return start(audio, callback, "tagAudio") { reader, control -> SpeechBundles.tags(tag(reader, options, control, model)) }
    }

    /** Cancels a queued or running analysis, answering `SESSION_CANCELLED` at once; other ids are ignored. zh-CN: 取消排队或运行中的分析. */
    fun cancel(opId: Int) {
        val operation = operations[opId] ?: return
        operation.control.cancel.cancel()
        operation.fail(SpeechBundles.failure(SpeechErrorCodes.SESSION_CANCELLED, "The analysis was cancelled", SpeechContract.CALL_VERSION_2))
    }

    private inner class Operation(val id: Int, private val callback: ISpeechProgressCallback?) {
        private val settled = AtomicBoolean(false)

        @Volatile
        private var last = 0L to 0L

        val control = Control(CancelToken()) { done, total ->
            last = done to total
            report()
        }

        fun report() {
            if (!settled.get()) runCatching { callback?.onProgress(id, last.first, last.second) }
        }

        fun complete(result: Bundle) {
            if (settled.compareAndSet(false, true)) runCatching { callback?.onCompleted(id, result) }
        }

        fun fail(error: Bundle) {
            if (settled.compareAndSet(false, true)) runCatching { callback?.onFailed(id, error) }
        }
    }

    private fun start(audio: ParcelFileDescriptor?, callback: ISpeechProgressCallback?, label: String, work: (MonoAudio, Control) -> Bundle): Int {
        val source = audio ?: throw SpeechException(SpeechErrorCodes.IO_FAILED, "An audio descriptor is required")
        val copy = try {
            source.dup()
        } catch (e: IOException) {
            throw SpeechException(SpeechErrorCodes.IO_FAILED, "The audio descriptor cannot be used: ${e.message}", e)
        }
        val operation = Operation(runtime.models.nextOperationId(), callback)
        operations[operation.id] = operation
        val death = IBinder.DeathRecipient { operation.control.cancel.cancel() }
        val binder = callback?.asBinder()
        runCatching { binder?.linkToDeath(death, 0) }
        val heartbeat: ScheduledFuture<*> = beat.scheduleWithFixedDelay({ operation.report() }, HEARTBEAT_MILLIS, HEARTBEAT_MILLIS, TimeUnit.MILLISECONDS)
        runCatching { callback?.onProgress(operation.id, 0, 0) }
        try {
            worker.execute {
                try {
                    val result = copy.use { descriptor ->
                        operation.control.cancel.throwIfCancelledAsSpeech()
                        AudioFiles.open(descriptor).use { reader -> work(reader, operation.control) }
                    }
                    operation.complete(result)
                } catch (e: SpeechException) {
                    operation.fail(SpeechBundles.failure(e, SpeechContract.CALL_VERSION_2))
                } catch (e: IOException) {
                    operation.fail(SpeechBundles.failure(SpeechErrorCodes.IO_FAILED, "Reading the audio failed: ${e.message}", SpeechContract.CALL_VERSION_2))
                } catch (e: OutOfMemoryError) {
                    Log.w(TAG, "Analysis ${operation.id} ($label) ran out of memory; ${SpeechRuntime.memory()}")
                    operation.fail(SpeechBundles.failure(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio is too long for this device's memory", SpeechContract.CALL_VERSION_2))
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Analysis ${operation.id} ($label) failed", e)
                    operation.fail(SpeechBundles.failure(SpeechErrorCodes.ENGINE_FAILED, e.message ?: e.javaClass.simpleName, SpeechContract.CALL_VERSION_2))
                } finally {
                    heartbeat.cancel(false)
                    operations.remove(operation.id)
                    runCatching { binder?.unlinkToDeath(death, 0) }
                }
            }
        } catch (e: RejectedExecutionException) {
            heartbeat.cancel(false)
            operations.remove(operation.id)
            runCatching { copy.close() }
            throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "The analysis thread is not available", e)
        }
        return operation.id
    }

    // ------------------------------------------------------------------ the analyses (any thread, one engine call at a time)

    /** The segmentation and speaker models of a diarization (both must be installed, D146). zh-CN: 分离所需的分割与说话人模型. */
    fun diarizationModels(options: DiarizeOptions): Pair<InstalledModel, InstalledModel> =
        runtime.resolve(SpeechContract.KIND_SEGMENT, options.model) to runtime.resolve(SpeechContract.KIND_SPEAKER, options.speakerModel)

    fun diarize(reader: MonoAudio, options: DiarizeOptions, control: Control, models: Pair<InstalledModel, InstalledModel> = diarizationModels(options)): Diarization =
        exclusive(control) { diarizeNow(reader, options, control, models).first }

    /**
     * [diarize] that also hands back the decoded 16 kHz samples, from which "diarize and transcribe" of the file tab
     * recognizes each turn (roadmap D153).
     * zh-CN: 同时交回已解码 16 kHz 采样的 [diarize], 文件标签的 "分离 + 转写" 从中识别每个轮次 (路线图 D153).
     */
    fun diarizeKeepingAudio(reader: MonoAudio, options: DiarizeOptions, control: Control, models: Pair<InstalledModel, InstalledModel> = diarizationModels(options)): Pair<Diarization, FloatArray> =
        exclusive(control) { diarizeNow(reader, options, control, models) }

    fun detectLanguage(reader: MonoAudio, options: LanguageOptions, control: Control, model: InstalledModel = runtime.resolve(SpeechContract.KIND_LANGID, options.model)): LanguageResult =
        exclusive(control) { detectLanguageNow(reader, options, control, model) }

    fun tag(reader: MonoAudio, options: TagOptions, control: Control, model: InstalledModel = runtime.resolve(SpeechContract.KIND_TAGGER, options.model)): TagResult =
        exclusive(control) { tagNow(reader, options, control, model) }

    /**
     * Runs one analysis at a time in the process (D150): the host's operations, the tools and the screens queue here, and a
     * cancel stops the wait.
     * zh-CN: 进程内一次只运行一个分析 (D150): 宿主的操作, 工具与界面都在此排队, 取消可结束等待.
     */
    private inline fun <T> exclusive(control: Control, block: () -> T): T {
        while (!busy.tryLock(QUEUE_POLL_MILLIS, TimeUnit.MILLISECONDS)) {
            if (control.cancel.isCancelled) throw cancelled()
        }
        try {
            if (control.cancel.isCancelled) throw cancelled()
            return block()
        } finally {
            busy.unlock()
        }
    }

    private fun diarizeNow(reader: MonoAudio, options: DiarizeOptions, control: Control, models: Pair<InstalledModel, InstalledModel>): Pair<Diarization, FloatArray> {
        val (segment, speaker) = models
        val started = SystemClock.elapsedRealtime()
        val samples = decode(reader, control, DECODE_SHARE_DIARIZATION)
        val total = AudioInput.millis(samples.size.toLong())
        val threshold = options.threshold ?: AnalysisEngines.clusterThreshold(speaker.modelId)
        val turns = diarizationLease(segment, speaker).use { lease ->
            val segments = lease.engine.process(samples, options.speakers, threshold) { done, all ->
                if (all > 0) control.progress(share(total, DECODE_SHARE_DIARIZATION + (1 - DECODE_SHARE_DIARIZATION) * done / all), total)
            }
            AnalysisRules.turns(segments)
        }
        if (control.cancel.isCancelled) throw cancelled()
        val count = turns.maxOfOrNull { it.speaker }?.plus(1) ?: 0
        val names = if (options.identify) identify(samples, turns, count, speaker, control) else null
        Log.i(TAG, "Diarized $total ms into ${turns.size} turns of $count speaker(s) in ${SystemClock.elapsedRealtime() - started} ms; ${SpeechRuntime.memory()}")
        return Diarization(turns, count, total, segment.modelId, speaker.modelId, names) to samples
    }

    private fun detectLanguageNow(reader: MonoAudio, options: LanguageOptions, control: Control, model: InstalledModel): LanguageResult {
        val started = SystemClock.elapsedRealtime()
        val declared = reader.durationMillis ?: 0L
        val windows = AnalysisRules.SpeechWindows(options.windows)
        var decoded = 0L
        SherpaEngines.vad(runtime.context.assets, SpeechRuntime.SESSION_VAD).use { vad ->
            val splitter = WindowSplitter(vad.windowSize)
            fun drain() {
                while (true) windows.accept(vad.poll() ?: return)
            }
            AudioInput.decode(reader, cancelled = { control.cancel.isCancelled || windows.isFull }) { chunk ->
                decoded += chunk.size
                if (AudioInput.millis(decoded) > SpeechLimits.MAX_AUDIO_MILLIS) throw tooLong()
                splitter.split(chunk) { window ->
                    vad.accept(window)
                    drain()
                }
                control.progress(share(declared, HALF * fraction(AudioInput.millis(decoded), declared)), declared)
            }
            if (control.cancel.isCancelled) throw cancelled()
            if (!windows.isFull) {
                splitter.remainder()?.let { vad.accept(it) }
                vad.flush()
                drain()
            }
        }
        val pieces = windows.finish()
        val total = maxOf(declared, AudioInput.millis(decoded))
        val heard = languageLease(model).use { lease ->
            pieces.mapIndexed { index, (range, speech) ->
                if (control.cancel.isCancelled) throw cancelled()
                val language = lease.engine.identify(speech)
                control.progress(share(total, HALF + HALF * (index + 1) / pieces.size), total)
                LanguageWindow(AudioInput.millis(range.first), AudioInput.millis(range.last + 1), language)
            }
        }
        val candidates = AnalysisRules.vote(heard)
        Log.i(TAG, "Identified the language of ${heard.size} window(s) in ${SystemClock.elapsedRealtime() - started} ms")
        return LanguageResult(candidates.first().first, candidates, heard, total, model.modelId)
    }

    private fun tagNow(reader: MonoAudio, options: TagOptions, control: Control, model: InstalledModel): TagResult {
        val started = SystemClock.elapsedRealtime()
        val samples = decode(reader, control, DECODE_SHARE_TAG)
        val total = AudioInput.millis(samples.size.toLong())
        val ranges = AnalysisRules.tagWindows(samples.size.toLong())
        val scored = taggerLease(model).use { lease ->
            ranges.mapIndexed { index, range ->
                if (control.cancel.isCancelled) throw cancelled()
                val scores = lease.engine.scores(samples.copyOfRange(range.first.toInt(), range.last.toInt() + 1))
                control.progress(share(total, DECODE_SHARE_TAG + (1 - DECODE_SHARE_TAG) * (index + 1) / ranges.size), total)
                range to scores
            }
        }
        val (labels, timeline) = AnalysisRules.aggregate(scored, options.topK, options.timeline)
        Log.i(TAG, "Tagged ${ranges.size} window(s) in ${SystemClock.elapsedRealtime() - started} ms")
        return TagResult(labels, timeline, options.topK, total, model.modelId)
    }

    /** `identify`: each diarized speaker's longest turns against the voiceprints of [speaker] (D147). zh-CN: 用最长的轮次辨认每位说话人. */
    private fun identify(samples: FloatArray, turns: List<SpeakerTurn>, count: Int, speaker: InstalledModel, control: Control): List<SpeakerName> {
        val recognition = speakers()
        return (0 until count).map { number ->
            if (control.cancel.isCancelled) throw cancelled()
            val speech = AnalysisRules.cut(samples, AnalysisRules.identifySpans(turns, number))
            val match = try {
                recognition.closest(speech, speaker)
            } catch (e: SpeechException) {
                if (e.code != SpeechErrorCodes.AUDIO_TOO_SHORT) throw e
                null
            }
            SpeakerName(number, match?.takeIf { it.matched }?.best?.name, match?.best?.score)
        }
    }

    /**
     * The whole file as 16 kHz samples, at most `MAX_AUDIO_MILLIS` (`LIMIT_EXCEEDED`); progress covers [share] of the
     * work over the declared length.
     * zh-CN: 整个文件的 16 kHz 采样, 至多 `MAX_AUDIO_MILLIS` (`LIMIT_EXCEEDED`); 进度按声明的长度覆盖工作的 [share].
     */
    private fun decode(reader: MonoAudio, control: Control, share: Double): FloatArray {
        val declared = reader.durationMillis ?: 0L
        val limit = AudioInput.samples(SpeechLimits.MAX_AUDIO_MILLIS)
        var buffer = FloatArray(AudioInput.samples(declared.coerceIn(1000L, SpeechLimits.MAX_AUDIO_MILLIS)).toInt() + AudioInput.SAMPLE_RATE)
        var size = 0
        AudioInput.decode(reader, cancelled = { control.cancel.isCancelled }) { chunk ->
            if (size + chunk.size > limit) throw tooLong()
            if (size + chunk.size > buffer.size) buffer = buffer.copyOf(minOf(limit, maxOf(size + chunk.size, buffer.size + buffer.size / 2).toLong()).toInt())
            System.arraycopy(chunk, 0, buffer, size, chunk.size)
            size += chunk.size
            control.progress(share(declared, share * fraction(AudioInput.millis(size.toLong()), declared)), declared)
        }
        if (control.cancel.isCancelled) throw cancelled()
        return if (size == buffer.size) buffer else buffer.copyOf(size)
    }

    private fun diarizationLease(segment: InstalledModel, speaker: InstalledModel): EngineCache.Lease<DiarizationEngine> =
        runtime.engines.acquire(EngineKey(segment.modelId, segment.versionCode, "diarization:${speaker.modelId}:${speaker.versionCode}"), segment.installedBytes + speaker.installedBytes) {
            AnalysisEngines.diarization(segment, speaker)
        }

    private fun languageLease(model: InstalledModel) =
        runtime.engines.acquire(EngineKey(model.modelId, model.versionCode, CatalogRules.KIND_LANGID), model.installedBytes) { AnalysisEngines.language(model) }

    private fun taggerLease(model: InstalledModel) =
        runtime.engines.acquire(EngineKey(model.modelId, model.versionCode, CatalogRules.KIND_TAGGER), model.installedBytes) { AnalysisEngines.tagger(model) }

    private fun share(total: Long, part: Double): Long = (total * part.coerceIn(0.0, 1.0)).toLong()

    private fun fraction(done: Long, total: Long): Double = if (total <= 0) 0.0 else (done.toDouble() / total).coerceIn(0.0, 1.0)

    private fun CancelToken.throwIfCancelledAsSpeech() {
        if (isCancelled) throw cancelled()
    }

    private fun cancelled() = SpeechException(SpeechErrorCodes.SESSION_CANCELLED, "The analysis was cancelled")

    private fun tooLong() = SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio is longer than ${SpeechLimits.MAX_AUDIO_MILLIS / 60_000} minutes")

    companion object {
        private const val TAG = "MacawAnalysis"

        @Volatile
        private var instance: SpeechAnalysis? = null

        /** The process-wide instance (one analysis at a time for the host, the tools and the screens). zh-CN: 进程内唯一实例 (宿主, 工具与界面一次一个分析). */
        fun get(context: Context): SpeechAnalysis = instance ?: synchronized(this) {
            instance ?: SpeechAnalysis(SpeechRuntime.get(context)) { SpeakerRecognition.get(context) }.also { instance = it }
        }

        /** Progress repeats this often while an analysis is queued or in a phase without progress. zh-CN: 排队或无进度阶段时重复进度的间隔. */
        const val HEARTBEAT_MILLIS = 30_000L

        /** How often a queued analysis looks at its cancel token. zh-CN: 排队中的分析检查取消标记的间隔. */
        private const val QUEUE_POLL_MILLIS = 200L

        /** Shares of the work the decoding takes (the rest is the engine's). zh-CN: 解码在工作中所占的比例 (其余为引擎). */
        private const val DECODE_SHARE_DIARIZATION = 0.1
        private const val DECODE_SHARE_TAG = 0.3
        private const val HALF = 0.5
    }
}
