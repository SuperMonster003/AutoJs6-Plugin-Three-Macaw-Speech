package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.RuntimeRoles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaEngines
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.Closeable

/**
 * A loaded TTS model as the synthesis loop sees it: sherpa-onnx `OfflineTts` on devices ([TtsEngine]), fakes in the
 * JVM tests. [generate] synthesizes one chunk and hands every generated piece to [onSamples], which answers false to
 * stop.
 * zh-CN: 合成循环所见的已加载 TTS 模型: 设备上为 sherpa-onnx `OfflineTts` ([TtsEngine]), JVM 测试中为替身. [generate] 合成一段, 把生成的每一块交给
 * [onSamples], 后者返回 false 即停止.
 */
interface TtsGenerator {
    val sampleRate: Int
    val speakerCount: Int

    fun generate(text: String, speakerId: Int, speed: Float, onSamples: (FloatArray) -> Boolean)
}

/**
 * Speech rate of the two synthesis paths (roadmap D19, D20): the Binder request takes `speed` 0.5 to 2.0 (the
 * contract range), the system engine maps Android's speech rate (100 = normal) to `rate / 100` within 0.25 to 4.
 * sherpa-onnx divides the model's length scale by the speed. Pitch has no sherpa-onnx equivalent and is ignored.
 * zh-CN: 两条合成路径的语速 (路线图 D19, D20): Binder 请求接受 `speed` 0.5 到 2.0 (契约范围), 系统引擎把 Android 语速 (100 为正常) 映射为
 * `rate / 100`, 范围 0.25 到 4. sherpa-onnx 以语速除模型的时长系数. 音高在 sherpa-onnx 中没有对应参数, 被忽略.
 */
object TtsSpeed {
    const val MIN_ENGINE = 0.25f
    const val MAX_ENGINE = 4f

    fun fromAndroidRate(rate: Int): Float = (rate / 100f).takeIf { it.isFinite() && rate > 0 }?.coerceIn(MIN_ENGINE, MAX_ENGINE) ?: 1f
}

/**
 * The synthesis loop shared by `synthesizeToFile` and the system engine (roadmap P4.1): [SentenceSplitter] chunks the
 * text, every speakable chunk goes to the generator, [sink] receives the samples as they come (and answers false to
 * stop), a short pause separates sentences, and [onProgress] reports the characters done after every chunk.
 * [isCancelled] is checked between chunks and inside the generator's callback, so a cancel stops within one sentence.
 * The callback runs inside the native generation: nothing may leave it as an exception, so a failure of [sink] stops the
 * generation and is thrown after it returned.
 * zh-CN: `synthesizeToFile` 与系统引擎共用的合成循环 (路线图 P4.1): [SentenceSplitter] 切分文本, 每个可朗读的段交给生成器, [sink] 随生成接收
 * 采样 (返回 false 即停止), 句间插入短暂停顿, [onProgress] 在每段之后报告已完成的字符数. [isCancelled] 在段之间与生成器回调中检查, 因此取消
 * 在一句之内生效. 回调运行在原生生成过程中, 不能有异常从中抛出, 因此 [sink] 的失败会停止生成, 并在生成返回后抛出.
 */
object TtsSynthesis {

    /** Silence between two chunks. zh-CN: 两段之间的静音. */
    const val PAUSE_MILLIS = 120

    data class Outcome(val samples: Long, val chunks: Int, val cancelled: Boolean)

    fun run(
        generator: TtsGenerator,
        text: String,
        speakerId: Int,
        speed: Float,
        isCancelled: () -> Boolean,
        onProgress: (done: Int, total: Int) -> Unit,
        sink: (FloatArray) -> Boolean,
        maxChars: Int = SentenceSplitter.DEFAULT_MAX_CHARS,
    ): Outcome {
        val chunks = SentenceSplitter.split(text, maxChars)
        val pause = FloatArray(generator.sampleRate * PAUSE_MILLIS / 1000)
        var samples = 0L
        var spoken = 0
        for (chunk in chunks) {
            if (isCancelled()) return Outcome(samples, spoken, cancelled = true)
            if (chunk.speakable) {
                if (spoken > 0) {
                    if (!sink(pause)) return Outcome(samples, spoken, cancelled = true)
                    samples += pause.size
                }
                var stopped = false
                var failure: Throwable? = null
                generator.generate(chunk.text, speakerId, speed) { piece ->
                    val keep = !stopped && try {
                        !isCancelled() && (piece.isEmpty() || sink(piece))
                    } catch (t: Throwable) {
                        failure = t
                        false
                    }
                    if (keep) samples += piece.size else stopped = true
                    keep
                }
                failure?.let { throw it }
                if (stopped || isCancelled()) return Outcome(samples, spoken + 1, cancelled = true)
                spoken++
            }
            onProgress(chunk.end, text.length)
        }
        return Outcome(samples, spoken, cancelled = false)
    }
}

/**
 * A loaded sherpa-onnx `OfflineTts` ([TtsGenerator]); generation is serialized on the instance because the system
 * engine and Binder syntheses may share it.
 * zh-CN: 已加载的 sherpa-onnx `OfflineTts` ([TtsGenerator]); 系统引擎与 Binder 合成可能共用同一实例, 因此生成在实例上串行进行.
 */
class TtsEngine(private val tts: OfflineTts, val modelId: String, val speakers: TtsSpeakers) : Closeable, TtsGenerator {
    override val sampleRate: Int = tts.sampleRate()
    override val speakerCount: Int = speakers.count

    override fun generate(text: String, speakerId: Int, speed: Float, onSamples: (FloatArray) -> Boolean) {
        synchronized(this) {
            tts.generateWithCallback(text, speakerId, speed, SampleCallback(onSamples))
        }
    }

    override fun close() = synchronized(this) { tts.release() }

    /**
     * The generation callback as a class: `libsherpa-onnx-jni.so` calls `invoke([F)Ljava/lang/Integer;` by name, which a
     * Kotlin 2 lambda (compiled through invokedynamic) does not declare; answers 1 to go on, 0 to stop. Kept by
     * `proguard-rules.pro`.
     * zh-CN: 以类实现的生成回调: `libsherpa-onnx-jni.so` 按名称调用 `invoke([F)Ljava/lang/Integer;`, 而 Kotlin 2 的 lambda (经 invokedynamic 编译)
     * 不声明该方法; 返回 1 继续, 0 停止. 由 `proguard-rules.pro` 保留.
     */
    class SampleCallback(private val onSamples: (FloatArray) -> Boolean) : (FloatArray) -> Int {
        override fun invoke(samples: FloatArray): Int = if (onSamples(samples)) 1 else 0
    }
}

/**
 * Builds sherpa-onnx 1.13.8 `OfflineTts` instances from installed TTS models (roadmap P4.1, D13): the configuration
 * follows the model's `runtime.config` (catalog models) or the roles [RuntimeRoles] infers for imported ones, for the
 * three model types of the catalog: `vits` (model, tokens, lexicon[], dataDir; MeloTTS, Piper, AISHELL-3), `matcha`
 * (acousticModel + vocoder, tokens, lexicon[], dataDir) and `kokoro` (model, voices, tokens, lexicon[], dataDir).
 * Lexicons and rule FSTs / FARs are joined with commas, as sherpa-onnx expects; one sentence per generation
 * (`maxNumSentences = 1`), the plugin splits the text itself.
 * zh-CN: 由已安装的 TTS 模型构建 sherpa-onnx 1.13.8 `OfflineTts` 实例 (路线图 P4.1, D13): 配置依模型的 `runtime.config` (目录模型) 或 [RuntimeRoles]
 * 为导入模型推断的角色, 支持目录中的三种模型类型: `vits` (model, tokens, lexicon[], dataDir; MeloTTS, Piper, AISHELL-3), `matcha`
 * (acousticModel + vocoder, tokens, lexicon[], dataDir) 与 `kokoro` (model, voices, tokens, lexicon[], dataDir). 词典与规则 FST / FAR 按
 * sherpa-onnx 的要求以逗号连接; 每次生成一句 (`maxNumSentences = 1`), 由插件自行切分文本.
 */
object TtsEngineCore {

    /**
     * onnxruntime intra-op threads of a synthesizer, at most the processor count. Four instead of the two of the
     * recognizers: on the G8441 the real-time factor of MeloTTS fell from 2.99 to 1.67 and of Kokoro from 3.9 to 2.8, on
     * the XQ-DQ72 from 0.83 to 0.54 and from 1.04 to 0.71 (roadmap P4.1, `docs/development/tts-baseline.md`).
     * zh-CN: 合成器的 onnxruntime 线程数, 不超过处理器数. 用 4 个而非识别器的 2 个: G8441 上 MeloTTS 的实时率由 2.99 降到 1.67, Kokoro 由 3.9 降到
     * 2.8, XQ-DQ72 上由 0.83 降到 0.54, 由 1.04 降到 0.71 (路线图 P4.1, `docs/development/tts-baseline.md`).
     */
    const val TTS_THREADS = 4

    val MODEL_TYPES = setOf("vits", "matcha", "kokoro")

    fun threads(): Int = minOf(TTS_THREADS, Runtime.getRuntime().availableProcessors().coerceAtLeast(1))

    fun config(model: InstalledModel, roles: RuntimeRoles = RuntimeRoles.of(model), threads: Int = threads()): OfflineTtsConfig {
        val lexicon = roles.files(model, "lexicon").joinToString(",")
        val dataDir = roles.optionalDirectory(model, "dataDir").orEmpty()
        val tokens = roles.file(model, "tokens")
        val modelConfig = when (model.modelType) {
            "vits" -> OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(model = roles.file(model, "model"), lexicon = lexicon, tokens = tokens, dataDir = dataDir),
                numThreads = threads,
            )
            "matcha" -> OfflineTtsModelConfig(
                matcha = OfflineTtsMatchaModelConfig(
                    acousticModel = roles.file(model, "acousticModel"),
                    vocoder = roles.file(model, "vocoder"),
                    lexicon = lexicon,
                    tokens = tokens,
                    dataDir = dataDir,
                ),
                numThreads = threads,
            )
            "kokoro" -> OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = roles.file(model, "model"),
                    voices = roles.file(model, "voices"),
                    tokens = tokens,
                    dataDir = dataDir,
                    lexicon = lexicon,
                ),
                numThreads = threads,
            )
            else -> throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "${model.modelId} is a ${model.modelType} model; 3-Macaw Speech synthesizes with ${MODEL_TYPES.joinToString()} models")
        }
        return OfflineTtsConfig(
            model = modelConfig,
            ruleFsts = roles.files(model, "ruleFsts").joinToString(","),
            ruleFars = roles.files(model, "ruleFars").joinToString(","),
            maxNumSentences = 1,
        )
    }

    /** The speakers of [model] with the names of its `speakers.txt`, before the model is loaded. zh-CN: 加载前得到的 [model] 的说话人. */
    fun speakers(model: InstalledModel, count: Int = model.speakers ?: 1): TtsSpeakers {
        val names = model.files.firstOrNull { it.path == TtsSpeakers.NAMES_FILE }?.let { entry ->
            runCatching { TtsSpeakers.parseNames(model.resolve(entry.path).readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
        }.orEmpty()
        val speakers = maxOf(1, count)
        return TtsSpeakers(speakers, names, TtsSpeakerHints.defaultSpeaker(model.modelId, speakers))
    }

    /** Loads [model] (seconds for the large models); `ENGINE_FAILED` when sherpa-onnx refuses it. zh-CN: 加载 [model]. */
    fun load(model: InstalledModel): TtsEngine {
        val config = config(model)
        val tts = try {
            OfflineTts(null, config)
        } catch (e: RuntimeException) {
            throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "Loading ${model.modelId} failed: ${e.message ?: e.javaClass.simpleName}", e)
        } catch (e: UnsatisfiedLinkError) {
            throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "The sherpa-onnx runtime is unavailable: ${e.message}", RuntimeException(e))
        }
        SherpaEngines.requireNative(tts, model.modelId)
        return TtsEngine(tts, model.modelId, speakers(model, tts.numSpeakers()))
    }

    /** Bytes the engine budget counts for [model]: its installed files. zh-CN: 引擎预算为 [model] 计入的字节数: 其安装文件. */
    fun weight(model: InstalledModel): Long = model.installedBytes
}
