package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.os.Bundle
import android.os.Parcel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CommercialUse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscript
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.OptionSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechWarning
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VoiceActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerMatch
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.Voiceprint
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.autojs.plugin.speech.api.SpeechModelKeys
import org.autojs.plugin.speech.api.SpeechResultKeys

/**
 * Bundle encoding of the speech contract on the plugin side (`docs/dev/speech-plugin-protocol-v1.md`): every answer
 * and completion carries `contractVersion`, failures carry only `errorCode` and `errorMessage` (English, at most 4 KiB
 * of UTF-8), and calls that cannot answer with a Bundle throw `CODE: detail`.
 * zh-CN: 插件侧语音契约的 Bundle 编码 (`docs/dev/speech-plugin-protocol-v1.md`): 每个应答与完成回调都带 `contractVersion`, 失败只带
 * `errorCode` 与 `errorMessage` (英文, 不超过 4 KiB UTF-8), 无法以 Bundle 应答的调用抛出 `CODE: detail`.
 */
internal object SpeechBundles {

    /**
     * An answer stamped with [version], the version of its request (roadmap D136): 1 for the calls of contract version 1,
     * which hosts before version 2 accept only, 2 for the speaker calls.
     * zh-CN: 带 [version] 的应答, 即其请求的版本 (路线图 D136): 契约版本 1 的调用为 1 (版本 2 之前的宿主只接受它), 说话人调用为 2.
     */
    fun answer(version: Int = SpeechContract.CALL_VERSION_1, build: Bundle.() -> Unit = {}): Bundle = Bundle().apply {
        putInt(SpeechContract.KEY_CONTRACT_VERSION, version)
        build()
    }

    fun failure(code: String, message: String?, version: Int = SpeechContract.CALL_VERSION_1): Bundle = answer(version) {
        putString(SpeechContract.KEY_ERROR_CODE, code)
        putString(SpeechContract.KEY_ERROR_MESSAGE, bounded(message ?: code))
    }

    fun failure(error: SpeechException, version: Int = SpeechContract.CALL_VERSION_1): Bundle = failure(error.code, error.message, version)

    /** The exception a Binder call throws for [code] (decoded by the host's `SpeechErrorCodes.decode`). */
    fun thrown(code: String, message: String?): RuntimeException {
        val text = SpeechErrorCodes.encode(code, bounded(message ?: ""))
        return if (code == SpeechErrorCodes.INVALID_OPTIONS || code == SpeechErrorCodes.MODEL_MISSING) IllegalArgumentException(text) else IllegalStateException(text)
    }

    fun thrown(error: SpeechException): RuntimeException = thrown(error.code, error.message)

    /**
     * One model as `SpeechModelKeys`. The contract's `commercialUse` is a Boolean, so the catalog value `"unknown"`
     * reads as false, which also means "needs `acceptLicense`" (roadmap D11).
     * zh-CN: 以 `SpeechModelKeys` 表示的单个模型. 契约中的 `commercialUse` 为布尔值, 目录取值 `"unknown"` 因此读作 false, 同样表示需要
     * `acceptLicense` (路线图 D11).
     */
    fun model(entry: ModelEntry): Bundle = Bundle().apply {
        putString(SpeechModelKeys.ID, entry.id)
        putString(SpeechModelKeys.KIND, entry.kind)
        putString(SpeechModelKeys.MODEL_TYPE, entry.modelType)
        putString(SpeechModelKeys.NAME, entry.name)
        putStringArray(SpeechModelKeys.LANGUAGES, entry.languages.toTypedArray())
        putLong(SpeechModelKeys.SIZE_BYTES, entry.sizeBytes)
        entry.installedBytes?.let { if (entry.installed) putLong(SpeechModelKeys.INSTALLED_BYTES, it) }
        putBoolean(SpeechModelKeys.INSTALLED, entry.installed)
        putBoolean(SpeechModelKeys.IS_DEFAULT, entry.isDefault)
        putBoolean(SpeechModelKeys.RECOMMENDED, entry.recommended)
        putInt(SpeechModelKeys.VERSION, entry.version)
        putBundle(SpeechModelKeys.LICENSE, Bundle().apply {
            putString(SpeechModelKeys.LICENSE_SPDX, entry.licenseSpdx)
            putBoolean(SpeechModelKeys.LICENSE_COMMERCIAL_USE, entry.commercialUse == CommercialUse.ALLOWED)
            entry.noticeUrl?.let { putString(SpeechModelKeys.LICENSE_NOTICE_URL, it) }
        })
        entry.speakers?.let { putInt(SpeechModelKeys.SPEAKERS, it) }
        entry.sampleRate?.let { putInt(SpeechModelKeys.SAMPLE_RATE, it) }
        putString(SpeechModelKeys.SOURCE, entry.source)
    }

    /** Truncates to the protocol's message ceiling on a character boundary. zh-CN: 按协议上限在字符边界截断. */
    fun bounded(message: String): String {
        if (message.toByteArray(Charsets.UTF_8).size <= SpeechLimits.MAX_ERROR_MESSAGE_BYTES) return message
        var end = minOf(message.length, SpeechLimits.MAX_ERROR_MESSAGE_BYTES)
        while (end > 0 && message.substring(0, end).toByteArray(Charsets.UTF_8).size > SpeechLimits.MAX_ERROR_MESSAGE_BYTES - 3) end--
        if (end > 0 && message[end - 1].isHighSurrogate()) end--
        return message.substring(0, end) + "..."
    }

    // Session events (ISpeechSessionCallback / ISpeechWakeCallback) and recognition answers (roadmap P3).

    fun ready(sessionId: String, modelId: String, round: Int?): Bundle = answer {
        putString(SpeechResultKeys.SESSION_ID, sessionId)
        putString(SpeechResultKeys.MODEL_ID, modelId)
        round?.let { putInt(SpeechResultKeys.ROUND, it) }
    }

    fun partial(text: String, segmentIndex: Int): Bundle = answer {
        putString(SpeechResultKeys.TEXT, text)
        putInt(SpeechResultKeys.SEGMENT_INDEX, segmentIndex)
    }

    fun utterance(utterance: Utterance): Bundle = answer { putUtterance(utterance, withIndex = true) }

    fun activity(active: Boolean): Bundle = answer { putBoolean(SpeechResultKeys.ACTIVE, active) }

    fun end(reason: String, text: String?, round: Int?): Bundle = answer {
        putString(SpeechResultKeys.REASON, reason)
        text?.let { putString(SpeechResultKeys.TEXT, it) }
        round?.let { putInt(SpeechResultKeys.ROUND, it) }
    }

    fun wake(keyword: String, score: Float, timestampMillis: Long, round: Int?): Bundle = answer {
        putString(SpeechResultKeys.KEYWORD, keyword)
        putFloat(SpeechResultKeys.SCORE, score)
        putLong(SpeechResultKeys.TIMESTAMP_MILLIS, timestampMillis)
        round?.let { putInt(SpeechResultKeys.ROUND, it) }
    }

    fun snapshot(sessionId: String, state: String, modelId: String?, text: String): Bundle = answer {
        putString(SpeechResultKeys.SESSION_ID, sessionId)
        putString(SpeechResultKeys.STATE, state)
        modelId?.let { putString(SpeechResultKeys.MODEL_ID, it) }
        putString(SpeechResultKeys.TEXT, text)
    }

    /**
     * A `recognizeFile` answer; when the segments would push it past `SpeechLimits.MAX_EVENT_BYTES` their texts are
     * left out (the whole text stays) and a warning says so.
     * zh-CN: `recognizeFile` 应答; 语音段会使其超过 `SpeechLimits.MAX_EVENT_BYTES` 时省略各段文本 (保留全文) 并给出警告.
     */
    fun transcript(result: FileTranscript): Bundle {
        fun build(withSegmentText: Boolean, warnings: List<SpeechWarning>) = answer {
            putString(SpeechResultKeys.TEXT, result.text)
            putParcelableArrayList(SpeechResultKeys.SEGMENTS, ArrayList(result.segments.map { segment ->
                Bundle().apply { putUtterance(if (withSegmentText) segment else segment.copy(text = ""), withIndex = false) }
            }))
            result.language?.let { putString(SpeechResultKeys.LANGUAGE, it) }
            putLong(SpeechResultKeys.DURATION_MILLIS, result.durationMillis)
            putString(SpeechResultKeys.MODEL_ID, result.modelId)
            putParcelableArrayList(SpeechResultKeys.WARNINGS, ArrayList(warnings.map { problem(it.code, it.message) }))
        }
        val full = build(true, result.warnings)
        if (parcelSize(full) <= SpeechLimits.MAX_EVENT_BYTES) return full
        val notice = SpeechWarning(SpeechErrorCodes.LIMIT_EXCEEDED, "Segment texts were left out to keep the answer below ${SpeechLimits.MAX_EVENT_BYTES} bytes")
        return build(false, result.warnings + notice)
    }

    fun voiceActivity(result: VoiceActivity): Bundle = answer {
        putParcelableArrayList(SpeechResultKeys.SEGMENTS, ArrayList(result.segments.map { (start, end) ->
            Bundle().apply {
                putLong(SpeechResultKeys.START_MILLIS, start)
                putLong(SpeechResultKeys.END_MILLIS, end)
            }
        }))
        putLong(SpeechResultKeys.DURATION_MILLIS, result.durationMillis)
    }

    // Speakers (contract version 2, roadmap D132 - D134): names, scores, thresholds and counts, never an embedding.

    fun enrollment(result: SpeakerRecognition.Enrollment): Bundle = answer(SpeechContract.CALL_VERSION_2) {
        putString(SpeechResultKeys.NAME, result.voiceprint.name)
        putInt(SpeechResultKeys.SAMPLES, result.voiceprint.samples)
        putLong(SpeechResultKeys.DURATION_MILLIS, result.speechMillis)
        putString(SpeechResultKeys.MODEL_ID, result.voiceprint.modelId)
    }

    /**
     * `verifySpeaker`, or with [identify] `identifySpeaker`: its `name` only when the best score reached the threshold,
     * and `candidates`.
     * zh-CN: `verifySpeaker` 的应答; [identify] 时为 `identifySpeaker` 的应答: 最高分达到阈值时才带 `name`, 并带 `candidates`.
     */
    fun speakerMatch(match: SpeakerMatch, identify: Boolean): Bundle = answer(SpeechContract.CALL_VERSION_2) {
        match.best?.let { best ->
            if (!identify || match.matched) putString(SpeechResultKeys.NAME, best.name)
            putFloat(SpeechResultKeys.SCORE, best.score.toFloat())
        }
        putBoolean(SpeechResultKeys.MATCHED, match.matched)
        putFloat(SpeechResultKeys.THRESHOLD, match.threshold.toFloat())
        putString(SpeechResultKeys.MODEL_ID, match.modelId)
        if (identify) {
            putParcelableArrayList(SpeechResultKeys.CANDIDATES, ArrayList(match.candidates.map { candidate ->
                Bundle().apply {
                    putString(SpeechResultKeys.NAME, candidate.name)
                    putFloat(SpeechResultKeys.SCORE, candidate.score.toFloat())
                }
            }))
        }
    }

    fun speakers(voiceprints: List<Voiceprint>): Bundle = answer(SpeechContract.CALL_VERSION_2) {
        putParcelableArrayList(SpeechResultKeys.SPEAKERS, ArrayList(voiceprints.take(SpeechLimits.MAX_SPEAKERS).map { voiceprint ->
            Bundle().apply {
                putString(SpeechResultKeys.NAME, voiceprint.name)
                putInt(SpeechResultKeys.SAMPLES, voiceprint.samples)
                putLong(SpeechResultKeys.UPDATED_AT, voiceprint.updatedAt)
                putString(SpeechResultKeys.MODEL_ID, voiceprint.modelId)
            }
        }))
    }

    /** A warning or error Bundle (`errorCode`, `errorMessage`). zh-CN: 警告或错误 Bundle (`errorCode`, `errorMessage`). */
    fun problem(code: String, message: String?): Bundle = answer {
        putString(SpeechContract.KEY_ERROR_CODE, code)
        putString(SpeechContract.KEY_ERROR_MESSAGE, bounded(message ?: code))
    }

    fun parcelSize(bundle: Bundle): Int {
        val parcel = Parcel.obtain()
        return try {
            bundle.writeToParcel(parcel, 0)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    private fun Bundle.putUtterance(utterance: Utterance, withIndex: Boolean) {
        putString(SpeechResultKeys.TEXT, utterance.text)
        if (withIndex) putInt(SpeechResultKeys.SEGMENT_INDEX, utterance.segmentIndex)
        putLong(SpeechResultKeys.START_MILLIS, utterance.startMillis)
        putLong(SpeechResultKeys.END_MILLIS, utterance.endMillis)
        utterance.language?.let { putString(SpeechResultKeys.LANGUAGE, it) }
        if (utterance.emotion != null || utterance.event != null) {
            putBundle(SpeechResultKeys.TAGS, Bundle().apply {
                utterance.emotion?.let { putString(SpeechResultKeys.TAG_EMOTION, it) }
                utterance.event?.let { putString(SpeechResultKeys.TAG_EVENT, it) }
            })
        }
    }
}

/**
 * A request Bundle as [OptionSource]: nested Bundles and lists of Bundles are wrapped the same way.
 * zh-CN: 作为 [OptionSource] 的请求 Bundle: 嵌套 Bundle 与 Bundle 列表同样包装.
 */
internal class BundleOptionSource(private val bundle: Bundle) : OptionSource {
    override fun has(key: String): Boolean = bundle.containsKey(key)

    override fun value(key: String): Any? {
        @Suppress("DEPRECATION")
        return when (val value = bundle.get(key)) {
            is Bundle -> BundleOptionSource(value)
            is List<*> -> value.map { item -> if (item is Bundle) BundleOptionSource(item) else item }
            else -> value
        }
    }
}
