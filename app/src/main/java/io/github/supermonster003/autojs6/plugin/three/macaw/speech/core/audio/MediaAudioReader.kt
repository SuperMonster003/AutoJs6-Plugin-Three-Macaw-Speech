package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.ParcelFileDescriptor
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Decodes the non-WAV audio files of roadmap D81 - D85 with the system: `MediaExtractor` reads the container (an Ogg
 * file the system extractor refuses goes to the plugin's own [OggOpusStream] instead, roadmap Q25), the first audio
 * track is decoded by `MediaCodec` (an `audio/raw` track is read as it is), and every output buffer is mixed down to
 * mono as soon as it arrives, so memory stays at a few buffers (D83). Video tracks are ignored (D82). [open] decodes
 * until the first samples, which fixes [sampleRate]; a file that yields none is refused there. Errors follow D85.
 * zh-CN: 用系统解码路线图 D81 - D85 的非 WAV 音频文件: `MediaExtractor` 读取容器 (系统提取器拒绝的 Ogg 文件改用插件自带的 [OggOpusStream],
 * 路线图 Q25), 第一条音轨由 `MediaCodec` 解码 (`audio/raw` 音轨直接读取), 每个输出缓冲一到即混为单声道, 内存只占几个缓冲 (D83). 视频轨被忽略
 * (D82). [open] 解码到出现第一批采样为止, 由此确定 [sampleRate]; 解不出任何采样的文件在此被拒. 错误按 D85.
 */
class MediaAudioReader private constructor(
    private val packets: Packets,
    private val codec: MediaCodec?,
    private val container: AudioContainer,
) : MonoAudio {

    /** Whether the plugin's own Ogg parser reads this file. zh-CN: 是否由插件自带的 Ogg 解析读取此文件. */
    val ownOggParser: Boolean get() = packets is OggPackets

    private val info = MediaCodec.BufferInfo()
    private val pending = ArrayDeque<FloatArray>()
    private var pendingOffset = 0
    private var rate = 0
    private var channels = 0
    private var encoding = PcmEncoding.PCM_16BIT
    private var formatKnown = false
    private var inputDone = false
    private var outputDone = false
    private var stalled = 0
    private var guard: DecodeRules.DurationGuard? = null
    private val raw: ByteBuffer? = if (codec == null) ByteBuffer.allocateDirect(packets.maxSampleBytes) else null

    override val sampleRate: Int get() = rate

    override val durationMillis: Long? =
        if (packets.format.containsKey(MediaFormat.KEY_DURATION)) packets.format.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 }?.div(1000) else null

    override fun read(maxFrames: Int): FloatArray {
        require(maxFrames > 0)
        while (pending.isEmpty() && !outputDone) step()
        if (pending.isEmpty()) return FloatArray(0)
        var available = -pendingOffset
        for (chunk in pending) {
            available += chunk.size
            if (available >= maxFrames) break
        }
        val out = FloatArray(minOf(available, maxFrames))
        var filled = 0
        while (filled < out.size) {
            val chunk = pending.first()
            val take = minOf(chunk.size - pendingOffset, out.size - filled)
            System.arraycopy(chunk, pendingOffset, out, filled, take)
            filled += take
            pendingOffset += take
            if (pendingOffset == chunk.size) {
                pending.removeFirst()
                pendingOffset = 0
            }
        }
        return out
    }

    override fun close() {
        codec?.let { runCatching { it.stop() }; runCatching { it.release() } }
        runCatching { packets.close() }
    }

    /** Decodes until the first samples arrive. zh-CN: 解码到第一批采样出现为止. */
    private fun prime() {
        while (pending.isEmpty() && !outputDone) step()
        if (pending.isEmpty()) throw DecodeRules.empty(container)
    }

    private fun step() {
        try {
            if (codec == null) stepRaw() else stepCodec(codec)
        } catch (e: SpeechException) {
            throw e
        } catch (e: IOException) {
            throw SpeechException(SpeechErrorCodes.IO_FAILED, "The audio could not be read: ${e.message}", e)
        } catch (e: IllegalStateException) {
            // MediaCodec.CodecException is an IllegalStateException.
            throw DecodeRules.damaged(decodedMillis(), e)
        } catch (e: IllegalArgumentException) {
            throw DecodeRules.damaged(decodedMillis(), e)
        }
    }

    private fun stepRaw() {
        val buffer = raw!!
        buffer.clear()
        val size = packets.read(buffer)
        if (size < 0) {
            outputDone = true
            return
        }
        buffer.position(0)
        buffer.limit(size)
        deliver(PcmFrames.mono(buffer, encoding, channels))
    }

    private fun stepCodec(codec: MediaCodec) {
        var progress = false
        if (!inputDone) {
            val index = codec.dequeueInputBuffer(TIMEOUT_MICROS)
            if (index >= 0) {
                val buffer = codec.getInputBuffer(index) ?: throw IllegalStateException("no input buffer $index")
                buffer.clear()
                val size = packets.read(buffer)
                if (size < 0) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    inputDone = true
                } else {
                    codec.queueInputBuffer(index, 0, size, packets.timeMicros, 0)
                }
                progress = true
            }
        }
        val index = codec.dequeueOutputBuffer(info, TIMEOUT_MICROS)
        when {
            index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                outputFormat(codec.outputFormat)
                progress = true
            }
            index >= 0 -> {
                if (!formatKnown) outputFormat(codec.outputFormat)
                val buffer = codec.getOutputBuffer(index)
                if (buffer != null && info.size > 0) {
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    deliver(PcmFrames.mono(buffer, encoding, channels))
                }
                codec.releaseOutputBuffer(index, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                progress = true
            }
        }
        stalled = if (progress) 0 else stalled + 1
        if (stalled > STALL_STEPS) throw DecodeRules.damaged(decodedMillis(), IllegalStateException("the decoder stopped producing audio"))
    }

    private fun outputFormat(format: MediaFormat) {
        val newRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val newChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val newEncoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING) else PcmEncoding.PCM_16BIT
        if (PcmEncoding.bytes(newEncoding) == null || newChannels !in 1..PcmFrames.MAX_CHANNELS || newRate !in WavReader.MIN_SAMPLE_RATE..WavReader.MAX_SAMPLE_RATE) {
            throw DecodeRules.unreadablePcm(newEncoding, newChannels)
        }
        if (guard != null && newRate != rate) throw DecodeRules.rateChanged(rate, newRate)
        rate = newRate
        channels = newChannels
        encoding = newEncoding
        formatKnown = true
    }

    private fun deliver(frames: FloatArray) {
        if (frames.isEmpty()) return
        val counter = guard ?: DecodeRules.DurationGuard(rate).also { guard = it }
        counter.add(frames.size)
        pending.addLast(frames)
    }

    private fun decodedMillis(): Long = guard?.millis ?: 0L

    /** The compressed samples of one track. zh-CN: 一条音轨的压缩样本. */
    private interface Packets : Closeable {
        /** The decoder's input format (or the PCM layout of an `audio/raw` track). zh-CN: 解码器的输入格式 (或 `audio/raw` 音轨的 PCM 布局). */
        val format: MediaFormat
        val maxSampleBytes: Int
        val timeMicros: Long

        /** Copies the next sample into [buffer] and returns its size, or -1 at the end. zh-CN: 把下一个样本复制到 [buffer] 并返回其大小; 结束时为 -1. */
        fun read(buffer: ByteBuffer): Int
    }

    private class ExtractorPackets(
        private val extractor: MediaExtractor,
        private val descriptor: ParcelFileDescriptor,
        override val format: MediaFormat,
        private val mime: String,
    ) : Packets {
        override var timeMicros = 0L
        override val maxSampleBytes: Int =
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) maxOf(format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE), MIN_RAW_BYTES) else DEFAULT_RAW_BYTES

        override fun read(buffer: ByteBuffer): Int {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) return -1
            if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED) != 0) throw DecodeRules.encrypted(mime)
            timeMicros = extractor.sampleTime
            extractor.advance()
            return size
        }

        override fun close() {
            runCatching { extractor.release() }
            runCatching { descriptor.close() }
        }
    }

    private class OggPackets(private val stream: OggOpusStream) : Packets {
        override var timeMicros = 0L
        override val maxSampleBytes = DEFAULT_RAW_BYTES
        override val format: MediaFormat = MediaFormat.createAudioFormat(MIME_OPUS, OpusPackets.SAMPLE_RATE, stream.head.channels).apply {
            setByteBuffer("csd-0", ByteBuffer.wrap(stream.head.raw))
            setByteBuffer("csd-1", ByteBuffer.wrap(stream.head.codecDelay()))
            setByteBuffer("csd-2", ByteBuffer.wrap(stream.head.seekPreRoll()))
        }

        override fun read(buffer: ByteBuffer): Int {
            val packet = stream.next() ?: return -1
            if (packet.data.size > buffer.remaining()) throw DecodeRules.damaged(stream.millis, IllegalStateException("an Opus packet of ${packet.data.size} bytes"))
            buffer.put(packet.data)
            timeMicros = packet.timeMicros
            return packet.data.size
        }

        override fun close() = stream.close()
    }

    companion object {
        const val MIME_OPUS = "audio/opus"
        private const val MIME_RAW = "audio/raw"
        private const val TIMEOUT_MICROS = 10_000L
        private const val STALL_STEPS = 1000
        private const val MIN_RAW_BYTES = 16 * 1024
        private const val DEFAULT_RAW_BYTES = 256 * 1024

        /**
         * Opens [descriptor] (owned by the reader from now on, closed with it or on failure) of a [length] byte file
         * that [container] describes. An Ogg file goes to the system first and, when the system refuses it with
         * `UNSUPPORTED_FORMAT`, to the plugin's own Opus parser on a duplicate of the descriptor; if that refuses it
         * too, the system's reason stands unless the file is an Ogg Opus one (then the parser's reason is the more
         * precise). [forceOwnOgg] skips the system for Ogg Opus (device tests).
         * zh-CN: 打开 [length] 字节, 容器为 [container] 的文件的 [descriptor] (此后归读取器所有, 随之关闭, 失败时也关闭). Ogg 文件先交给系统,
         * 系统以 `UNSUPPORTED_FORMAT` 拒绝时再用描述符副本交给插件自带的 Opus 解析; 也被拒时保留系统的原因, 但 Ogg Opus 文件取解析的原因 (更具体).
         * [forceOwnOgg] 让 Ogg Opus 跳过系统 (设备测试用).
         */
        fun open(descriptor: ParcelFileDescriptor, length: Long, container: AudioContainer, forceOwnOgg: Boolean = false): MediaAudioReader {
            if (container == AudioContainer.OGG_OPUS && forceOwnOgg) return open(descriptor, container) { ogg(descriptor) }
            if (container != AudioContainer.OGG_OPUS && container != AudioContainer.OGG) return open(descriptor, container) { extractor(descriptor, length, container) }
            val spare = runCatching { descriptor.dup() }.getOrNull()
            val refused = try {
                return open(descriptor, container) { extractor(descriptor, length, container) }.also { spare?.close() }
            } catch (e: SpeechException) {
                if (spare == null || e.code != SpeechErrorCodes.UNSUPPORTED_FORMAT) {
                    spare?.close()
                    throw e
                }
                e
            } catch (e: Throwable) {
                spare?.close()
                throw e
            }
            try {
                return open(spare, container) { ogg(spare) }
            } catch (e: SpeechException) {
                throw if (container == AudioContainer.OGG_OPUS && e.code == SpeechErrorCodes.UNSUPPORTED_FORMAT) e.also { it.addSuppressed(refused) } else refused
            }
        }

        private fun open(descriptor: ParcelFileDescriptor, container: AudioContainer, source: () -> Packets): MediaAudioReader {
            val packets = try {
                source()
            } catch (e: Throwable) {
                runCatching { descriptor.close() }
                throw e
            }
            var codec: MediaCodec? = null
            try {
                val mime = packets.format.getString(MediaFormat.KEY_MIME).orEmpty()
                val reader = if (mime == MIME_RAW) {
                    MediaAudioReader(packets, null, container).also { it.outputFormat(packets.format) }
                } else {
                    codec = decoder(packets.format, mime)
                    MediaAudioReader(packets, codec, container)
                }
                reader.prime()
                return reader
            } catch (e: Throwable) {
                codec?.let { runCatching { it.stop() }; runCatching { it.release() } }
                runCatching { packets.close() }
                throw e
            }
        }

        private fun ogg(descriptor: ParcelFileDescriptor): Packets {
            val input = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            try {
                input.channel.position(0)
                return OggPackets(OggOpusStream(input))
            } catch (e: Throwable) {
                runCatching { input.close() }
                throw if (e is IOException) SpeechException(SpeechErrorCodes.IO_FAILED, "The audio could not be read: ${e.message}", e) else e
            }
        }

        private fun extractor(descriptor: ParcelFileDescriptor, length: Long, container: AudioContainer): Packets {
            val extractor = MediaExtractor()
            try {
                try {
                    extractor.setDataSource(descriptor.fileDescriptor, 0, length)
                } catch (e: IOException) {
                    throw DecodeRules.unreadable(container, e)
                } catch (e: IllegalArgumentException) {
                    throw DecodeRules.unreadable(container, e)
                }
                var hasVideo = false
                for (track in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(track)
                    val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("video/")) hasVideo = true
                    if (!mime.startsWith("audio/")) continue
                    DecodeRules.requireTrackDuration(if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else null)
                    extractor.selectTrack(track)
                    return ExtractorPackets(extractor, descriptor, format, mime)
                }
                throw DecodeRules.noAudioTrack(container, hasVideo)
            } catch (e: Throwable) {
                runCatching { extractor.release() }
                throw e
            }
        }

        private fun decoder(format: MediaFormat, mime: String): MediaCodec {
            val name = runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(format) }.getOrNull()
            val codec = try {
                if (name != null) MediaCodec.createByCodecName(name) else MediaCodec.createDecoderByType(mime)
            } catch (e: IOException) {
                throw DecodeRules.noDecoder(mime, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, e)
            } catch (e: IllegalArgumentException) {
                throw DecodeRules.noDecoder(mime, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, e)
            }
            try {
                codec.configure(format, null, null, 0)
                codec.start()
            } catch (e: Exception) {
                runCatching { codec.release() }
                throw DecodeRules.noDecoder(mime, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, e)
            }
            return codec
        }
    }
}
