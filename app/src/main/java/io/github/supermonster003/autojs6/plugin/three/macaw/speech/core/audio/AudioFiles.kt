package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import androidx.annotation.VisibleForTesting
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import java.io.IOException

/**
 * Opens the audio file of `recognizeFile` / `detectVoice` (roadmap D15, D81): a WAV through [WavReader], every other
 * container through the system decoders of [MediaAudioReader]. A WAV that [WavReader] refuses (a compressed format tag
 * such as A-law) gets a second try with the system decoders and keeps its own error when they refuse it too. The
 * caller's descriptor stays open and its offset never moves (the head is read with `pread`, each reader works on a
 * duplicate), so every reader starts at byte 0.
 * zh-CN: 打开 `recognizeFile` / `detectVoice` 的音频文件 (路线图 D15, D81): WAV 经 [WavReader], 其他容器经 [MediaAudioReader] 的系统解码器.
 * [WavReader] 拒绝的 WAV (A-law 等压缩格式标签) 再交给系统解码器试一次, 也被拒时保留它自己的错误. 调用方的描述符保持打开, 偏移从不移动 (文件头用
 * `pread` 读, 每个读取器使用副本), 因此每个读取器都从第 0 字节开始.
 */
object AudioFiles {

    /** Device tests: decode Ogg Opus with the plugin's own parser on every Android version. zh-CN: 设备测试用: 在任何 Android 版本上都用自带解析读 Ogg Opus. */
    @VisibleForTesting
    @Volatile
    var forceOwnOggOpus = false

    fun open(audio: ParcelFileDescriptor): MonoAudio {
        val length = audio.statSize.takeIf { it >= 0 }
        if (length != null && length > SpeechLimits.MAX_AUDIO_FILE_BYTES) {
            throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio file has $length bytes, more than ${SpeechLimits.MAX_AUDIO_FILE_BYTES}")
        }
        val container = AudioContainers.sniff(head(audio))
        if (container != AudioContainer.WAV) return MediaAudioReader.open(duplicate(audio), length ?: Long.MAX_VALUE, container, forceOwnOggOpus)
        return try {
            wav(audio)
        } catch (refused: SpeechException) {
            if (refused.code != SpeechErrorCodes.UNSUPPORTED_FORMAT) throw refused
            try {
                MediaAudioReader.open(duplicate(audio), length ?: Long.MAX_VALUE, container)
            } catch (e: SpeechException) {
                throw if (e.code == SpeechErrorCodes.UNSUPPORTED_FORMAT) refused else e
            }
        }
    }

    private fun head(audio: ParcelFileDescriptor): ByteArray {
        val head = ByteArray(AudioContainers.SNIFF_BYTES)
        val read = try {
            Os.pread(audio.fileDescriptor, head, 0, head.size, 0)
        } catch (e: ErrnoException) {
            throw SpeechException(SpeechErrorCodes.IO_FAILED, "The audio descriptor cannot be read: ${e.message}", e)
        }
        return head.copyOf(maxOf(read, 0))
    }

    private fun duplicate(audio: ParcelFileDescriptor): ParcelFileDescriptor = try {
        audio.dup()
    } catch (e: IOException) {
        throw SpeechException(SpeechErrorCodes.IO_FAILED, "The audio descriptor cannot be read: ${e.message}", e)
    }

    private fun wav(audio: ParcelFileDescriptor): WavReader {
        val copy = duplicate(audio)
        val length = copy.statSize.takeIf { it >= 0 }
        val input = ParcelFileDescriptor.AutoCloseInputStream(copy)
        return try {
            WavReader(input, length)
        } catch (e: SpeechException) {
            runCatching { input.close() }
            throw e
        } catch (e: IOException) {
            runCatching { input.close() }
            throw SpeechException(SpeechErrorCodes.IO_FAILED, "The audio could not be read: ${e.message}", e)
        }
    }
}
