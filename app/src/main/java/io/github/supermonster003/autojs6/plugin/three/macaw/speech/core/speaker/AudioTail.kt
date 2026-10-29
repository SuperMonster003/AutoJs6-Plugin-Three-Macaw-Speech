package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker

/**
 * The last [capacity] samples of a recording (roadmap D139): the resident wake word writes every microphone frame here
 * on the ASR thread, and a wake word hit reads the audio before it for the speaker check. Thread-safe, pure JVM.
 * zh-CN: 一段录音的最后 [capacity] 个采样 (路线图 D139): 常驻唤醒在 ASR 线程上把每个麦克风帧写入这里, 唤醒词命中时读取其之前的音频用于说话人检查.
 * 线程安全, 纯 JVM.
 */
class AudioTail(val capacity: Int) {
    private val ring = FloatArray(capacity)
    private var next = 0
    private var size = 0

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    @Synchronized
    fun write(samples: FloatArray) {
        val start = maxOf(0, samples.size - capacity)
        for (i in start until samples.size) {
            ring[next] = samples[i]
            next = (next + 1) % capacity
        }
        size = minOf(capacity, size + samples.size - start)
    }

    /** The newest [count] samples (fewer when less was written), oldest first. zh-CN: 最新的 [count] 个采样 (写入不足时更少), 按时间顺序. */
    @Synchronized
    fun last(count: Int): FloatArray {
        val n = minOf(count, size)
        val result = FloatArray(n)
        val first = (next - n + capacity) % capacity
        for (i in 0 until n) result[i] = ring[(first + i) % capacity]
        return result
    }

    @Synchronized
    fun clear() {
        next = 0
        size = 0
    }
}
