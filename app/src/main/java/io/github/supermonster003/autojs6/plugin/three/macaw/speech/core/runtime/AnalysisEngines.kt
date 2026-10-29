package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime

import com.k2fsa.sherpa.onnx.AudioTagging
import com.k2fsa.sherpa.onnx.AudioTaggingConfig
import com.k2fsa.sherpa.onnx.AudioTaggingModelConfig
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentification
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentificationConfig
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentificationWhisperConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.AnalysisRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagLabel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.InstalledModel
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.Closeable
import java.io.File

/**
 * A loaded `OfflineSpeakerDiarization` (roadmap P8.4.3, D146): a pyannote segmentation model and a speaker embedding
 * model; [process] sets the clustering of one call (the known speaker count, or the distance threshold) and runs the
 * whole file, one call at a time per instance. Progress comes per embedded chunk, which the engine reports only after
 * the segmentation of the whole file, and sherpa-onnx 1.13.8 ignores the callback's answer, so a call cannot stop early.
 * zh-CN: 已加载的 `OfflineSpeakerDiarization` (路线图 P8.4.3, D146): pyannote 分割模型与说话人嵌入模型; [process] 为一次调用设置聚类 (已知人数
 * 或距离阈值) 并处理整个文件, 每个实例一次一个调用. 进度按已提取嵌入的块计, 引擎在整段分割完成后才开始报告; sherpa-onnx 1.13.8 忽略回调的返回值,
 * 因此调用无法提前停止.
 */
class DiarizationEngine(
    private val diarization: OfflineSpeakerDiarization,
    private val base: OfflineSpeakerDiarizationConfig,
    val modelId: String,
    val speakerModelId: String,
) : Closeable {

    /** Segments as (start, end) seconds and the clustering's speaker. zh-CN: 分段, 以 (开始, 结束) 秒与聚类得到的说话人表示. */
    fun process(samples: FloatArray, speakers: Int, threshold: Float, onProgress: (Int, Int) -> Unit): List<Triple<Float, Float, Int>> = synchronized(this) {
        diarization.setConfig(base.copy(clustering = FastClusteringConfig(numClusters = if (speakers > 0) speakers else -1, threshold = threshold)))
        diarization.processWithCallback(samples, Progress(onProgress), 0L).map { Triple(it.start, it.end, it.speaker) }
    }

    override fun close() = diarization.release()

    /**
     * The progress callback of `processWithCallback`: the JNI side calls `invoke(IIJ)Ljava/lang/Integer;` by name, which
     * a Kotlin lambda lacks (proguard-rules.pro keeps it). It always answers 0 (continue).
     * zh-CN: `processWithCallback` 的进度回调: JNI 一侧按名调用 `invoke(IIJ)Ljava/lang/Integer;`, Kotlin lambda 没有该方法 (由 proguard-rules.pro
     * 保留). 始终返回 0 (继续).
     */
    class Progress(private val block: (Int, Int) -> Unit) : Function3<Int, Int, Long, Int> {
        override fun invoke(done: Int, total: Int, arg: Long): Int {
            runCatching { block(done, total) }
            return 0
        }
    }
}

/**
 * A loaded whisper language identifier (roadmap D144): one 16 kHz window of at most 30 seconds in, a whisper language
 * code out (sherpa-onnx gives no scores).
 * zh-CN: 已加载的 whisper 语种识别器 (路线图 D144): 输入一个至多 30 秒的 16 kHz 窗, 输出 whisper 语种代码 (sherpa-onnx 不给分数).
 */
class LanguageEngine(private val identification: SpokenLanguageIdentification, val modelId: String) : Closeable {

    fun identify(samples: FloatArray): String = synchronized(this) {
        val stream = identification.createStream()
        try {
            stream.acceptWaveform(samples, AudioInput.SAMPLE_RATE)
            identification.compute(stream).trim().ifEmpty { throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "$modelId answered no language") }
        } finally {
            stream.release()
        }
    }

    override fun close() = identification.release()
}

/**
 * A loaded CED audio tagger (roadmap D145) with its AudioSet [labels]: one window of at most 10 seconds (CED refuses
 * longer input) in, the score of every label out.
 * zh-CN: 已加载的 CED 音频标签模型 (路线图 D145) 及其 AudioSet [labels]: 输入一个至多 10 秒的窗 (CED 拒绝更长的输入), 输出每个标签的分数.
 */
class TaggerEngine(private val tagging: AudioTagging, val labels: List<TagLabel>, val modelId: String) : Closeable {

    fun scores(samples: FloatArray): List<Pair<TagLabel, Float>> = synchronized(this) {
        val stream = tagging.createStream()
        try {
            stream.acceptWaveform(samples, AudioInput.SAMPLE_RATE)
            tagging.compute(stream, labels.size).mapNotNull { event -> labels.getOrNull(event.index)?.let { it to event.prob } }
        } finally {
            stream.release()
        }
    }

    override fun close() = tagging.release()
}

/**
 * Builds the analysis engines from installed models (roadmap P8.4.3): file roles from the catalog's `runtime.config`
 * ([RuntimeRoles]), the recognizers' thread count, and the native pointer checked like every other engine.
 * zh-CN: 由已安装模型构建分析引擎 (路线图 P8.4.3): 文件角色取自目录的 `runtime.config` ([RuntimeRoles]), 线程数与识别器相同, 与其他引擎一样检查原生指针.
 */
object AnalysisEngines {

    /** pyannote window shift (roadmap D154): 0.5 of the 10 second window. zh-CN: pyannote 窗口步长 (路线图 D154). */
    const val WINDOW_SHIFT_RATIO = 0.5f

    /** Shortest speech kept as a turn and shortest pause that splits one speaker's turns, seconds (sherpa-onnx defaults). zh-CN: 最短语音与最短停顿 (秒). */
    const val MIN_DURATION_ON = 0.3f
    const val MIN_DURATION_OFF = 0.5f

    /**
     * Clustering distance of a diarization without a speaker count, by embedding model (roadmap D154: 0.85 counted the
     * speakers of all 14 test dialogs right with ERes2Net and the fp32 segmentation).
     * zh-CN: 未给人数时的聚类距离, 按嵌入模型取值 (路线图 D154: ERes2Net 与 fp32 分割在 0.85 时 14 段测试对话的人数全对).
     */
    val CLUSTER_THRESHOLDS: Map<String, Float> = mapOf("speaker.eres2net.3dspeaker.en-voxceleb" to 0.85f)
    const val FALLBACK_CLUSTER_THRESHOLD = 0.85f

    fun clusterThreshold(speakerModelId: String): Float = CLUSTER_THRESHOLDS[speakerModelId] ?: FALLBACK_CLUSTER_THRESHOLD

    fun diarization(segmentation: InstalledModel, speaker: InstalledModel): DiarizationEngine {
        val config = OfflineSpeakerDiarizationConfig(
            segmentation = OfflineSpeakerSegmentationModelConfig(
                pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(model = RuntimeRoles.of(segmentation).file(segmentation, "model"), windowShiftRatio = WINDOW_SHIFT_RATIO),
                numThreads = SherpaEngines.ASR_THREADS,
            ),
            embedding = SpeakerEmbeddingExtractorConfig(model = RuntimeRoles.of(speaker).file(speaker, "model"), numThreads = SherpaEngines.ASR_THREADS),
            clustering = FastClusteringConfig(numClusters = -1, threshold = 0.5f),
            minDurationOn = MIN_DURATION_ON,
            minDurationOff = MIN_DURATION_OFF,
        )
        val diarization = load(segmentation.modelId) { OfflineSpeakerDiarization(null, config) }
        SherpaEngines.requireNative(diarization, segmentation.modelId)
        return DiarizationEngine(diarization, config, segmentation.modelId, speaker.modelId)
    }

    fun language(model: InstalledModel): LanguageEngine {
        val roles = RuntimeRoles.of(model)
        val config = SpokenLanguageIdentificationConfig(
            whisper = SpokenLanguageIdentificationWhisperConfig(encoder = roles.file(model, "encoder"), decoder = roles.file(model, "decoder")),
            numThreads = SherpaEngines.ASR_THREADS,
        )
        val identification = load(model.modelId) { SpokenLanguageIdentification(null, config) }
        SherpaEngines.requireNative(identification, model.modelId)
        return LanguageEngine(identification, model.modelId)
    }

    fun tagger(model: InstalledModel): TaggerEngine {
        val roles = RuntimeRoles.of(model)
        val labelsPath = roles.file(model, "labels")
        val labels = AnalysisRules.labels(File(labelsPath).readText(Charsets.UTF_8))
        val config = AudioTaggingConfig(model = AudioTaggingModelConfig(ced = roles.file(model, "model"), numThreads = SherpaEngines.ASR_THREADS), labels = labelsPath, topK = labels.size)
        val tagging = load(model.modelId) { AudioTagging(null, config) }
        SherpaEngines.requireNative(tagging, model.modelId)
        return TaggerEngine(tagging, labels, model.modelId)
    }

    private inline fun <T> load(modelId: String, create: () -> T): T = try {
        create()
    } catch (e: SpeechException) {
        throw e
    } catch (e: RuntimeException) {
        throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "Loading $modelId failed: ${e.message ?: e.javaClass.simpleName}", e)
    } catch (e: UnsatisfiedLinkError) {
        throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "The sherpa-onnx runtime is unavailable: ${e.message}", RuntimeException(e))
    }
}
