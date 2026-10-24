package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import java.io.Closeable

/**
 * The runtime pieces a session drives, kept free of sherpa-onnx types so the session logic runs in JVM tests with
 * fakes; `core/runtime/SherpaEngines` adapts them to `OnlineRecognizer`, `OfflineRecognizer`, `Vad` and
 * `KeywordSpotter` (roadmap 5.4). Every call happens on the thread that owns the session.
 * zh-CN: 会话驱动的运行时部件, 不含 sherpa-onnx 类型, 因此会话逻辑可在 JVM 测试中以假实现运行; `core/runtime/SherpaEngines` 把它们适配到
 * `OnlineRecognizer`, `OfflineRecognizer`, `Vad` 与 `KeywordSpotter` (路线图 5.4). 所有调用都发生在持有会话的线程上.
 */
interface StreamingStream : Closeable {
    /** Appends 16 kHz mono samples. zh-CN: 追加 16 kHz 单声道采样. */
    fun accept(samples: FloatArray)

    /** Decodes every chunk that is ready. zh-CN: 解码所有已就绪的块. */
    fun decode()

    /** The text of the current utterance so far. zh-CN: 当前语句到目前为止的文本. */
    fun text(): String

    /** Starts a new utterance (keeps the stream). zh-CN: 开始新的语句 (保留流). */
    fun reset()
}

/** Text and SenseVoice tags of one recognized segment. zh-CN: 一个已识别语音段的文本与 SenseVoice 标签. */
data class RecognizedText(val text: String, val language: String? = null, val emotion: String? = null, val event: String? = null)

/** An offline recognizer: one speech segment in, its text out. zh-CN: 离线识别器: 输入一个语音段, 输出其文本. */
fun interface SegmentRecognizer {
    fun recognize(samples: FloatArray): RecognizedText
}

/** One speech segment of the VAD, in 16 kHz samples from the start of its input. zh-CN: VAD 的一个语音段, 以输入起点的 16 kHz 采样计. */
class VoiceSegment(val start: Long, val samples: FloatArray) {
    val end: Long get() = start + samples.size
}

/** A silero VAD instance fed window by window. zh-CN: 逐窗口送入的 silero VAD 实例. */
interface VoiceDetector : Closeable {
    /** Samples per window (512 at 16 kHz). zh-CN: 每窗口采样数 (16 kHz 下为 512). */
    val windowSize: Int

    fun accept(window: FloatArray)

    /** Whether speech is going on after the last window. zh-CN: 最后一个窗口之后是否处于语音中. */
    fun isSpeech(): Boolean

    /** The next finished speech segment, or null. zh-CN: 下一个已结束的语音段, 或 null. */
    fun poll(): VoiceSegment?

    /** Ends the input: a pending speech segment becomes available to [poll]. zh-CN: 结束输入: 未结束的语音段可由 [poll] 取出. */
    fun flush()

    /** Forgets the state and any pending segment (roadmap P8.1, the wake gate). zh-CN: 丢弃状态与未取出的语音段 (路线图 P8.1, 唤醒门控). */
    fun reset() = Unit
}

/**
 * A keyword spotting stream with the session's keywords already encoded; [poll] answers the index of the keyword
 * that fired (the stream restarts its search after a hit).
 * zh-CN: 已编码会话关键词的关键词检测流; [poll] 返回命中的关键词序号 (命中后流重新开始搜索).
 */
interface KeywordStream : Closeable {
    fun accept(samples: FloatArray)

    /** Decodes the ready chunks and answers the index of a hit, or null. zh-CN: 解码已就绪的块, 返回命中的关键词序号, 或 null. */
    fun poll(): Int?

    /** Forgets everything heard so far (after a dictation round). zh-CN: 忘记目前听到的一切 (听写轮次之后). */
    fun restart()
}
