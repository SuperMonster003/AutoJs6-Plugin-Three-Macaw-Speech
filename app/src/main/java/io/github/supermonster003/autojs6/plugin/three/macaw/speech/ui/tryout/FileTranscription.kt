package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileFailure
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileRecognitionListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.RecognizeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.FileRecognitionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
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
    fun describe(): FileDescription

    fun open(): ParcelFileDescriptor

    companion object {
        fun of(context: Context, uri: Uri): FileSource = DocumentSource(context.applicationContext, uri)

        fun of(file: File): FileSource = object : FileSource {
            override fun describe() = FileDescription(file.name, file.length())
            override fun open(): ParcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
}

private class DocumentSource(private val context: Context, private val uri: Uri) : FileSource {

    override fun describe(): FileDescription = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getString)
        val size = cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong)
        FileDescription(name?.takeIf { it.isNotBlank() }, size?.takeIf { it >= 0 })
    } ?: FileDescription(null, null)

    override fun open(): ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException("The provider returned no descriptor")
}

/** The choices of the file tab when a run starts (roadmap D114). zh-CN: 开始识别时文件标签上的选择 (路线图 D114). */
internal data class FileRunOptions(val modelId: String, val language: String?, val punctuation: Boolean, val vad: Boolean)

/**
 * A run in progress: [declaredMillis] is the length the file declares (null until it is open or when unknown),
 * [recognizedMillis] how much audio went to the recognizer, [segments] the finals so far.
 * zh-CN: 进行中的识别: [declaredMillis] 为文件声明的时长 (打开之前或未知时为 null), [recognizedMillis] 为已送入识别的音频时长, [segments] 为目前的整句.
 */
internal data class FileRun(
    val phase: Phase,
    val modelId: String,
    val displayName: String? = null,
    val sizeBytes: Long? = null,
    val declaredMillis: Long? = null,
    val recognizedMillis: Long = 0,
    val segments: List<Utterance> = emptyList(),
) {
    /** Opening the file and loading the model, recognizing, stopping after a cancel. zh-CN: 打开文件与加载模型, 识别中, 取消后停止中. */
    enum class Phase { PREPARING, RUNNING, CANCELLING }
}

/**
 * The file tab's recognition (roadmap D110 - D113, P7.8.2), one per process so that a run outlives the tryout screen:
 * one file at a time on its own worker thread, straight through `SpeechRuntime.recognizeFile` (no Binder), under a
 * partial wake lock of at most 60 minutes and the foreground service [FileRecognitionService]. A descriptor that cannot
 * seek (a pipe of some cloud providers) is copied to `cacheDir/tryout/input` first, within the 200 MB limit. Every
 * finished run (completed, cancelled with its segments so far, failed with its reason) is written to
 * `cacheDir/tryout/file-result.json` and read back once per process. [run] and [record] are Compose state, changed on
 * the main thread only; never log the file name or the recognized text (D85).
 * zh-CN: 文件标签的识别 (路线图 D110 - D113, P7.8.2), 每个进程一份, 因此识别不随试听试说页结束: 一次一个文件, 在自己的工作线程上直接调用
 * `SpeechRuntime.recognizeFile` (不经 Binder), 期间持最长 60 分钟的部分唤醒锁并运行前台服务 [FileRecognitionService]. 不能定位的描述符
 * (部分云盘返回的管道) 先在 200 MB 限制内复制到 `cacheDir/tryout/input`. 每次结束的识别 (完成, 取消时带已有分段, 失败时带原因) 写入
 * `cacheDir/tryout/file-result.json`, 每个进程读回一次. [run] 与 [record] 是 Compose 状态, 只在主线程上修改; 不记录文件名与识别文字 (D85).
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
        record = null
        val first = FileRun(FileRun.Phase.PREPARING, options.modelId)
        run = first
        FileRecognitionService.start(app, first)
        worker.execute { recognize(source, options) }
        return true
    }

    /** The button of the tab, the notification action and the service time limit. zh-CN: 标签上的按钮, 通知动作与前台服务时间上限. */
    fun cancel() {
        val current = run ?: return
        if (current.phase == FileRun.Phase.CANCELLING) return
        cancelled.set(true)
        update(current.copy(phase = FileRun.Phase.CANCELLING))
    }

    /** Removes the result from the tab and the cache. zh-CN: 从标签与缓存中删除结果. */
    fun clear() {
        if (run != null) return
        record = null
        worker.execute { resultFile.delete() }
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

    private fun recognize(source: FileSource, options: FileRunOptions) {
        val lock = acquireWakeLock()
        val segments = ArrayList<Utterance>()
        var description = FileDescription(null, null)
        var declared: Long? = null
        var fed = 0L
        var posted = 0L

        fun publish(recognizing: Boolean) {
            posted = SystemClock.elapsedRealtime()
            val name = description.displayName
            val size = description.sizeBytes
            val total = declared
            val millis = fed
            val list = segments.toList()
            main.post {
                val current = run ?: return@post
                val phase = if (recognizing && current.phase == FileRun.Phase.PREPARING) FileRun.Phase.RUNNING else current.phase
                update(current.copy(phase = phase, displayName = name, sizeBytes = size, declaredMillis = total, recognizedMillis = millis, segments = list))
            }
        }

        val listener = object : FileRecognitionListener {
            override fun opened(durationMillis: Long?) {
                declared = durationMillis
                publish(recognizing = true)
            }

            override fun progress(millis: Long) {
                fed = millis
                if (SystemClock.elapsedRealtime() - posted >= PROGRESS_INTERVAL_MILLIS) publish(recognizing = true)
            }

            override fun segment(utterance: Utterance) {
                segments += utterance
                publish(recognizing = true)
            }
        }

        val record = try {
            directory.mkdirs()
            resultFile.delete()
            description = runCatching { source.describe() }.getOrDefault(description)
            publish(recognizing = false)
            val request = RecognizeOptions(model = options.modelId, language = options.language, punctuation = options.punctuation, vad = options.vad)
            val transcript = open(source).use { descriptor -> SpeechRuntime.get(app).recognizeFile(descriptor, request, listener) { cancelled.get() } }
            FileTranscriptRecord(
                state = if (cancelled.get()) FileTranscriptRecord.State.CANCELLED else FileTranscriptRecord.State.COMPLETED,
                displayName = description.displayName,
                sizeBytes = description.sizeBytes,
                modelId = transcript.modelId,
                language = transcript.language,
                punctuation = options.punctuation,
                declaredMillis = declared,
                recognizedMillis = transcript.durationMillis,
                segments = transcript.segments,
                warnings = transcript.warnings,
            )
        } catch (e: Throwable) {
            // Native and linkage errors too: the tab must never stay busy. zh-CN: 原生与链接错误也在内: 标签不能一直处于识别中.
            val stopped = cancelled.get()
            val failure = if (stopped) null else FileFailure.of(e)
            if (failure != null) Log.w(TAG, "File recognition failed: ${failure.code}: ${failure.message}")
            FileTranscriptRecord(
                state = if (stopped) FileTranscriptRecord.State.CANCELLED else FileTranscriptRecord.State.FAILED,
                displayName = description.displayName,
                sizeBytes = description.sizeBytes,
                modelId = options.modelId,
                language = segments.mapNotNull { it.language }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
                punctuation = options.punctuation,
                declaredMillis = declared,
                recognizedMillis = fed,
                segments = segments.toList(),
                failure = failure,
            )
        } finally {
            inputFile.delete()
        }
        write(record)
        if (lock?.isHeld == true) runCatching { lock.release() }
        main.post { finish(record) }
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
