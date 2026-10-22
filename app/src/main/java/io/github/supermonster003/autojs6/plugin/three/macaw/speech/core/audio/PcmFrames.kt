package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The PCM encodings a system decoder may output (the values of `android.media.AudioFormat`, copied so the JVM tests
 * need no Android stubs; roadmap D85).
 * zh-CN: 系统解码器可能输出的 PCM 编码 (`android.media.AudioFormat` 的取值, 复制于此使 JVM 测试不依赖 Android 桩; 路线图 D85).
 */
object PcmEncoding {
    const val PCM_16BIT = 2
    const val PCM_8BIT = 3
    const val PCM_FLOAT = 4
    const val PCM_24BIT_PACKED = 21
    const val PCM_32BIT = 22

    /** Bytes of one sample, or null for an encoding the plugin cannot read. zh-CN: 单个采样的字节数; 插件不能读取的编码为 null. */
    fun bytes(encoding: Int): Int? = when (encoding) {
        PCM_8BIT -> 1
        PCM_16BIT -> 2
        PCM_24BIT_PACKED -> 3
        PCM_32BIT, PCM_FLOAT -> 4
        else -> null
    }
}

/**
 * Interleaved decoder output to mono floats in -1..1 (roadmap D85). Pure JVM.
 * zh-CN: 交错排列的解码器输出转为 -1..1 的单声道浮点 (路线图 D85). 纯 JVM.
 */
object PcmFrames {

    /** The most channels the plugin mixes (as for WAV). zh-CN: 插件混合的最多声道数 (与 WAV 相同). */
    const val MAX_CHANNELS = WavReader.MAX_CHANNELS

    /**
     * Mixes the complete frames between [buffer]'s position and limit down to mono; a trailing partial frame is
     * ignored. The buffer's position is left at its limit.
     * zh-CN: 把 [buffer] 的 position 与 limit 之间的完整帧混为单声道; 末尾不完整的帧被忽略. 结束后 position 位于 limit.
     */
    fun mono(buffer: ByteBuffer, encoding: Int, channels: Int): FloatArray {
        val width = requireNotNull(PcmEncoding.bytes(encoding)) { "PCM encoding $encoding" }
        require(channels in 1..MAX_CHANNELS) { "$channels channels" }
        val data = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        val frames = data.remaining() / (width * channels)
        val out = FloatArray(frames)
        for (frame in 0 until frames) {
            var sum = 0f
            for (channel in 0 until channels) sum += sample(data, encoding)
            out[frame] = (sum / channels).coerceIn(-1f, 1f)
        }
        buffer.position(buffer.limit())
        return out
    }

    private fun sample(data: ByteBuffer, encoding: Int): Float = when (encoding) {
        PcmEncoding.PCM_8BIT -> ((data.get().toInt() and 0xff) - 128) / 128f
        PcmEncoding.PCM_16BIT -> data.short / 32768f
        PcmEncoding.PCM_24BIT_PACKED -> {
            val low = data.get().toInt() and 0xff
            val middle = data.get().toInt() and 0xff
            val high = data.get().toInt()
            ((high shl 16) or (middle shl 8) or low) / 8388608f
        }
        PcmEncoding.PCM_32BIT -> data.int / 2147483648f
        else -> data.float.let { if (it.isNaN()) 0f else it }
    }
}
