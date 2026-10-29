package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

/**
 * A slice of 16 kHz samples already in memory, read as a [MonoAudio] (roadmap D153): "diarize and transcribe" of the
 * file tab decodes the file once and recognizes each speaker turn from its slice, without copying the whole audio.
 * zh-CN: 已在内存中的 16 kHz 采样的一段, 作为 [MonoAudio] 读取 (路线图 D153): 文件标签的 "分离 + 转写" 只解码一次文件, 每个说话人轮次从自己的那一段
 * 识别, 不复制整段音频.
 */
class PcmAudio(private val samples: FloatArray, private val from: Int = 0, private val to: Int = samples.size) : MonoAudio {
    private var position = from

    init {
        require(from in 0..to && to <= samples.size) { "Bad slice $from..$to of ${samples.size} samples" }
    }

    override val sampleRate: Int get() = AudioInput.SAMPLE_RATE

    override val durationMillis: Long get() = AudioInput.millis((to - from).toLong())

    override fun read(maxFrames: Int): FloatArray {
        val count = minOf(maxFrames, to - position)
        if (count <= 0) return FloatArray(0)
        return samples.copyOfRange(position, position + count).also { position += count }
    }

    override fun close() = Unit
}
