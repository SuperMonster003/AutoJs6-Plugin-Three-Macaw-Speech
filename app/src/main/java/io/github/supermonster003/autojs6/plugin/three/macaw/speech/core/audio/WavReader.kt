package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * The sample layout of a WAV file (roadmap D15): integer PCM of 8 (unsigned), 16, 24 or 32 bits, or IEEE float of 32
 * or 64 bits, any channel count up to [WavReader.MAX_CHANNELS] and any rate within [WavReader.MIN_SAMPLE_RATE] ..
 * [WavReader.MAX_SAMPLE_RATE]. [dataBytes] counts the sample bytes the reader will deliver (the declared data chunk,
 * cut to the stream when the stream is shorter or the size is a streaming placeholder).
 * zh-CN: WAV 文件的采样布局 (路线图 D15): 8 (无符号), 16, 24 或 32 位整数 PCM, 或 32 / 64 位 IEEE 浮点, 声道数与采样率在上下限之内. [dataBytes]
 * 为读取器将交付的采样字节数 (声明的 data 块; 流更短或大小为流式占位值时截到流长).
 */
data class WavFormat(
    val float: Boolean,
    val bitsPerSample: Int,
    val channels: Int,
    val sampleRate: Int,
    val dataBytes: Long,
) {
    val blockAlign: Int get() = channels * bitsPerSample / 8

    val frames: Long get() = dataBytes / blockAlign

    val durationMillis: Long get() = frames * 1000L / sampleRate
}

/**
 * Streaming WAV decoder of roadmap D15 (pure JVM): parses the RIFF header up to the data chunk, refuses anything that
 * is not a supported WAV with `UNSUPPORTED_FORMAT` and inputs beyond `SpeechLimits.MAX_AUDIO_FILE_BYTES` or
 * `MAX_AUDIO_MILLIS` with `LIMIT_EXCEEDED`, then delivers the samples mixed down to mono floats in -1..1 at the
 * source rate. A data chunk cut short by the end of the stream yields the complete frames it holds. Other containers
 * (MP3, AAC, Ogg) go to the system decoders of [MediaAudioReader] instead (roadmap D81).
 * zh-CN: 路线图 D15 的流式 WAV 解码器 (纯 JVM): 解析 RIFF 头直到 data 块, 不受支持的 WAV 以外的一切报 `UNSUPPORTED_FORMAT`, 超过
 * `SpeechLimits.MAX_AUDIO_FILE_BYTES` 或 `MAX_AUDIO_MILLIS` 的输入报 `LIMIT_EXCEEDED`, 然后以源采样率交付混为单声道的 -1..1 浮点采样. 被流
 * 末尾截断的 data 块交付其中完整的帧. 其他容器 (MP3, AAC, Ogg) 改由 [MediaAudioReader] 的系统解码器读取 (路线图 D81).
 */
class WavReader(input: InputStream, streamLength: Long? = null) : MonoAudio {

    private val input = input.buffered(BUFFER_BYTES)
    private var remaining: Long

    val format: WavFormat

    init {
        if (streamLength != null && streamLength > SpeechLimits.MAX_AUDIO_FILE_BYTES) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio file has $streamLength bytes, more than ${SpeechLimits.MAX_AUDIO_FILE_BYTES}")
        }
        format = parse(streamLength)
        remaining = format.dataBytes
        if (format.durationMillis > SpeechLimits.MAX_AUDIO_MILLIS) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio lasts ${format.durationMillis} ms, more than ${SpeechLimits.MAX_AUDIO_MILLIS}")
        }
    }

    override val sampleRate: Int get() = format.sampleRate

    override val durationMillis: Long get() = format.durationMillis

    /**
     * Reads up to [maxFrames] frames as mono samples; an empty array at the end of the data.
     * zh-CN: 读取至多 [maxFrames] 帧单声道采样; 数据结束时返回空数组.
     */
    override fun read(maxFrames: Int): FloatArray {
        require(maxFrames > 0)
        val block = format.blockAlign
        val frames = minOf(maxFrames.toLong(), remaining / block).toInt()
        if (frames == 0) return FloatArray(0)
        val bytes = ByteArray(frames * block)
        var filled = 0
        while (filled < bytes.size) {
            val count = input.read(bytes, filled, bytes.size - filled)
            if (count < 0) break
            filled += count
        }
        val complete = filled / block
        remaining = if (filled < bytes.size) 0 else remaining - bytes.size
        return decode(bytes, complete)
    }

    override fun close() = input.close()

    private fun decode(bytes: ByteArray, frames: Int): FloatArray {
        val channels = format.channels
        val width = format.bitsPerSample / 8
        val out = FloatArray(frames)
        var offset = 0
        for (frame in 0 until frames) {
            var sum = 0f
            for (channel in 0 until channels) {
                sum += sample(bytes, offset)
                offset += width
            }
            out[frame] = (sum / channels).coerceIn(-1f, 1f)
        }
        return out
    }

    private fun sample(b: ByteArray, o: Int): Float = when {
        format.float && format.bitsPerSample == 32 -> finite(Float.fromBits(int32(b, o)))
        format.float -> finite((Double.fromBits((int32(b, o).toLong() and 0xffffffffL) or (int32(b, o + 4).toLong() shl 32))).toFloat())
        format.bitsPerSample == 8 -> ((b[o].toInt() and 0xff) - 128) / 128f
        format.bitsPerSample == 16 -> ((b[o].toInt() and 0xff) or (b[o + 1].toInt() shl 8)) / 32768f
        format.bitsPerSample == 24 -> ((b[o].toInt() and 0xff) or ((b[o + 1].toInt() and 0xff) shl 8) or (b[o + 2].toInt() shl 16)) / 8388608f
        else -> int32(b, o) / 2147483648f
    }

    private fun parse(streamLength: Long?): WavFormat {
        val riff = readHeader(12) ?: unsupported("The input is not a WAV file (too short)")
        if (ascii(riff, 0) != "RIFF" || ascii(riff, 8) != "WAVE") unsupported("The input is not a RIFF / WAVE file")
        var position = 12L
        var fmt: WavFormat? = null
        while (true) {
            val chunk = readHeader(8) ?: unsupported("The WAV file has no data chunk")
            position += 8
            val id = ascii(chunk, 0)
            val size = int32(chunk, 4).toLong() and 0xffffffffL
            when (id) {
                "fmt " -> {
                    if (size < 16 || size > MAX_FMT_BYTES) unsupported("The WAV fmt chunk has an invalid size $size")
                    val body = readHeader(size.toInt()) ?: unsupported("The WAV fmt chunk is truncated")
                    fmt = format(body)
                    if (size % 2 == 1L) skip(1)
                    position += size + size % 2
                }
                "data" -> {
                    val layout = fmt ?: unsupported("The WAV data chunk precedes the fmt chunk")
                    val available = streamLength?.let { (it - position).coerceAtLeast(0) }
                    val declared = if (size == 0L || size == 0xffffffffL) available ?: size else size
                    val bytes = if (available != null) minOf(declared, available) else declared
                    return layout.copy(dataBytes = bytes - bytes % layout.blockAlign)
                }
                else -> {
                    if (streamLength != null && position + size > streamLength) unsupported("The WAV chunk '$id' is truncated")
                    skip(size + size % 2)
                    position += size + size % 2
                }
            }
        }
    }

    private fun format(body: ByteArray): WavFormat {
        var tag = int16(body, 0)
        val channels = int16(body, 2)
        val sampleRate = int32(body, 4)
        val blockAlign = int16(body, 12)
        val bits = int16(body, 14)
        if (tag == WAVE_FORMAT_EXTENSIBLE) {
            if (body.size < 40) unsupported("The WAVE_FORMAT_EXTENSIBLE header is truncated")
            tag = int16(body, 24)
        }
        val float = when (tag) {
            WAVE_FORMAT_PCM -> false
            WAVE_FORMAT_IEEE_FLOAT -> true
            else -> unsupported("WAV format tag 0x${Integer.toHexString(tag)} is not PCM or IEEE float")
        }
        if (float && bits != 32 && bits != 64) unsupported("Float WAV samples have $bits bits, not 32 or 64")
        if (!float && bits !in setOf(8, 16, 24, 32)) unsupported("PCM WAV samples have $bits bits, not 8, 16, 24 or 32")
        if (channels !in 1..MAX_CHANNELS) unsupported("The WAV file has $channels channels")
        if (sampleRate !in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE) unsupported("The WAV sample rate $sampleRate Hz is out of range")
        if (blockAlign != channels * bits / 8) unsupported("The WAV block alignment $blockAlign does not match $channels x $bits bits")
        return WavFormat(float, bits, channels, sampleRate, 0)
    }

    private fun readHeader(count: Int): ByteArray? {
        val bytes = ByteArray(count)
        var filled = 0
        while (filled < count) {
            val read = input.read(bytes, filled, count - filled)
            if (read < 0) return null
            filled += read
        }
        return bytes
    }

    private fun skip(count: Long) {
        var left = count
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) {
                if (input.read() < 0) throw SpeechException(SpeechErrorCodes.UNSUPPORTED_FORMAT, "The WAV header is truncated", EOFException())
                left--
            } else {
                left -= skipped
            }
        }
    }

    companion object {
        const val MAX_CHANNELS = 8
        const val MIN_SAMPLE_RATE = 4_000
        const val MAX_SAMPLE_RATE = 384_000

        private const val WAVE_FORMAT_PCM = 1
        private const val WAVE_FORMAT_IEEE_FLOAT = 3
        private const val WAVE_FORMAT_EXTENSIBLE = 0xFFFE
        private const val MAX_FMT_BYTES = 4096L
        private const val BUFFER_BYTES = 64 * 1024

        private fun ascii(bytes: ByteArray, offset: Int) = String(bytes, offset, 4, Charsets.ISO_8859_1)

        private fun int16(b: ByteArray, o: Int) = (b[o].toInt() and 0xff) or ((b[o + 1].toInt() and 0xff) shl 8)

        private fun int32(b: ByteArray, o: Int) =
            (b[o].toInt() and 0xff) or ((b[o + 1].toInt() and 0xff) shl 8) or ((b[o + 2].toInt() and 0xff) shl 16) or (b[o + 3].toInt() shl 24)

        private fun finite(value: Float) = if (value.isNaN()) 0f else value

        private fun unsupported(message: String): Nothing = throw SpeechException(SpeechErrorCodes.UNSUPPORTED_FORMAT, message, IOException(message))
    }
}
