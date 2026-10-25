package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflinePunctuation
import com.k2fsa.sherpa.onnx.OfflinePunctuationConfig
import com.k2fsa.sherpa.onnx.OfflinePunctuationModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlinePunctuation
import com.k2fsa.sherpa.onnx.OnlinePunctuationConfig
import com.k2fsa.sherpa.onnx.OnlinePunctuationModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.Vad
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.kws.KeywordTokenizer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.KeywordStream
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PunctuationRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Punctuator
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.RecognizedText
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SegmentRecognizer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.StreamingStream
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VoiceDetector
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VoiceSegment
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.Closeable
import java.io.File

/**
 * A loaded `OnlineRecognizer` (streaming transducer, roadmap D17). Native calls that touch the shared model are made
 * under the instance's monitor; streams belong to one session each.
 * zh-CN: 已加载的 `OnlineRecognizer` (流式 transducer, 路线图 D17). 触及共享模型的原生调用在实例监视器内进行; 每个流只属于一个会话.
 */
class OnlineEngine(val recognizer: OnlineRecognizer, val modelId: String, val hotwords: Boolean) : Closeable {
    override fun close() = recognizer.release()

    /** A new stream; [hotwords] (one per line) only for an instance loaded for hotwords. zh-CN: 新的流; 只有为热词加载的实例才接受 [hotwords]. */
    fun stream(hotwords: List<String>): StreamingStream = OnlineEngineStream(this, hotwords)
}

/** A loaded `OfflineRecognizer` (SenseVoice, NeMo transducer). zh-CN: 已加载的 `OfflineRecognizer` (SenseVoice, NeMo transducer). */
class OfflineEngine(val recognizer: OfflineRecognizer, val modelId: String) : Closeable, SegmentRecognizer {
    override fun close() = recognizer.release()

    override fun recognize(samples: FloatArray): RecognizedText = synchronized(this) {
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(samples, AudioInput.SAMPLE_RATE)
            recognizer.decode(stream)
            val result = recognizer.getResult(stream)
            RecognizedText(result.text, result.lang, result.emotion, result.event)
        } finally {
            stream.release()
        }
    }
}

/**
 * A loaded `KeywordSpotter` with the token set and the lexicon of its model (roadmap D18); keywords are encoded per
 * session ([KeywordTokenizer]) and handed to `createStream`.
 * zh-CN: 已加载的 `KeywordSpotter` 及其模型的 token 集合与词典 (路线图 D18); 关键词按会话编码 ([KeywordTokenizer]) 后交给 `createStream`.
 */
class KeywordEngine(val spotter: KeywordSpotter, val modelId: String, val tokens: Set<String>, val lexicons: List<File>) : Closeable {
    override fun close() = spotter.release()

    fun stream(buffer: String, count: Int): KeywordStream = KeywordEngineStream(this, buffer, count)
}

/**
 * A loaded punctuation model (roadmap D107): `OfflinePunctuation` for ct-transformer, `OnlinePunctuation` for
 * cnn-bilstm; both work on whole texts, one call at a time per instance.
 * zh-CN: 已加载的标点模型 (路线图 D107): ct-transformer 用 `OfflinePunctuation`, cnn-bilstm 用 `OnlinePunctuation`; 两者都处理整段文本, 每个实例
 * 一次一个调用.
 */
class PunctuationEngine(
    val modelId: String,
    val modelType: String,
    val languages: List<String>,
    private val add: (String) -> String,
    private val release: () -> Unit,
) : Closeable, Punctuator {
    override fun punctuate(text: String): String = synchronized(this) { add(text) }

    override fun close() = release()
}

private class OnlineEngineStream(private val engine: OnlineEngine, hotwords: List<String>) : StreamingStream {
    private val stream: OnlineStream = synchronized(engine) {
        engine.recognizer.createStream(if (engine.hotwords) hotwords.joinToString("/") else "")
    }.also { SherpaEngines.requireNative(it, "OnlineStream") }

    override fun accept(samples: FloatArray) = stream.acceptWaveform(samples, AudioInput.SAMPLE_RATE)

    override fun decode() = synchronized(engine) {
        while (engine.recognizer.isReady(stream)) engine.recognizer.decode(stream)
    }

    override fun text(): String = synchronized(engine) { engine.recognizer.getResult(stream).text }

    override fun reset() = synchronized(engine) { engine.recognizer.reset(stream) }

    override fun close() = stream.release()
}

private class KeywordEngineStream(private val engine: KeywordEngine, private val buffer: String, private val count: Int) : KeywordStream {
    private var stream: OnlineStream = create()

    private fun create(): OnlineStream = synchronized(engine) { engine.spotter.createStream(buffer) }.also { SherpaEngines.requireNative(it, "keyword stream") }

    override fun accept(samples: FloatArray) = stream.acceptWaveform(samples, AudioInput.SAMPLE_RATE)

    override fun poll(): Int? = synchronized(engine) {
        while (engine.spotter.isReady(stream)) {
            engine.spotter.decode(stream)
            val label = engine.spotter.getResult(stream).keyword
            if (label.isNotBlank()) {
                engine.spotter.reset(stream)
                KeywordTokenizer.indexOf(label, count)?.let { return@synchronized it }
            }
        }
        null
    }

    override fun restart() {
        stream.release()
        stream = create()
    }

    override fun close() = stream.release()
}

/** The bundled silero VAD (roadmap D5) behind [VoiceDetector]. zh-CN: 以 [VoiceDetector] 包装的随包 silero VAD (路线图 D5). */
class SileroVoiceDetector(private val vad: Vad) : VoiceDetector {
    override val windowSize: Int = SherpaRuntime.VAD_WINDOW_SIZE

    override fun accept(window: FloatArray) = vad.acceptWaveform(window)

    override fun isSpeech(): Boolean = vad.isSpeechDetected()

    override fun poll(): VoiceSegment? {
        if (vad.empty()) return null
        val segment = vad.front()
        vad.pop()
        return VoiceSegment(segment.start.toLong(), segment.samples)
    }

    override fun flush() = vad.flush()

    override fun reset() = vad.reset()

    override fun close() = vad.release()
}

/**
 * Builds sherpa-onnx 1.13.8 instances from installed models (roadmap D13, D17, D18): file roles come from the model's
 * `runtime.config` (catalog models) or are inferred from upstream file names (imported models, [RuntimeRoles]). Every
 * role file is checked before any native call, and an instance whose native pointer stayed 0 (the JNI side refused
 * the configuration) is reported as `ENGINE_FAILED` instead of crashing later.
 * zh-CN: 由已安装模型构建 sherpa-onnx 1.13.8 实例 (路线图 D13, D17, D18): 文件角色取自模型的 `runtime.config` (目录模型), 或按上游文件名推断 (导入
 * 模型, [RuntimeRoles]). 调用原生代码之前先检查每个角色文件; 原生指针仍为 0 (JNI 一侧拒绝了配置) 的实例报告为 `ENGINE_FAILED`, 而不是之后崩溃.
 */
object SherpaEngines {

    /** onnxruntime intra-op threads of the recognizers; KWS and VAD use one. zh-CN: 识别器的 onnxruntime 线程数; KWS 与 VAD 使用 1 个. */
    const val ASR_THREADS = 2

    /** `keywordsThreshold` default of `KeywordSpotterConfig` (the reported score when a keyword sets none). zh-CN: `KeywordSpotterConfig` 的阈值默认值. */
    const val DEFAULT_KEYWORD_THRESHOLD = 0.25f

    const val SENSE_VOICE = "sense-voice"
    const val OFFLINE_TRANSDUCER = "offline-transducer"

    fun online(model: InstalledModel, hotwords: Boolean): OnlineEngine {
        val roles = RuntimeRoles.of(model)
        val bpeVocab = roles.optionalFile(model, "bpeVocab")
        val canHotword = hotwords && bpeVocab != null
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = AudioInput.SAMPLE_RATE, featureDim = 80),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = roles.file(model, "encoder"),
                    decoder = roles.file(model, "decoder"),
                    joiner = roles.file(model, "joiner"),
                ),
                tokens = roles.file(model, "tokens"),
                numThreads = ASR_THREADS,
                modelType = roles.value("modelType"),
                modelingUnit = if (canHotword) modelingUnit(model) else "",
                bpeVocab = if (canHotword) bpeVocab.orEmpty() else "",
            ),
            enableEndpoint = false,
            decodingMethod = if (canHotword) "modified_beam_search" else "greedy_search",
            maxActivePaths = 4,
        )
        val recognizer = load(model) { OnlineRecognizer(null, config) }
        requireNative(recognizer, model.modelId)
        return OnlineEngine(recognizer, model.modelId, canHotword)
    }

    /**
     * An offline recognizer: SenseVoice with [language] (empty for automatic detection), or a NeMo transducer
     * (`offline-transducer`, roadmap D131), which takes no language; an imported one without a `modelType` role is read
     * as `nemo_transducer`, the only value the catalog allows.
     * zh-CN: 离线识别器: 带 [language] 的 SenseVoice (空为自动识别), 或不接受语言的 NeMo transducer (`offline-transducer`, 路线图 D131); 没有
     * `modelType` 角色的导入模型按 `nemo_transducer` 读取, 这是目录允许的唯一取值.
     */
    fun offline(model: InstalledModel, language: String?): OfflineEngine {
        val roles = RuntimeRoles.of(model)
        val modelConfig = when (model.modelType) {
            OFFLINE_TRANSDUCER -> OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(
                    encoder = roles.file(model, "encoder"),
                    decoder = roles.file(model, "decoder"),
                    joiner = roles.file(model, "joiner"),
                ),
                tokens = roles.file(model, "tokens"),
                numThreads = ASR_THREADS,
                modelType = roles.value("modelType").ifEmpty { CatalogRules.NEMO_TRANSDUCER },
            )
            else -> OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(model = roles.file(model, "model"), language = language.orEmpty(), useInverseTextNormalization = true),
                tokens = roles.file(model, "tokens"),
                numThreads = ASR_THREADS,
            )
        }
        val config = OfflineRecognizerConfig(featConfig = FeatureConfig(sampleRate = AudioInput.SAMPLE_RATE, featureDim = 80), modelConfig = modelConfig)
        val recognizer = load(model) { OfflineRecognizer(null, config) }
        requireNative(recognizer, model.modelId)
        return OfflineEngine(recognizer, model.modelId)
    }

    /**
     * A keyword spotter whose built-in keyword list is [emptyKeywords] (an empty file): the sessions bring their
     * own keywords through `createStream`.
     * zh-CN: 内置关键词列表为 [emptyKeywords] (空文件) 的关键词检测器: 会话经 `createStream` 带入各自的关键词.
     */
    fun keywords(model: InstalledModel, emptyKeywords: File): KeywordEngine {
        val roles = RuntimeRoles.of(model)
        val tokensPath = roles.file(model, "tokens")
        val tokens = File(tokensPath).inputStream().use(KeywordTokenizer::readTokens)
        val config = KeywordSpotterConfig(
            featConfig = FeatureConfig(sampleRate = AudioInput.SAMPLE_RATE, featureDim = 80),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = roles.file(model, "encoder"),
                    decoder = roles.file(model, "decoder"),
                    joiner = roles.file(model, "joiner"),
                ),
                tokens = tokensPath,
                numThreads = 1,
                modelType = roles.value("modelType"),
            ),
            keywordsFile = emptyKeywords.path,
        )
        val spotter = load(model) { KeywordSpotter(null, config) }
        requireNative(spotter, model.modelId)
        return KeywordEngine(spotter, model.modelId, tokens, roles.files(model, "lexicon").map(::File))
    }

    /** A punctuation model of roadmap D107 (one onnxruntime thread). zh-CN: 路线图 D107 的标点模型 (onnxruntime 单线程). */
    fun punctuation(model: InstalledModel): PunctuationEngine {
        val roles = RuntimeRoles.of(model)
        return when (model.modelType) {
            PunctuationRules.CT_TRANSFORMER -> {
                val config = OfflinePunctuationConfig(OfflinePunctuationModelConfig(ctTransformer = roles.file(model, "model"), numThreads = 1))
                val punctuation = load(model) { OfflinePunctuation(null, config) }
                requireNative(punctuation, model.modelId)
                PunctuationEngine(model.modelId, model.modelType, model.languages, punctuation::addPunctuation, punctuation::release)
            }
            PunctuationRules.CNN_BILSTM -> {
                val config = OnlinePunctuationConfig(
                    OnlinePunctuationModelConfig(cnnBilstm = roles.file(model, "model"), bpeVocab = roles.file(model, "bpeVocab"), numThreads = 1),
                )
                val punctuation = load(model) { OnlinePunctuation(null, config) }
                requireNative(punctuation, model.modelId)
                PunctuationEngine(model.modelId, model.modelType, model.languages, punctuation::addPunctuation, punctuation::release)
            }
            else -> throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "${model.modelId} is not a punctuation model this build can load (${model.modelType})")
        }
    }

    fun vad(assets: AssetManager, settings: SherpaRuntime.VadSettings): SileroVoiceDetector = SileroVoiceDetector(SherpaRuntime.createVad(assets, settings))

    /** The hotword modeling unit of a transducer with `bpe.vocab`. zh-CN: 带 `bpe.vocab` 的 transducer 的热词建模单元. */
    private fun modelingUnit(model: InstalledModel): String = if (model.languages.any { it == "zh" || it.startsWith("zh-") }) "cjkchar+bpe" else "bpe"

    private inline fun <T> load(model: InstalledModel, create: () -> T): T = try {
        create()
    } catch (e: SpeechException) {
        throw e
    } catch (e: RuntimeException) {
        throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "Loading ${model.modelId} failed: ${e.message ?: e.javaClass.simpleName}", e)
    } catch (e: UnsatisfiedLinkError) {
        throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "The sherpa-onnx runtime is unavailable: ${e.message}", RuntimeException(e))
    }

    /**
     * Fails with `ENGINE_FAILED` when a sherpa-onnx object kept the native pointer 0 (`ptr`, kept by the R8 rules).
     * zh-CN: sherpa-onnx 对象的原生指针仍为 0 时以 `ENGINE_FAILED` 失败 (`ptr` 字段由 R8 规则保留).
     */
    fun requireNative(instance: Any, label: String) {
        val pointer = runCatching {
            instance.javaClass.getDeclaredField("ptr").apply { isAccessible = true }.getLong(instance)
        }.getOrDefault(-1L)
        if (pointer == 0L) throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "sherpa-onnx refused the configuration of $label")
    }
}

/**
 * File roles of an installed model for sherpa-onnx (roadmap P2.3 deviation 3): the catalog's `runtime.config`, or,
 * for imported models without one, roles inferred from the upstream file names (`encoder*.onnx`, `decoder*.onnx`,
 * `joiner*.onnx`, `model*.onnx`, int8 preferred for encoders, joiners and models; `tokens.txt`, `bpe.vocab`, `*.phone`;
 * for TTS models (roadmap P4.1) also `lexicon*.txt`, top-level `*.fst` / `*.far` rules, `voices.bin`,
 * `espeak-ng-data/`, a `model-steps-*.onnx` acoustic model and a `hifigan*` / `vocos*` vocoder).
 * zh-CN: 已安装模型供 sherpa-onnx 使用的文件角色 (路线图 P2.3 偏差 3): 目录的 `runtime.config`; 没有它的导入模型按上游文件名推断角色
 * (`encoder*.onnx`, `decoder*.onnx`, `joiner*.onnx`, `model*.onnx`, 编码器, joiner 与模型优先 int8; `tokens.txt`, `bpe.vocab`, `*.phone`;
 * TTS 模型 (路线图 P4.1) 另有 `lexicon*.txt`, 顶层 `*.fst` / `*.far` 规则, `voices.bin`, `espeak-ng-data/`, `model-steps-*.onnx` 声学模型与
 * `hifigan*` / `vocos*` 声码器).
 */
class RuntimeRoles private constructor(private val roles: Map<String, Any>) {

    fun file(model: InstalledModel, role: String): String =
        optionalFile(model, role) ?: throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "${model.modelId} has no '$role' file")

    fun optionalFile(model: InstalledModel, role: String): String? {
        val path = roles[role] as? String ?: return null
        val file = model.resolve(path)
        if (!file.isFile || file.length() == 0L) throw SpeechException(SpeechErrorCodes.MODEL_MISSING, "${model.modelId} is incomplete: $path is missing")
        return file.path
    }

    fun files(model: InstalledModel, role: String): List<String> = (roles[role] as? List<*>).orEmpty().mapNotNull { it as? String }.map { path ->
        model.resolve(path).also { if (!it.isFile) throw SpeechException(SpeechErrorCodes.MODEL_MISSING, "${model.modelId} is incomplete: $path is missing") }.path
    }

    fun value(role: String): String = roles[role] as? String ?: ""

    /** A directory role (`dataDir`): its absolute path, or null when the model has none. zh-CN: 目录角色 (`dataDir`) 的绝对路径. */
    fun optionalDirectory(model: InstalledModel, role: String): String? {
        val path = roles[role] as? String ?: return null
        val directory = model.resolve(path)
        if (!directory.isDirectory) throw SpeechException(SpeechErrorCodes.MODEL_MISSING, "${model.modelId} is incomplete: $path is missing")
        return directory.path
    }

    companion object {
        fun of(model: InstalledModel): RuntimeRoles {
            if (model.runtimeConfig.isNotEmpty()) return RuntimeRoles(model.runtimeConfig)
            val names = model.files.map { it.path }
            fun pick(stem: String, preferInt8: Boolean): String? {
                val matches = names.filter { it.substringAfterLast('/').startsWith(stem) && it.endsWith(".onnx") }
                return matches.firstOrNull { it.contains("int8") == preferInt8 } ?: matches.firstOrNull()
            }
            val inferred = LinkedHashMap<String, Any>()
            pick("encoder", true)?.let { inferred["encoder"] = it }
            pick("decoder", false)?.let { inferred["decoder"] = it }
            pick("joiner", true)?.let { inferred["joiner"] = it }
            pick("model", true)?.let { inferred["model"] = it }
            names.firstOrNull { it.substringAfterLast('/') == "tokens.txt" }?.let { inferred["tokens"] = it }
            names.firstOrNull { it.substringAfterLast('/') == "bpe.vocab" }?.let { inferred["bpeVocab"] = it }
            names.filter { it.endsWith(".phone") }.takeIf { it.isNotEmpty() }?.let { inferred["lexicon"] = it }
            if (model.kind == "tts") {
                val top = names.filter { '/' !in it }
                top.filter { it.startsWith("lexicon") && it.endsWith(".txt") }.takeIf { it.isNotEmpty() }?.let { inferred["lexicon"] = it }
                top.filter { it.endsWith(".fst") }.takeIf { it.isNotEmpty() }?.let { inferred["ruleFsts"] = it }
                top.filter { it.endsWith(".far") }.takeIf { it.isNotEmpty() }?.let { inferred["ruleFars"] = it }
                top.firstOrNull { it == "voices.bin" }?.let { inferred["voices"] = it }
                if (names.any { it.startsWith("espeak-ng-data/") }) inferred["dataDir"] = "espeak-ng-data"
                top.firstOrNull { it.startsWith("model-steps") && it.endsWith(".onnx") }?.let { inferred["acousticModel"] = it }
                top.firstOrNull { (it.startsWith("hifigan") || it.startsWith("vocos")) && it.endsWith(".onnx") }?.let { inferred["vocoder"] = it }
                if (inferred["model"] == null) top.firstOrNull { it.endsWith(".onnx") && it != inferred["acousticModel"] && it != inferred["vocoder"] }?.let { inferred["model"] = it }
            }
            return RuntimeRoles(inferred)
        }
    }
}
