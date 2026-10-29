package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MonoAudio
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.VoiceDetector
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WindowSplitter
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits

/**
 * The speech of a recording a speaker embedding is computed from (roadmap D132): at most
 * `SpeechLimits.MAX_SPEAKER_AUDIO_MILLIS` of 16 kHz audio (`LIMIT_EXCEEDED` beyond), of which only the segments the VAD
 * finds are kept, so pauses and room noise do not dilute the voiceprint. Pure JVM.
 * zh-CN: 计算说话人嵌入所用的录音语音 (路线图 D132): 至多 `SpeechLimits.MAX_SPEAKER_AUDIO_MILLIS` 的 16 kHz 音频 (超出则 `LIMIT_EXCEEDED`),
 * 只保留 VAD 找到的语音段, 停顿与环境噪声因此不会冲淡声纹. 纯 JVM.
 */
object SpeakerAudio {

    /** [samples] are the speech, [audioMillis] the whole recording. zh-CN: [samples] 为语音, [audioMillis] 为整段录音. */
    class Speech(val samples: FloatArray, val audioMillis: Long) {
        val speechMillis: Long get() = AudioInput.millis(samples.size.toLong())
    }

    /** The speech of a decoded file ([label] names it in errors). zh-CN: 已解码文件的语音 ([label] 用于错误信息). */
    fun speech(reader: MonoAudio, vad: VoiceDetector, label: String, cancelled: () -> Boolean = { false }): Speech {
        val limit = AudioInput.samples(SpeechLimits.MAX_SPEAKER_AUDIO_MILLIS)
        val collector = Collector(vad)
        var total = 0L
        AudioInput.decode(reader, cancelled = cancelled) { chunk ->
            total += chunk.size
            if (total > limit) {
                throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "$label is longer than ${SpeechLimits.MAX_SPEAKER_AUDIO_MILLIS / 1000} seconds")
            }
            collector.accept(chunk)
        }
        return Speech(collector.finish(), AudioInput.millis(total))
    }

    /** The speech of 16 kHz samples already in memory (a recording of the plugin). zh-CN: 内存中 16 kHz 采样 (插件自己的录音) 的语音. */
    fun speech(samples: FloatArray, vad: VoiceDetector): Speech {
        val collector = Collector(vad)
        collector.accept(samples)
        return Speech(collector.finish(), AudioInput.millis(samples.size.toLong()))
    }

    /** `AUDIO_TOO_SHORT` below one second of speech. zh-CN: 语音不足一秒时为 `AUDIO_TOO_SHORT`. */
    fun requireEnough(speech: Speech, label: String): Speech {
        if (speech.speechMillis < SpeakerRules.MIN_SPEECH_MILLIS) {
            throw SpeechException(
                SpeechErrorCodes.AUDIO_TOO_SHORT,
                "$label has ${speech.speechMillis} ms of speech; at least ${SpeakerRules.MIN_SPEECH_MILLIS} ms are needed",
            )
        }
        return speech
    }

    private class Collector(private val vad: VoiceDetector) {
        private val windows = WindowSplitter(vad.windowSize)
        private var buffer = FloatArray(AudioInput.SAMPLE_RATE * 4)
        private var size = 0

        fun accept(chunk: FloatArray) = windows.split(chunk) { window ->
            vad.accept(window)
            drain()
        }

        fun finish(): FloatArray {
            windows.remainder()?.let { vad.accept(it) }
            vad.flush()
            drain()
            return buffer.copyOf(size)
        }

        private fun drain() {
            while (true) {
                val segment = vad.poll() ?: return
                if (size + segment.samples.size > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + segment.samples.size))
                System.arraycopy(segment.samples, 0, buffer, size, segment.samples.size)
                size += segment.samples.size
            }
        }
    }
}
