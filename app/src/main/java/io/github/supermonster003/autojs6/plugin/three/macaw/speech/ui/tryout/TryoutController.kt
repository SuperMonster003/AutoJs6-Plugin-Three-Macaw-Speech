package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.LocalSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechPluginBinder
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.ISpeechWakeCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.io.File
import java.util.concurrent.Executors

/** A line of the dictation transcript. zh-CN: 听写记录的一行. */
internal data class TranscriptLine(val text: String, val language: String?)

/** A wake hit of the keyword tryout, [atMillis] after detection started. zh-CN: 唤醒词试验的一次命中, [atMillis] 为检测开始后的时间. */
internal data class WakeHit(val keyword: String, val score: Float, val atMillis: Long)

/** State of one tryout part. zh-CN: 试验部分的状态. */
internal enum class TryoutPhase { IDLE, LOADING, RUNNING, STOPPING }

/** A problem to show, as a contract error code. zh-CN: 待显示的问题, 以契约错误码表示. */
internal data class TryoutProblem(val code: String, val serial: Int)

/**
 * The tryout screen's engine side (roadmap P5.3): dictation, wake word and synthesis through the plugin's own
 * `ISpeechPlugin` binder in the same process, so the tryout exercises exactly what scripts get (options, events, error
 * codes, the single microphone). Callbacks arrive on the plugin's ASR / synthesis threads and are posted to the main
 * thread. [release] stops the microphone and the player when the screen leaves.
 * zh-CN: 试听试说页的引擎部分 (路线图 P5.3): 听写, 唤醒词与合成经同进程内插件自己的 `ISpeechPlugin` Binder 运行, 因此试验的正是脚本得到的行为 (选项,
 * 事件, 错误码, 唯一的麦克风). 回调在插件的 ASR / 合成线程上到达, 投递到主线程. 界面离开时 [release] 停止麦克风与播放器.
 */
internal class TryoutController(context: Context) {
    private val app = context.applicationContext
    private val binder = SpeechPluginBinder(app)
    private val preferences = SpeechPreferences(app)
    private val main = Handler(Looper.getMainLooper())
    private var problems = 0

    var asrModels by mutableStateOf<List<ModelEntry>>(emptyList())
        private set
    var kwsModels by mutableStateOf<List<ModelEntry>>(emptyList())
        private set
    var ttsModels by mutableStateOf<List<ModelEntry>>(emptyList())
        private set
    var problem by mutableStateOf<TryoutProblem?>(null)
        private set

    // Dictation
    var dictationPhase by mutableStateOf(TryoutPhase.IDLE)
        private set
    val transcript = mutableStateListOf<TranscriptLine>()
    var partial by mutableStateOf("")
        private set
    var asrModel by mutableStateOf<String?>(null)
    var language by mutableStateOf<String?>(null)
    var voiceActive by mutableStateOf(false)
        private set
    private var dictation: ISpeechSession? = null

    // Wake word
    var wakePhase by mutableStateOf(TryoutPhase.IDLE)
        private set
    val hits = mutableStateListOf<WakeHit>()
    private var wake: ISpeechSession? = null
    private var wakeStarted = 0L

    // Synthesis
    var ttsModel by mutableStateOf<String?>(null)
    var speaker by mutableStateOf<Int?>(null)
    var speed by mutableStateOf(preferences.speed)
    var speakers by mutableStateOf<List<String>>(emptyList())
        private set
    var synthesisPhase by mutableStateOf(TryoutPhase.IDLE)
        private set
    var synthesisProgress by mutableStateOf(0f)
        private set
    var playing by mutableStateOf(false)
        private set
    private var synthesisId: Int? = null
    private var player: MediaPlayer? = null
    private var lastWav: Pair<String, File>? = null
    private val output = File(app.cacheDir, "tryout/tryout.wav")
    private val pending = File(app.cacheDir, "tryout/pending.wav")

    // File (roadmap D114: its own model, language and punctuation, apart from dictation; D153: the action and the
    // settings of the analyses)
    var fileModel by mutableStateOf<String?>(null)
    var fileLanguage by mutableStateOf<String?>(null)
    var filePunctuation by mutableStateOf(true)
    private var fileActionState by mutableStateOf(preferences.fileAction)

    /** Saved for the next visit (roadmap D160). zh-CN: 保存到下次进入 (路线图 D160). */
    var fileAction: FileAction
        get() = fileActionState
        set(value) {
            fileActionState = value
            preferences.fileAction = value
        }
    var fileSpeakers by mutableIntStateOf(0)
    var fileIdentify by mutableStateOf(false)
    var languageModel by mutableStateOf<String?>(null)

    /** Installed models of the analysis kinds (segment, speaker, langid, tagger). zh-CN: 已安装的分析类模型. */
    var analysisModels by mutableStateOf<List<ModelEntry>>(emptyList())
        private set

    val languageModels: List<ModelEntry> get() = analysisModels.filter { it.kind == SpeechContract.KIND_LANGID }

    val endpointMillis: Long get() = preferences.endpointMillis
    val vad: Boolean get() = preferences.vad

    /** Loads the installed models off the main thread. zh-CN: 在主线程之外加载已安装的模型. */
    fun load() {
        io.execute {
            val models = runCatching { SpeechModels.get(app).list(installed = true) }.getOrDefault(emptyList())
            main.post {
                asrModels = models.filter { it.kind == SpeechContract.KIND_ASR }
                kwsModels = models.filter { it.kind == SpeechContract.KIND_KWS }
                ttsModels = models.filter { it.kind == SpeechContract.KIND_TTS }
                if (asrModel !in asrModels.map { it.id }) asrModel = asrModels.firstOrNull { it.isDefault }?.id ?: asrModels.firstOrNull()?.id
                if (fileModel !in asrModels.map { it.id }) fileModel = asrModels.firstOrNull { it.isDefault }?.id ?: asrModels.firstOrNull()?.id
                if (ttsModel !in ttsModels.map { it.id }) selectTtsModel(ttsModels.firstOrNull { it.isDefault }?.id ?: ttsModels.firstOrNull()?.id)
                analysisModels = models.filter { it.kind in ANALYSIS_KINDS }
                if (languageModel !in languageModels.map { it.id }) languageModel = languageModels.firstOrNull { it.isDefault }?.id ?: languageModels.firstOrNull()?.id
            }
        }
    }

    val busy: Boolean get() = dictationPhase != TryoutPhase.IDLE || wakePhase != TryoutPhase.IDLE || synthesisPhase != TryoutPhase.IDLE || playing

    /**
     * The kinds [action] needs and that have no installed model, in the order of the home screen (D153).
     * zh-CN: [action] 需要但没有已安装模型的类别, 按首页顺序 (D153).
     */
    fun missingKinds(action: FileAction): List<String> {
        val needed = buildList {
            if (action.diarizes) addAll(listOf(SpeechContract.KIND_SEGMENT, SpeechContract.KIND_SPEAKER))
            if (action.transcribes) add(SpeechContract.KIND_ASR)
            if (action == FileAction.LANGUAGE) add(SpeechContract.KIND_LANGID)
            if (action == FileAction.TAG) add(SpeechContract.KIND_TAGGER)
        }
        val installed = (asrModels + analysisModels).map { it.kind }.toSet()
        return needed.filter { it !in installed }
    }

    /** The name of an installed model, or its id. zh-CN: 已安装模型的名称, 没有时为其 ID. */
    fun modelName(modelId: String): String = (asrModels + analysisModels).firstOrNull { it.id == modelId }?.name ?: modelId

    /** Whether the recognizer takes a language (SenseVoice); the others detect it or speak one. zh-CN: 识别器是否接受语言 (SenseVoice). */
    fun takesLanguage(modelId: String?): Boolean = asrModels.firstOrNull { it.id == modelId }?.modelType in SpeechRuntime.LANGUAGE_TYPES

    // Dictation -----------------------------------------------------------------------------------------------------

    fun startDictation() {
        if (dictationPhase != TryoutPhase.IDLE) return
        stopWake()
        transcript.clear()
        partial = ""
        val options = Bundle().apply {
            asrModel?.let { putString(SpeechOptionKeys.MODEL, it) }
            language?.takeIf { takesLanguage(asrModel) }?.let { putString(SpeechOptionKeys.LANGUAGE, it) }
            putBoolean(SpeechOptionKeys.VAD, preferences.vad)
            putLong(SpeechOptionKeys.SILENCE_END_MILLIS, preferences.endpointMillis)
            // The tryout keeps listening until stopped: no speech timeout, the longest session the protocol allows.
            putLong(SpeechOptionKeys.TIMEOUT_MILLIS, 0)
        }
        dictationPhase = TryoutPhase.LOADING
        // Roadmap D57: a wake session of a client yields the microphone while the tryout listens.
        dictation = call { binder.openLocalSession(options, dictationCallback, LocalSession()) }
        if (dictation == null) dictationPhase = TryoutPhase.IDLE
    }

    fun stopDictation() {
        val session = dictation ?: return
        dictationPhase = TryoutPhase.STOPPING
        runCatching { session.finish() }
    }

    private val dictationCallback = object : ISpeechSessionCallback.Stub() {
        override fun onReady(event: Bundle?) = post { if (dictationPhase == TryoutPhase.LOADING) dictationPhase = TryoutPhase.RUNNING }
        override fun onPartial(event: Bundle?) = post { partial = event?.getString(SpeechResultKeys.TEXT).orEmpty() }
        override fun onFinal(event: Bundle?) = post {
            val text = event?.getString(SpeechResultKeys.TEXT).orEmpty()
            if (text.isNotBlank()) transcript += TranscriptLine(text, event?.getString(SpeechResultKeys.LANGUAGE))
            partial = ""
        }
        override fun onEndpoint() = Unit
        override fun onVoiceActivity(event: Bundle?) = post { voiceActive = event?.getBoolean(SpeechResultKeys.ACTIVE) == true }
        override fun onWarning(event: Bundle?) = post { report(event?.getString(SpeechContract.KEY_ERROR_CODE)) }
        override fun onError(event: Bundle?) = post { report(event?.getString(SpeechContract.KEY_ERROR_CODE)) }
        override fun onEnd(event: Bundle?) = post {
            dictation = null
            dictationPhase = TryoutPhase.IDLE
            voiceActive = false
            if (partial.isNotBlank()) transcript += TranscriptLine(partial, null)
            partial = ""
        }
    }

    // Wake word -----------------------------------------------------------------------------------------------------

    /** [keywords] one per line or comma; written as the script would. zh-CN: [keywords] 每行或以逗号分隔一个; 写法与脚本相同. */
    fun startWake(keywords: List<String>) {
        if (wakePhase != TryoutPhase.IDLE || keywords.isEmpty()) return
        stopDictation()
        hits.clear()
        val options = Bundle().apply {
            // The contract takes one Bundle per keyword (`keyword`, optional `boost` / `threshold`).
            putParcelableArrayList(SpeechOptionKeys.KEYWORDS, ArrayList(keywords.map { keyword -> Bundle().apply { putString(SpeechOptionKeys.KEYWORD, keyword) } }))
            kwsModels.firstOrNull { it.isDefault }?.let { putString(SpeechOptionKeys.MODEL, it.id) }
        }
        wakePhase = TryoutPhase.LOADING
        wakeStarted = System.currentTimeMillis()
        wake = call { binder.openLocalWakeSession(options, wakeCallback) }
        if (wake == null) wakePhase = TryoutPhase.IDLE
    }

    fun stopWake() {
        val session = wake ?: return
        wakePhase = TryoutPhase.STOPPING
        runCatching { session.finish() }
    }

    private val wakeCallback = object : ISpeechWakeCallback.Stub() {
        override fun onReady(event: Bundle?) = post { if (wakePhase == TryoutPhase.LOADING) wakePhase = TryoutPhase.RUNNING }
        override fun onWake(event: Bundle?) = post {
            val keyword = event?.getString(SpeechResultKeys.KEYWORD).orEmpty()
            // The event carries the wall clock time; the list shows the time since detection started.
            val at = event?.getLong(SpeechResultKeys.TIMESTAMP_MILLIS)?.let { (it - wakeStarted).coerceAtLeast(0) } ?: 0L
            hits.add(0, WakeHit(keyword, event?.getFloat(SpeechResultKeys.SCORE) ?: 0f, at))
        }
        override fun onWarning(event: Bundle?) = post { report(event?.getString(SpeechContract.KEY_ERROR_CODE)) }
        override fun onError(event: Bundle?) = post {
            Log.w(TAG, "Wake trial error: ${event?.getString(SpeechContract.KEY_ERROR_MESSAGE)}")
            report(event?.getString(SpeechContract.KEY_ERROR_CODE))
        }
        override fun onEnd(event: Bundle?) = post {
            wake = null
            wakePhase = TryoutPhase.IDLE
        }
    }

    // Synthesis -----------------------------------------------------------------------------------------------------

    fun selectTtsModel(modelId: String?) {
        ttsModel = modelId
        speaker = modelId?.let(preferences::speaker)
        speakers = emptyList()
        val id = modelId ?: return
        io.execute {
            val names = runCatching {
                val runtime = SpeechRuntime.get(app)
                val model = runtime.models.store.installed(id) ?: return@runCatching emptyList()
                val known = runtime.speakers(model)
                List(known.count) { index -> known.names.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "" }
            }.getOrDefault(emptyList())
            main.post { if (ttsModel == id) speakers = names }
        }
    }

    /** Synthesizes [text] into the cache and plays it, or plays the cached file when nothing changed. zh-CN: 合成到缓存并播放. */
    fun play(text: String) = synthesize(text) { file -> startPlayer(file) }

    /** Writes the synthesis of [text] to [target] (a document the user created). zh-CN: 把 [text] 的合成写入 [target]. */
    fun save(text: String, target: Uri, done: (Boolean) -> Unit) = synthesize(text) { file ->
        io.execute {
            val ok = runCatching {
                app.contentResolver.openOutputStream(target, "wt")?.use { stream -> file.inputStream().use { it.copyTo(stream) } } ?: error("no stream")
            }.isSuccess
            main.post { done(ok) }
        }
    }

    fun stopSynthesis() {
        synthesisId?.let { id -> runCatching { binder.cancelOperation(id) } }
        stopPlayer()
    }

    /** Writes an export of the file tab to [target] (a document the user created). zh-CN: 把文件标签的导出写入 [target]. */
    fun writeDocument(target: Uri, bytes: ByteArray, done: (Boolean) -> Unit) {
        io.execute {
            val ok = runCatching {
                app.contentResolver.openOutputStream(target, "wt")?.use { it.write(bytes) } ?: error("no stream")
            }.isSuccess
            main.post { done(ok) }
        }
    }

    private fun key(text: String) = listOf(text, ttsModel, speaker, speed).joinToString("\u0000")

    private fun synthesize(text: String, then: (File) -> Unit) {
        if (synthesisPhase != TryoutPhase.IDLE || text.isBlank()) return
        stopPlayer()
        val wanted = key(text)
        lastWav?.takeIf { it.first == wanted && it.second.isFile }?.let { then(it.second); return }
        pending.parentFile?.mkdirs()
        val descriptor = runCatching {
            ParcelFileDescriptor.open(pending, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE)
        }.getOrElse {
            report(SpeechErrorCodes.IO_FAILED)
            return
        }
        val request = Bundle().apply {
            putString(SpeechOptionKeys.TEXT, text)
            ttsModel?.let { putString(SpeechOptionKeys.MODEL, it) }
            speaker?.let { putInt(SpeechOptionKeys.SPEAKER, it) }
            putFloat(SpeechOptionKeys.SPEED, speed)
        }
        synthesisPhase = TryoutPhase.RUNNING
        synthesisProgress = 0f
        val callback = object : ISpeechProgressCallback.Stub() {
            override fun onProgress(opId: Int, done: Long, total: Long) = post { if (total > 0) synthesisProgress = (done.toFloat() / total).coerceIn(0f, 1f) }
            override fun onCompleted(opId: Int, result: Bundle?) = post {
                synthesisPhase = TryoutPhase.IDLE
                synthesisId = null
                if (pending.renameTo(output) || (output.delete() && pending.renameTo(output))) {
                    lastWav = wanted to output
                    then(output)
                } else {
                    report(SpeechErrorCodes.IO_FAILED)
                }
            }
            override fun onFailed(opId: Int, error: Bundle?) = post {
                synthesisPhase = TryoutPhase.IDLE
                synthesisId = null
                pending.delete()
                val code = error?.getString(SpeechContract.KEY_ERROR_CODE)
                if (code != SpeechErrorCodes.SESSION_CANCELLED) report(code)
            }
        }
        synthesisId = call { binder.synthesizeToFile(request, descriptor, callback) }
        runCatching { descriptor.close() }
        if (synthesisId == null) synthesisPhase = TryoutPhase.IDLE
    }

    private fun startPlayer(file: File) {
        stopPlayer()
        val created = runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { stopPlayer() }
                prepare()
                start()
            }
        }.getOrNull()
        if (created == null) {
            report(SpeechErrorCodes.IO_FAILED)
            return
        }
        player = created
        playing = true
    }

    private fun stopPlayer() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        playing = false
    }

    // Lifecycle -----------------------------------------------------------------------------------------------------

    /** Leaving the screen ends the microphone sessions, the synthesis and the playback. zh-CN: 离开界面时结束麦克风会话, 合成与播放. */
    fun release() {
        dictation?.let { runCatching { it.cancel() } }
        wake?.let { runCatching { it.cancel() } }
        stopSynthesis()
    }

    private inline fun <T> call(block: () -> T): T? = try {
        block()
    } catch (e: RuntimeException) {
        // The binder throws `CODE: detail` as the contract defines. zh-CN: Binder 按契约以 `CODE: detail` 抛出.
        Log.w(TAG, "Tryout call refused: ${e.message}")
        report(SpeechErrorCodes.decode(e.message.orEmpty())?.first)
        null
    }

    private fun report(code: String?) {
        problem = TryoutProblem(code ?: SpeechErrorCodes.INTERNAL, ++problems)
    }

    private fun post(block: () -> Unit) {
        main.post(block)
    }

    private companion object {
        val ANALYSIS_KINDS = setOf(SpeechContract.KIND_SEGMENT, SpeechContract.KIND_SPEAKER, SpeechContract.KIND_LANGID, SpeechContract.KIND_TAGGER)
        const val TAG = "MacawTryout"
        val io = Executors.newSingleThreadExecutor { Thread(it, "MacawTryout").apply { isDaemon = true } }
    }
}
