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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity

/**
 * The system voice input (roadmap P5.6, D41 - D47): answers `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` of any app with a
 * bottom sheet over the caller's screen and returns the recognized text in `EXTRA_RESULTS` only. It is exported
 * without a permission (3-Stove Agent's microphone button, for one, starts it) and has no other effect: no audio, no
 * model or setting changes, no downloads. Leaving the screen ends the input and releases the microphone.
 * zh-CN: 系统语音输入 (路线图 P5.6, D41 - D47): 以覆盖在调用方界面上的底部弹层应答任何应用的 `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, 只经
 * `EXTRA_RESULTS` 返回识别文字. 无权限导出 (例如 3-Stove Agent 的麦克风按钮会启动它), 没有其他作用: 不返回音频, 不改模型或设置, 不下载. 离开界面即结束输入并
 * 释放麦克风.
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
        request = readRequest(intent)
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
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::dismiss)
                .testTag("macaw.voice.scrim"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(visibleState = shown, enter = slideInVertically { it } + fadeIn()) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        // The sheet itself does not dismiss. zh-CN: 点击弹层本身不会关闭.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                        .testTag("macaw.voice.sheet"),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val problem = voice.problem
                        if (problem != null) Problem(problem) else Listening()
                    }
                }
            }
        }
    }

    @Composable
    private fun Listening() {
        Text(
            request.prompt ?: stringResource(R.string.voice_input_speak_now),
            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("macaw.voice.title"),
        )
        Spacer(Modifier.height(20.dp))
        MicrophoneIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(
                when (voice.phase) {
                    VoiceInputPhase.PREPARING, VoiceInputPhase.LOADING -> R.string.voice_input_loading
                    VoiceInputPhase.STOPPING, VoiceInputPhase.DONE -> R.string.voice_input_recognizing
                    else -> if (voice.voiceActive) R.string.voice_input_hearing else R.string.voice_input_listening
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("macaw.voice.state"),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            voice.partial,
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("macaw.voice.partial"),
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = voice::cancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.voice.cancel")) {
                Text(stringResource(R.string.ui_dialog_cancel))
            }
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = voice::stop,
                enabled = voice.phase == VoiceInputPhase.LISTENING,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.voice.stop"),
            ) { Text(stringResource(R.string.voice_input_stop)) }
        }
    }

    /** Pulses while the VAD hears speech; the contract has no level events (D42). zh-CN: VAD 听到语音时脉动; 契约没有电平事件 (D42). */
    @Composable
    private fun MicrophoneIndicator() {
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = PULSE_SCALE,
            animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
            label = "scale",
        )
        val loading = voice.phase == VoiceInputPhase.PREPARING || voice.phase == VoiceInputPhase.LOADING
        val description = stringResource(R.string.voice_input_microphone)
        Box(modifier = Modifier.size(96.dp).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(if (voice.voiceActive) pulse else 1f)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (loading) 0.38f else 1f), CircleShape),
            )
            Icon(painterResource(R.drawable.ic_mic_24), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
            if (loading) CircularProgressIndicator(modifier = Modifier.size(88.dp).testTag("macaw.voice.loading"), strokeWidth = 3.dp)
        }
    }

    @Composable
    private fun Problem(problem: VoiceInputProblem) {
        val (icon, title, body) = when (problem.kind) {
            VoiceInputProblemKind.MICROPHONE -> Triple(R.drawable.ic_mic_24, R.string.voice_input_microphone_title, R.string.voice_input_microphone_body)
            VoiceInputProblemKind.NO_MODEL -> Triple(R.drawable.ic_model_24, R.string.voice_input_no_model_title, R.string.voice_input_no_model_body)
            VoiceInputProblemKind.BUSY -> Triple(R.drawable.ic_mic_24, R.string.voice_input_busy_title, R.string.voice_input_busy_body)
            VoiceInputProblemKind.FAILED -> Triple(R.drawable.ic_warning_24, R.string.voice_input_failed_title, R.string.voice_input_failed_body)
        }
        Column(modifier = Modifier.fillMaxWidth().testTag("macaw.voice.problem.${problem.kind.name.lowercase()}"), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                if (problem.kind == VoiceInputProblemKind.FAILED) stringResource(body, problem.code) else stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = voice::close, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.voice.problem.close")) {
                    Text(stringResource(R.string.voice_input_close))
                }
                when (problem.kind) {
                    VoiceInputProblemKind.MICROPHONE -> ProblemAction(R.string.voice_input_open_settings) {
                        runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) }
                    }
                    VoiceInputProblemKind.NO_MODEL -> ProblemAction(R.string.voice_input_install_models) {
                        // D47: the home screen opens at the recognition models and the caller hears the cause.
                        runCatching { startActivity(HomeActivity.intent(this@VoiceInputActivity)) }
                        voice.close()
                    }
                    else -> Unit
                }
            }
        }
    }

    @Composable
    private fun ProblemAction(label: Int, onClick: () -> Unit) {
        Spacer(Modifier.size(8.dp))
        Button(onClick = onClick, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.voice.problem.action")) { Text(stringResource(label)) }
    }

    private companion object {
        const val TAG = "MacawVoiceInput"
        const val SCRIM_ALPHA = 0.32f
        const val PULSE_SCALE = 1.18f
        const val PULSE_MILLIS = 600

        /** `RecognizerIntent.EXTRA_BIASING_STRINGS` (API 33), read on every API level. zh-CN: 在所有 API 级别上读取. */
        const val EXTRA_BIASING_STRINGS = "android.speech.extra.BIASING_STRINGS"

        /**
         * The caller's extras (D44 - D46); a malformed bundle of another app reads as an empty request.
         * zh-CN: 调用方的 extra (D44 - D46); 其他应用的畸形 Bundle 按空请求处理.
         */
        fun readRequest(intent: Intent?): VoiceInputRequest = runCatching {
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
    }
}
