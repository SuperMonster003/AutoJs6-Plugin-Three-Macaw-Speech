package io.github.supermonster003.autojs6.plugin.three.macaw.speech.recognition

import android.Manifest
import android.content.Context
import android.content.ContextParams
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.speech.RecognitionService
import android.speech.RecognitionSupport
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.LocalSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechPluginBinder
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.DictationPlan
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputController
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputPolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputRequest
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.readVoiceInputRequest
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechResultKeys

/**
 * The system recognition service (roadmap P5.7, D56): the default `SpeechRecognizer` service once 3-Macaw Speech is the
 * digital assistant (the voice interaction service names it), and reachable by any app that names this component. One
 * recognition at a time; each runs one dictation of the voice input path (model choice D45, biasing strings D46, the
 * first usable final D44) through the in-process binder, takes the microphone over from a wake session (D57) and
 * records without the microphone foreground service (the caller is visible; as the default service the system binds it
 * with the caller's capabilities). The recognized text is never logged.
 * zh-CN: 系统识别服务 (路线图 P5.7, D56): 3-Macaw Speech 成为数字助理后即为默认的 `SpeechRecognizer` 服务 (语音交互服务声明了它), 任何应用也可显式
 * 指定本组件. 同一时刻一次识别; 每次经进程内 Binder 运行语音输入路径的一次听写 (选模型 D45, 偏置词 D46, 第一个可用 final 即结束 D44), 从唤醒会话手中
 * 接管麦克风 (D57), 录音时不使用麦克风前台服务 (调用方可见; 作为默认服务时系统绑定它并传递能力). 识别文字从不写日志.
 */
class MacawRecognitionService : RecognitionService() {

    private var run: RecognitionRun? = null

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback) {
        run?.cancel()
        val callerAudio = recognizerIntent?.hasExtra(EXTRA_AUDIO_SOURCE) == true
        val access = RecognitionPolicy.access(Build.VERSION.SDK_INT, granted(android.os.Process.myUid()), granted(listener.callingUid), callerAudio)
        RecognitionPolicy.refusal(access)?.let { code ->
            Log.w(TAG, "Recognition refused for uid ${listener.callingUid}: $access")
            runCatching { listener.error(code) }
            return
        }
        val attribution = if (access == RecognitionAccess.ATTRIBUTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) attributed(listener) else null
        val partialResults = recognizerIntent?.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) == true
        run = RecognitionRun(this, readVoiceInputRequest(recognizerIntent), partialResults, listener, attribution) { done -> if (run === done) run = null }.also { it.start() }
    }

    override fun onStopListening(listener: Callback) {
        run?.stop()
    }

    override fun onCancel(listener: Callback) {
        run?.cancel()
        run = null
    }

    override fun onDestroy() {
        run?.cancel()
        run = null
        super.onDestroy()
    }

    /**
     * API 33: the languages of the installed recognition models, all on the device; nothing is downloaded on request (the
     * home screen asks for the license and the network, roadmap D11 / D34).
     * zh-CN: API 33: 已安装识别模型的语言, 均在设备上; 不按请求下载 (首页需要确认许可与网络, 路线图 D11 / D34).
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCheckRecognitionSupport(recognizerIntent: Intent, supportCallback: SupportCallback) {
        val app = applicationContext
        VoiceInputController.io.execute {
            val languages = runCatching {
                SpeechModels.get(app).list(kind = SpeechContract.KIND_ASR, installed = true).flatMap { it.languages }.mapNotNull(VoiceInputPolicy::language).distinct()
            }
            main.post {
                languages.fold(
                    { runCatching { supportCallback.onSupportResult(RecognitionSupport.Builder().setInstalledOnDeviceLanguages(it).build()) } },
                    { runCatching { supportCallback.onError(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT) } },
                )
            }
        }
    }

    private fun granted(uid: Int): Boolean = checkPermission(Manifest.permission.RECORD_AUDIO, -1, uid) == PackageManager.PERMISSION_GRANTED

    /** API 31+: the recording is attributed to the caller (D56). zh-CN: 录音归属于调用方 (D56). */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun attributed(listener: Callback): Context? = runCatching {
        createContext(ContextParams.Builder().setNextAttributionSource(listener.callingAttributionSource).build())
    }.onFailure { Log.w(TAG, "No attribution context: ${it.javaClass.simpleName}") }.getOrNull()

    private companion object {
        const val TAG = "MacawRecognition"

        /** `RecognizerIntent.EXTRA_AUDIO_SOURCE` (API 33), read on every API level. zh-CN: 在所有 API 级别上读取. */
        const val EXTRA_AUDIO_SOURCE = "android.speech.extra.AUDIO_SOURCE"

        val main = Handler(Looper.getMainLooper())
    }
}

/**
 * One recognition of [MacawRecognitionService]: the dictation session and the [RecognitionFlow] that turns its events
 * into callbacks, all on the main thread. Ends once (result, error or cancellation) and then calls [onDone].
 * zh-CN: [MacawRecognitionService] 的一次识别: 听写会话以及把其事件转为回调的 [RecognitionFlow], 全部在主线程上. 只结束一次 (结果, 错误或取消), 随后
 * 调用 [onDone].
 */
private class RecognitionRun(
    context: Context,
    private val request: VoiceInputRequest,
    partialResults: Boolean,
    private val listener: RecognitionService.Callback,
    private val attribution: Context?,
    private val onDone: (RecognitionRun) -> Unit,
) {
    private val app = context.applicationContext
    private val binder = SpeechPluginBinder(app)
    private val preferences = SpeechPreferences(app)
    private val main = Handler(Looper.getMainLooper())
    private val flow = RecognitionFlow(partialResults)
    private var session: ISpeechSession? = null
    private var recording = false
    private var stopRequested = false
    private var released = false
    private var modelLanguages: List<String> = emptyList()

    fun start() {
        VoiceInputController.io.execute {
            val plan = runCatching { DictationPlan.of(app, request) }
            main.post {
                if (released) return@post
                val chosen = plan.getOrElse {
                    Log.w(TAG, "Listing the recognition models failed: ${it.javaClass.simpleName}")
                    deliver(flow.onError(SpeechErrorCodes.INTERNAL, recording = false))
                    return@post
                }
                if (chosen == null) {
                    deliver(flow.onError(SpeechErrorCodes.MODEL_MISSING, recording = false))
                    return@post
                }
                modelLanguages = chosen.languages
                // D56: the VAD drives onBeginningOfSpeech and onRmsChanged, so it is always on here.
                open(chosen.options(request, vad = true, endpointMillis = preferences.endpointMillis))
            }
        }
    }

    private fun open(options: Bundle) {
        session = try {
            binder.openLocalSession(options, callback, LocalSession(takeover = true, foreground = false, attribution = attribution))
        } catch (e: RuntimeException) {
            val code = SpeechErrorCodes.decode(e.message.orEmpty())?.first
            Log.w(TAG, "Recognition refused: ${code ?: e.javaClass.simpleName}")
            deliver(flow.onError(code, recording = false))
            null
        }
        if (stopRequested) session?.let { runCatching { it.finish() } }
    }

    /** `stopListening`: the final of what was heard follows. zh-CN: `stopListening`: 随后给出已听到内容的 final. */
    fun stop() {
        stopRequested = true
        session?.let { runCatching { it.finish() } }
    }

    fun cancel() {
        flow.cancel()
        release()
    }

    private fun release() {
        if (released) return
        released = true
        session?.let { runCatching { it.cancel() } }
        session = null
        onDone(this)
    }

    private fun deliver(steps: List<RecognitionStep>) {
        try {
            steps.forEach { step ->
                when (step) {
                    RecognitionStep.ReadyForSpeech -> listener.readyForSpeech(Bundle())
                    RecognitionStep.BeginningOfSpeech -> listener.beginningOfSpeech()
                    is RecognitionStep.Rms -> listener.rmsChanged(step.decibels)
                    is RecognitionStep.Partial -> listener.partialResults(results(step.text))
                    RecognitionStep.EndOfSpeech -> listener.endOfSpeech()
                    is RecognitionStep.Results -> listener.results(results(step.text))
                    is RecognitionStep.Error -> listener.error(step.code)
                }
            }
        } catch (e: RemoteException) {
            Log.w(TAG, "The recognition client is gone")
            flow.cancel()
        }
        if (flow.done) release()
        steps.lastOrNull()?.let { last -> if (last is RecognitionStep.Results || last is RecognitionStep.Error) Log.i(TAG, "Recognition ended: ${describe(last)}") }
    }

    /** One text (D46); the text itself is never logged. zh-CN: 一条文字 (D46); 文字本身从不写日志. */
    private fun results(text: String): Bundle = Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text)) }

    private fun describe(step: RecognitionStep): String = when (step) {
        is RecognitionStep.Results -> "results (${step.text.length} chars)"
        is RecognitionStep.Error -> "error ${step.code}"
        else -> step.toString()
    }

    private val callback = object : ISpeechSessionCallback.Stub() {
        override fun onReady(event: Bundle?) = post {
            recording = true
            deliver(flow.onReady())
        }

        override fun onPartial(event: Bundle?) = post { deliver(flow.onPartial(event?.getString(SpeechResultKeys.TEXT).orEmpty())) }

        override fun onFinal(event: Bundle?) = post {
            val language = VoiceInputPolicy.spokenLanguage(event?.getString(SpeechResultKeys.LANGUAGE), modelLanguages)
            deliver(flow.onFinal(event?.getString(SpeechResultKeys.TEXT).orEmpty(), language))
        }

        override fun onEndpoint() = Unit

        override fun onVoiceActivity(event: Bundle?) = post { deliver(flow.onVoiceActivity(event?.getBoolean(SpeechResultKeys.ACTIVE) == true)) }

        override fun onWarning(event: Bundle?) = post { Log.i(TAG, "Recognition warning ${event?.getString(SpeechContract.KEY_ERROR_CODE)}") }

        override fun onError(event: Bundle?) = post {
            val code = event?.getString(SpeechContract.KEY_ERROR_CODE)
            Log.w(TAG, "Recognition error $code")
            deliver(flow.onError(code, recording))
        }

        override fun onEnd(event: Bundle?) = post {
            session = null
            deliver(flow.onEnd(event?.getString(SpeechResultKeys.REASON), event?.getString(SpeechResultKeys.TEXT), VoiceInputPolicy.spokenLanguage(null, modelLanguages)))
            release()
        }
    }

    private fun post(block: () -> Unit) {
        main.post { if (!released) block() }
    }

    private companion object {
        const val TAG = "MacawRecognition"
    }
}
