package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import java.io.Closeable

/** One finished utterance (`final` event, file segment). zh-CN: 一个已结束的语句 (`final` 事件, 文件语音段). */
data class Utterance(
    val text: String,
    val segmentIndex: Int,
    val startMillis: Long,
    val endMillis: Long,
    val language: String? = null,
    val emotion: String? = null,
    val event: String? = null,
)

/** What a [Dictation] reports while it consumes audio. zh-CN: [Dictation] 消费音频时报告的内容. */
interface DictationSink {
    fun partial(text: String, segmentIndex: Int)
    fun final(utterance: Utterance)
    fun endpoint()
    fun voiceActivity(active: Boolean)
}

/**
 * Turns 16 kHz audio into utterances (roadmap D17). [position] counts the samples consumed, [lastActivity] the sample
 * at which speech or new text was last seen (the session's `timeoutMillis` runs from it).
 * zh-CN: 把 16 kHz 音频转为语句 (路线图 D17). [position] 为已消费的采样数, [lastActivity] 为最近一次出现语音或新文本的采样位置 (会话的
 * `timeoutMillis` 从这里起算).
 */
interface Dictation : Closeable {
    val position: Long
    val lastActivity: Long

    /** Texts of the finished utterances so far. zh-CN: 目前已结束语句的文本. */
    val finals: List<String>

    fun accept(samples: FloatArray)

    /** Ends the input; the pending utterance becomes a `final` when [emitFinal]. zh-CN: 结束输入; [emitFinal] 时未结束的语句成为 `final`. */
    fun flush(emitFinal: Boolean)
}

/** Splits arbitrary chunks into VAD windows. zh-CN: 把任意长度的块切成 VAD 窗口. */
internal class WindowSplitter(private val size: Int) {
    private val pending = FloatArray(size)
    private var filled = 0

    fun split(samples: FloatArray, window: (FloatArray) -> Unit) {
        var offset = 0
        while (offset < samples.size) {
            val count = minOf(size - filled, samples.size - offset)
            System.arraycopy(samples, offset, pending, filled, count)
            filled += count
            offset += count
            if (filled == size) {
                window(pending.copyOf())
                filled = 0
            }
        }
    }

    /** The incomplete last window padded with silence, or null. zh-CN: 以静音补齐的末尾不完整窗口, 或 null. */
    fun remainder(): FloatArray? = if (filled == 0) null else pending.copyOf().also { it.fill(0f, filled, size); filled = 0 }
}

/**
 * Streaming dictation (roadmap D17): an `OnlineRecognizer` stream decodes as audio arrives and reports `partial` per
 * word; the silero VAD closes an utterance after [silenceEndMillis] of silence (or after [MAX_UTTERANCE_MILLIS]), the
 * stream is flushed with a tail of silence, the utterance becomes `final` + `endpoint` when it has text, and the
 * stream starts over. With [gate] (`vad: true`, the default) only speech reaches the recognizer: up to
 * [PREROLL_MILLIS] before the VAD fired, then everything until the utterance closes; without it every sample is
 * decoded. sherpa-onnx's own endpoint rules are off: the session's `silenceEndMillis` cannot change a shared
 * recognizer's configuration.
 * zh-CN: 流式听写 (路线图 D17): `OnlineRecognizer` 流随音频到达而解码, 逐词报告 `partial`; silero VAD 在 [silenceEndMillis] 静音后 (或
 * [MAX_UTTERANCE_MILLIS] 后) 结束一个语句, 以一段静音冲刷流, 有文本的语句成为 `final` + `endpoint`, 然后流重新开始. 启用 [gate] 时
 * (`vad: true`, 默认) 只有语音进入识别器: VAD 触发前至多 [PREROLL_MILLIS] 的音频, 之后直到语句结束的全部音频; 不启用时每个采样都被解码.
 * sherpa-onnx 自身的端点规则关闭: 会话的 `silenceEndMillis` 无法改变共享识别器的配置.
 */
class StreamingDictation(
    private val stream: StreamingStream,
    private val vad: VoiceDetector,
    private val sink: DictationSink,
    private val gate: Boolean,
    private val reportVad: Boolean,
    silenceEndMillis: Long,
    private val punctuation: Boolean,
    private val onClose: () -> Unit = {},
) : Dictation {

    private val silenceEnd = AudioInput.samples(silenceEndMillis)
    private val windows = WindowSplitter(vad.windowSize)
    private val preroll = ArrayDeque<FloatArray>()
    private val prerollWindows = maxOf(1, (AudioInput.samples(PREROLL_MILLIS) / vad.windowSize).toInt())

    override var position = 0L
        private set
    override var lastActivity = 0L
        private set
    override val finals = ArrayList<String>()

    private var speech = false
    private var inUtterance = false
    private var utteranceStart = 0L
    private var lastSpeech = 0L
    private var lastPartial = ""
    private var idle = 0L
    private var segmentIndex = 0

    override fun accept(samples: FloatArray) {
        var fed = 0
        windows.split(samples) { chunk ->
            if (window(chunk)) fed++
            if (inUtterance && (position - lastSpeech >= silenceEnd || position - utteranceStart >= MAX_UTTERANCE_SAMPLES)) {
                // Close at once: a long external chunk may hold the start of the next utterance.
                closeUtterance(emitFinal = true)
                fed = 0
            } else if (fed >= DECODE_WINDOWS) {
                decodePartial()
                fed = 0
            }
            if (!gate && !inUtterance && idle >= IDLE_RESET_SAMPLES) {
                // Without the gate, long silence only grows the stream; start it over.
                stream.reset()
                idle = 0
            }
        }
        if (fed > 0) decodePartial()
    }

    override fun flush(emitFinal: Boolean) {
        windows.remainder()?.let { window(it) }
        if (inUtterance || lastPartial.isNotEmpty()) closeUtterance(emitFinal) else stream.reset()
    }

    override fun close() = onClose()

    /** Handles one VAD window; answers whether audio reached the recognizer. zh-CN: 处理一个 VAD 窗口; 返回音频是否送入识别器. */
    private fun window(window: FloatArray): Boolean {
        vad.accept(window)
        while (vad.poll() != null) Unit
        position += window.size
        val now = vad.isSpeech()
        if (now != speech) {
            speech = now
            if (reportVad) sink.voiceActivity(now)
        }
        if (now) {
            lastSpeech = position
            lastActivity = position
        }
        if (!gate) {
            stream.accept(window)
            if (now && !inUtterance) begin(position - window.size)
            idle = if (now) 0 else idle + window.size
            return true
        }
        if (inUtterance) {
            stream.accept(window)
            return true
        }
        preroll.addLast(window)
        while (preroll.size > prerollWindows) preroll.removeFirst()
        if (!now) return false
        begin(position - preroll.sumOf { it.size })
        while (preroll.isNotEmpty()) stream.accept(preroll.removeFirst())
        return true
    }

    private fun begin(start: Long) {
        inUtterance = true
        utteranceStart = start
        lastSpeech = position
    }

    private fun decodePartial() {
        stream.decode()
        val text = SpeechText.clean(stream.text(), punctuation)
        if (text.isNotEmpty() && text != lastPartial) {
            lastPartial = text
            lastActivity = position
            if (!inUtterance) begin(position)
            sink.partial(text, segmentIndex)
        }
    }

    private fun closeUtterance(emitFinal: Boolean) {
        stream.accept(FloatArray(TAIL_PADDING_SAMPLES))
        stream.decode()
        val text = SpeechText.clean(stream.text(), punctuation)
        stream.reset()
        if (emitFinal && text.isNotEmpty()) {
            val end = if (lastSpeech > utteranceStart) lastSpeech else position
            sink.final(Utterance(text, segmentIndex, AudioInput.millis(utteranceStart), AudioInput.millis(end)))
            sink.endpoint()
            finals += text
            segmentIndex++
        }
        inUtterance = false
        lastPartial = ""
        preroll.clear()
        idle = 0
    }

    companion object {
        /**
         * Audio before the VAD fired that opens an utterance. It must reach back into the silence before the first
         * word: the streaming transducers lose up to two seconds of words when their stream starts inside speech
         * (device measurement of roadmap P3.2: a stream started 448 ms into `en.wav` drops five words, one started at
         * 224 ms none), and the VAD fires 0.3 to 0.5 s after the speech began.
         * zh-CN: 开启语句时 VAD 触发之前的音频. 它必须回溯到首个词之前的静音: 流式 transducer 的流若从语音中间开始, 会丢掉至多两秒的词
         * (路线图 P3.2 的设备实测: 从 `en.wav` 448 ms 处开始的流丢了五个词, 从 224 ms 开始的一个不丢), 而 VAD 在语音开始 0.3 到 0.5 秒后才触发.
         */
        const val PREROLL_MILLIS = 1000L
        const val MAX_UTTERANCE_MILLIS = 20_000L
        private val MAX_UTTERANCE_SAMPLES = AudioInput.samples(MAX_UTTERANCE_MILLIS)
        private val TAIL_PADDING_SAMPLES = AudioInput.samples(400).toInt()
        private val IDLE_RESET_SAMPLES = AudioInput.samples(10_000)

        /** Decode at least every 10 windows (320 ms) inside long chunks, so partials keep coming. zh-CN: 长块中至少每 10 个窗口 (320 ms) 解码一次, 使 partial 持续到达. */
        private const val DECODE_WINDOWS = 10
    }
}

/**
 * Dictation with an offline model (SenseVoice, roadmap D17): the silero VAD cuts speech segments (at most
 * [MAX_SEGMENT_MILLIS] each), every segment is recognized as soon as it ends and the utterance so far is reported as
 * `partial` (segment level, not word level); [silenceEndMillis] of silence after the last segment turns the utterance
 * into `final` + `endpoint`. The `language`, `emotion` and `event` of a final come from its last segment.
 * zh-CN: 离线模型听写 (SenseVoice, 路线图 D17): silero VAD 切出语音段 (每段至多 [MAX_SEGMENT_MILLIS]), 每段一结束即识别, 当前语句以
 * `partial` 报告 (段级而非词级); 最后一段之后静音 [silenceEndMillis] 时语句成为 `final` + `endpoint`. `final` 的 `language`, `emotion` 与
 * `event` 取自其最后一段.
 */
class OfflineDictation(
    private val recognizer: SegmentRecognizer,
    private val vad: VoiceDetector,
    private val sink: DictationSink,
    private val reportVad: Boolean,
    silenceEndMillis: Long,
    private val punctuation: Boolean,
    private val onClose: () -> Unit = {},
) : Dictation {

    private val silenceEnd = AudioInput.samples(silenceEndMillis)
    private val windows = WindowSplitter(vad.windowSize)
    private val parts = ArrayList<Pair<VoiceSegment, RecognizedText>>()

    override var position = 0L
        private set
    override var lastActivity = 0L
        private set
    override val finals = ArrayList<String>()

    private var speech = false
    private var segmentIndex = 0

    override fun accept(samples: FloatArray) = windows.split(samples, ::window)

    override fun flush(emitFinal: Boolean) {
        windows.remainder()?.let(::window)
        vad.flush()
        drain()
        if (parts.isNotEmpty()) closeUtterance(emitFinal)
    }

    override fun close() = onClose()

    private fun window(window: FloatArray) {
        vad.accept(window)
        position += window.size
        val now = vad.isSpeech()
        if (now != speech) {
            speech = now
            if (reportVad) sink.voiceActivity(now)
        }
        if (now) lastActivity = position
        drain()
        val last = parts.lastOrNull()?.first ?: return
        if (!speech && position - last.end >= silenceEnd) closeUtterance(emitFinal = true)
    }

    private fun drain() {
        while (true) {
            val segment = vad.poll() ?: return
            val result = recognizer.recognize(segment.samples)
            val text = SpeechText.clean(result.text, punctuation)
            if (text.isEmpty()) continue
            parts += segment to result.copy(text = text)
            lastActivity = position
            sink.partial(SpeechText.join(parts.map { it.second.text }), segmentIndex)
        }
    }

    private fun closeUtterance(emitFinal: Boolean) {
        val text = SpeechText.join(parts.map { it.second.text })
        if (emitFinal && text.isNotEmpty()) {
            val tags = parts.last().second
            sink.final(
                Utterance(
                    text, segmentIndex, AudioInput.millis(parts.first().first.start), AudioInput.millis(parts.last().first.end),
                    SpeechText.tag(tags.language), SpeechText.tag(tags.emotion), SpeechText.tag(tags.event),
                ),
            )
            sink.endpoint()
            finals += text
            segmentIndex++
        }
        parts.clear()
    }

    companion object {
        const val MAX_SEGMENT_MILLIS = 20_000L
    }
}
