package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import kotlin.math.sqrt

/**
 * The voiceprint of one name (roadmap D125, D134): [mean] is the mean of the L2-normalized speaker embeddings of its
 * [samples] recordings, computed by [modelId] at [modelVersion]; scores compare it with a recording by cosine
 * similarity. It never leaves the plugin.
 * zh-CN: 一个名字的声纹 (路线图 D125, D134): [mean] 为其 [samples] 段录音经 L2 归一化后的说话人嵌入的平均值, 由 [modelId] 的 [modelVersion]
 * 计算; 打分时与录音按余弦相似度比较. 声纹从不离开插件.
 */
class Voiceprint(
    val name: String,
    val modelId: String,
    val modelVersion: Int,
    val samples: Int,
    val mean: FloatArray,
    val updatedAt: Long,
) {
    val dim: Int get() = mean.size
}

/** One scored voiceprint. zh-CN: 一个打过分的声纹. */
data class SpeakerScore(val name: String, val score: Double)

/**
 * The answer of a verification or an identification (roadmap D133): [best] is the highest score (null without any
 * voiceprint), [matched] whether it reached [threshold], [candidates] the best first.
 * zh-CN: 验证或辨认的结果 (路线图 D133): [best] 为最高分 (没有任何声纹时为 null), [matched] 表示是否达到 [threshold], [candidates] 按分数从高到低.
 */
data class SpeakerMatch(val best: SpeakerScore?, val threshold: Double, val candidates: List<SpeakerScore>, val modelId: String) {
    val matched: Boolean get() = best != null && best.score >= threshold
}

/**
 * Vector rules of the voiceprints: sherpa-onnx 1.13.8 `SpeakerEmbeddingManager` answers only names and booleans, so
 * the plugin keeps the means itself and scores them here (derived decision of roadmap P8.3.3). Pure JVM.
 * zh-CN: 声纹的向量规则: sherpa-onnx 1.13.8 的 `SpeakerEmbeddingManager` 只返回名字与布尔值, 因此插件自己保存平均值并在此打分 (路线图 P8.3.3
 * 的派生决定). 纯 JVM.
 */
object SpeakerMath {

    fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { it.toDouble() * it })
        if (norm == 0.0 || !norm.isFinite()) throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "The speaker embedding is empty")
        return FloatArray(vector.size) { (vector[it] / norm).toFloat() }
    }

    /** Cosine similarity, -1 to 1. zh-CN: 余弦相似度, -1 到 1. */
    fun cosine(a: FloatArray, b: FloatArray): Double {
        require(a.size == b.size) { "Embeddings of ${a.size} and ${b.size} dimensions" }
        var dot = 0.0
        var left = 0.0
        var right = 0.0
        for (i in a.indices) {
            dot += a[i].toDouble() * b[i]
            left += a[i].toDouble() * a[i]
            right += b[i].toDouble() * b[i]
        }
        if (left == 0.0 || right == 0.0) return 0.0
        return (dot / sqrt(left * right)).coerceIn(-1.0, 1.0)
    }

    /**
     * The mean after adding [embeddings] to [samples] earlier recordings whose mean is [mean] (null for a new name):
     * every recording weighs the same (roadmap D134).
     * zh-CN: 在均值为 [mean] 的 [samples] 段已有录音上加入 [embeddings] 后的新均值 ([mean] 为 null 表示新名字): 每段录音权重相同 (路线图 D134).
     */
    fun merge(mean: FloatArray?, samples: Int, embeddings: List<FloatArray>): FloatArray {
        require(embeddings.isNotEmpty()) { "No embeddings to add" }
        val dim = embeddings.first().size
        val sum = DoubleArray(dim)
        if (mean != null && samples > 0) {
            require(mean.size == dim) { "A mean of ${mean.size} dimensions cannot take ${dim}-dimensional embeddings" }
            for (i in 0 until dim) sum[i] = mean[i].toDouble() * samples
        }
        embeddings.forEach { embedding ->
            require(embedding.size == dim) { "Embeddings of different dimensions" }
            val unit = normalize(embedding)
            for (i in 0 until dim) sum[i] += unit[i].toDouble()
        }
        val count = (if (mean != null) samples else 0) + embeddings.size
        return FloatArray(dim) { (sum[it] / count).toFloat() }
    }

    /**
     * Scores [embedding] against [voiceprints] (all of one model), best first, at most [topK].
     * zh-CN: 以 [embedding] 对 [voiceprints] (均属同一模型) 打分, 按分数从高到低, 至多 [topK] 个.
     */
    fun rank(embedding: FloatArray, voiceprints: List<Voiceprint>, topK: Int): List<SpeakerScore> = voiceprints
        .filter { it.dim == embedding.size }
        .map { SpeakerScore(it.name, cosine(embedding, it.mean)) }
        .sortedWith(compareByDescending<SpeakerScore> { it.score }.thenBy { it.name })
        .take(topK)
}

/**
 * Argument rules of the speaker calls, the same as the host checks (`SpeechScriptArguments.speakerName`) so a direct
 * Binder caller meets them too. Pure JVM.
 * zh-CN: 说话人调用的参数规则, 与宿主的检查 (`SpeechScriptArguments.speakerName`) 相同, 直接调用 Binder 的调用方同样受约束. 纯 JVM.
 */
object SpeakerRules {

    /** Speech a recording needs after voice activity detection (`AUDIO_TOO_SHORT` below). zh-CN: 录音经 VAD 后至少需要的语音. */
    const val MIN_SPEECH_MILLIS = SpeechLimits.MIN_SPEAKER_SPEECH_MILLIS

    /** The threshold of a model the plugin has no measured value for. zh-CN: 插件没有实测值的模型使用的阈值. */
    const val FALLBACK_THRESHOLD = 0.5

    /**
     * Default thresholds per speaker model, measured in roadmap P8.3.1: the lowest cosine score with about 1% false
     * accepts on 2 s clips (D138); another model is measured the same way before it is added.
     * zh-CN: 各说话人模型的默认阈值, 在路线图 P8.3.1 实测: 2 s 片段误收约 1% 以内的最低余弦分数 (D138); 新增模型前照此另测.
     */
    val THRESHOLDS: Map<String, Double> = mapOf("speaker.eres2net.3dspeaker.en-voxceleb" to 0.50)

    fun threshold(modelId: String): Double = THRESHOLDS[modelId] ?: FALLBACK_THRESHOLD

    /** A name: trimmed, 1 to 64 UTF-16 characters, no control characters. zh-CN: 名字: 去首尾空白, 1 到 64 个 UTF-16 字符, 不含控制字符. */
    fun name(value: String?): String {
        val text = value?.trim() ?: throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "name is required")
        if (text.isEmpty()) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "name must not be blank")
        if (text.length > SpeechLimits.MAX_SPEAKER_NAME_CHARS) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "name exceeds ${SpeechLimits.MAX_SPEAKER_NAME_CHARS} characters")
        }
        if (text.any { Character.isISOControl(it) }) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "name must not contain control characters or line breaks")
        return text
    }

    /** A threshold between 0 and 1, or the model's default for null. zh-CN: 0 到 1 之间的阈值, 为 null 时取模型默认值. */
    fun threshold(value: Double?, modelId: String): Double {
        if (value == null) return threshold(modelId)
        if (!value.isFinite() || value < 0.0 || value > 1.0) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "threshold must be between 0 and 1, got $value")
        return value
    }

    fun topK(value: Int?): Int {
        val k = value ?: SpeechLimits.DEFAULT_IDENTIFY_CANDIDATES
        if (k < 1 || k > SpeechLimits.MAX_IDENTIFY_CANDIDATES) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "topK must be 1 to ${SpeechLimits.MAX_IDENTIFY_CANDIDATES}, got $k")
        return k
    }

    fun recordings(count: Int) {
        if (count < 1) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "At least one recording is required")
        if (count > SpeechLimits.MAX_ENROLL_FILES) throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "At most ${SpeechLimits.MAX_ENROLL_FILES} recordings, not $count")
    }
}
