package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.SystemClock
import android.provider.OpenableColumns
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.Diarization
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.DiarizeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerTurn
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.PcmAudio
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CancelToken
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAnalysis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileFailure
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileRecognitionListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.RecognizeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechWarning
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.FileRecognitionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechAnalysis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** What a picked document tells about itself; either part may be unknown. zh-CN: 所选文档自述的信息; 两项都可能未知. */
internal data class FileDescription(val displayName: String?, val sizeBytes: Long?)

/**
 * The audio file of one run (roadmap D110): a document picked with `ACTION_OPEN_DOCUMENT`, or a plain file of the device
 * tests, which skip the system picker. Both calls run on the recognition worker, since a cloud provider may block while
 * it downloads the document.
 * zh-CN: 一次识别的音频文件 (路线图 D110): 经 `ACTION_OPEN_DOCUMENT` 选择的文档, 或设备测试绕过系统选择器使用的普通文件. 两个调用都在识别工作线程上
 * 进行, 因为云盘提供方下载文档时可能阻塞.
 */
internal interface FileSource {
    /** Where the file can be opened again after a restart (D170), if at all. zh-CN: 重启后能再次打开文件的位置 (D170), 没有时为 null. */
    val uri: String? get() = null

    fun describe(): FileDescription

    fun open(): ParcelFileDescriptor

    companion object {
        fun of(context: Context, uri: Uri): FileSource = DocumentSource(context.applicationContext, uri)

        fun of(file: File): FileSource = object : FileSource {
            override val uri get() = Uri.fromFile(file).toString()

            override fun describe() = FileDescription(file.name, file.length())
            override fun open(): ParcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
}

private class DocumentSource(private val context: Context, private val document: Uri) : FileSource {

    override val uri get() = document.toString()

    override fun describe(): FileDescription = context.contentResolver.query(document, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getString)
        val size = cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong)
        FileDescription(name?.takeIf { it.isNotBlank() }, size?.takeIf { it >= 0 })
    } ?: FileDescription(null, null)

    override fun open(): ParcelFileDescriptor = context.contentResolver.openFileDescriptor(document, "r") ?: throw FileNotFoundException("The provider returned no descriptor")
}

/**
 * The choices of the file tab when a run starts (roadmap D114, D153): the action, the recognizer settings of the
 * transcribing actions ([modelId] is null when no recognizer is installed), the speaker count (0: automatic) and
 * identify switch of the diarizing ones and the language identification model.
 * zh-CN: 开始时文件标签上的选择 (路线图 D114, D153): 操作, 转写类操作的识别设置 (没有安装识别模型时 [modelId] 为 null), 分离类操作的说话人数
 * (0 为自动) 与认人开关, 以及语种识别模型.
 */
internal data class FileRunOptions(
    val modelId: String?,
    val language: String?,
    val punctuation: Boolean,
    val vad: Boolean,
    val action: FileAction = FileAction.TRANSCRIBE,
    val speakers: Int = 0,
    val identify: Boolean = false,
    val languageModel: String? = null,
)

/**
 * A run in progress: [declaredMillis] is the length the file declares (null until it is open or when unknown),
 * [recognizedMillis] how much audio went to the recognizer, [segments] the finals so far. The analyses (roadmap D153)
 * count [recognizedMillis] of [declaredMillis] as the share of their work done; "diarize and transcribe" has the
 * speaker of each segment in [segmentSpeakers] once [speakers] is known.
 * zh-CN: 进行中的识别: [declaredMillis] 为文件声明的时长 (打开之前或未知时为 null), [recognizedMillis] 为已送入识别的音频时长, [segments] 为目前的整句.
 * 分析 (路线图 D153) 以 [recognizedMillis] / [declaredMillis] 表示已完成的工作比例; "分离 + 转写" 在 [speakers] 已知后于 [segmentSpeakers]
 * 给出每个分段的说话人.
 */
internal data class FileRun(
    val phase: Phase,
    val action: FileAction = FileAction.TRANSCRIBE,
    val stage: Stage = Stage.TRANSCRIBING,
    val displayName: String? = null,
    val sizeBytes: Long? = null,
    val declaredMillis: Long? = null,
    val recognizedMillis: Long = 0,
    val segments: List<Utterance> = emptyList(),
    val segmentSpeakers: List<Int> = emptyList(),
    val speakers: FileAnalysis.Speakers? = null,
) {
    /** Opening the file and loading the model, recognizing, stopping after a cancel. zh-CN: 打开文件与加载模型, 识别中, 取消后停止中. */
    enum class Phase { PREPARING, RUNNING, CANCELLING }

    /**
     * What the run does now: transcribing the file, one of the analyses, or transcribing the turns found by the
     * diarization.
     * zh-CN: 当前所做的工作: 转写文件, 某个分析, 或转写分离得到的轮次.
     */
    enum class Stage {
        TRANSCRIBING, DIARIZING, TURNS, LANGUAGE, TAGGING;

        companion object {
            fun first(action: FileAction): Stage = when (action) {
                FileAction.TRANSCRIBE -> TRANSCRIBING
                FileAction.DIARIZE, FileAction.DIARIZE_TRANSCRIBE -> DIARIZING
                FileAction.LANGUAGE -> LANGUAGE
                FileAction.TAG -> TAGGING
            }
        }
    }
}

/**
 * The file tab's recognition (roadmap D110 - D113, P7.8.2), one per process so that a run outlives the tryout screen:
 * one file at a time on its own worker thread, straight through `SpeechRuntime.recognizeFile` (no Binder), under a
 * partial wake lock of at most 60 minutes and the foreground service [FileRecognitionService]. A descriptor that cannot
 * seek (a pipe of some cloud providers) is copied to `cacheDir/tryout/input` first, within the 200 MB limit. Every
 * finished run (completed, cancelled with its segments so far, failed with its reason) is written to
 * `cacheDir/tryout/file-result.json` and read back once per process. [run] and [record] are Compose state, changed on
 * the main thread only; never log the file name or the recognized text (D85). The analyses of roadmap D153 run through
 * [SpeechAnalysis] (queued behind any analysis of the process); "diarize and transcribe" decodes the file once,
 * diarizes it and recognizes each turn from memory, the segments carrying their turn's speaker.
 * zh-CN: 文件标签的识别 (路线图 D110 - D113, P7.8.2), 每个进程一份, 因此识别不随试听试说页结束: 一次一个文件, 在自己的工作线程上直接调用
 * `SpeechRuntime.recognizeFile` (不经 Binder), 期间持最长 60 分钟的部分唤醒锁并运行前台服务 [FileRecognitionService]. 不能定位的描述符
 * (部分云盘返回的管道) 先在 200 MB 限制内复制到 `cacheDir/tryout/input`. 每次结束的识别 (完成, 取消时带已有分段, 失败时带原因) 写入
 * `cacheDir/tryout/file-result.json`, 每个进程读回一次. [run] 与 [record] 是 Compose 状态, 只在主线程上修改; 不记录文件名与识别文字 (D85).
 * 路线图 D153 的分析经 [SpeechAnalysis] 运行 (排在进程内其他分析之后); "分离 + 转写" 只解码一次文件, 分离后从内存逐个轮次识别, 分段带有所在轮次的说话人.
 */
internal class FileTranscription private constructor(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val cancelled = AtomicBoolean(false)
    private val directory = File(app.cacheDir, "tryout")
    private val resultFile = File(directory, RESULT_NAME)
    private val inputFile = File(directory, INPUT_NAME)
    private var loaded = false
    private var screens = 0

    @Volatile
    private var token = CancelToken()

    /** The source of the latest run of this process. zh-CN: 本进程最近一次识别的来源. */
    private var source: FileSource? = null

    /** The run in progress, or null. zh-CN: 进行中的识别, 没有时为 null. */
    var run by mutableStateOf<FileRun?>(null)
        private set

    /** The latest finished run. zh-CN: 最近一次结束的识别. */
    var record by mutableStateOf<FileTranscriptRecord?>(null)
        private set

    /** Whether a tryout screen is started (visible). zh-CN: 是否有试听试说页处于开始 (可见) 状态. */
    val screenVisible: Boolean get() = screens > 0

    /** Reads the cached result once per process. zh-CN: 每个进程读一次缓存的结果. */
    fun load() {
        if (loaded) return
        loaded = true
        worker.execute {
            val cached = read()
            main.post { if (run == null && record == null) record = cached }
        }
    }

    /** Starts a run unless one is in progress. zh-CN: 没有进行中的识别时开始识别. */
    fun start(source: FileSource, options: FileRunOptions): Boolean {
        if (run != null) return false
        // A new run replaces whatever the cache holds. zh-CN: 新的识别覆盖缓存中的内容.
        loaded = true
        cancelled.set(false)
        val stop = CancelToken()
        token = stop
        record = null
        this.source = source
        val first = FileRun(FileRun.Phase.PREPARING, options.action, FileRun.Stage.first(options.action))
        run = first
        FileRecognitionService.start(app, first)
        worker.execute { recognize(source, options, stop) }
        return true
    }

    /** The button of the tab, the notification action and the service time limit. zh-CN: 标签上的按钮, 通知动作与前台服务时间上限. */
    fun cancel() {
        val current = run ?: return
        if (current.phase == FileRun.Phase.CANCELLING) return
        cancelled.set(true)
        token.cancel()
        update(current.copy(phase = FileRun.Phase.CANCELLING))
    }

    /** Removes the result from the tab and the cache. zh-CN: 从标签与缓存中删除结果. */
    fun clear() {
        if (run != null) return
        record = null
        source = null
        holdDocument(null)
        worker.execute { resultFile.delete() }
    }

    /**
     * What plays [record]: the source of this process's run, else the document it names (roadmap D170).
     * zh-CN: 播放 [record] 的来源: 本进程识别的来源, 否则为记录写明的文档 (路线图 D170).
     */
    fun playbackSource(record: FileTranscriptRecord): FileSource? = source ?: record.sourceUri?.let { FileSource.of(app, Uri.parse(it)) }

    /**
     * Keeps the read grant of [document] over restarts and gives every other up, so the plugin holds at most one
     * (roadmap D170). A provider may refuse it: the file then plays only while the picker's grant lasts.
     * zh-CN: 让 [document] 的读取授权跨重启保留, 并放弃其他授权, 使插件至多持有一个 (路线图 D170). 提供方可能拒绝: 此时文件只在选择器的授权有效期内可播放.
     */
    fun holdDocument(document: Uri?) {
        val resolver = app.contentResolver
        runCatching {
            resolver.persistedUriPermissions.filter { it.uri != document }.forEach { resolver.releasePersistableUriPermission(it.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            if (document != null) resolver.takePersistableUriPermission(document, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { Log.i(TAG, "No lasting read grant: ${it.javaClass.simpleName}") }
    }

    /** What a restarted process finds in the cache. zh-CN: 重启后的进程在缓存中读到的内容. */
    @VisibleForTesting
    fun readBack() {
        source = null
        record = read()
    }

    fun screenStarted() {
        screens += 1
        FileRecognitionService.clearResult(app)
    }

    fun screenStopped() {
        screens = maxOf(screens - 1, 0)
    }

    private fun update(next: FileRun) {
        run = next
        FileRecognitionService.update(app, next)
    }

    // Worker -----------------------------------------------------------------------------------------------------

    /** What one run has found so far, published to the main thread. zh-CN: 一次运行目前得到的内容, 发布到主线程. */
    private inner class Job(private val options: FileRunOptions, private val sourceUri: String?) {
        var description = FileDescription(null, null)
        var stage = FileRun.Stage.first(options.action)
        var declared: Long? = null
        var fed = 0L
        var modelId = options.modelId.orEmpty()
        var speakers: FileAnalysis.Speakers? = null
        val segments = ArrayList<Utterance>()
        val segmentSpeakers = ArrayList<Int>()
        private var posted = 0L

        fun publish(recognizing: Boolean) {
            posted = SystemClock.elapsedRealtime()
            val name = description.displayName
            val size = description.sizeBytes
            val now = stage
            val total = declared
            val millis = fed
            val list = segments.toList()
            val owners = segmentSpeakers.toList()
            val found = speakers
            main.post {
                val current = run ?: return@post
                val phase = if (recognizing && current.phase == FileRun.Phase.PREPARING) FileRun.Phase.RUNNING else current.phase
                update(
                    current.copy(
                        phase = phase, stage = now, displayName = name, sizeBytes = size, declaredMillis = total, recognizedMillis = millis,
                        segments = list, segmentSpeakers = owners, speakers = found,
                    ),
                )
            }
        }

        fun progress(done: Long, total: Long) {
            fed = done
            declared = total.takeIf { it > 0 }
            if (SystemClock.elapsedRealtime() - posted >= PROGRESS_INTERVAL_MILLIS) publish(recognizing = true)
        }

        /** Recognition of the whole file ([turn] null) or of one turn starting [offset] ms into the work of the turns. zh-CN: 整个文件或一个轮次的识别. */
        fun listener(turn: SpeakerTurn? = null, offset: Long = 0) = object : FileRecognitionListener {
            override fun opened(durationMillis: Long?) {
                if (turn == null) declared = durationMillis
                publish(recognizing = true)
            }

            override fun progress(millis: Long) {
                fed = offset + millis
                if (SystemClock.elapsedRealtime() - posted >= PROGRESS_INTERVAL_MILLIS) publish(recognizing = true)
            }

            override fun segment(utterance: Utterance) {
                // The record keeps no emotion or event, so the shown result equals the one read back from the cache.
                // zh-CN: 记录不保存情绪与事件, 因此显示的结果与从缓存读回的相同.
                val plain = utterance.copy(emotion = null, event = null)
                if (turn == null) {
                    segments += plain
                } else {
                    segments += plain.copy(segmentIndex = segments.size, startMillis = utterance.startMillis + turn.startMillis, endMillis = utterance.endMillis + turn.startMillis)
                    segmentSpeakers += turn.speaker
                }
                publish(recognizing = true)
            }
        }

        fun record(
            state: FileTranscriptRecord.State,
            language: String? = null,
            recognized: Long = fed,
            warnings: List<SpeechWarning> = emptyList(),
            analysis: FileAnalysis? = speakers,
            failure: FileFailure? = null,
        ) = FileTranscriptRecord(
            state = state,
            displayName = description.displayName,
            sizeBytes = description.sizeBytes,
            modelId = modelId,
            language = language,
            punctuation = options.punctuation,
            declaredMillis = declared,
            recognizedMillis = recognized,
            segments = segments.toList(),
            warnings = warnings,
            failure = failure,
            action = options.action,
            analysis = analysis,
            segmentSpeakers = if (options.action == FileAction.DIARIZE_TRANSCRIBE) segmentSpeakers.toList() else emptyList(),
            sourceUri = sourceUri,
        )

        /** The language most segments report. zh-CN: 多数分段报告的语言. */
        fun spokenLanguage(): String? = segments.mapNotNull { it.language }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }

    private fun recognize(source: FileSource, options: FileRunOptions, stop: CancelToken) {
        val lock = acquireWakeLock()
        val job = Job(options, source.uri)
        val record = try {
            directory.mkdirs()
            resultFile.delete()
            job.description = runCatching { source.describe() }.getOrDefault(job.description)
            job.publish(recognizing = false)
            if (options.action == FileAction.TRANSCRIBE) transcribe(source, options, job) else analyze(source, options, job, stop)
        } catch (e: Throwable) {
            // Native and linkage errors too: the tab must never stay busy. zh-CN: 原生与链接错误也在内: 标签不能一直处于识别中.
            val stopped = cancelled.get()
            val failure = if (stopped) null else FileFailure.of(e)
            if (failure != null) Log.w(TAG, "File ${options.action.name.lowercase()} failed: ${failure.code}: ${failure.message}")
            job.record(
                state = if (stopped) FileTranscriptRecord.State.CANCELLED else FileTranscriptRecord.State.FAILED,
                language = job.spokenLanguage(),
                failure = failure,
            )
        } finally {
            inputFile.delete()
        }
        write(record)
        if (lock?.isHeld == true) runCatching { lock.release() }
        main.post { finish(record) }
    }

    private fun state(): FileTranscriptRecord.State = if (cancelled.get()) FileTranscriptRecord.State.CANCELLED else FileTranscriptRecord.State.COMPLETED

    private fun transcribe(source: FileSource, options: FileRunOptions, job: Job): FileTranscriptRecord {
        val request = RecognizeOptions(model = options.modelId, language = options.language, punctuation = options.punctuation, vad = options.vad)
        val transcript = open(source).use { descriptor -> SpeechRuntime.get(app).recognizeFile(descriptor, request, job.listener()) { cancelled.get() } }
        job.modelId = transcript.modelId
        return job.record(state(), transcript.language, transcript.durationMillis, transcript.warnings)
    }

    /** The actions of roadmap D153; the models are resolved before the file opens, as for `recognizeFile`. zh-CN: D153 的分析操作. */
    private fun analyze(source: FileSource, options: FileRunOptions, job: Job, stop: CancelToken): FileTranscriptRecord {
        val analysis = SpeechAnalysis.get(app)
        val control = SpeechAnalysis.Control(stop) { done, total -> job.progress(done, total) }
        return when (options.action) {
            FileAction.DIARIZE, FileAction.DIARIZE_TRANSCRIBE -> {
                val request = DiarizeOptions(speakers = options.speakers, identify = options.identify)
                val models = analysis.diarizationModels(request)
                val recognizer = if (options.action.transcribes) RecognizeOptions(model = options.modelId, language = options.language, punctuation = options.punctuation, vad = options.vad) else null
                // A missing recognizer fails before the diarization, not after it. zh-CN: 缺少识别模型时在分离之前失败.
                recognizer?.let { SpeechRuntime.get(app).resolve(SpeechContract.KIND_ASR, it.model) }
                job.modelId = models.first.modelId
                val (diarization, samples) = open(source).use { descriptor -> AudioFiles.open(descriptor).use { reader -> analysis.diarizeKeepingAudio(reader, request, control, models) } }
                job.speakers = FileAnalysis.Speakers(diarization.turns, diarization.speakers, diarization.speakerModelId, diarization.names)
                if (recognizer == null) return job.record(state(), recognized = diarization.durationMillis)
                transcribeTurns(samples, diarization, recognizer, options, job)
            }
            FileAction.LANGUAGE -> {
                val model = SpeechRuntime.get(app).resolve(SpeechContract.KIND_LANGID, options.languageModel)
                job.modelId = model.modelId
                try {
                    val result = open(source).use { descriptor -> AudioFiles.open(descriptor).use { reader -> analysis.detectLanguage(reader, LanguageOptions(model.modelId), control, model) } }
                    job.record(state(), result.language, result.durationMillis, analysis = FileAnalysis.Languages(result.language, result.candidates, result.segments))
                } catch (e: SpeechException) {
                    // No speech at all: an empty result, as a transcription without segments. zh-CN: 完全没有语音: 空结果, 与没有分段的转写相同.
                    if (e.code != SpeechErrorCodes.AUDIO_TOO_SHORT) throw e
                    job.record(state(), analysis = null)
                }
            }
            FileAction.TAG -> {
                val model = SpeechRuntime.get(app).resolve(SpeechContract.KIND_TAGGER, null)
                job.modelId = model.modelId
                val result = open(source).use { descriptor -> AudioFiles.open(descriptor).use { reader -> analysis.tag(reader, TagOptions(model.modelId), control, model) } }
                job.record(state(), recognized = result.durationMillis, analysis = FileAnalysis.Tags(result.labels))
            }
            FileAction.TRANSCRIBE -> throw IllegalArgumentException("Not an analysis")
        }
    }

    /** "Diarize and transcribe" (D153): each turn recognized from the decoded samples. zh-CN: "分离 + 转写": 从已解码的采样逐个识别轮次. */
    private fun transcribeTurns(samples: FloatArray, diarization: Diarization, request: RecognizeOptions, options: FileRunOptions, job: Job): FileTranscriptRecord {
        job.stage = FileRun.Stage.TURNS
        job.modelId = options.modelId.orEmpty()
        job.fed = 0
        job.declared = diarization.turns.sumOf { it.endMillis - it.startMillis }.takeIf { it > 0 }
        job.publish(recognizing = true)
        val warnings = LinkedHashSet<SpeechWarning>()
        var done = 0L
        for (turn in diarization.turns) {
            if (cancelled.get()) break
            val from = AudioInput.samples(turn.startMillis).toInt().coerceIn(0, samples.size)
            val to = AudioInput.samples(turn.endMillis).toInt().coerceIn(from, samples.size)
            val transcript = SpeechRuntime.get(app).recognizeAudio(PcmAudio(samples, from, to), request, job.listener(turn, done)) { cancelled.get() }
            job.modelId = transcript.modelId
            warnings += transcript.warnings
            done += turn.endMillis - turn.startMillis
            job.fed = done
        }
        return job.record(state(), job.spokenLanguage() ?: request.language?.substringBefore('-')?.lowercase(), diarization.durationMillis, warnings.toList())
    }

    private fun finish(finished: FileTranscriptRecord) {
        run = null
        record = finished
        FileRecognitionService.finish(app, finished, showResult = !screenVisible)
    }

    private fun open(source: FileSource): ParcelFileDescriptor {
        val descriptor = source.open()
        return if (seekable(descriptor)) descriptor else copy(descriptor)
    }

    private fun seekable(descriptor: ParcelFileDescriptor): Boolean = try {
        Os.lseek(descriptor.fileDescriptor, 0, OsConstants.SEEK_CUR)
        true
    } catch (_: ErrnoException) {
        false
    }

    /** Copies a pipe to the cache, closing it (D110). zh-CN: 把管道复制到缓存并关闭它 (D110). */
    private fun copy(descriptor: ParcelFileDescriptor): ParcelFileDescriptor {
        var total = 0L
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
            FileOutputStream(inputFile).use { output ->
                val buffer = ByteArray(COPY_BUFFER_BYTES)
                while (true) {
                    if (cancelled.get()) throw CopyCancelled()
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > SpeechLimits.MAX_AUDIO_FILE_BYTES) {
                        throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio file has more than ${SpeechLimits.MAX_AUDIO_FILE_BYTES} bytes")
                    }
                    output.write(buffer, 0, read)
                }
            }
        }
        return ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun write(finished: FileTranscriptRecord) {
        val temporary = File(directory, "$RESULT_NAME.tmp")
        try {
            directory.mkdirs()
            temporary.writeBytes(finished.toJson())
            if (!temporary.renameTo(resultFile)) throw IOException("The result could not be renamed")
        } catch (e: Exception) {
            // The tab still shows the result; only a restart loses it. zh-CN: 标签照常显示结果, 只有重启后才会丢失.
            temporary.delete()
            Log.w(TAG, "The file result could not be cached: ${e.javaClass.simpleName}")
        }
    }

    private fun read(): FileTranscriptRecord? = try {
        if (resultFile.isFile) FileTranscriptRecord.parse(resultFile.readBytes()) else null
    } catch (_: IOException) {
        null
    }

    private fun acquireWakeLock(): PowerManager.WakeLock? = runCatching {
        app.getSystemService(PowerManager::class.java)?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_MILLIS)
        }
    }.getOrNull()

    private class CopyCancelled : IOException("Cancelled while copying")

    companion object {
        private const val TAG = "MacawFileTab"
        const val RESULT_NAME = "file-result.json"
        const val INPUT_NAME = "input"
        private const val WAKE_LOCK_TAG = "ThreeMacawSpeech:FileRecognition"
        private const val WAKE_LOCK_MILLIS = 60 * 60 * 1000L
        private const val PROGRESS_INTERVAL_MILLIS = 250L
        private const val COPY_BUFFER_BYTES = 64 * 1024

        private val worker = Executors.newSingleThreadExecutor { Thread(it, "MacawFileRecognition").apply { isDaemon = true } }

        @Volatile
        private var instance: FileTranscription? = null

        fun get(context: Context): FileTranscription = instance ?: synchronized(this) { instance ?: FileTranscription(context).also { instance = it } }
    }
}
