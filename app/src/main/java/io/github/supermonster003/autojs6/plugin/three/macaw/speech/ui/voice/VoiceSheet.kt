package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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

/**
 * The bottom sheet of the system voice input (roadmap D42) that the digital assistant reuses (D48): a translucent
 * window with a scrim and a bottom card; tapping the scrim calls [onDismiss], the card itself does not dismiss.
 * zh-CN: 系统语音输入的底部弹层 (路线图 D42), 数字助理复用它 (D48): 透明窗口, 遮罩与底部卡片; 点遮罩调用 [onDismiss], 点卡片本身不关闭.
 */
@Composable
internal fun VoiceSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .testTag("macaw.voice.scrim"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(visibleState = shown, enter = slideInVertically { it } + fadeIn()) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .imePadding()
                    // The sheet itself does not dismiss. zh-CN: 点击弹层本身不会关闭.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .testTag("macaw.voice.sheet"),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
            }
        }
    }
}

/**
 * The listening card (D42): the title, the microphone that pulses with the VAD, the state, the partial text in gray,
 * "Cancel" and "Stop".
 * zh-CN: 收音卡片 (D42): 标题, 随 VAD 脉动的麦克风, 状态, 灰色的 partial 文字, "取消" 与 "停止".
 */
@Composable
internal fun VoiceListening(title: String, phase: VoiceInputPhase, voiceActive: Boolean, partial: String, onCancel: () -> Unit, onStop: () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth().testTag("macaw.voice.title"),
    )
    Spacer(Modifier.height(20.dp))
    MicrophoneIndicator(phase, voiceActive)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(
            when (phase) {
                VoiceInputPhase.PREPARING, VoiceInputPhase.LOADING -> R.string.voice_input_loading
                VoiceInputPhase.STOPPING, VoiceInputPhase.DONE -> R.string.voice_input_recognizing
                else -> if (voiceActive) R.string.voice_input_hearing else R.string.voice_input_listening
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("macaw.voice.state"),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        partial,
        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("macaw.voice.partial"),
    )
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.voice.cancel")) {
            Text(stringResource(R.string.ui_dialog_cancel))
        }
        Spacer(Modifier.size(8.dp))
        Button(
            onClick = onStop,
            enabled = phase == VoiceInputPhase.LISTENING,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("macaw.voice.stop"),
        ) { Text(stringResource(R.string.voice_input_stop)) }
    }
}

/** Pulses while the VAD hears speech; the contract has no level events (D42). zh-CN: VAD 听到语音时脉动; 契约没有电平事件 (D42). */
@Composable
private fun MicrophoneIndicator(phase: VoiceInputPhase, voiceActive: Boolean) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = PULSE_SCALE,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "scale",
    )
    val loading = phase == VoiceInputPhase.PREPARING || phase == VoiceInputPhase.LOADING
    val description = stringResource(R.string.voice_input_microphone)
    Box(modifier = Modifier.size(96.dp).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(if (voiceActive) pulse else 1f)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (loading) 0.38f else 1f), CircleShape),
        )
        Icon(painterResource(R.drawable.ic_mic_24), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
        if (loading) CircularProgressIndicator(modifier = Modifier.size(88.dp).testTag("macaw.voice.loading"), strokeWidth = 3.dp)
    }
}

/** A guidance action button (D47). zh-CN: 引导中的操作按钮 (D47). */
internal data class VoiceAction(@StringRes val label: Int, val onClick: () -> Unit)

/**
 * The guidance instead of listening (D47): an icon, the title, the explanation, "Close" and the action of the cause.
 * zh-CN: 代替收音的引导 (D47): 图标, 标题, 说明, "关闭" 与对应原因的操作.
 */
@Composable
internal fun VoiceProblem(problem: VoiceInputProblem, onClose: () -> Unit, action: VoiceAction?) {
    val (icon, title, body) = when (problem.kind) {
        VoiceInputProblemKind.MICROPHONE -> Triple(R.drawable.ic_mic_24, R.string.voice_input_microphone_title, R.string.voice_input_microphone_body)
        VoiceInputProblemKind.NO_MODEL -> Triple(R.drawable.ic_model_24, R.string.voice_input_no_model_title, R.string.voice_input_no_model_body)
        VoiceInputProblemKind.BUSY -> Triple(R.drawable.ic_mic_24, R.string.voice_input_busy_title, R.string.voice_input_busy_body)
        VoiceInputProblemKind.FAILED -> Triple(R.drawable.ic_warning_24, R.string.voice_input_failed_title, R.string.voice_input_failed_body)
    }
    VoiceMessage(
        icon = icon,
        title = stringResource(title),
        body = if (problem.kind == VoiceInputProblemKind.FAILED) stringResource(body, problem.code) else stringResource(body),
        tag = "macaw.voice.problem.${problem.kind.name.lowercase()}",
        closeTag = "macaw.voice.problem.close",
        actionTag = "macaw.voice.problem.action",
        onClose = onClose,
        action = action,
    )
}

/** An icon, a title, a text, "Close" and an optional action. zh-CN: 图标, 标题, 正文, "关闭" 与可选的操作. */
@Composable
internal fun VoiceMessage(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    tag: String,
    closeTag: String,
    actionTag: String,
    onClose: () -> Unit,
    action: VoiceAction?,
) {
    Column(modifier = Modifier.fillMaxWidth().testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag(closeTag)) {
                Text(stringResource(R.string.voice_input_close))
            }
            if (action != null) {
                Spacer(Modifier.size(8.dp))
                Button(onClick = action.onClick, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag(actionTag)) { Text(stringResource(action.label)) }
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.32f
private const val PULSE_SCALE = 1.18f
private const val PULSE_MILLIS = 600
