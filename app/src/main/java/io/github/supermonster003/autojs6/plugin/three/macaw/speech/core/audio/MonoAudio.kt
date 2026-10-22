package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import java.io.Closeable

/**
 * A decoded audio file (roadmap D15, D83): mono floats in -1..1 at [sampleRate], read chunk by chunk so a 30 minute
 * file never sits in memory. [WavReader] parses WAV itself; [MediaAudioReader] runs the system decoders for the other
 * containers. [AudioInput.decode] resamples either to the internal 16 kHz.
 * zh-CN: 一个已解码的音频文件 (路线图 D15, D83): 以 [sampleRate] 采样率逐块读出的 -1..1 单声道浮点, 30 分钟的文件也不会整段驻留内存.
 * [WavReader] 自行解析 WAV; [MediaAudioReader] 用系统解码器读其他容器. [AudioInput.decode] 把两者都重采样到内部的 16 kHz.
 */
interface MonoAudio : Closeable {

    /** Frames per second of [read]'s samples. zh-CN: [read] 所返回采样的采样率. */
    val sampleRate: Int

    /**
     * Reads up to [maxFrames] mono frames; an empty array at the end of the audio.
     * zh-CN: 读取至多 [maxFrames] 帧单声道采样; 音频结束时返回空数组.
     */
    fun read(maxFrames: Int): FloatArray
}
