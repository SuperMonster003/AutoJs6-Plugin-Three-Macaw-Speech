package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The device test recording and its reference segmentation (roadmap P0.2).
 *
 * The reference comes from sherpa-onnx 1.13.8 itself: its Python wheel on Windows x64 ran `silero_vad_v5.onnx` over
 * `test_silero_vad.wav` with the defaults of [io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaRuntime.VadSettings]
 * (window 512, threshold 0.5, 0.25 s minimum silence and speech, 5 s maximum speech), fed window by window and flushed
 * at the end. Devices may round differently near the threshold, so boundaries are compared within [TOLERANCE_SAMPLES].
 *
 * zh-CN: 设备测试录音及其参考切分 (路线图 P0.2). 参考值来自 sherpa-onnx 1.13.8 本身: 其 Windows x64 Python wheel 以
 * `VadSettings` 的默认参数 (窗口 512, 阈值 0.5, 最短静音与语音 0.25 秒, 最长语音 5 秒) 逐窗口处理 `test_silero_vad.wav` 并在结尾冲刷.
 * 不同设备在阈值附近的舍入可能不同, 因此边界在 [TOLERANCE_SAMPLES] 之内比较.
 */
internal object VadReference {

    const val ASSET = "test_silero_vad.wav"

    /** Two VAD windows (64 ms). zh-CN: 两个 VAD 窗口 (64 毫秒). */
    const val TOLERANCE_SAMPLES = 1024

    /** (start sample, sample count) of every expected speech segment. zh-CN: 每个预期语音段的 (起始采样, 采样数). */
    val SEGMENTS: List<Pair<Int, Int>> = listOf(
        0 to 32864, 42464 to 67200, 148960 to 93152, 245216 to 8320, 260576 to 26240,
        293856 to 20096, 325088 to 196576, 523232 to 79840, 607200 to 14976, 637920 to 55424,
        697824 to 14976, 720352 to 29312, 781280 to 18048, 817120 to 87520, 905696 to 53792,
    )

    /**
     * Reads a canonical 16 kHz mono PCM16 WAV into samples in -1..1, rejecting any other format.
     * zh-CN: 读取标准 16 kHz 单声道 PCM16 WAV 为 -1..1 的采样, 拒绝其他格式.
     */
    fun readPcm16Mono(input: InputStream): FloatArray {
        val bytes = DataInputStream(input).use { it.readBytes() }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        check(String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE") { "not a WAV file" }
        var offset = 12
        var data: Pair<Int, Int>? = null
        while (offset + 8 <= bytes.size && data == null) {
            val id = String(bytes, offset, 4, Charsets.US_ASCII)
            val size = buffer.getInt(offset + 4)
            when (id) {
                "fmt " -> {
                    check(buffer.getShort(offset + 8).toInt() == 1) { "PCM expected" }
                    check(buffer.getShort(offset + 10).toInt() == 1) { "mono expected" }
                    check(buffer.getInt(offset + 12) == 16000) { "16 kHz expected" }
                    check(buffer.getShort(offset + 22).toInt() == 16) { "16-bit expected" }
                }
                "data" -> data = offset + 8 to size
            }
            offset += 8 + size + (size and 1)
        }
        val (start, size) = checkNotNull(data) { "no data chunk" }
        return FloatArray(size / 2) { index -> buffer.getShort(start + index * 2) / 32768f }
    }

}
