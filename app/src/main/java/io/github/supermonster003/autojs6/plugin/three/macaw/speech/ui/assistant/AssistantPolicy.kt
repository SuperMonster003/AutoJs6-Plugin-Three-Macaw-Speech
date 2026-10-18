package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant

/**
 * An app activity as `PackageManager` resolved it for `VOICE_COMMAND` (roadmap D50), before [AssistantPolicy.targets]
 * checks it.
 * zh-CN: `PackageManager` 为 `VOICE_COMMAND` 解析出的应用 Activity (路线图 D50), 尚未经 [AssistantPolicy.targets] 检查.
 */
internal data class ResolvedTarget(val packageName: String, val className: String, val label: String?, val exported: Boolean, val enabled: Boolean)

/** A target button of the assistant sheet (roadmap D49, D50). zh-CN: 助理弹层的一个目标按钮 (路线图 D49, D50). */
internal data class VoiceCommandTarget(val packageName: String, val className: String, val label: String)

/** What a send, a copy or another way out of the sheet needs first (roadmap D55). zh-CN: 发送, 复制或离开弹层前需要先做的事 (路线图 D55). */
internal enum class AssistantGate {
    /** Go ahead now. zh-CN: 立即进行. */
    NOW,

    /** `KeyguardManager.requestDismissKeyguard` (API 26+), then go ahead on success only. zh-CN: 先请求解锁, 成功后才进行. */
    REQUEST_UNLOCK,

    /** API 24 - 25: `FLAG_DISMISS_KEYGUARD` on the window, then go ahead once the keyguard is gone. zh-CN: 以窗口标志解锁, 锁屏消失后再进行. */
    WINDOW_UNLOCK,
}

/**
 * The rules of the digital assistant (roadmap P5.7, decided with the maintainer on 2026-10-17 as D48 - D57). Pure Kotlin.
 * zh-CN: 数字助理的规则 (路线图 P5.7, 2026-10-17 与维护者确定为 D48 - D57). 纯 Kotlin.
 */
internal object AssistantPolicy {
    /** D50: the action a target app answers with an exported activity (+ DEFAULT). zh-CN: D50: 目标应用以导出的 Activity 应答的 action. */
    const val ACTION_VOICE_COMMAND = "org.autojs.plugin.action.VOICE_COMMAND"

    /** D50: the BCP 47 language of the recognized text, when known. zh-CN: D50: 识别文字的 BCP 47 语言 (已知时). */
    const val EXTRA_VOICE_LANGUAGE = "org.autojs.plugin.extra.VOICE_LANGUAGE"

    /**
     * The longest text the sheet edits and sends (a dictation has at most 60 s of speech, D44; 3-Stone AI cuts at the same
     * length, its D37).
     * zh-CN: 弹层可编辑与发送的最长文字 (一次听写至多 60 秒, D44; 3-Stone AI 在相同长度截断, 其 D37).
     */
    const val MAX_TEXT_CHARS = 16_384

    /** API level of `KeyguardManager.requestDismissKeyguard`. zh-CN: `KeyguardManager.requestDismissKeyguard` 的 API 级别. */
    private const val REQUEST_DISMISS_API = 26

    /**
     * D50, D53: the installed apps that answer `VOICE_COMMAND`, found by the entry and never by a package name: the
     * plugin's own package is left out (it never answers its own action), as are activities that are not exported or
     * disabled and duplicates; the label falls back to the package name; sorted by label with [collator], then by
     * package and class. No default target and no memory of the last choice (D49).
     * zh-CN: D50, D53: 应答 `VOICE_COMMAND` 的已安装应用, 按入口而不按包名探测: 排除插件自身 (从不应答自己的 action), 未导出或已禁用的 Activity 与
     * 重复项; label 为空时用包名; 按 [collator] 比较 label 排序, 再按包名与类名. 没有默认目标, 不记住上次的选择 (D49).
     */
    fun targets(resolved: List<ResolvedTarget>, self: String, collator: Comparator<String>): List<VoiceCommandTarget> = resolved.asSequence()
        .filter { it.packageName != self && it.exported && it.enabled }
        .distinctBy { it.packageName to it.className }
        .map { VoiceCommandTarget(it.packageName, it.className, it.label?.trim()?.takeIf { label -> label.isNotEmpty() } ?: it.packageName) }
        .sortedWith(Comparator<VoiceCommandTarget> { a, b -> collator.compare(a.label, b.label) }.thenBy { it.packageName }.thenBy { it.className })
        .toList()

    /**
     * D49: the edited text a target receives: trimmed, at most [MAX_TEXT_CHARS] without splitting a surrogate pair; null
     * when nothing is left (the buttons are disabled then).
     * zh-CN: D49: 目标收到的编辑后文字: 去首尾空白, 至多 [MAX_TEXT_CHARS] 个字符且不拆开代理对; 为空时为 null (此时按钮不可用).
     */
    fun text(edited: String): String? {
        val text = edited.trim().takeIf { it.isNotEmpty() } ?: return null
        if (text.length <= MAX_TEXT_CHARS) return text
        val end = if (Character.isHighSurrogate(text[MAX_TEXT_CHARS - 1])) MAX_TEXT_CHARS - 1 else MAX_TEXT_CHARS
        return text.substring(0, end)
    }

    /**
     * D55: a send, a copy, "install models" or "open settings" on the lock screen unlocks first; dictation itself needs
     * no unlock.
     * zh-CN: D55: 锁屏上的发送, 复制, "安装模型" 或 "打开设置" 先解锁; 听写本身不需要解锁.
     */
    fun gate(keyguardLocked: Boolean, sdk: Int): AssistantGate = when {
        !keyguardLocked -> AssistantGate.NOW
        sdk >= REQUEST_DISMISS_API -> AssistantGate.REQUEST_UNLOCK
        else -> AssistantGate.WINDOW_UNLOCK
    }
}
