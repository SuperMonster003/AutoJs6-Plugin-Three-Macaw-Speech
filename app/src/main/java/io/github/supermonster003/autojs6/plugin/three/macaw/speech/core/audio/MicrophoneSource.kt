package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import kotlin.concurrent.thread

/**
 * The plugin's microphone (roadmap D6, D16, P3.1): `AudioRecord` on the `VOICE_RECOGNITION` source, 16 kHz mono PCM16,
 * read on a capture thread of its own in 20 ms frames that [Listener.onAudio] receives as floats. Silenced audio is
 * detected two ways: a full second of zero samples, or `AudioRecordingConfiguration.isClientSilenced` (API 29+); the
 * listener hears [Listener.onSilenced] once per change (the system silences a process without the microphone
 * capability, roadmap D22). One instance records once; [stop] ends the capture and releases the recorder and may be
 * called from any thread.
 * zh-CN: 插件自己的麦克风 (路线图 D6, D16, P3.1): `VOICE_RECOGNITION` 音源上的 `AudioRecord`, 16 kHz 单声道 PCM16, 在独立的采集线程上以 20 ms
 * 帧读取, [Listener.onAudio] 收到浮点采样. 两种方式判断被静音: 连续一秒的零采样, 或 `AudioRecordingConfiguration.isClientSilenced` (API 29+);
 * 每次变化通知一次 [Listener.onSilenced] (系统会静音没有麦克风能力的进程, 路线图 D22). 一个实例只录音一次; [stop] 结束采集并释放录音器, 可从
 * 任意线程调用.
 */
class MicrophoneSource(private val listener: Listener) {

    interface Listener {
        fun onAudio(samples: FloatArray)
        fun onSilenced(silenced: Boolean)
        fun onFailure(error: SpeechException)
    }

    @Volatile
    private var running = false
    private var worker: Thread? = null
    private var record: AudioRecord? = null

    /**
     * Opens and starts the recorder; failures to open are thrown (`ENGINE_FAILED`), later read errors reach
     * [Listener.onFailure]. The caller checked `RECORD_AUDIO`.
     * zh-CN: 打开并启动录音器; 打开失败直接抛出 (`ENGINE_FAILED`), 之后的读取错误交给 [Listener.onFailure]. 调用方已检查 `RECORD_AUDIO`.
     */
    @SuppressLint("MissingPermission")
    fun start() {
        check(record == null) { "MicrophoneSource records once" }
        val minimum = AudioRecord.getMinBufferSize(AudioInput.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "The device cannot record 16 kHz mono PCM16 (getMinBufferSize = $minimum)")
        val recorder = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, AudioInput.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, BUFFER_BYTES))
        } catch (e: RuntimeException) {
            throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "AudioRecord could not be created: ${e.message}", e)
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "AudioRecord did not initialize; another app may hold the microphone")
        }
        try {
            recorder.startRecording()
        } catch (e: RuntimeException) {
            recorder.release()
            throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "AudioRecord could not start: ${e.message}", e)
        }
        if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            recorder.release()
            throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "AudioRecord did not start recording; another app may hold the microphone")
        }
        record = recorder
        running = true
        worker = thread(name = "MacawMicrophone", priority = Thread.MAX_PRIORITY) { capture(recorder) }
    }

    fun stop() {
        running = false
        val recorder = synchronized(this) { record }
        runCatching { recorder?.stop() }
        val current = worker
        if (current != null && current !== Thread.currentThread()) runCatching { current.join(JOIN_MILLIS) }
    }

    private fun capture(recorder: AudioRecord) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val buffer = ShortArray(FRAME_SAMPLES)
        var zeroSamples = 0L
        var silenced = false
        var lastCheck = SystemClock.elapsedRealtime()
        try {
            while (running) {
                val count = recorder.read(buffer, 0, buffer.size)
                if (!running) break
                if (count < 0) {
                    listener.onFailure(SpeechException(SpeechErrorCodes.ENGINE_FAILED, "AudioRecord.read failed with $count"))
                    break
                }
                if (count == 0) continue
                var zero = true
                for (index in 0 until count) {
                    if (buffer[index].toInt() != 0) {
                        zero = false
                        break
                    }
                }
                zeroSamples = if (zero) zeroSamples + count else 0
                var nowSilenced = zeroSamples >= SILENCE_SAMPLES
                val now = SystemClock.elapsedRealtime()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && now - lastCheck >= CHECK_MILLIS) {
                    lastCheck = now
                    if (recorder.activeRecordingConfiguration?.isClientSilenced == true) nowSilenced = true
                }
                if (nowSilenced != silenced) {
                    silenced = nowSilenced
                    listener.onSilenced(silenced)
                }
                listener.onAudio(AudioInput.pcm16(buffer, count))
            }
        } catch (e: RuntimeException) {
            if (running) listener.onFailure(SpeechException(SpeechErrorCodes.ENGINE_FAILED, "Recording failed: ${e.message}", e))
        } finally {
            running = false
            synchronized(this) {
                runCatching { recorder.stop() }
                recorder.release()
                record = null
            }
        }
    }

    companion object {
        /** 20 ms at 16 kHz (roadmap P3.1). zh-CN: 16 kHz 下 20 ms. */
        const val FRAME_SAMPLES = 320
        private const val BUFFER_BYTES = 16_000 * 2 / 2
        private const val SILENCE_SAMPLES = 16_000L
        private const val CHECK_MILLIS = 1000L
        private const val JOIN_MILLIS = 2000L
    }
}
