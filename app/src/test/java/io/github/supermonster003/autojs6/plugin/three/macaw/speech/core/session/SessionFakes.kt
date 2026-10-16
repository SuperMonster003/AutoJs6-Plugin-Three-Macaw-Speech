package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import kotlin.math.abs

/**
 * Deterministic stand-ins for the sherpa-onnx pieces (roadmap P3 JVM tests): loud samples (|x| >= 0.1) are speech.
 * zh-CN: sherpa-onnx 部件的确定性替身 (路线图 P3 的 JVM 测试): 响亮的采样 (|x| >= 0.1) 即语音.
 */
internal object Audio {
    fun speech(millis: Long) = FloatArray(AudioInput.samples(millis).toInt()) { 0.5f }
    fun silence(millis: Long) = FloatArray(AudioInput.samples(millis).toInt())
}

/** A VAD without hysteresis: a window is speech when loud; a segment ends at the first quiet window. zh-CN: 无滞后的 VAD. */
internal class FakeVad(override val windowSize: Int = 512) : VoiceDetector {
    var closed = false
    private var position = 0L
    private var speech = false
    private var start = 0L
    private val current = ArrayList<Float>()
    private val ready = ArrayDeque<VoiceSegment>()

    override fun accept(window: FloatArray) {
        require(window.size == windowSize)
        val loud = window.any { abs(it) >= 0.1f }
        if (loud) {
            if (!speech) start = position
            window.forEach { current += it }
        } else if (speech) {
            emit()
        }
        speech = loud
        position += window.size
    }

    override fun isSpeech() = speech

    override fun poll(): VoiceSegment? = ready.removeFirstOrNull()

    override fun flush() {
        if (speech) emit()
        speech = false
    }

    override fun close() {
        closed = true
    }

    private fun emit() {
        ready.addLast(VoiceSegment(start, current.toFloatArray()))
        current.clear()
    }
}

/** A streaming stream that writes one word `w` per 100 ms of loud audio it decoded. zh-CN: 每解码 100 ms 响亮音频写出一个词 `w` 的流式流. */
internal class FakeStream : StreamingStream {
    var fed = 0L
    var firstFed: Long? = null
    var resets = 0
    var closed = false
    var failOnDecode = false
    private var pending = 0L
    private var loud = 0L
    private var decodedLoud = 0L

    override fun accept(samples: FloatArray) {
        if (firstFed == null) firstFed = fed
        fed += samples.size
        loud += samples.count { abs(it) >= 0.1f }
        pending += samples.size
    }

    override fun decode() {
        if (failOnDecode) throw IllegalStateException("decoder exploded")
        decodedLoud = loud
        pending = 0
    }

    override fun text(): String = List((decodedLoud / AudioInput.samples(100)).toInt()) { "w" }.joinToString(" ")

    override fun reset() {
        resets++
        loud = 0
        decodedLoud = 0
    }

    override fun close() {
        closed = true
    }
}

/** An offline recognizer that names a segment by its length in 100 ms units. zh-CN: 以 100 ms 为单位按长度命名语音段的离线识别器. */
internal class FakeRecognizer : SegmentRecognizer {
    val calls = ArrayList<Int>()
    override fun recognize(samples: FloatArray): RecognizedText {
        calls += samples.size
        return RecognizedText("s${samples.size / AudioInput.samples(100)}.", "<|zh|>", "<|NEUTRAL|>", "<|Speech|>")
    }
}

/** A keyword stream that fires the scheduled keyword index once the given sample position is reached. zh-CN: 到达指定采样位置时命中预定关键词. */
internal class FakeKeywords(private val hits: MutableList<Pair<Long, Int>>) : KeywordStream {
    var position = 0L
    var restarts = 0
    var closed = false
    private val fired = ArrayDeque<Int>()

    override fun accept(samples: FloatArray) {
        position += samples.size
        while (hits.isNotEmpty() && hits.first().first <= position) fired.addLast(hits.removeAt(0).second)
    }

    override fun poll(): Int? = fired.removeFirstOrNull()

    override fun restart() {
        restarts++
        fired.clear()
    }

    override fun close() {
        closed = true
    }
}

/** Records events as short strings. zh-CN: 以短字符串记录事件. */
internal class RecordingEvents : DictationEvents, WakeEvents {
    val log = ArrayList<String>()
    val finals = ArrayList<Utterance>()

    override fun ready(sessionId: String, modelId: String, round: Int?) {
        log += "ready:$modelId" + (round?.let { ":r$it" } ?: "")
    }

    override fun ready(sessionId: String, modelId: String) {
        log += "wake-ready:$modelId"
    }

    override fun partial(text: String, segmentIndex: Int) {
        log += "partial:$segmentIndex:$text"
    }

    override fun final(utterance: Utterance) {
        finals += utterance
        log += "final:${utterance.segmentIndex}:${utterance.text}"
    }

    override fun endpoint() {
        log += "endpoint"
    }

    override fun voiceActivity(active: Boolean) {
        log += "vad:$active"
    }

    override fun warning(code: String, message: String) {
        log += "warning:$code"
    }

    override fun error(code: String, message: String) {
        log += "error:$code"
    }

    override fun end(reason: String, text: String, round: Int?) {
        log += "end:$reason:$text" + (round?.let { ":r$it" } ?: "")
    }

    override fun wake(keyword: String, score: Float, timestampMillis: Long, round: Int?) {
        log += "wake:$keyword:$score" + (round?.let { ":r$it" } ?: "")
    }

    override fun end(reason: String) {
        log += "wake-end:$reason"
    }

    /** The log without the repeated partials, which depend on chunking. zh-CN: 去掉依赖分块方式的重复 partial 后的日志. */
    fun milestones(): List<String> = log.filterNot { it.startsWith("partial:") }
}
