package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRouting
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import kotlin.concurrent.thread

/**
 * The plugin's microphone (roadmap D6, D16, P3.1): `AudioRecord` on the `VOICE_RECOGNITION` source, 16 kHz mono PCM16,
 * read on a capture thread of its own in 20 ms frames that [Listener.onAudio] receives as floats. Silenced audio is
 * detected two ways: a full second of zero samples, or `AudioRecordingConfiguration.isClientSilenced` (API 29+); the
 * listener hears [Listener.onSilenced] once per change (the system silences a process without the microphone
 * capability, roadmap D22). One instance records once; [stop] ends the capture and releases the recorder and may be
 * called from any thread. [attribution] (API 31+) is the context the recording is attributed to: the caller of the
 * system recognition service (roadmap D56), whose `AttributionSource` the context carries. [frameSamples] is the read
 * step; the power saving mode of the resident wake word reads longer frames (roadmap P8.1, D128).
 * zh-CN: 插件自己的麦克风 (路线图 D6, D16, P3.1): `VOICE_RECOGNITION` 音源上的 `AudioRecord`, 16 kHz 单声道 PCM16, 在独立的采集线程上以 20 ms
 * 帧读取, [Listener.onAudio] 收到浮点采样. 两种方式判断被静音: 连续一秒的零采样, 或 `AudioRecordingConfiguration.isClientSilenced` (API 29+);
 * 每次变化通知一次 [Listener.onSilenced] (系统会静音没有麦克风能力的进程, 路线图 D22). 一个实例只录音一次; [stop] 结束采集并释放录音器, 可从
 * 任意线程调用. [attribution] (API 31+) 是录音所归属的 Context: 系统识别服务的调用方 (路线图 D56), 该 Context 携带其 `AttributionSource`.
 * [frameSamples] 是读取步长; 常驻唤醒的省电模式读取更长的帧 (路线图 P8.1, D128).
 */
class MicrophoneSource(
    private val listener: Listener,
    private val attribution: Context? = null,
    private val frameSamples: Int = FRAME_SAMPLES,
) : AudioCapture {

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
    override fun start() {
        check(record == null) { "MicrophoneSource records once" }
        val minimum = AudioRecord.getMinBufferSize(AudioInput.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) throw SpeechException(SpeechErrorCodes.ENGINE_FAILED, "The device cannot record 16 kHz mono PCM16 (getMinBufferSize = $minimum)")
        val recorder = try {
            if (attribution != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AudioRecord.Builder()
                    .setContext(attribution)
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(
                        AudioFormat.Builder().setSampleRate(AudioInput.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
                    )
                    .setBufferSizeInBytes(bufferBytes(minimum))
                    .build()
            } else {
                AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, AudioInput.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes(minimum))
            }
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
        Log.i(TAG, "Recording started")
        // The input in use, for the device runs: the headset's microphone of roadmap D68 comes once its audio link is up.
        recorder.addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener { routing -> Log.i(TAG, "Recording routed to ${inputName(routing.routedDevice?.type)}") }, null)
        worker = thread(name = "MacawMicrophone", priority = Thread.MAX_PRIORITY) { capture(recorder) }
    }

    /** At least two read steps, so a long step never waits on a full buffer. zh-CN: 至少两个读取步长, 长步长也不会遇到缓冲区已满. */
    private fun bufferBytes(minimum: Int): Int = maxOf(minimum, BUFFER_BYTES, frameSamples * 2 * 2)

    override fun stop() {
        running = false
        val recorder = synchronized(this) { record }
        runCatching { recorder?.stop() }
        val current = worker
        if (current != null && current !== Thread.currentThread()) runCatching { current.join(JOIN_MILLIS) }
    }

    private fun capture(recorder: AudioRecord) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val buffer = ShortArray(frameSamples)
        val started = SystemClock.elapsedRealtime()
        var heard = false
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
                if (!zero && !heard) {
                    heard = true
                    Log.i(TAG, "First sound ${SystemClock.elapsedRealtime() - started} ms after the recording started")
                }
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
        private const val TAG = "MacawMicrophone"
        private const val BUFFER_BYTES = 16_000 * 2 / 2
        private const val SILENCE_SAMPLES = 16_000L
        private const val CHECK_MILLIS = 1000L
        private const val JOIN_MILLIS = 2000L

        private fun inputName(type: Int?): String = when (type) {
            null -> "no device"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "the built-in microphone"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "a Bluetooth headset (SCO)"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "a wired headset"
            else -> "device type $type"
        }
    }
}

/** A recording of the plugin's microphone sessions. zh-CN: 插件麦克风会话的一次录音. */
interface AudioCapture {
    fun start()
    fun stop()
}

/**
 * Opens the capture of a microphone session: the real [MicrophoneSource], or the virtual room a device test installs
 * in [override] (an emulator has no audio input; roadmap P5.7 E3). Production code never sets it.
 * zh-CN: 打开麦克风会话的录音: 真实的 [MicrophoneSource], 或设备测试在 [override] 中装入的虚拟房间 (模拟器没有音频输入; 路线图 P5.7 E3).
 * 正式代码从不设置它.
 */
object AudioCaptures {
    @Volatile
    var override: ((MicrophoneSource.Listener) -> AudioCapture)? = null

    fun open(listener: MicrophoneSource.Listener, attribution: Context?, frameSamples: Int = MicrophoneSource.FRAME_SAMPLES): AudioCapture =
        override?.invoke(listener) ?: MicrophoneSource(listener, attribution, frameSamples)
}
