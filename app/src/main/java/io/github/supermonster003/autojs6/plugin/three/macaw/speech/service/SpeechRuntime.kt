package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Debug
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.kws.KeywordTokenizer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelManager
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineCache
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.EngineKey
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.IdleScheduler
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.KeywordEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.OfflineEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.OnlineEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.RuntimeRoles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaEngines
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Dictation
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.DictationSink
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileRecognition
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscript
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FixedSegments
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Keyword
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.ListenOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.OfflineDictation
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.RecognizeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SessionTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechWarning
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.StreamingDictation
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VadOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VoiceActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsEngineCore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsModelVoices
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsSpeakerHints
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsSpeakers
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.Closeable
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

/**
 * The recognition runtime of the plugin process (roadmap D30, P3.1): engines load on the model loader thread (never
 * on a Binder main path for sessions) and are shared through the [EngineCache] (released after [IDLE_MILLIS] without
 * use); every session event and every audio frame is processed on the single ASR thread, in order; the microphone
 * records on a capture thread of its own; file calls run on the calling Binder thread, as the protocol allows, and
 * share the engines. [sessions] admits one microphone session and up to four external sessions per caller. Synthesis
 * (roadmap P4) shares the cache and its memory budget: Binder syntheses run one after another on the [tts] thread, the
 * system TTS engine synthesizes on the thread Android gives it, and both use the same loaded models.
 * zh-CN: 插件进程的识别运行时 (路线图 D30, P3.1): 引擎在模型加载线程上加载 (会话从不在 Binder 主路径上加载), 经 [EngineCache] 共享 (无人使用
 * [IDLE_MILLIS] 后释放); 所有会话事件与音频帧在唯一的 ASR 线程上按序处理; 麦克风在独立的采集线程上录音; 文件调用按协议在调用方 Binder 线程上运行,
 * 共享引擎. [sessions] 准入一个麦克风会话, 以及每个调用方至多四个外部会话. 合成 (路线图 P4) 共用缓存及其内存预算: Binder 合成在 [tts] 线程上
 * 依次运行, 系统 TTS 引擎在 Android 分配的线程上合成, 两者使用相同的已加载模型.
 */
internal class SpeechRuntime private constructor(val context: Context) {

    val models: ModelManager = SpeechModels.get(context)

    /** The ASR thread: sessions, frames and timers. zh-CN: ASR 线程: 会话, 音频帧与计时. */
    val asr: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { Thread(it, "MacawAsr").apply { isDaemon = true } }

    private val loader: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "MacawEngineLoad").apply { isDaemon = true } }

    /** The synthesis thread of `synthesizeToFile` (roadmap D30). zh-CN: `synthesizeToFile` 的合成线程 (路线图 D30). */
    val tts: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "MacawTts").apply { isDaemon = true } }

    private val idle: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { Thread(it, "MacawEngineIdle").apply { isDaemon = true } }

    val engines = EngineCache(IDLE_MILLIS, IdleScheduler.of(idle), { Log.i(TAG, "$it; ${memory()}") }, engineBudget(context))

    private val speakerNames = ConcurrentHashMap<String, List<String>>()

    val sessions = SessionTable()

    private val ids = AtomicInteger(0)

    private val emptyKeywords: File by lazy {
        File(context.noBackupFilesDir, "kws/no-keywords.txt").apply {
            parentFile?.mkdirs()
            if (!isFile || length() != 0L) writeBytes(ByteArray(0))
        }
    }

    init {
        models.isModelInUse = { engines.inUse(it) }
    }

    fun nextId(prefix: String): String = "$prefix-${ids.incrementAndGet()}"

    fun microphoneGranted(): Boolean = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /**
     * `PERMISSION_DENIED` with the way to grant: until the plugin's own screen (roadmap P5.1) asks for it, the system
     * permission dialog of [MicrophonePermissionActivity] does.
     * zh-CN: 带授权方式的 `PERMISSION_DENIED`: 在插件自己的界面 (路线图 P5.1) 申请之前, 由 [MicrophonePermissionActivity] 的系统权限对话框申请.
     */
    fun requireMicrophone() {
        if (microphoneGranted()) return
        throw SpeechException(
            SpeechErrorCodes.PERMISSION_DENIED,
            "3-Macaw Speech does not hold the microphone permission (RECORD_AUDIO). Grant it on the plugin's permission screen: " +
                "start the activity ${context.packageName}/${MicrophonePermissionActivity::class.java.name}, then try again",
        )
    }

    /** Runs on the loader thread. zh-CN: 在加载线程上运行. */
    fun load(task: () -> Unit) = loader.execute(task)

    // Models -----------------------------------------------------------------------------------------------------------

    /**
     * The installed model of [kind]: [requested] or the default; `MODEL_MISSING` when unknown or not installed,
     * `INVALID_OPTIONS` when it is a model of another kind (a TTS model given to recognition).
     * zh-CN: [kind] 的已安装模型: [requested] 或默认模型; 未知或未安装时为 `MODEL_MISSING`, 种类不符 (把 TTS 模型用于识别) 时为 `INVALID_OPTIONS`.
     */
    fun resolve(kind: String, requested: String?): InstalledModel {
        if (requested == null) {
            return models.defaultModel(kind)
                ?: throw SpeechException(SpeechErrorCodes.MODEL_MISSING, "No $kind model is installed; install one with speech.models.install()")
        }
        val installed = models.store.installed(requested)?.takeIf { models.store.isIntact(it) }
        if (installed == null) {
            val known = models.entry(requested)
            if (known != null && known.kind != kind) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "$requested is a ${known.kind} model, not a $kind model")
            throw SpeechException(SpeechErrorCodes.MODEL_MISSING, if (known == null) "Unknown model $requested" else "$requested is not installed")
        }
        if (installed.kind != kind) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "$requested is a ${installed.kind} model, not a $kind model")
        return installed
    }

    fun isOffline(model: InstalledModel): Boolean = model.modelType in OFFLINE_TYPES

    /**
     * Non-fatal conditions of a recognition request: hotwords on a model without hotword support (roadmap D17), a
     * language SenseVoice does not know (it falls back to automatic detection).
     * zh-CN: 识别请求的非致命情况: 不支持热词的模型收到热词 (路线图 D17), SenseVoice 不认识的语言 (回退为自动识别).
     */
    fun warnings(model: InstalledModel, language: String?, hotwords: List<String>): List<SpeechWarning> {
        val out = ArrayList<SpeechWarning>()
        if (hotwords.isNotEmpty() && !supportsHotwords(model)) {
            out += SpeechWarning(SpeechErrorCodes.INVALID_OPTIONS, "${model.modelId} does not support hotwords; ${hotwords.size} hotwords were ignored")
        }
        if (language != null && isOffline(model) && senseVoiceLanguage(language) == null) {
            out += SpeechWarning(SpeechErrorCodes.INVALID_OPTIONS, "${model.modelId} does not know the language '$language'; it detects the language itself")
        }
        return out
    }

    fun supportsHotwords(model: InstalledModel): Boolean = !isOffline(model) && runCatching { RuntimeRoles.of(model).optionalFile(model, "bpeVocab") != null }.getOrDefault(false)

    // Engines ----------------------------------------------------------------------------------------------------------

    /** A recognizer lease for [model]; loads on the calling thread. zh-CN: [model] 的识别器租约; 在调用线程上加载. */
    fun acquireAsr(model: InstalledModel, language: String?, hotwords: Boolean): EngineCache.Lease<out Closeable> = if (isOffline(model)) {
        val code = language?.let(::senseVoiceLanguage) ?: ""
        engines.acquire(EngineKey(model.modelId, model.versionCode, "sense-voice:${code.ifEmpty { "auto" }}"), model.installedBytes) { SherpaEngines.offline(model, code) }
    } else {
        val withHotwords = hotwords && supportsHotwords(model)
        engines.acquire(EngineKey(model.modelId, model.versionCode, if (withHotwords) "hotwords" else "greedy"), model.installedBytes) { SherpaEngines.online(model, withHotwords) }
    }

    fun acquireKeywords(model: InstalledModel): EngineCache.Lease<KeywordEngine> =
        engines.acquire(EngineKey(model.modelId, model.versionCode, "kws"), model.installedBytes) { SherpaEngines.keywords(model, emptyKeywords) }

    // Synthesis (roadmap P4) ---------------------------------------------------------------------------------------------

    /** A synthesizer lease for [model]; loads on the calling thread. zh-CN: [model] 的合成器租约; 在调用线程上加载. */
    fun acquireTts(model: InstalledModel): EngineCache.Lease<TtsEngine> =
        engines.acquire(EngineKey(model.modelId, model.versionCode, "tts"), TtsEngineCore.weight(model)) { TtsEngineCore.load(model) }

    /** Loads [model] on the loader thread and leaves it idle in the cache, so the first sentence starts sooner. zh-CN: 预加载. */
    fun preloadTts(model: InstalledModel) = load {
        runCatching { acquireTts(model).close() }.onFailure { Log.w(TAG, "Preloading ${model.modelId} failed: ${it.message}") }
    }

    /** The speakers of [model] before it is loaded (catalog count, `speakers.txt` names). zh-CN: 加载前的 [model] 说话人. */
    fun speakers(model: InstalledModel): TtsSpeakers {
        val names = speakerNames.getOrPut("${model.modelId}/${model.versionCode}") { TtsEngineCore.speakers(model).names }
        val count = maxOf(1, model.speakers ?: 1)
        return TtsSpeakers(count, names, TtsSpeakerHints.defaultSpeaker(model.modelId, count))
    }

    /** Installed and intact TTS models, the default one first. zh-CN: 已安装且完整的 TTS 模型, 默认模型在前. */
    fun ttsModels(): List<InstalledModel> {
        val default = models.defaultModel(SpeechContract.KIND_TTS)?.modelId
        return models.store.installed().filter { it.kind == SpeechContract.KIND_TTS && it.modelType in TtsEngineCore.MODEL_TYPES && models.store.isIntact(it) }
            .sortedWith(compareByDescending<InstalledModel> { it.modelId == default }.thenBy { it.modelId })
    }

    /**
     * The voices of the system TTS engine for the models installed now (roadmap D19, appendix D Q6); the TTS languages of
     * the catalog tell `LANG_MISSING_DATA` from `LANG_NOT_SUPPORTED`.
     * zh-CN: 当前已安装模型对应的系统 TTS 引擎语音 (路线图 D19, 附录 D Q6); 目录的 TTS 语言用于区分 `LANG_MISSING_DATA` 与 `LANG_NOT_SUPPORTED`.
     */
    fun voiceTable(): TtsVoiceTable {
        val default = models.defaultModel(SpeechContract.KIND_TTS)?.modelId
        val voices = ttsModels().map { model ->
            val speakers = speakers(model)
            TtsModelVoices(
                model.modelId, model.languages, speakers.count, model.sampleRate, model.modelId == default, speakers.names,
                TtsSpeakerHints.speakerLanguages(model.modelId, speakers.count), speakers.defaultId,
            )
        }
        val catalogLanguages = runCatching { models.list(kind = SpeechContract.KIND_TTS).flatMap { it.languages } }.getOrDefault(emptyList()).distinct()
        return TtsVoiceTable(voices, catalogLanguages)
    }

    /**
     * A dictation on [engine] for a session or a wake round (roadmap D17): streaming models gate with the VAD when
     * `vad` is on; offline models always cut with the VAD. Built on the ASR thread.
     * zh-CN: 在 [engine] 上为会话或唤醒轮次构建听写 (路线图 D17): 流式模型在 `vad` 开启时以 VAD 门控; 离线模型总是以 VAD 切段. 在 ASR 线程上构建.
     */
    fun dictation(engine: Closeable, options: ListenOptions, sink: DictationSink): Dictation = when (engine) {
        is OnlineEngine -> {
            val vad = SherpaEngines.vad(context.assets, SESSION_VAD)
            val stream = engine.stream(options.hotwords)
            StreamingDictation(stream, vad, sink, gate = options.vad, reportVad = options.vad, options.silenceEndMillis, options.punctuation) {
                runCatching { stream.close() }
                runCatching { vad.close() }
            }
        }
        is OfflineEngine -> {
            val vad = SherpaEngines.vad(context.assets, SESSION_VAD)
            OfflineDictation(engine, vad, sink, reportVad = options.vad, options.silenceEndMillis, options.punctuation) { runCatching { vad.close() } }
        }
        else -> throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "Not a recognizer: ${engine.javaClass.simpleName}")
    }

    /**
     * The `KeywordSpotter` buffer of [keywords] for [model] (roadmap D18); `INVALID_OPTIONS` when a keyword cannot be
     * spelled with the model's tokens. Reads `tokens.txt`, the pinyin table and the lexicon, never loads the model.
     * zh-CN: [model] 上 [keywords] 的 `KeywordSpotter` 缓冲区 (路线图 D18); 无法用模型 token 拼写时为 `INVALID_OPTIONS`. 只读取 `tokens.txt`,
     * 拼音表与词典, 不加载模型.
     */
    fun keywordBuffer(model: InstalledModel, keywords: List<Keyword>): String {
        val roles = RuntimeRoles.of(model)
        val tokens = File(roles.file(model, "tokens")).inputStream().use(KeywordTokenizer::readTokens)
        val (words, characters) = KeywordTokenizer.needs(keywords.map { it.text })
        val readings = if (characters.isEmpty()) emptyMap() else context.assets.open(PINYIN_ASSET).use { KeywordTokenizer.readReadings(it, characters) }
        val phones = HashMap<String, List<String>>()
        if (words.isNotEmpty()) {
            roles.files(model, "lexicon").forEach { path ->
                File(path).bufferedReader(Charsets.UTF_8).let { reader -> KeywordTokenizer.readPhones(reader, words - phones.keys) }.forEach { (k, v) -> phones.putIfAbsent(k, v) }
            }
        }
        val tokenizer = KeywordTokenizer(tokens, readings::get, phones::get)
        return KeywordTokenizer.buffer(keywords, keywords.map { tokenizer.encode(it.text) })
    }

    // File calls -------------------------------------------------------------------------------------------------------

    /** `recognizeFile` on the calling Binder thread (roadmap P3.3). zh-CN: 在调用方 Binder 线程上执行 `recognizeFile` (路线图 P3.3). */
    fun recognizeFile(audio: ParcelFileDescriptor, options: RecognizeOptions): FileTranscript {
        val model = resolve(SpeechContract.KIND_ASR, options.model)
        val warnings = warnings(model, options.language, options.hotwords)
        return openWav(audio).use { reader ->
            acquireAsr(model, options.language, options.hotwords.isNotEmpty()).use { lease ->
                FileRecognition.recognize(reader, model.modelId, options.language?.substringBefore('-')?.lowercase(), warnings) { sink -> fileDictation(lease.engine, options, sink) }
            }
        }
    }

    /** `detectVoice` with the bundled silero VAD. zh-CN: 以随包 silero VAD 执行 `detectVoice`. */
    fun detectVoice(audio: ParcelFileDescriptor, options: VadOptions): VoiceActivity {
        if (options.model != null && options.model != SpeechModels.BUNDLED_VAD_ID) {
            throw SpeechException(SpeechErrorCodes.MODEL_MISSING, "Unknown voice activity model ${options.model}; the plugin bundles ${SpeechModels.BUNDLED_VAD_ID}")
        }
        val minSpeech = (options.minSpeechMillis ?: 250L) / 1000f
        val settings = try {
            SherpaRuntime.VadSettings(
                threshold = options.threshold ?: 0.5f,
                minSilenceSeconds = (options.minSilenceMillis ?: 250L) / 1000f,
                minSpeechSeconds = minSpeech,
                maxSpeechSeconds = maxOf(FILE_MAX_SPEECH_SECONDS, minSpeech + 1f).coerceAtMost(1800f),
            )
        } catch (e: IllegalArgumentException) {
            throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, e.message ?: "Invalid voice activity options")
        }
        return openWav(audio).use { reader -> SherpaEngines.vad(context.assets, settings).use { vad -> FileRecognition.detectVoice(reader, vad) } }
    }

    private fun fileDictation(engine: Closeable, options: RecognizeOptions, sink: DictationSink): Dictation = when (engine) {
        is OnlineEngine -> {
            val vad = SherpaEngines.vad(context.assets, SESSION_VAD)
            val stream = engine.stream(options.hotwords)
            StreamingDictation(stream, vad, sink, gate = options.vad, reportVad = false, FILE_SILENCE_END_MILLIS, options.punctuation) {
                runCatching { stream.close() }
                runCatching { vad.close() }
            }
        }
        is OfflineEngine -> {
            val detector = if (options.vad) SherpaEngines.vad(context.assets, SESSION_VAD) else FixedSegments()
            OfflineDictation(engine, detector, sink, reportVad = false, 0L, options.punctuation) { runCatching { detector.close() } }
        }
        else -> throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "Not a recognizer: ${engine.javaClass.simpleName}")
    }

    /** A WAV reader on a duplicate of the host's read-only descriptor. zh-CN: 基于宿主只读描述符副本的 WAV 读取器. */
    private fun openWav(audio: ParcelFileDescriptor): WavReader {
        val copy = try {
            audio.dup()
        } catch (e: java.io.IOException) {
            throw SpeechException(SpeechErrorCodes.IO_FAILED, "The audio descriptor cannot be read: ${e.message}", e)
        }
        val length = copy.statSize.takeIf { it >= 0 }
        val input = ParcelFileDescriptor.AutoCloseInputStream(copy)
        return try {
            WavReader(input, length)
        } catch (e: SpeechException) {
            runCatching { input.close() }
            throw e
        } catch (e: java.io.IOException) {
            runCatching { input.close() }
            throw SpeechException(SpeechErrorCodes.IO_FAILED, "The audio could not be read: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "MacawEngines"
        const val PINYIN_ASSET = "kws/pinyin.txt"

        /** Idle release of loaded engines (roadmap D30). zh-CN: 已加载引擎的空闲释放时间 (路线图 D30). */
        const val IDLE_MILLIS = 5 * 60_000L

        /** A file utterance closes after this much silence (streaming models). zh-CN: 文件中的语句在此静音时长后结束 (流式模型). */
        const val FILE_SILENCE_END_MILLIS = 600L

        private const val FILE_MAX_SPEECH_SECONDS = 20f

        val OFFLINE_TYPES = setOf("sense-voice")

        /** The VAD of sessions and file recognition: upstream defaults with 20 s segments. zh-CN: 会话与文件识别使用的 VAD: 上游默认值, 语音段至多 20 秒. */
        val SESSION_VAD = SherpaRuntime.VadSettings(maxSpeechSeconds = 20f)

        private val SENSE_VOICE_LANGUAGES = setOf("zh", "en", "ja", "ko", "yue")

        fun senseVoiceLanguage(language: String): String? = language.substringBefore('-').lowercase().takeIf { it in SENSE_VOICE_LANGUAGES }

        @Volatile
        private var instance: SpeechRuntime? = null

        fun get(context: Context): SpeechRuntime = instance ?: synchronized(this) {
            instance ?: SpeechRuntime(context.applicationContext).also { instance = it }
        }

        /**
         * Memory budget of the loaded engines (roadmap P4.1): a quarter of the device memory, within 384 MiB and 1.5 GiB
         * (about 0.9 GiB on a 3.7 GiB phone, enough for a streaming and an offline recognizer plus Kokoro).
         * zh-CN: 已加载引擎的内存预算 (路线图 P4.1): 设备内存的四分之一, 介于 384 MiB 与 1.5 GiB 之间 (3.7 GiB 手机上约 0.9 GiB, 足以同时容纳一个流式
         * 与一个离线识别器以及 Kokoro).
         */
        fun engineBudget(context: Context): Long {
            val info = ActivityManager.MemoryInfo()
            context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
            val total = info.totalMem.takeIf { it > 0 } ?: (4L shl 30)
            return (total / 4).coerceIn(384L shl 20, 1536L shl 20)
        }

        /** Native and Java heap of the process, for the logs of roadmap D30. zh-CN: 进程的原生堆与 Java 堆, 供路线图 D30 的日志使用. */
        fun memory(): String {
            val runtime = Runtime.getRuntime()
            return "native heap ${Debug.getNativeHeapAllocatedSize() shr 20} MiB, java heap ${(runtime.totalMemory() - runtime.freeMemory()) shr 20} MiB"
        }
    }
}
