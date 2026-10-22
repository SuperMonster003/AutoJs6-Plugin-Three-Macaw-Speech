package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.tool.api.PluginToolCall
import org.autojs.plugin.tool.api.PluginToolResult
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * `speech_speak` (roadmap P7.6.3, D96, D100): reads text aloud through the Android `TextToSpeech` API, 3-Macaw Speech's
 * own engine when one of its voices speaks the language (as the read-aloud of the resident wake word does), else the
 * system default engine, as assistant audio. The rules and the waiting live in [SpeakRules] and [Speaker]; this class
 * connects them to `TextToSpeech`. The text is never logged (only its length).
 * zh-CN: `speech_speak` (路线图 P7.6.3, D96, D100): 经 Android `TextToSpeech` API 朗读文字; 3-Macaw Speech 有语音说该语言时用它自己的引擎 (与常驻
 * 唤醒的朗读相同), 否则用系统默认引擎, 以助理音频播放. 规则与等待在 [SpeakRules] 与 [Speaker] 中; 本类把它们接到 `TextToSpeech`. 文字从不写日志
 * (只记长度).
 */
internal class SpeechSpeakTool(context: Context) {
    private val app = context.applicationContext
    private val runtime = SpeechRuntime.get(app)
    private val outputs = TextToSpeechOutputs(app)

    fun call(call: PluginToolCall, request: SpeakRequest): PluginToolResult {
        val deadline = now() + (call.timeoutMs.takeIf { it > 0 } ?: SpeechToolCatalog.LONG_TIMEOUT_MS)
        val plan = SpeakRules.plan(request, runtime.voiceTable())
        val report = speaker.speak(outputs, request, plan, deadline, object : ToolCancellation {
            override val cancelled: Boolean get() = call.isCancelled

            override fun onCancel(hook: Runnable) = call.onCancel(hook)
        })
        val state = when {
            report.completed -> "read"
            request.wait -> "interrupted"
            else -> "started"
        }
        Log.i(TAG, "Speak: ${request.text.length} chars in ${report.language} with ${report.enginePackage}, $state${report.durationMillis?.let { " in $it ms" }.orEmpty()}")
        return PluginToolResult(String(StrictJson.write(SpeakRules.result(report, app.packageName)), Charsets.UTF_8).trimEnd())
    }

    private companion object {
        const val TAG = "MacawTools"

        /** One reading at a time in the process, also after a call that did not wait returned. zh-CN: 进程内同一时刻一个朗读. */
        val speaker = Speaker(::now)

        fun now(): Long = SystemClock.elapsedRealtime()
    }
}

/**
 * [SpeechOutputs] on `TextToSpeech`: a new connection per call, shut down when its reading ends. The three-argument
 * constructor falls back to the default engine only when the named one cannot be bound, which does not happen to the
 * plugin's own engine in its own process.
 * zh-CN: 基于 `TextToSpeech` 的 [SpeechOutputs]: 每次调用一个新连接, 朗读结束时关闭. 三参数构造函数只在指定引擎无法绑定时退回默认引擎, 插件自己进程中
 * 的自己引擎不会如此.
 */
private class TextToSpeechOutputs(private val app: Context) : SpeechOutputs {
    override val ownPackage: String = app.packageName

    override fun open(enginePackage: String?, timeoutMillis: Long): SpeechOutputs.Opened {
        val status = AtomicInteger(PENDING)
        val ready = CountDownLatch(1)
        val tts = TextToSpeech(app, { status.set(it); ready.countDown() }, enginePackage)
        val answered = ready.await(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!answered || status.get() != TextToSpeech.SUCCESS) {
            runCatching { tts.shutdown() }
            return SpeechOutputs.Opened.Refused(if (answered) "it did not start (${status.get()})" else "it did not start within $timeoutMillis ms")
        }
        return SpeechOutputs.Opened.Ready(TextToSpeechOutput(tts, enginePackage ?: runCatching { tts.defaultEngine }.getOrNull().orEmpty()))
    }

    private companion object {
        const val PENDING = Int.MIN_VALUE
    }
}

private class TextToSpeechOutput(private val tts: TextToSpeech, override val enginePackage: String) : SpeechOutput {

    override fun language(tag: String): Int = runCatching { tts.setLanguage(Locale.forLanguageTag(tag)) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)

    override fun voice(name: String): Boolean {
        val voice = runCatching { tts.voices }.getOrNull()?.firstOrNull { it.name == name } ?: return false
        return tts.setVoice(voice) == TextToSpeech.SUCCESS
    }

    override fun rate(speed: Float) {
        tts.setSpeechRate(speed)
    }

    override fun speak(text: String, listener: SpeakListener): Boolean {
        // USAGE_ASSISTANT exists since Android 8.0; older versions play it as media.
        val usage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) AudioAttributes.USAGE_ASSISTANT else AudioAttributes.USAGE_MEDIA
        tts.setAudioAttributes(AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = listener.started()

            override fun onDone(utteranceId: String?) = listener.done()

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = listener.failed(TextToSpeech.ERROR)

            override fun onError(utteranceId: String?, errorCode: Int) = listener.failed(errorCode)

            override fun onStop(utteranceId: String?, interrupted: Boolean) = listener.stopped()
        })
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE) == TextToSpeech.SUCCESS
    }

    override fun isSpeaking(): Boolean = runCatching { tts.isSpeaking }.getOrDefault(false)

    override fun stop() {
        runCatching { tts.stop() }
    }

    override fun shutdown() {
        runCatching { tts.shutdown() }
    }

    private companion object {
        const val UTTERANCE = "macaw-speech-speak"
    }
}
