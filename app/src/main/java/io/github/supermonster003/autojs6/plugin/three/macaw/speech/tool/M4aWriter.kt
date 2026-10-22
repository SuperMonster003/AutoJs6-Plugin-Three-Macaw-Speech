package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.Pcm16
import java.io.File
import java.io.IOException

/**
 * Writes mono float samples as AAC-LC in an MPEG-4 file (`m4a` of `speech_synthesize`, roadmap D98) through the system
 * AAC encoder and `MediaMuxer`, at the voice's sample rate and the bit rate of [SynthesizeRules.aac]. The encoded bytes
 * count against [maxBytes] as they come, so an output that grows too large fails with `LIMIT_EXCEEDED` (the message of
 * `WavWriter`) long before the file is done. Call [finish] once after the last [write]; [close] releases the encoder
 * and the muxer in any state. Not thread safe.
 * zh-CN: 经系统 AAC 编码器与 `MediaMuxer` 把单声道浮点采样写成 MPEG-4 文件中的 AAC-LC (`speech_synthesize` 的 `m4a`, 路线图 D98), 采样率取语音的
 * 采样率, 码率取 [SynthesizeRules.aac]. 编码后的字节随产出计入 [maxBytes], 因此过大的输出在文件写完之前很早就以 `LIMIT_EXCEEDED` 失败 (信息与
 * `WavWriter` 相同). 最后一次 [write] 之后调用一次 [finish]; [close] 在任何状态下释放编码器与封装器. 非线程安全.
 */
internal class M4aWriter(
    private val file: File,
    val sampleRate: Int,
    private val maxBytes: Long = SynthesizeRules.MAX_OUTPUT_BYTES,
) : AudioFileWriter {

    private val format = SynthesizeRules.aac(sampleRate)
    private val codec: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    private val muxer: MediaMuxer
    private val info = MediaCodec.BufferInfo()
    private var pcm = ByteArray(0)
    private var track = -1
    private var muxing = false
    private var encodedBytes = 0L
    private var ended = false

    /** Samples handed to the encoder so far. zh-CN: 已交给编码器的采样数. */
    var sampleCount: Long = 0
        private set

    val durationMillis: Long get() = sampleCount * 1000 / sampleRate

    init {
        try {
            codec.configure(
                MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, format.sampleRate, 1).apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, format.bitRate)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, INPUT_BYTES)
                },
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE,
            )
            codec.start()
            muxer = MediaMuxer(file.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            runCatching { codec.release() }
            throw e
        }
    }

    override fun write(samples: FloatArray) {
        check(!ended) { "The writer is finished" }
        var offset = 0
        var left = samples.size
        while (left > 0) {
            val index = codec.dequeueInputBuffer(TIMEOUT_US)
            if (index < 0) {
                drain(endOfStream = false)
                continue
            }
            val buffer = codec.getInputBuffer(index) ?: throw IOException("The AAC encoder gave no input buffer")
            buffer.clear()
            val n = minOf(left, buffer.remaining() / 2, INPUT_BYTES / 2)
            if (pcm.size < n * 2) pcm = ByteArray(n * 2)
            val bytes = Pcm16.encode(samples, offset, n, pcm)
            buffer.put(pcm, 0, bytes)
            codec.queueInputBuffer(index, 0, bytes, SynthesizeRules.presentationTimeUs(sampleCount, sampleRate), 0)
            sampleCount += n
            offset += n
            left -= n
            drain(endOfStream = false)
        }
    }

    /** Ends the stream, writes the rest and closes the MPEG-4 file; answers its size. zh-CN: 结束流, 写完剩余部分并收尾 MPEG-4 文件; 返回其大小. */
    override fun finish(): Long {
        check(!ended) { "The writer is finished" }
        ended = true
        while (true) {
            val index = codec.dequeueInputBuffer(TIMEOUT_US)
            if (index >= 0) {
                codec.queueInputBuffer(index, 0, 0, SynthesizeRules.presentationTimeUs(sampleCount, sampleRate), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                break
            }
            drain(endOfStream = false)
        }
        drain(endOfStream = true)
        if (!muxing) throw IOException("The AAC encoder produced no audio")
        muxer.stop()
        muxing = false
        val size = file.length()
        SynthesizeRules.checkSize(size, maxBytes)
        return size
    }

    override fun close() {
        runCatching { codec.stop() }
        runCatching { codec.release() }
        if (muxing) runCatching { muxer.stop() }
        runCatching { muxer.release() }
    }

    private fun drain(endOfStream: Boolean) {
        val deadline = System.nanoTime() + DRAIN_TIMEOUT_NS
        while (true) {
            val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                    if (System.nanoTime() > deadline) throw IOException("The AAC encoder did not end the stream")
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(track < 0) { "The AAC encoder changed its format twice" }
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxing = true
                }
                index >= 0 -> {
                    val done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                        check(muxing) { "The AAC encoder produced audio before its format" }
                        val buffer = codec.getOutputBuffer(index) ?: throw IOException("The AAC encoder gave no output buffer")
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        encodedBytes += info.size
                        SynthesizeRules.checkSize(encodedBytes + CONTAINER_BYTES, maxBytes)
                        muxer.writeSampleData(track, buffer, info)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (done) return
                }
            }
        }
    }

    private companion object {
        /** 1024 frames of 16-bit mono a few times over: the input the encoder takes at once. zh-CN: 编码器一次接收的输入. */
        const val INPUT_BYTES = 16 * 1024
        const val TIMEOUT_US = 10_000L
        const val DRAIN_TIMEOUT_NS = 5_000_000_000L

        /**
         * A bound of the MPEG-4 boxes around the AAC frames counted while encoding; [finish] checks the real size.
         * zh-CN: 编码时计入的 AAC 帧之外 MPEG-4 容器的估计量; [finish] 检查真实大小.
         */
        const val CONTAINER_BYTES = 4096L
    }
}
