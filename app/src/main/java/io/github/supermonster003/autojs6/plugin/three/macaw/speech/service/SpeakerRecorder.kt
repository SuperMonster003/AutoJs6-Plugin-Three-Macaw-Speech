package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.content.Context
import android.os.Process
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioCaptures
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MicrophoneSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Records the microphone for the speaker screen (roadmap D140): a microphone place of the session table, so the
 * resident wake word and other wake sessions are silenced while it records (D57, D76), and the plugin's own capture at
 * 16 kHz. Blocks the calling worker thread for [millis] at most; [cancelled] ends it early with what was recorded. The
 * caller checked `RECORD_AUDIO` (a missing permission fails with `PERMISSION_DENIED`).
 * zh-CN: 为说话人页面录音 (路线图 D140): 占用会话表的麦克风位置, 录音期间常驻唤醒与其他唤醒会话被静默 (D57, D76), 使用插件自己的 16 kHz 采集. 阻塞调用方
 * 工作线程至多 [millis]; [cancelled] 使其提前结束并返回已录到的音频. 调用方已检查 `RECORD_AUDIO` (缺少权限时为 `PERMISSION_DENIED`).
 */
internal object SpeakerRecorder {

    fun record(context: Context, millis: Long, progress: (Float) -> Unit = {}, cancelled: () -> Boolean = { false }): FloatArray {
        val runtime = SpeechRuntime.get(context)
        runtime.requireMicrophone()
        val slot = runtime.sessions.reserve(Process.myUid(), microphone = true, takeover = true)
        try {
            val total = AudioInput.samples(millis).toInt()
            val buffer = FloatArray(total)
            val lock = Any()
            var size = 0
            var failure: SpeechException? = null
            val done = CountDownLatch(1)
            val capture = AudioCaptures.open(object : MicrophoneSource.Listener {
                override fun onAudio(samples: FloatArray) {
                    val recorded = synchronized(lock) {
                        val count = minOf(samples.size, total - size)
                        System.arraycopy(samples, 0, buffer, size, count)
                        size += count
                        size
                    }
                    progress(recorded.toFloat() / total)
                    if (recorded >= total) done.countDown()
                }

                override fun onSilenced(silenced: Boolean) = Unit

                override fun onFailure(error: SpeechException) {
                    failure = error
                    done.countDown()
                }
            }, null)
            capture.start()
            try {
                while (!done.await(POLL_MILLIS, TimeUnit.MILLISECONDS)) {
                    if (cancelled()) break
                }
            } finally {
                capture.stop()
            }
            failure?.let { throw it }
            return synchronized(lock) { buffer.copyOf(size) }
        } finally {
            runtime.sessions.release(slot)
        }
    }

    private const val POLL_MILLIS = 50L
}
