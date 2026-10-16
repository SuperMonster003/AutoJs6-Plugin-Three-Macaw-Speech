package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.roundToInt

/**
 * PCM16 encoding of the synthesized float samples (-1..1, clamped), little endian. zh-CN: 合成浮点采样 (-1..1, 截断) 的 PCM16 小端编码.
 */
object Pcm16 {
    fun sample(value: Float): Short = (value.coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()

    /** Writes [count] samples of [samples] from [from] into [target] at [offset]; answers the bytes written. zh-CN: 写入并返回字节数. */
    fun encode(samples: FloatArray, from: Int, count: Int, target: ByteArray, offset: Int = 0): Int {
        var position = offset
        for (index in from until from + count) {
            val value = sample(samples[index]).toInt()
            target[position++] = value.toByte()
            target[position++] = (value shr 8).toByte()
        }
        return count * 2
    }
}

/**
 * Writes a mono PCM16 WAV file at the model's [sampleRate] into [channel] (roadmap D20): a canonical 44 byte header
 * whose sizes are patched by [finish], then the samples as they are synthesized. A file that would grow past
 * [maxBytes] (header included) fails with `LIMIT_EXCEEDED` before the samples that cross it are written. The channel
 * belongs to the caller. Pure JVM.
 * zh-CN: 以模型的 [sampleRate] 向 [channel] 写出单声道 PCM16 WAV 文件 (路线图 D20): 先写 44 字节的标准文件头, 其大小字段由 [finish] 回填, 之后按
 * 合成进度写入采样. 文件 (含文件头) 将超过 [maxBytes] 时, 在写入越界的采样之前以 `LIMIT_EXCEEDED` 失败. 通道归调用方所有. 纯 JVM.
 */
class WavWriter(
    private val channel: FileChannel,
    val sampleRate: Int,
    private val maxBytes: Long = SpeechLimits.MAX_SYNTH_OUTPUT_BYTES,
) : Closeable {

    private val buffer = ByteArray(BUFFER_SAMPLES * 2)

    /** Bytes of PCM data written so far. zh-CN: 已写入的 PCM 数据字节数. */
    var dataBytes: Long = 0
        private set

    val sampleCount: Long get() = dataBytes / 2

    val durationMillis: Long get() = sampleCount * 1000 / sampleRate

    /** Total size of the file so far. zh-CN: 当前文件总大小. */
    val sizeBytes: Long get() = HEADER_BYTES + dataBytes

    init {
        require(sampleRate in 1000..384_000) { "Unsupported sample rate $sampleRate" }
        channel.truncate(0)
        channel.position(0)
        writeFully(header(0))
    }

    fun write(samples: FloatArray, from: Int = 0, count: Int = samples.size - from) {
        require(from >= 0 && count >= 0 && from + count <= samples.size) { "Invalid sample range" }
        if (sizeBytes + count * 2L > maxBytes) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The synthesized audio exceeds $maxBytes bytes; synthesize less text at a time")
        }
        var offset = from
        var left = count
        while (left > 0) {
            val n = minOf(left, BUFFER_SAMPLES)
            val bytes = Pcm16.encode(samples, offset, n, buffer)
            writeFully(ByteBuffer.wrap(buffer, 0, bytes))
            dataBytes += bytes
            offset += n
            left -= n
        }
    }

    /** Patches the RIFF and data sizes; answers the file size. zh-CN: 回填 RIFF 与 data 大小; 返回文件大小. */
    fun finish(): Long {
        val end = channel.position()
        channel.position(0)
        writeFully(header(dataBytes))
        channel.position(end)
        channel.force(false)
        return sizeBytes
    }

    override fun close() = Unit

    private fun header(data: Long): ByteBuffer = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
        put(RIFF).putInt((36 + data).toInt()).put(WAVE)
        put(FMT).putInt(16).putShort(1).putShort(1).putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        put(DATA).putInt(data.toInt())
        flip()
    }

    private fun writeFully(bytes: ByteBuffer) {
        while (bytes.hasRemaining()) channel.write(bytes)
    }

    companion object {
        const val HEADER_BYTES = 44
        private const val BUFFER_SAMPLES = 8192
        private val RIFF = "RIFF".toByteArray(Charsets.US_ASCII)
        private val WAVE = "WAVE".toByteArray(Charsets.US_ASCII)
        private val FMT = "fmt ".toByteArray(Charsets.US_ASCII)
        private val DATA = "data".toByteArray(Charsets.US_ASCII)
    }
}
