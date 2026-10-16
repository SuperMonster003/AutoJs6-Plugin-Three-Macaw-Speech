package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ttsengine

import android.content.ComponentCallbacks2
import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.Pcm16
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsSpeed
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsSynthesis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Android system TTS engine of 3-Macaw Speech (roadmap D3, D19, P4.2): AutoJs6's `tts` module (`engine` option),
 * `speech.speak` and any other app reach the installed TTS models through it. Voices come from [TtsVoiceTable]
 * (`<modelId>/<speakerId>` in the model's first language, `<modelId>/<speakerId>/<languageTag>` in its others); a
 * request without a voice gets the default voice of its language. [onSynthesizeText] runs on the thread Android gives
 * it, loads the model through the shared engine cache (the first request after an idle release waits for the load),
 * synthesizes sentence by sentence and streams PCM16 at the model's sample rate through [SynthesisCallback]. The speech
 * rate maps to the sherpa-onnx speed ([TtsSpeed]); pitch has no equivalent and is ignored. [onStop] stops the current
 * synthesis within one sentence. Without an installed model every language is `LANG_MISSING_DATA` (when a catalog model
 * speaks it) and a synthesis fails with `ERROR_NOT_INSTALLED_YET`. The service is exported without a permission:
 * Android's `TextToSpeech` binds it from the client app's own process.
 * zh-CN: 3-Macaw Speech 的 Android 系统 TTS 引擎 (路线图 D3, D19, P4.2): AutoJs6 的 `tts` 模块 (`engine` 选项), `speech.speak` 与其他应用经它使用已安装的
 * TTS 模型. 语音来自 [TtsVoiceTable] (模型首个语言下为 `<modelId>/<speakerId>`, 其他语言下为 `<modelId>/<speakerId>/<languageTag>`); 未指定语音的请求
 * 使用其语言的默认语音. [onSynthesizeText] 在 Android 分配的线程上运行, 经共享的引擎缓存加载模型 (空闲释放后的首个请求需等待加载), 逐句合成并经
 * [SynthesisCallback] 以模型采样率流式传回 PCM16. 语速映射为 sherpa-onnx 的 speed ([TtsSpeed]); 音高没有对应参数, 被忽略. [onStop] 在一句之内停止
 * 当前合成. 没有已安装模型时, 每种语言均为 `LANG_MISSING_DATA` (有目录模型支持时), 合成以 `ERROR_NOT_INSTALLED_YET` 失败. 服务无权限导出: Android 的
 * `TextToSpeech` 在客户端应用自己的进程中绑定它.
 */
class ThreeMacawSpeechTtsEngineService : TextToSpeechService() {

    private val runtime: SpeechRuntime by lazy { SpeechRuntime.get(this) }

    /** Increments on every [onStop]; a synthesis stops when it changed. zh-CN: 每次 [onStop] 递增; 发生变化时合成停止. */
    private val stops = AtomicInteger(0)

    @Volatile
    private var language: Array<String>? = null

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int = table().availability(lang, country)

    override fun onGetLanguage(): Array<String> {
        language?.let { return it }
        val voice = table().defaultVoice(null, null)
        if (voice != null) return arrayOf(voice.iso3Language, voice.iso3Country, "")
        val locale = Locale.getDefault()
        return arrayOf(TtsVoiceTable.iso3Language(locale.language) ?: "", TtsVoiceTable.iso3Country(locale.country) ?: "", "")
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val table = table()
        val availability = table.availability(lang, country)
        if (availability >= TextToSpeech.LANG_AVAILABLE) {
            val voice = table.defaultVoice(lang, country)
            language = arrayOf(voice?.iso3Language ?: lang.orEmpty(), if (availability == TextToSpeech.LANG_COUNTRY_AVAILABLE) voice?.iso3Country.orEmpty() else "", "")
            voice?.let(::preload)
        }
        return availability
    }

    override fun onGetFeaturesForLanguage(lang: String?, country: String?, variant: String?): Set<String> = emptySet()

    override fun onGetVoices(): List<Voice> = table().voices.map { voice ->
        Voice(voice.name, Locale.forLanguageTag(voice.languageTag), voice.quality, Voice.LATENCY_NORMAL, false, emptySet())
    }

    override fun onIsValidVoiceName(voiceName: String?): Int = if (voiceName != null && table().voice(voiceName) != null) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    override fun onLoadVoice(voiceName: String?): Int {
        val voice = voiceName?.let { table().voice(it) } ?: return TextToSpeech.ERROR
        preload(voice)
        return TextToSpeech.SUCCESS
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? = table().defaultVoice(lang, country)?.name

    override fun onStop() {
        stops.incrementAndGet()
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val generation = stops.get()
        val text = request.charSequenceText?.toString().orEmpty()
        val table = table()
        val voice = request.voiceName?.takeIf { it.isNotEmpty() }?.let(table::voice)
            ?: table.defaultVoice(request.language, request.country)
        val model = voice?.let { runtime.ttsModels().firstOrNull { it.modelId == voice.modelId } }
        if (voice == null || model == null) {
            Log.w(TAG, "No installed voice for ${request.voiceName.orEmpty().ifEmpty { "${request.language}-${request.country}" }}")
            callback.error(TextToSpeech.ERROR_NOT_INSTALLED_YET)
            return
        }
        if (request.pitch != 100 && pitchNoticed.compareAndSet(false, true)) Log.i(TAG, "Pitch ${request.pitch} is ignored: sherpa-onnx models have no pitch control")
        try {
            runtime.acquireTts(model).use { lease ->
                val engine = lease.engine
                if (callback.start(engine.sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1) != TextToSpeech.SUCCESS) return
                val bytes = ByteArray(maxOf(2, callback.maxBufferSize) and 1.inv())
                val outcome = TtsSynthesis.run(
                    generator = engine,
                    text = text,
                    speakerId = voice.speakerId.takeIf { it < engine.speakerCount } ?: 0,
                    speed = TtsSpeed.fromAndroidRate(request.speechRate),
                    isCancelled = { stops.get() != generation },
                    onProgress = { _, _ -> },
                    sink = { samples -> deliver(callback, samples, bytes) },
                )
                if (!outcome.cancelled) callback.done()
                // No text in the log: only its length, the voice and the rate (roadmap appendix D Q8 keeps content out of logs).
                Log.i(TAG, "${if (outcome.cancelled) "Stopped" else "Synthesized"} ${text.length} chars with ${voice.name} at rate ${request.speechRate} (speed ${TtsSpeed.fromAndroidRate(request.speechRate)}), pitch ${request.pitch}: ${outcome.samples * 1000 / engine.sampleRate} ms audio")
            }
        } catch (e: SpeechException) {
            Log.w(TAG, "Synthesis with ${voice.name} failed: ${e.code} ${e.message}")
            callback.error(TextToSpeech.ERROR_SYNTHESIS)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Synthesis with ${voice.name} failed", e)
            callback.error(TextToSpeech.ERROR_SYNTHESIS)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Idle models are the largest allocations of the process; the next request loads its model again.
        @Suppress("DEPRECATION")
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) runtime.engines.trim()
    }

    /** Streams [samples] as PCM16 in pieces the callback accepts; false once the framework stopped the request. zh-CN: 分块传回 PCM16. */
    private fun deliver(callback: SynthesisCallback, samples: FloatArray, bytes: ByteArray): Boolean {
        var offset = 0
        while (offset < samples.size) {
            val count = minOf(samples.size - offset, bytes.size / 2)
            val length = Pcm16.encode(samples, offset, count, bytes)
            if (callback.audioAvailable(bytes, 0, length) != TextToSpeech.SUCCESS) return false
            offset += count
        }
        return true
    }

    private fun preload(voice: TtsVoice) {
        runtime.ttsModels().firstOrNull { it.modelId == voice.modelId }?.let(runtime::preloadTts)
    }

    private fun table(): TtsVoiceTable = runtime.voiceTable()

    private companion object {
        const val TAG = "MacawTtsEngine"
        val pitchNoticed = AtomicBoolean(false)
    }
}
