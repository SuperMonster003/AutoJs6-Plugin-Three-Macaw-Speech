package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MonoAudio

/** A non-fatal condition reported with a result (`warnings`). zh-CN: 随结果报告的非致命情况 (`warnings`). */
data class SpeechWarning(val code: String, val message: String)

/** The answer of `recognizeFile` (roadmap appendix B.3). zh-CN: `recognizeFile` 的应答 (附录 B.3). */
data class FileTranscript(
    val text: String,
    val segments: List<Utterance>,
    val language: String?,
    val durationMillis: Long,
    val modelId: String,
    val warnings: List<SpeechWarning>,
)

/**
 * Progress of [FileRecognition.recognize] for the file tab of the tryout screen (roadmap D112), called on the
 * recognizing thread. Scripts, MCP and the system entry pass [NONE], so their results stay as they were.
 * zh-CN: 试听试说页文件标签所用的 [FileRecognition.recognize] 进度 (路线图 D112), 在识别线程上调用. 脚本, MCP 与系统入口传 [NONE],
 * 结果因此不变.
 */
interface FileRecognitionListener {

    /** Decoding starts; [durationMillis] is what the file declares, null when unknown. zh-CN: 开始解码; [durationMillis] 为文件声明的时长, 未知时为 null. */
    fun opened(durationMillis: Long?) = Unit

    /** [millis] of audio have gone to the recognizer so far. zh-CN: 已送入识别的音频共 [millis] 毫秒. */
    fun progress(millis: Long) = Unit

    /** A segment is final, in the order of the result's `segments`. zh-CN: 一个语音段已最终确定, 顺序与结果的 `segments` 相同. */
    fun segment(utterance: Utterance) = Unit

    companion object {
        val NONE = object : FileRecognitionListener {}
    }
}

/** The answer of `detectVoice`. zh-CN: `detectVoice` 的应答. */
data class VoiceActivity(val segments: List<Pair<Long, Long>>, val durationMillis: Long)

/**
 * File recognition and voice activity detection (roadmap P3.3, D15, D83): the file (WAV or a container of D81) is
 * decoded and resampled to 16 kHz in chunks and streamed through a [Dictation] (streaming models consume it as a
 * session would; offline models get VAD segments), so memory stays bounded for 30 minute files. Every final becomes a
 * segment with its start and end in milliseconds; the result's language is the one most segments report (SenseVoice),
 * else the request's hint. Pure JVM.
 * zh-CN: 文件识别与语音活动检测 (路线图 P3.3, D15, D83): 文件 (WAV 或 D81 的容器) 按块解码并重采样到 16 kHz, 流经一个 [Dictation] (流式模型如同会话一样消费; 离线模型得到
 * VAD 语音段), 因此 30 分钟的文件内存占用也有上限. 每个 final 成为一个带起止毫秒的语音段; 结果的语言取多数语音段报告的语言 (SenseVoice), 否则取
 * 请求的提示. 纯 JVM.
 */
object FileRecognition {

    fun recognize(
        reader: MonoAudio,
        modelId: String,
        languageHint: String?,
        warnings: List<SpeechWarning>,
        cancelled: () -> Boolean = { false },
        listener: FileRecognitionListener = FileRecognitionListener.NONE,
        dictation: (DictationSink) -> Dictation,
    ): FileTranscript {
        val segments = ArrayList<Utterance>()
        val sink = object : DictationSink {
            override fun partial(text: String, segmentIndex: Int) = Unit
            override fun final(utterance: Utterance) {
                segments += utterance
                listener.segment(utterance)
            }
            override fun endpoint() = Unit
            override fun voiceActivity(active: Boolean) = Unit
        }
        val samples = dictation(sink).use { pipeline ->
            listener.opened(reader.durationMillis)
            var fed = 0L
            val total = AudioInput.decode(reader, cancelled = cancelled) { chunk ->
                pipeline.accept(chunk)
                fed += chunk.size
                listener.progress(AudioInput.millis(fed))
            }
            pipeline.flush(emitFinal = true)
            total
        }
        val language = segments.mapNotNull { it.language }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: languageHint
        return FileTranscript(
            text = SpeechText.join(segments.map { it.text }),
            segments = segments,
            language = language,
            durationMillis = AudioInput.millis(samples),
            modelId = modelId,
            warnings = warnings,
        )
    }

    fun detectVoice(reader: MonoAudio, vad: VoiceDetector, cancelled: () -> Boolean = { false }): VoiceActivity {
        val segments = ArrayList<Pair<Long, Long>>()
        val windows = WindowSplitter(vad.windowSize)
        fun drain() {
            while (true) {
                val segment = vad.poll() ?: return
                segments += AudioInput.millis(segment.start) to AudioInput.millis(segment.end)
            }
        }
        val samples = AudioInput.decode(reader, cancelled = cancelled) { chunk ->
            windows.split(chunk) { window ->
                vad.accept(window)
                drain()
            }
        }
        // The trailing partial window is dropped, as in the upstream examples (SherpaRuntime.detectSegments).
        vad.flush()
        drain()
        return VoiceActivity(segments, AudioInput.millis(samples))
    }
}

/**
 * A [VoiceDetector] for offline models without VAD (`vad: false`): fixed segments of [segmentSamples], every one
 * closed at once (no speech is ever "going on"), so each becomes its own utterance.
 * zh-CN: 不使用 VAD 的离线模型所用的 [VoiceDetector] (`vad: false`): 固定长度 [segmentSamples] 的语音段, 每段立即结束 (从不处于语音中), 因此
 * 每段各成一个语句.
 */
class FixedSegments(override val windowSize: Int = 512, private val segmentSamples: Int = AudioInput.samples(OfflineDictation.MAX_SEGMENT_MILLIS).toInt()) : VoiceDetector {
    private var buffer = FloatArray(segmentSamples)
    private var filled = 0
    private var start = 0L
    private val ready = ArrayDeque<VoiceSegment>()

    override fun accept(window: FloatArray) {
        var offset = 0
        while (offset < window.size) {
            val count = minOf(segmentSamples - filled, window.size - offset)
            System.arraycopy(window, offset, buffer, filled, count)
            filled += count
            offset += count
            if (filled == segmentSamples) emit()
        }
    }

    override fun isSpeech(): Boolean = false

    override fun poll(): VoiceSegment? = ready.removeFirstOrNull()

    override fun flush() {
        if (filled > 0) emit()
    }

    override fun close() = Unit

    private fun emit() {
        ready.addLast(VoiceSegment(start, buffer.copyOf(filled)))
        start += filled
        filled = 0
        buffer = FloatArray(segmentSamples)
    }
}
