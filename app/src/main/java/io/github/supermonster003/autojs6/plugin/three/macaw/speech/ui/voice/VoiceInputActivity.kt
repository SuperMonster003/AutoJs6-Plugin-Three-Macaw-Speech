package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity

/**
 * The system voice input (roadmap P5.6, D41 - D47): answers `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` of any app with a
 * bottom sheet over the caller's screen and returns the recognized text in `EXTRA_RESULTS` only. It is exported
 * without a permission (3-Stove Agent's microphone button, for one, starts it) and has no other effect: no audio, no
 * model or setting changes, no downloads. Leaving the screen ends the input and releases the microphone. A wake session
 * that holds the microphone yields it while the sheet listens (roadmap D57).
 * zh-CN: 系统语音输入 (路线图 P5.6, D41 - D47): 以覆盖在调用方界面上的底部弹层应答任何应用的 `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, 只经
 * `EXTRA_RESULTS` 返回识别文字. 无权限导出 (例如 3-Stove Agent 的麦克风按钮会启动它), 没有其他作用: 不返回音频, 不改模型或设置, 不下载. 离开界面即结束输入并
 * 释放麦克风. 弹层收音期间, 占用麦克风的唤醒会话会让出麦克风 (路线图 D57).
 */
class VoiceInputActivity : StandaloneActivity() {

    private lateinit var voice: VoiceInputController
    private var request = VoiceInputRequest()
    private var awaitingPermission = false

    override val translucentWindow: Boolean get() = true

    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        awaitingPermission = false
        if (granted) voice.start() else voice.microphoneDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        request = readVoiceInputRequest(intent)
        voice = VoiceInputController(this, request, ::deliver)
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) { dismiss() }
        begin()
    }

    override fun onResume() {
        super.onResume()
        // Back from the app settings with the permission granted: listen now (D47). zh-CN: 从应用设置授权返回后立即收音.
        if (voice.problem?.kind == VoiceInputProblemKind.MICROPHONE && microphoneGranted()) voice.start()
    }

    override fun onStop() {
        // The input belongs to the visible sheet: leaving it ends the input (the guidance waits for the settings).
        if (!isChangingConfigurations && !awaitingPermission && voice.busy) voice.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        voice.release()
        super.onDestroy()
    }

    override fun holdsAppearance(): Boolean = voice.busy

    private fun microphoneGranted() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun begin() {
        if (microphoneGranted()) {
            voice.start()
        } else {
            // D47: the system permission dialog right in the sheet. zh-CN: D47: 直接在弹层中弹出系统权限对话框.
            awaitingPermission = true
            runCatching { microphone.launch(Manifest.permission.RECORD_AUDIO) }.onFailure {
                awaitingPermission = false
                voice.microphoneDenied()
            }
        }
    }

    private fun dismiss() = if (voice.problem != null) voice.close() else voice.cancel()

    private fun deliver(outcome: VoiceInputOutcome) {
        val code = VoiceInputPolicy.resultCode(outcome)
        val text = (outcome as? VoiceInputOutcome.Text)?.text
        // D46: one result, whatever EXTRA_MAX_RESULTS asks; no confidence scores. The text itself is never logged.
        setResult(code, text?.let { Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf(it)) })
        Log.i(TAG, "Voice input result $code (${text?.length ?: 0} chars)")
        finish()
    }

    @Composable
    override fun Content() {
        VoiceSheet(onDismiss = ::dismiss) {
            val problem = voice.problem
            if (problem != null) {
                VoiceProblem(problem, onClose = voice::close, action = problemAction(problem))
            } else {
                VoiceListening(request.prompt ?: stringResource(R.string.voice_input_speak_now), voice.phase, voice.voiceActive, voice.partial, voice::cancel, voice::stop)
            }
        }
    }

    private fun problemAction(problem: VoiceInputProblem): VoiceAction? = when (problem.kind) {
        VoiceInputProblemKind.MICROPHONE -> VoiceAction(R.string.voice_input_open_settings) {
            runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) }
        }
        VoiceInputProblemKind.NO_MODEL -> VoiceAction(R.string.voice_input_install_models) {
            // D47: the home screen opens at the recognition models and the caller hears the cause.
            runCatching { startActivity(HomeActivity.intent(this)) }
            voice.close()
        }
        else -> null
    }

    private companion object {
        const val TAG = "MacawVoiceInput"
    }
}

/** `RecognizerIntent.EXTRA_BIASING_STRINGS` (API 33), read on every API level. zh-CN: 在所有 API 级别上读取. */
private const val EXTRA_BIASING_STRINGS = "android.speech.extra.BIASING_STRINGS"

/**
 * The caller's `RecognizerIntent` extras (D44 - D46) for the voice input sheet and the recognition service (D56); a
 * malformed bundle of another app reads as an empty request.
 * zh-CN: 调用方的 `RecognizerIntent` extra (D44 - D46), 供语音输入弹层与识别服务 (D56) 使用; 其他应用的畸形 Bundle 按空请求处理.
 */
internal fun readVoiceInputRequest(intent: Intent?): VoiceInputRequest = runCatching {
    val extras = intent?.extras ?: return@runCatching VoiceInputRequest()
    @Suppress("DEPRECATION")
    val silence = (extras.get(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS) as? Number)?.toLong()
    VoiceInputPolicy.request(
        prompt = extras.getCharSequence(RecognizerIntent.EXTRA_PROMPT),
        language = extras.getString(RecognizerIntent.EXTRA_LANGUAGE),
        silenceMillis = silence,
        biasing = extras.getStringArrayList(EXTRA_BIASING_STRINGS),
    )
}.getOrElse { VoiceInputRequest() }
