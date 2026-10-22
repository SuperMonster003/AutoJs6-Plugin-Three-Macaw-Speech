package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits

/**
 * The limits and errors of decoded audio files (roadmap D83, D85): the 30 minute ceiling checked from the track's
 * duration before decoding and from the decoded frames while decoding, both with [PADDING_MILLIS] for encoder padding,
 * and the `UNSUPPORTED_FORMAT` messages that tell the script why a file was refused. Messages never name the file.
 * Pure JVM.
 * zh-CN: 已解码音频文件的上限与错误 (路线图 D83, D85): 30 分钟上限在解码前按音轨时长, 解码中按已解出的帧数检查, 两者都允许 [PADDING_MILLIS]
 * 的编码填充; 以及告诉脚本文件为何被拒的 `UNSUPPORTED_FORMAT` 信息. 信息从不写出文件名. 纯 JVM.
 */
object DecodeRules {

    /** Encoder padding accepted beyond `SpeechLimits.MAX_AUDIO_MILLIS`. zh-CN: 超出 `SpeechLimits.MAX_AUDIO_MILLIS` 时容许的编码填充. */
    const val PADDING_MILLIS = 1000L

    /** Fails with `LIMIT_EXCEEDED` when a track declares more than 30 minutes. zh-CN: 音轨声明超过 30 分钟时以 `LIMIT_EXCEEDED` 失败. */
    fun requireTrackDuration(durationMicros: Long?) {
        if (durationMicros == null || durationMicros <= 0) return
        val millis = durationMicros / 1000
        if (millis > SpeechLimits.MAX_AUDIO_MILLIS + PADDING_MILLIS) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio lasts $millis ms, more than ${SpeechLimits.MAX_AUDIO_MILLIS}")
        }
    }

    /**
     * Counts decoded frames and fails with `LIMIT_EXCEEDED` once they pass 30 minutes, for files whose header
     * understates their length.
     * zh-CN: 统计已解出的帧数, 超过 30 分钟即以 `LIMIT_EXCEEDED` 失败, 用于头部少报时长的文件.
     */
    class DurationGuard(private val sampleRate: Int) {
        var frames = 0L
            private set

        val millis: Long get() = frames * 1000L / sampleRate

        fun add(count: Int) {
            frames += count
            if (millis > SpeechLimits.MAX_AUDIO_MILLIS + PADDING_MILLIS) {
                throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio lasts more than ${SpeechLimits.MAX_AUDIO_MILLIS} ms")
            }
        }
    }

    fun unreadable(container: AudioContainer, cause: Throwable? = null): SpeechException =
        unsupported("The file is not audio this device can read (it looks like ${container.label})", cause)

    fun noAudioTrack(container: AudioContainer, hasVideo: Boolean): SpeechException =
        unsupported(if (hasVideo) "The ${container.label} file has video but no audio track" else "The ${container.label} file has no audio track")

    fun noDecoder(mime: String, release: String, sdk: Int, cause: Throwable? = null): SpeechException =
        unsupported("This device (Android $release, API $sdk) has no decoder for $mime", cause)

    fun encrypted(mime: String): SpeechException = unsupported("The $mime audio track is encrypted (DRM)")

    fun unreadablePcm(encoding: Int, channels: Int): SpeechException =
        unsupported("The decoder delivers PCM the plugin cannot read (encoding $encoding, $channels channels)")

    fun rateChanged(from: Int, to: Int): SpeechException = unsupported("The sample rate changes from $from Hz to $to Hz while decoding")

    fun damaged(atMillis: Long, cause: Throwable? = null): SpeechException =
        unsupported("The audio is damaged: decoding failed at $atMillis ms${cause?.message?.let { " ($it)" }.orEmpty()}", cause)

    fun empty(container: AudioContainer): SpeechException = unsupported("No audio could be decoded from the ${container.label} file")

    private fun unsupported(message: String, cause: Throwable? = null) = SpeechException(SpeechErrorCodes.UNSUPPORTED_FORMAT, message, cause)
}
