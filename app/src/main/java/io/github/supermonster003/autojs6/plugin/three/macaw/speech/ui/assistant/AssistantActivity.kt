package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant

import android.Manifest
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.AssistantSessions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.LocalSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputController
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputOutcome
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputProblem
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputProblemKind
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputRequest
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceListening
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceMessage
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceProblem
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceSheet
import java.text.Collator

/**
 * The sheet of the digital assistant (roadmap P5.7, D48 - D55): the bottom card of the voice input (D42) listens once
 * through the voice input path (D44 - D47, taking the microphone over from a wake session, D57); then the recognized
 * text can be edited and goes to one of the apps that answer `VOICE_COMMAND` (D49, D50, D53), or is copied, or the user
 * speaks again. Nothing is sent before a target button is tapped, no target is the default and no choice is remembered.
 * It also shows over the lock screen; sending, copying and the guidance actions unlock first (D55). Started by the
 * session of the voice interaction service, and not exported.
 * zh-CN: 数字助理的弹层 (路线图 P5.7, D48 - D55): 语音输入的底部卡片 (D42) 经语音输入路径听写一次 (D44 - D47, 从唤醒会话手中接管麦克风, D57); 之后可以
 * 编辑识别文字, 交给应答 `VOICE_COMMAND` 的应用之一 (D49, D50, D53), 或复制, 或重说. 点目标按钮之前不发送任何内容, 没有默认目标, 不记住选择. 弹层也
 * 显示在锁屏之上; 发送, 复制与引导中的操作先解锁 (D55). 由语音交互服务的会话启动, 不导出.
 */
class AssistantActivity : StandaloneActivity() {

    private enum class Stage { LISTENING, RESULT, NO_MATCH }

    private var voice by mutableStateOf<VoiceInputController?>(null)
    private var stage by mutableStateOf(Stage.LISTENING)
    private var text by mutableStateOf("")
    private var notice by mutableStateOf<String?>(null)
    private var targets by mutableStateOf<List<VoiceCommandTarget>>(emptyList())
    private var language: String? = null
    private var awaitingPermission = false
    private var awaitingUnlock = false
    private var pendingUnlock: (() -> Unit)? = null

    override val translucentWindow: Boolean get() = true

    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        awaitingPermission = false
        val current = voice ?: return@registerForActivityResult
        if (granted) current.start() else current.microphoneDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverKeyguard()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) { dismiss() }
        AssistantSessions.opened(this)
        listen()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The assist gesture again: listen again unless listening already.
        if (stage != Stage.LISTENING || voice?.problem != null) redo()
    }

    override fun onResume() {
        super.onResume()
        targets = VoiceCommandTargets.query(this)
        val current = voice
        // Back from the app settings with the permission granted: listen now (D47).
        if (current?.problem?.kind == VoiceInputProblemKind.MICROPHONE && microphoneGranted()) current.start()
    }

    override fun onStop() {
        // The assistant belongs to the visible sheet: leaving it ends listening and closes the sheet (D48).
        if (!isChangingConfigurations && !awaitingPermission && !awaitingUnlock && !isFinishing) {
            voice?.release()
            finish()
        }
        super.onStop()
    }

    override fun onDestroy() {
        voice?.release()
        AssistantSessions.closed(this)
        super.onDestroy()
    }

    override fun holdsAppearance(): Boolean = voice?.busy == true || stage == Stage.RESULT

    /** The system hid the session (Home, another app, the lock screen). zh-CN: 系统隐藏了会话 (Home 键, 其他应用, 锁屏). */
    internal fun onSessionHidden() {
        if (!awaitingPermission && !awaitingUnlock && !isFinishing) finish()
    }

    private fun microphoneGranted() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun keyguardLocked(): Boolean = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    @Suppress("DEPRECATION")
    private fun showOverKeyguard() {
        // D55: dictation works on the lock screen. zh-CN: D55: 锁屏上可以听写.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setShowWhenLocked(true) else window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }

    /** One dictation of the voice input path; the sheet is visible, so no microphone foreground service. zh-CN: 一次语音输入路径的听写. */
    private fun listen() {
        voice?.release()
        notice = null
        stage = Stage.LISTENING
        val controller = VoiceInputController(this, VoiceInputRequest(), ::onOutcome, LocalSession(takeover = true, foreground = false))
        voice = controller
        if (microphoneGranted()) {
            controller.start()
        } else {
            // D47: the system permission dialog right in the sheet.
            awaitingPermission = true
            runCatching { microphone.launch(Manifest.permission.RECORD_AUDIO) }.onFailure {
                awaitingPermission = false
                controller.microphoneDenied()
            }
        }
    }

    /** "Speak again" (D49): the text is dropped and a new dictation starts. zh-CN: "重说" (D49): 丢弃文字, 开始新的听写. */
    private fun redo() {
        text = ""
        language = null
        listen()
    }

    private fun onOutcome(outcome: VoiceInputOutcome) {
        when (outcome) {
            is VoiceInputOutcome.Text -> {
                text = outcome.text.take(AssistantPolicy.MAX_TEXT_CHARS)
                language = outcome.language
                stage = Stage.RESULT
                Log.i(TAG, "Dictation done (${outcome.text.length} chars, language ${outcome.language ?: "unknown"})")
            }
            VoiceInputOutcome.NoMatch -> stage = Stage.NO_MATCH
            VoiceInputOutcome.Cancelled, is VoiceInputOutcome.Failed -> finish()
        }
    }

    private fun dismiss() {
        val current = voice
        when {
            current?.problem != null -> current.close()
            current?.busy == true -> current.cancel()
            else -> finish()
        }
    }

    /**
     * D55: on the lock screen, [action] runs only after the user unlocked; a cancelled unlock keeps the sheet.
     * zh-CN: D55: 锁屏时 [action] 只在用户解锁后运行; 取消解锁则留在弹层.
     */
    private fun whenUnlocked(action: () -> Unit) {
        notice = null
        when (AssistantPolicy.gate(keyguardLocked(), Build.VERSION.SDK_INT)) {
            AssistantGate.NOW -> action()
            AssistantGate.REQUEST_UNLOCK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val keyguard = getSystemService(KeyguardManager::class.java) ?: return action()
                awaitingUnlock = true
                keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        awaitingUnlock = false
                        action()
                    }

                    override fun onDismissCancelled() {
                        awaitingUnlock = false
                        notice = getString(R.string.assistant_unlock_needed)
                    }

                    override fun onDismissError() {
                        awaitingUnlock = false
                        notice = getString(R.string.assistant_unlock_needed)
                    }
                })
            }
            AssistantGate.WINDOW_UNLOCK -> {
                awaitingUnlock = true
                pendingUnlock = action
                window.addFlags(LEGACY_DISMISS_KEYGUARD)
                watchLegacyUnlock(System.currentTimeMillis() + LEGACY_UNLOCK_WAIT_MILLIS)
            }
        }
    }

    /**
     * API 24 - 25 (D55): `FLAG_DISMISS_KEYGUARD` has no callback, so the keyguard is checked until it is gone (go ahead) or
     * the wait ends (the user did not unlock; the sheet stays).
     * zh-CN: API 24 - 25 (D55): `FLAG_DISMISS_KEYGUARD` 没有回调, 因此反复检查锁屏, 直到锁屏消失 (继续) 或等待结束 (用户未解锁; 留在弹层).
     */
    private fun watchLegacyUnlock(deadline: Long) {
        window.decorView.postDelayed({
            val pending = pendingUnlock ?: return@postDelayed
            val unlocked = !keyguardLocked()
            if (!unlocked && System.currentTimeMillis() < deadline && !isFinishing) return@postDelayed watchLegacyUnlock(deadline)
            pendingUnlock = null
            awaitingUnlock = false
            window.clearFlags(LEGACY_DISMISS_KEYGUARD)
            if (unlocked) pending() else notice = getString(R.string.assistant_unlock_needed)
        }, LEGACY_UNLOCK_POLL_MILLIS)
    }

    /** D50: an explicit start of the target with the text and the language. zh-CN: D50: 以显式组件启动目标, 附带文字与语言. */
    private fun send(target: VoiceCommandTarget) {
        val value = AssistantPolicy.text(text) ?: return
        whenUnlocked {
            val intent = VoiceCommandTargets.intent(target, value, language)
            try {
                startActivity(intent)
                // The text itself is never logged. zh-CN: 文字本身从不写日志.
                Log.i(TAG, "Sent ${value.length} chars to ${target.packageName}")
                finish()
            } catch (e: ActivityNotFoundException) {
                failed(target, e)
            } catch (e: SecurityException) {
                failed(target, e)
            }
        }
    }

    private fun failed(target: VoiceCommandTarget, e: Exception) {
        Log.w(TAG, "${target.packageName} could not be started: ${e.javaClass.simpleName}")
        notice = getString(R.string.assistant_send_failed, target.label)
        targets = VoiceCommandTargets.query(this)
    }

    /** D49: "Copy text", always available (D53). zh-CN: D49: "复制文本", 始终可用 (D53). */
    private fun copy() {
        val value = AssistantPolicy.text(text) ?: return
        whenUnlocked {
            getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), value))
            // Android 13+ confirms a copy itself. zh-CN: Android 13+ 自行提示复制.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(this, R.string.assistant_copied, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Composable
    override fun Content() {
        VoiceSheet(onDismiss = ::dismiss) {
            val current = voice
            val problem = current?.problem
            when {
                current != null && problem != null -> VoiceProblem(problem, onClose = current::close, action = problemAction(current, problem))
                stage == Stage.LISTENING && current != null ->
                    VoiceListening(stringResource(R.string.voice_input_speak_now), current.phase, current.voiceActive, current.partial, current::cancel, current::stop)
                stage == Stage.NO_MATCH -> VoiceMessage(
                    icon = R.drawable.ic_mic_24,
                    title = stringResource(R.string.assistant_no_match_title),
                    body = stringResource(R.string.assistant_no_match_body),
                    tag = "macaw.assistant.noMatch",
                    closeTag = "macaw.assistant.close",
                    actionTag = "macaw.assistant.redo",
                    onClose = ::finish,
                    action = VoiceAction(R.string.assistant_redo, ::redo),
                )
                else -> Result()
            }
        }
    }

    @Composable
    private fun Result() {
        val sendable = AssistantPolicy.text(text) != null
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(AssistantPolicy.MAX_TEXT_CHARS) },
            label = { Text(stringResource(R.string.assistant_text_label)) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 240.dp).testTag("macaw.assistant.text"),
        )
        notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }.testTag("macaw.assistant.notice"),
            )
        }
        Spacer(Modifier.height(16.dp))
        if (targets.isEmpty()) {
            // D53: without a target the assistant still dictates and copies.
            Text(
                stringResource(R.string.assistant_no_targets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().testTag("macaw.assistant.noTargets"),
            )
        } else {
            Text(
                stringResource(R.string.assistant_targets),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            targets.forEach { target -> TargetButton(target, sendable) }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = ::redo, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.assistant.redo")) {
                Text(stringResource(R.string.assistant_redo))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = ::finish, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.assistant.close")) {
                Text(stringResource(R.string.voice_input_close))
            }
            TextButton(onClick = ::copy, enabled = sendable, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.assistant.copy")) {
                Text(stringResource(R.string.assistant_copy))
            }
        }
    }

    /** D50: the label and icon of the target activity. zh-CN: D50: 目标 Activity 的 label 与图标. */
    @Composable
    private fun TargetButton(target: VoiceCommandTarget, enabled: Boolean) {
        val size = with(LocalDensity.current) { ICON_DP.dp.roundToPx() }
        val icon = remember(target) { VoiceCommandTargets.icon(this, target, size)?.asImageBitmap() }
        FilledTonalButton(
            onClick = { send(target) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).padding(bottom = 8.dp).testTag("macaw.assistant.target.${target.className}"),
        ) {
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(ICON_DP.dp))
                Spacer(Modifier.size(12.dp))
            }
            Text(target.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).testTag("macaw.assistant.target.${target.className}.label"))
        }
    }

    private fun problemAction(current: VoiceInputController, problem: VoiceInputProblem): VoiceAction? = when (problem.kind) {
        VoiceInputProblemKind.MICROPHONE -> VoiceAction(R.string.voice_input_open_settings) {
            whenUnlocked { runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) } }
        }
        VoiceInputProblemKind.NO_MODEL -> VoiceAction(R.string.voice_input_install_models) {
            whenUnlocked {
                runCatching { startActivity(HomeActivity.intent(this)) }
                current.close()
            }
        }
        else -> null
    }

    companion object {
        private const val TAG = "MacawAssistant"
        private const val ICON_DP = 24
        private const val LEGACY_UNLOCK_POLL_MILLIS = 250L
        private const val LEGACY_UNLOCK_WAIT_MILLIS = 60_000L

        @Suppress("DEPRECATION")
        private const val LEGACY_DISMISS_KEYGUARD = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD

        /** The intent the assistant session starts. zh-CN: 助理会话启动的 Intent. */
        fun intent(context: Context): Intent = Intent(context, AssistantActivity::class.java)
    }
}

/**
 * The targets of roadmap D50 on the device: discovery through `queryIntentActivities` (the `<queries>` element makes them
 * visible on API 30+), their icons, and the explicit intent that carries the text.
 * zh-CN: 设备上路线图 D50 的目标: 经 `queryIntentActivities` 发现 (`<queries>` 元素使其在 API 30+ 可见), 其图标, 以及携带文字的显式 Intent.
 */
internal object VoiceCommandTargets {
    fun query(context: Context): List<VoiceCommandTarget> {
        val manager = context.packageManager
        val intent = Intent(AssistantPolicy.ACTION_VOICE_COMMAND).addCategory(Intent.CATEGORY_DEFAULT)
        val resolved = runCatching {
            @Suppress("DEPRECATION")
            val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            infos.mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                ResolvedTarget(activity.packageName, activity.name, runCatching { info.loadLabel(manager).toString() }.getOrNull(), activity.exported, activity.isEnabled)
            }
        }.getOrDefault(emptyList())
        val collator = Collator.getInstance(context.resources.configuration.locales[0])
        return AssistantPolicy.targets(resolved, context.packageName) { a, b -> collator.compare(a, b) }
    }

    fun icon(context: Context, target: VoiceCommandTarget, size: Int) =
        runCatching { context.packageManager.getActivityIcon(ComponentName(target.packageName, target.className)).toBitmap(size, size) }.getOrNull()

    fun intent(target: VoiceCommandTarget, text: String, language: String?): Intent = Intent(AssistantPolicy.ACTION_VOICE_COMMAND)
        .addCategory(Intent.CATEGORY_DEFAULT)
        .setClassName(target.packageName, target.className)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .apply { language?.let { putExtra(AssistantPolicy.EXTRA_VOICE_LANGUAGE, it) } }
}
