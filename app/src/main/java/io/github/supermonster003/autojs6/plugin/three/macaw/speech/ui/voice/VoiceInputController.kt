package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.LocalSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechPluginBinder
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.util.concurrent.Executors

/** State of the voice input sheet. zh-CN: 语音输入弹层的状态. */
internal enum class VoiceInputPhase { PREPARING, LOADING, LISTENING, STOPPING, PROBLEM, DONE }

/**
 * The engine side of the system voice input (roadmap P5.6): one dictation session through the plugin's own
 * `ISpeechPlugin` binder in the process, exactly the path of the tryout screen (options, error codes, the single
 * microphone), with the rules of [VoiceInputPolicy]. The first usable final ends the input (D44). Callbacks arrive on
 * the ASR thread and are posted to the main thread; [deliver] runs once with the outcome. The recognized text is never
 * logged (appendix D Q8 (a)). The digital assistant (roadmap P5.7, D48) runs the same dictation; both take the
 * microphone over from a wake session (D57) as [local] says.
 * zh-CN: 系统语音输入的引擎部分 (路线图 P5.6): 经同进程内插件自己的 `ISpeechPlugin` Binder 运行一个听写会话, 与试听试说页的路径完全相同 (选项, 错误码,
 * 唯一的麦克风), 规则见 [VoiceInputPolicy]. 第一个可用的 final 即结束输入 (D44). 回调在 ASR 线程上到达, 投递到主线程; [deliver] 以结局运行一次.
 * 识别文字从不写日志 (附录 D Q8 (a)). 数字助理 (路线图 P5.7, D48) 运行同一听写; 两者都按 [local] 从唤醒会话手中接管麦克风 (D57).
 */
internal class VoiceInputController(
    context: Context,
    private val request: VoiceInputRequest,
    private val deliver: (VoiceInputOutcome) -> Unit,
    private val local: LocalSession = LocalSession(),
) {
    private val app = context.applicationContext
    private val binder = SpeechPluginBinder(app)
    private val preferences = SpeechPreferences(app)
    private val main = Handler(Looper.getMainLooper())
    private var session: ISpeechSession? = null
    private var ready = false
    private var delivered = false
    private var generation = 0
    private var modelLanguages: List<String> = emptyList()

    var phase by mutableStateOf(VoiceInputPhase.PREPARING)
        private set
    var partial by mutableStateOf("")
        private set
    var voiceActive by mutableStateOf(false)
        private set
    var problem by mutableStateOf<VoiceInputProblem?>(null)
        private set

    /** The microphone or the model is being prepared or recorded: the sheet keeps its appearance. zh-CN: 准备或录音中. */
    val busy: Boolean get() = phase == VoiceInputPhase.PREPARING || phase == VoiceInputPhase.LOADING || phase == VoiceInputPhase.LISTENING || phase == VoiceInputPhase.STOPPING

    /**
     * Picks the model off the main thread (D45), then opens the session; also the retry after the microphone permission
     * was granted from the guidance.
     * zh-CN: 在主线程之外选模型 (D45), 然后打开会话; 也用于从引导授予麦克风权限后的重试.
     */
    fun start() {
        if (delivered || session != null) return
        val expected = ++generation
        problem = null
        partial = ""
        phase = VoiceInputPhase.PREPARING
        io.execute {
            val plan = runCatching { DictationPlan.of(app, request) }
            main.post {
                if (expected != generation || delivered) return@post
                val chosen = plan.getOrElse {
                    Log.w(TAG, "Listing the recognition models failed: ${it.javaClass.simpleName}")
                    show(VoiceInputPolicy.problem(SpeechErrorCodes.INTERNAL, recording = false))
                    return@post
                }
                if (chosen == null) {
                    show(VoiceInputPolicy.problem(SpeechErrorCodes.MODEL_MISSING, recording = false))
                } else {
                    modelLanguages = chosen.languages
                    open(chosen.options(request, preferences.vad, preferences.endpointMillis))
                }
            }
        }
    }

    /** The guidance of a refused microphone permission (D47). zh-CN: 麦克风权限被拒的引导 (D47). */
    fun microphoneDenied() = show(VoiceInputPolicy.problem(SpeechErrorCodes.PERMISSION_DENIED, recording = false))

    private fun open(options: Bundle) {
        ready = false
        phase = VoiceInputPhase.LOADING
        session = try {
            binder.openLocalSession(options, callback, local)
        } catch (e: RuntimeException) {
            // The binder throws `CODE: detail` as the contract defines. zh-CN: Binder 按契约以 `CODE: detail` 抛出.
            val code = SpeechErrorCodes.decode(e.message.orEmpty())?.first
            Log.w(TAG, "Voice input refused: ${code ?: e.javaClass.simpleName}")
            show(VoiceInputPolicy.problem(code, recording = false))
            null
        }
    }

    /** "Stop": ends the utterance now; the final of what was heard follows. zh-CN: "停止": 立即结束语句, 随后给出已听到内容的 final. */
    fun stop() {
        val current = session ?: return
        if (phase != VoiceInputPhase.LISTENING && phase != VoiceInputPhase.LOADING) return
        phase = VoiceInputPhase.STOPPING
        runCatching { current.finish() }
    }

    /** "Cancel", the scrim and the back key: no result (D42, D47). zh-CN: "取消", 遮罩与返回键: 不返回结果. */
    fun cancel() = complete(VoiceInputOutcome.Cancelled)

    /** Closing the guidance answers its cause (D47). zh-CN: 关闭引导时按原因返回 (D47). */
    fun close() {
        val shown = problem ?: return cancel()
        complete(VoiceInputOutcome.Failed(shown))
    }

    /** The sheet left the screen or was destroyed: the microphone is released at once. zh-CN: 弹层离开屏幕或被销毁: 立即释放麦克风. */
    fun release() {
        generation++
        session?.let { runCatching { it.cancel() } }
        session = null
    }

    private fun show(shown: VoiceInputProblem) {
        if (delivered) return
        session?.let { runCatching { it.cancel() } }
        session = null
        problem = shown
        phase = VoiceInputPhase.PROBLEM
        voiceActive = false
    }

    private fun complete(outcome: VoiceInputOutcome) {
        if (delivered) return
        delivered = true
        release()
        phase = VoiceInputPhase.DONE
        deliver(outcome)
    }

    private val callback = object : ISpeechSessionCallback.Stub() {
        override fun onReady(event: Bundle?) = post {
            ready = true
            if (phase == VoiceInputPhase.LOADING) phase = VoiceInputPhase.LISTENING
        }

        override fun onPartial(event: Bundle?) = post { partial = event?.getString(SpeechResultKeys.TEXT).orEmpty() }

        override fun onFinal(event: Bundle?) = post {
            // D44: the first final with text is the answer; an empty one (noise) keeps listening.
            val text = event?.getString(SpeechResultKeys.TEXT)?.trim().orEmpty()
            if (text.isNotEmpty()) complete(VoiceInputOutcome.Text(text, VoiceInputPolicy.spokenLanguage(event?.getString(SpeechResultKeys.LANGUAGE), modelLanguages)))
        }

        override fun onEndpoint() = Unit

        override fun onVoiceActivity(event: Bundle?) = post { voiceActive = event?.getBoolean(SpeechResultKeys.ACTIVE) == true }

        override fun onWarning(event: Bundle?) = post { Log.i(TAG, "Voice input warning ${event?.getString(SpeechContract.KEY_ERROR_CODE)}") }

        override fun onError(event: Bundle?) = post {
            val code = event?.getString(SpeechContract.KEY_ERROR_CODE)
            Log.w(TAG, "Voice input error $code")
            if (!delivered) show(VoiceInputPolicy.problem(code, recording = ready))
        }

        override fun onEnd(event: Bundle?) = post {
            session = null
            voiceActive = false
            if (delivered || phase == VoiceInputPhase.PROBLEM) return@post
            complete(VoiceInputPolicy.onEnd(event?.getString(SpeechResultKeys.REASON), event?.getString(SpeechResultKeys.TEXT), partial, modelLanguages))
        }
    }

    private fun post(block: () -> Unit) {
        val expected = generation
        main.post { if (expected == generation) block() }
    }

    internal companion object {
        private const val TAG = "MacawVoiceInput"

        /** Model choice off the main thread, shared with the recognition service. zh-CN: 在主线程之外选模型, 与识别服务共用. */
        val io = Executors.newSingleThreadExecutor { Thread(it, "MacawVoiceInput").apply { isDaemon = true } }
    }
}

/**
 * The model of one dictation of the voice input, the assistant or the recognition service (D45, D46): [modelId] null
 * leaves the default to the runtime; [hotwords] says whether the biasing strings reach the model; [languages] are the
 * model's languages (D50).
 * zh-CN: 语音输入, 数字助理或识别服务一次听写所用的模型 (D45, D46): [modelId] 为 null 时交给运行时的默认模型; [hotwords] 表示偏置词能否传给模型;
 * [languages] 为模型的语言 (D50).
 */
internal data class DictationPlan(val modelId: String?, val hotwords: Boolean, val languages: List<String>) {

    /**
     * The session options (D44 - D46): the endpoint of the settings screen (D35) unless the caller gives its silence, the
     * no-speech timeout and the utterance limit of D44, the biasing strings as hotwords when the model takes them, and no
     * language (SenseVoice detects it, D45).
     * zh-CN: 会话选项 (D44 - D46): 设置页的端点 (D35), 调用方给出静音时长时以之为先, D44 的无语音超时与单句上限, 模型支持时把偏置词作为热词, 不传
     * 语言 (SenseVoice 自行检测, D45).
     */
    fun options(request: VoiceInputRequest, vad: Boolean, endpointMillis: Long): Bundle = Bundle().apply {
        modelId?.let { putString(SpeechOptionKeys.MODEL, it) }
        putBoolean(SpeechOptionKeys.VAD, vad)
        putLong(SpeechOptionKeys.SILENCE_END_MILLIS, VoiceInputPolicy.silence(request.silenceMillis, endpointMillis))
        putLong(SpeechOptionKeys.TIMEOUT_MILLIS, VoiceInputPolicy.NO_SPEECH_MILLIS)
        putLong(SpeechOptionKeys.MAX_DURATION_MILLIS, VoiceInputPolicy.MAX_UTTERANCE_MILLIS)
        if (hotwords) putStringArray(SpeechOptionKeys.HOTWORDS, request.biasing.toTypedArray())
    }

    companion object {
        /** Null when no recognition model is installed; reads the model store (not on the main thread). zh-CN: 未安装识别模型时为 null; 读取模型存储. */
        fun of(context: Context, request: VoiceInputRequest): DictationPlan? {
            val runtime = SpeechRuntime.get(context)
            val models = runtime.models.list(kind = SpeechContract.KIND_ASR, installed = true)
            if (models.isEmpty()) return null
            val modelId = VoiceInputPolicy.chooseModel(models, request.language)
            val model = modelId?.let { runtime.models.store.installed(it) } ?: runtime.models.defaultModel(SpeechContract.KIND_ASR)
            // D46: biasing strings only reach a transducer with bpe.vocab; other models get none (and no warning).
            return DictationPlan(modelId, request.biasing.isNotEmpty() && model != null && runtime.supportsHotwords(model), model?.languages.orEmpty())
        }
    }
}

/**
 * The settings switch "use as the system voice input" (roadmap D43): the component state of [VoiceInputActivity] is the
 * setting itself, enabled by default as declared in the Manifest.
 * zh-CN: 设置开关 "作为系统语音输入" (路线图 D43): [VoiceInputActivity] 的组件状态即设置本身, 按 Manifest 默认启用.
 */
internal object VoiceInputComponent {
    fun component(context: Context) = ComponentName(context, VoiceInputActivity::class.java)

    fun isEnabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(component(context)) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    fun setEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(component(context), state, PackageManager.DONT_KILL_APP)
    }
}
