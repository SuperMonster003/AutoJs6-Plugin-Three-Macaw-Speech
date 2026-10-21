package io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Roadmap D69: reads the recognized text of a sheet opened by the wake word, through the Android `TextToSpeech` API with
 * 3-Macaw Speech's own engine when one of its TTS models speaks the language (its per-language default voice, D19), else
 * the system default engine; nothing is read when neither can. Main thread. The text is never logged.
 * zh-CN: 路线图 D69: 朗读由唤醒词打开的弹层的识别文字, 经 Android `TextToSpeech` API; 有 TTS 模型说该语言时用 3-Macaw Speech 自己的引擎 (按语言的
 * 默认语音, D19), 否则用系统默认引擎; 都不能时不朗读. 主线程. 文字从不写日志.
 */
internal class ReadAloud(context: Context) {

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var enginePackage: String? = null
    private var generation = 0
    private var closed = false

    /** Reads [text] in [language] (BCP 47 or null), replacing anything still being read. zh-CN: 以 [language] 朗读 [text], 取代正在朗读的内容. */
    fun speak(text: String, language: String?) {
        stop()
        if (closed || text.isBlank()) return
        val expected = ++generation
        val tag = ReadAloudPolicy.language(language, text)
        io.execute {
            val own = runCatching { ReadAloudPolicy.ownEngine(SpeechRuntime.get(app).ttsModels().map { it.languages }, tag) }.getOrDefault(false)
            main.post { if (expected == generation && !closed) start(text, tag, if (own) app.packageName else null, expected) }
        }
    }

    private fun start(text: String, tag: String, wanted: String?, expected: Int) {
        val current = engine
        if (current != null && enginePackage == wanted) return say(current, text, tag, wanted)
        current?.shutdown()
        engine = null
        enginePackage = wanted
        var created: TextToSpeech? = null
        created = TextToSpeech(app, { status ->
            main.post {
                val tts = created ?: return@post
                if (expected != generation || closed || engine !== tts) return@post
                if (status == TextToSpeech.SUCCESS) {
                    say(tts, text, tag, wanted)
                } else if (wanted != null) {
                    // The plugin's engine failed to start: the system default engine reads instead.
                    Log.w(TAG, "Read aloud: the plugin's engine did not start ($status), trying the default engine")
                    start(text, tag, null, expected)
                } else {
                    Log.w(TAG, "Read aloud: the default engine did not start ($status)")
                }
            }
        }, wanted)
        engine = created
    }

    private fun say(tts: TextToSpeech, text: String, tag: String, wanted: String?) {
        // USAGE_ASSISTANT exists since Android 8.0; older versions read it as media.
        val usage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) AudioAttributes.USAGE_ASSISTANT else AudioAttributes.USAGE_MEDIA
        tts.setAudioAttributes(AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        val available = runCatching { tts.setLanguage(Locale.forLanguageTag(tag)) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (available < TextToSpeech.LANG_AVAILABLE) {
            Log.i(TAG, "Read aloud: ${wanted ?: "the default engine"} cannot speak $tag ($available)")
            return
        }
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE)
        Log.i(TAG, "Read aloud: ${text.length} chars in $tag with ${wanted ?: "the default engine"} ($result)")
    }

    /** Stops reading (the sheet closes, speaks again, sends or copies). zh-CN: 停止朗读 (弹层关闭, 重说, 发送或复制). */
    fun stop() {
        generation++
        runCatching { engine?.stop() }
    }

    fun shutdown() {
        closed = true
        stop()
        runCatching { engine?.shutdown() }
        engine = null
    }

    private companion object {
        const val TAG = "MacawResident"
        const val UTTERANCE = "macaw-read-aloud"
        val io = Executors.newSingleThreadExecutor { Thread(it, "MacawReadAloud").apply { isDaemon = true } }
    }
}
