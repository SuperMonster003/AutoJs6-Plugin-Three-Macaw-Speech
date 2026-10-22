package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

/**
 * The internal audio format of roadmap D15: 16 kHz mono float in -1..1, and its conversions. Pure JVM.
 * zh-CN: 路线图 D15 的内部音频格式: 16 kHz 单声道 -1..1 浮点, 以及与之相关的换算. 纯 JVM.
 */
object AudioInput {

    const val SAMPLE_RATE = 16_000

    /** Samples to milliseconds at 16 kHz. zh-CN: 16 kHz 下采样数换算为毫秒. */
    fun millis(samples: Long): Long = samples * 1000L / SAMPLE_RATE

    /** Milliseconds to samples at 16 kHz. zh-CN: 16 kHz 下毫秒换算为采样数. */
    fun samples(millis: Long): Long = millis * SAMPLE_RATE / 1000L

    /**
     * 16-bit little-endian PCM (an external session's `feed`, an even byte count) to floats.
     * zh-CN: 16 位小端 PCM (外部会话的 `feed`, 字节数为偶数) 转为浮点.
     */
    fun pcm16(bytes: ByteArray): FloatArray = FloatArray(bytes.size / 2) { index ->
        ((bytes[2 * index].toInt() and 0xff) or (bytes[2 * index + 1].toInt() shl 8)) / 32768f
    }

    /** Samples of an `AudioRecord` read to floats. zh-CN: `AudioRecord` 读出的采样转为浮点. */
    fun pcm16(samples: ShortArray, count: Int): FloatArray = FloatArray(count) { index -> samples[index] / 32768f }

    /**
     * Decodes a whole audio file (WAV or any container of roadmap D81) into 16 kHz mono chunks of about [chunkFrames]
     * samples for [consumer], resampling when the file has another rate; [cancelled] is polled between chunks. Returns
     * the 16 kHz sample count.
     * zh-CN: 把整个音频文件 (WAV 或路线图 D81 的任意容器) 解码为约 [chunkFrames] 个采样的 16 kHz 单声道块交给 [consumer], 采样率不同时重采样;
     * 每块之间检查 [cancelled]. 返回 16 kHz 采样数.
     */
    fun decode(reader: MonoAudio, chunkFrames: Int = 8000, cancelled: () -> Boolean = { false }, consumer: (FloatArray) -> Unit): Long {
        val resampler = Resampler(reader.sampleRate, SAMPLE_RATE)
        val sourceFrames = maxOf(1, (chunkFrames.toLong() * reader.sampleRate / SAMPLE_RATE).toInt())
        var total = 0L
        while (!cancelled()) {
            val chunk = reader.read(sourceFrames)
            if (chunk.isEmpty()) break
            val out = resampler.process(chunk)
            if (out.isNotEmpty()) {
                total += out.size
                consumer(out)
            }
        }
        if (cancelled()) return total
        val tail = resampler.flush()
        if (tail.isNotEmpty()) {
            total += tail.size
            consumer(tail)
        }
        return total
    }
}
