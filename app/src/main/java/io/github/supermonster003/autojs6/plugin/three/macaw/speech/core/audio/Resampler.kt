package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Streaming band-limited resampler for WAV inputs of any rate (roadmap D15): a windowed-sinc (Blackman) interpolator
 * whose cutoff sits just below the lower of the two Nyquist frequencies, so downsampling 44.1 or 48 kHz to the 16 kHz
 * of the models does not fold the upper band into speech. The kernel is tabulated at [PHASES] positions per input
 * sample and interpolated linearly. Output sample `n` lies at input position `n * inputRate / outputRate`; [flush]
 * pads zeros so a whole input of `N` samples yields `ceil(N * outputRate / inputRate)` samples. Pure JVM; equal rates
 * pass through unchanged.
 * zh-CN: 适用于任意采样率 WAV 输入的流式带限重采样器 (路线图 D15): 加窗 sinc (Blackman) 插值, 截止频率略低于两个奈奎斯特频率中较低者, 因此
 * 把 44.1 或 48 kHz 降到模型的 16 kHz 时高频不会折叠进语音. 核函数按每个输入采样 [PHASES] 个位置制表并线性插值. 输出第 `n` 个采样位于输入
 * 位置 `n * inputRate / outputRate`; [flush] 补零, 使 `N` 个输入采样整体得到 `ceil(N * outputRate / inputRate)` 个输出. 纯 JVM; 采样率相同时
 * 原样通过.
 */
class Resampler(val inputRate: Int, val outputRate: Int, zeroCrossings: Int = 16) {

    init {
        require(inputRate > 0 && outputRate > 0) { "Sample rates must be positive" }
        require(zeroCrossings in 2..64)
    }

    private val passThrough = inputRate == outputRate

    /** Cutoff in cycles per input sample. zh-CN: 截止频率, 以每输入采样的周期数计. */
    private val cutoff = 0.5 * minOf(1.0, outputRate.toDouble() / inputRate) * ROLLOFF

    /** Half width of the kernel in input samples. zh-CN: 核函数半宽, 以输入采样计. */
    private val halfWidth = ceil(zeroCrossings / (2 * cutoff)).toInt()

    private val table: FloatArray = FloatArray(2 * halfWidth * PHASES + 2).also { table ->
        for (index in table.indices) {
            val t = index.toDouble() / PHASES - halfWidth
            table[index] = kernel(t).toFloat()
        }
    }

    // Input samples from absolute index [bufferStart]; indices before 0 read as silence.
    private var buffer = FloatArray(4096)
    private var bufferLength = 0
    private var bufferStart = 0L
    private var consumed = 0L
    private var produced = 0L

    /**
     * Feeds [input] and returns every output sample whose kernel is complete.
     * zh-CN: 送入 [input], 返回核函数已完整覆盖的全部输出采样.
     */
    fun process(input: FloatArray): FloatArray {
        if (passThrough) return input.copyOf().also { consumed += input.size; produced += input.size }
        append(input)
        consumed += input.size
        return drain(consumed)
    }

    /**
     * Ends the stream: emits the remaining samples (the tail kernels read zeros past the end).
     * zh-CN: 结束输入流: 输出其余采样 (尾部核函数在末尾之后读到零).
     */
    fun flush(): FloatArray {
        if (passThrough) return FloatArray(0)
        val total = ceilDiv(consumed * outputRate, inputRate.toLong())
        val out = FloatArray((total - produced).coerceAtLeast(0).toInt())
        for (index in out.indices) {
            out[index] = sampleAt(produced)
            produced++
        }
        return out
    }

    private fun drain(available: Long): FloatArray {
        var out = FloatArray(((available * outputRate) / inputRate - produced + 2).coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
        var count = 0
        while (true) {
            val center = position(produced)
            // The kernel reaches floor(center) + halfWidth; it must already be in the buffer.
            if (floor(center).toLong() + halfWidth >= available) break
            if (count == out.size) out = out.copyOf(out.size * 2 + 16)
            out[count++] = sampleAt(produced)
            produced++
        }
        trim(floor(position(produced)).toLong() - halfWidth)
        return if (count == out.size) out else out.copyOf(count)
    }

    private fun sampleAt(n: Long): Float {
        val center = position(n)
        val base = floor(center).toLong()
        val fraction = center - base
        var sum = 0.0
        for (offset in (1 - halfWidth)..halfWidth) {
            val index = base + offset
            val value = valueAt(index)
            if (value == 0f) continue
            val tablePosition = (offset - fraction + halfWidth) * PHASES
            val slot = tablePosition.toInt()
            val weight = tablePosition - slot
            val coefficient = table[slot] * (1 - weight) + table[slot + 1] * weight
            sum += value * coefficient
        }
        return sum.toFloat()
    }

    private fun valueAt(index: Long): Float {
        if (index < bufferStart || index >= bufferStart + bufferLength) return 0f
        return buffer[(index - bufferStart).toInt()]
    }

    private fun position(n: Long): Double = (n * inputRate).toDouble() / outputRate

    private fun append(input: FloatArray) {
        if (bufferLength + input.size > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, bufferLength + input.size))
        System.arraycopy(input, 0, buffer, bufferLength, input.size)
        bufferLength += input.size
    }

    private fun trim(firstNeeded: Long) {
        val drop = (firstNeeded - bufferStart).coerceIn(0L, bufferLength.toLong()).toInt()
        if (drop == 0) return
        System.arraycopy(buffer, drop, buffer, 0, bufferLength - drop)
        bufferLength -= drop
        bufferStart += drop
    }

    /** Windowed sinc with unit gain at DC, `t` in input samples. zh-CN: 直流增益为 1 的加窗 sinc, `t` 以输入采样计. */
    private fun kernel(t: Double): Double {
        if (t <= -halfWidth || t >= halfWidth) return 0.0
        val x = 2 * cutoff * t
        val sinc = if (x == 0.0) 1.0 else sin(PI * x) / (PI * x)
        val phase = PI * (t + halfWidth) / halfWidth
        val window = 0.42 - 0.5 * cos(phase) + 0.08 * cos(2 * phase)
        return 2 * cutoff * sinc * window
    }

    private companion object {
        const val PHASES = 256
        const val ROLLOFF = 0.94

        fun ceilDiv(a: Long, b: Long) = (a + b - 1) / b
    }
}
