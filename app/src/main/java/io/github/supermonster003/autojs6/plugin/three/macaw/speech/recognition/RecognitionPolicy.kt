package io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition

import android.speech.SpeechRecognizer
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes

/** One callback of `RecognitionService.Callback`, in the order [RecognitionFlow] emits them. zh-CN: `RecognitionService.Callback` 的一次回调. */
internal sealed class RecognitionStep {
    data object ReadyForSpeech : RecognitionStep()
    data object BeginningOfSpeech : RecognitionStep()
    data class Rms(val decibels: Float) : RecognitionStep()
    data class Partial(val text: String) : RecognitionStep()
    data object EndOfSpeech : RecognitionStep()
    data class Results(val text: String, val language: String?) : RecognitionStep()
    data class Error(val code: Int) : RecognitionStep()
}

/** How a recognition request may record (roadmap D56). zh-CN: 识别请求能否录音以及如何录音 (路线图 D56). */
internal enum class RecognitionAccess {
    /** API 31+: the recording is attributed to the caller's `AttributionSource`. zh-CN: 录音归属于调用方的 `AttributionSource`. */
    ATTRIBUTED,

    /** Below API 31: the caller's uid holds `RECORD_AUDIO`; the plugin records as itself. zh-CN: 调用方 uid 持有麦克风权限; 插件以自身身份录音. */
    CHECKED,

    /** The plugin or the caller lacks the microphone permission. zh-CN: 插件或调用方缺少麦克风权限. */
    DENIED,

    /** The caller brought its own audio (`EXTRA_AUDIO_SOURCE`), which the plugin does not take. zh-CN: 调用方自带音频, 插件不支持. */
    UNSUPPORTED,
}

/**
 * The callback order of one recognition of the system recognition service (roadmap D56): `onReadyForSpeech` once the
 * microphone records, `onBeginningOfSpeech` at the first voice activity, `onRmsChanged` approximated from the VAD state
 * (the contract has no level events, as in the sheet of D42), partial results only when the caller asked for them,
 * then `onEndOfSpeech` and exactly one of `onResults` (one text) or `onError`. A cancellation reports nothing. Pure
 * Kotlin; the first usable final ends the recognition as in the voice input (D44).
 * zh-CN: 系统识别服务一次识别的回调顺序 (路线图 D56): 麦克风开始录音后 `onReadyForSpeech`, 首次语音活动时 `onBeginningOfSpeech`, `onRmsChanged`
 * 按 VAD 状态近似 (契约没有电平事件, 与 D42 的弹层相同), 调用方要求时才报部分结果, 然后 `onEndOfSpeech` 与恰好一个 `onResults` (一条文字) 或 `onError`.
 * 取消不报告任何回调. 纯 Kotlin; 与语音输入相同, 第一个可用的 final 即结束识别 (D44).
 */
internal class RecognitionFlow(private val partialResults: Boolean) {
    private var ready = false
    private var began = false
    private var lastPartial = ""

    /** True once a result, an error or a cancellation ended the recognition. zh-CN: 结果, 错误或取消结束识别后为真. */
    var done = false
        private set

    fun onReady(): List<RecognitionStep> {
        if (done || ready) return emptyList()
        ready = true
        return listOf(RecognitionStep.ReadyForSpeech, RecognitionStep.Rms(RecognitionPolicy.RMS_QUIET))
    }

    fun onVoiceActivity(active: Boolean): List<RecognitionStep> {
        if (done || !ready) return emptyList()
        val steps = ArrayList<RecognitionStep>()
        if (active && !began) {
            began = true
            steps += RecognitionStep.BeginningOfSpeech
        }
        steps += RecognitionStep.Rms(if (active) RecognitionPolicy.RMS_SPEECH else RecognitionPolicy.RMS_QUIET)
        return steps
    }

    fun onPartial(text: String): List<RecognitionStep> {
        val trimmed = text.trim()
        if (done || !partialResults || trimmed.isEmpty() || trimmed == lastPartial) return emptyList()
        lastPartial = trimmed
        return listOf(RecognitionStep.Partial(trimmed))
    }

    /** An empty final (noise) keeps listening, as in the voice input. zh-CN: 空的 final (噪声) 不结束, 与语音输入相同. */
    fun onFinal(text: String, language: String?): List<RecognitionStep> {
        val trimmed = text.trim()
        if (done || trimmed.isEmpty()) return emptyList()
        return finish(RecognitionStep.Results(trimmed, language))
    }

    /** [recording]: the microphone had started (a failure then is an audio error). zh-CN: [recording] 表示麦克风已启动 (此后的失败属于音频错误). */
    fun onError(code: String?, recording: Boolean): List<RecognitionStep> {
        if (done) return emptyList()
        return finish(RecognitionStep.Error(RecognitionPolicy.error(code, recording)))
    }

    /**
     * The session ended without a usable final: its text or the last partial is the result; otherwise no speech before
     * the timeout is `ERROR_SPEECH_TIMEOUT` and anything else `ERROR_NO_MATCH`.
     * zh-CN: 会话结束而没有可用的 final: 有文字或最后的 partial 即为结果; 否则超时前没有语音为 `ERROR_SPEECH_TIMEOUT`, 其他为 `ERROR_NO_MATCH`.
     */
    fun onEnd(reason: String?, text: String?, language: String?): List<RecognitionStep> {
        if (done) return emptyList()
        if (reason == SpeechContract.REASON_CANCELLED) {
            done = true
            return emptyList()
        }
        val heard = text?.trim()?.takeIf { it.isNotEmpty() } ?: lastPartial.takeIf { it.isNotEmpty() }
        if (heard != null) return finish(RecognitionStep.Results(heard, language))
        val code = if (reason == SpeechContract.REASON_TIMEOUT && !began) SpeechRecognizer.ERROR_SPEECH_TIMEOUT else SpeechRecognizer.ERROR_NO_MATCH
        return finish(RecognitionStep.Error(code))
    }

    /** `cancel()` of the caller: nothing more is reported. zh-CN: 调用方的 `cancel()`: 不再报告任何回调. */
    fun cancel() {
        done = true
    }

    /** A result always follows a beginning and an end of speech; an error follows the end only when speech began. zh-CN: 结果前总有语音开始与结束. */
    private fun finish(last: RecognitionStep): List<RecognitionStep> {
        done = true
        return when {
            last is RecognitionStep.Results && !began -> listOf(RecognitionStep.BeginningOfSpeech, RecognitionStep.EndOfSpeech, last)
            began -> listOf(RecognitionStep.EndOfSpeech, last)
            else -> listOf(last)
        }
    }
}

/**
 * The rules of the system recognition service (roadmap P5.7, D56). Pure Kotlin: the `SpeechRecognizer` error codes are
 * compile-time constants.
 * zh-CN: 系统识别服务的规则 (路线图 P5.7, D56). 纯 Kotlin: `SpeechRecognizer` 的错误码是编译期常量.
 */
internal object RecognitionPolicy {
    /** `onRmsChanged` while the VAD hears speech and while it does not (dB, the range Google's recognizer reports). zh-CN: 有无语音时的电平. */
    const val RMS_SPEECH = 8f
    const val RMS_QUIET = -2f

    /** API level of `AttributionSource` (`RecognitionService.Callback.getCallingAttributionSource`). zh-CN: `AttributionSource` 的 API 级别. */
    private const val ATTRIBUTION_API = 31

    /**
     * D56: the error code for a contract error: no microphone permission is `ERROR_INSUFFICIENT_PERMISSIONS`, a busy
     * microphone `ERROR_RECOGNIZER_BUSY`, a failure while recording `ERROR_AUDIO`, invalid options from the caller's
     * intent `ERROR_CLIENT`, a missing model and every other failure the service's (`ERROR_SERVER`).
     * zh-CN: D56: 契约错误码对应的错误码: 无麦克风权限为 `ERROR_INSUFFICIENT_PERMISSIONS`, 麦克风被占用为 `ERROR_RECOGNIZER_BUSY`, 录音中的失败为
     * `ERROR_AUDIO`, 调用方 Intent 导致的无效选项为 `ERROR_CLIENT`, 缺模型及其他失败属于服务端 (`ERROR_SERVER`).
     */
    fun error(code: String?, recording: Boolean): Int = when (code) {
        SpeechErrorCodes.PERMISSION_DENIED -> SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
        SpeechErrorCodes.SESSION_BUSY -> SpeechRecognizer.ERROR_RECOGNIZER_BUSY
        SpeechErrorCodes.INVALID_OPTIONS -> SpeechRecognizer.ERROR_CLIENT
        SpeechErrorCodes.ENGINE_FAILED -> if (recording) SpeechRecognizer.ERROR_AUDIO else SpeechRecognizer.ERROR_SERVER
        else -> SpeechRecognizer.ERROR_SERVER
    }

    /**
     * D56: whether and how a request records: both the plugin and the caller's uid must hold `RECORD_AUDIO` (below API 31
     * the framework only checks the service itself); API 31+ attributes the recording to the caller; audio the caller
     * brings itself is not taken.
     * zh-CN: D56: 请求能否录音以及如何录音: 插件与调用方 uid 都必须持有 `RECORD_AUDIO` (API 31 以下框架只检查服务自身); API 31+ 录音归属于调用方;
     * 不接受调用方自带的音频.
     */
    fun access(sdk: Int, selfGranted: Boolean, callerGranted: Boolean, callerAudio: Boolean): RecognitionAccess = when {
        callerAudio -> RecognitionAccess.UNSUPPORTED
        !selfGranted || !callerGranted -> RecognitionAccess.DENIED
        sdk >= ATTRIBUTION_API -> RecognitionAccess.ATTRIBUTED
        else -> RecognitionAccess.CHECKED
    }

    /** The error code a refused [access] reports. zh-CN: 被拒绝的 [access] 报告的错误码. */
    fun refusal(access: RecognitionAccess): Int? = when (access) {
        RecognitionAccess.DENIED -> SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
        RecognitionAccess.UNSUPPORTED -> SpeechRecognizer.ERROR_CLIENT
        else -> null
    }
}
