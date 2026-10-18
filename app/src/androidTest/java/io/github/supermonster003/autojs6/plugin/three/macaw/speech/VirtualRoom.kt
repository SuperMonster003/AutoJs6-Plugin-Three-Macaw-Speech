package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioCapture
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioCaptures
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MicrophoneSource
import kotlin.concurrent.thread

/**
 * A room for emulators, which have no audio input (roadmap P5.7 E3): while installed, every microphone session of the
 * plugin records from it instead of `AudioRecord`, in real time and 20 ms frames, exactly what was [play]ed at that
 * moment (silence otherwise). Two sessions recording at once hear the same sound, like two microphones in one room.
 * The real microphone path is covered on phones through the speaker.
 * zh-CN: 供模拟器使用的房间, 模拟器没有音频输入 (路线图 P5.7 E3): 装入后, 插件的每个麦克风会话都从这里录音而不是 `AudioRecord`, 按真实时间以 20 ms 帧
 * 取得此刻 [play] 的声音 (否则为静音). 两个会话同时录音时听到同一声音, 如同一个房间里的两个麦克风. 真实麦克风路径在手机上经扬声器覆盖.
 */
internal object VirtualRoom {
    private val lock = Any()
    private var clip: FloatArray? = null
    private var clipStart = 0L

    fun install() {
        AudioCaptures.override = { listener -> Capture(listener) }
    }

    fun uninstall() {
        AudioCaptures.override = null
        synchronized(lock) { clip = null }
    }

    /** Plays 16 kHz [samples] from now; answers the duration in milliseconds. zh-CN: 从现在开始播放 16 kHz 采样; 返回时长 (毫秒). */
    fun play(samples: FloatArray): Long {
        synchronized(lock) {
            clip = samples
            clipStart = SystemClock.elapsedRealtimeNanos()
        }
        return AudioInput.millis(samples.size.toLong())
    }

    private fun frame(at: Long): FloatArray {
        val out = FloatArray(MicrophoneSource.FRAME_SAMPLES)
        synchronized(lock) {
            val current = clip ?: return out
            val offset = (at - clipStart) * AudioInput.SAMPLE_RATE / 1_000_000_000L
            for (index in out.indices) {
                val position = offset + index
                if (position >= 0 && position < current.size) out[index] = current[position.toInt()]
            }
        }
        return out
    }

    private class Capture(private val listener: MicrophoneSource.Listener) : AudioCapture {
        @Volatile
        private var running = false
        private var worker: Thread? = null

        override fun start() {
            running = true
            worker = thread(name = "MacawVirtualRoom") {
                var next = SystemClock.elapsedRealtimeNanos()
                while (running) {
                    val at = next
                    next += FRAME_NANOS
                    val wait = (next - SystemClock.elapsedRealtimeNanos()) / 1_000_000L
                    if (wait > 0) Thread.sleep(wait)
                    if (running) listener.onAudio(frame(at))
                }
            }
        }

        override fun stop() {
            running = false
            worker?.takeIf { it !== Thread.currentThread() }?.join(2_000)
        }
    }

    private const val FRAME_NANOS = 20_000_000L
}
