package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.MacawVoiceInteractionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant.AssistantActivity

/** The entries of roadmap P7.1 (D59) that open the assistant sheet. zh-CN: 路线图 P7.1 (D59) 中打开助理弹层的入口. */
internal enum class TriggerSource(val id: String) {
    TILE("tile"),
    WIDGET("widget"),
    SHORTCUT("shortcut"),
    FLOATING("floating"),
    KEY("key"),
    ;

    companion object {
        fun of(id: String?): TriggerSource? = entries.firstOrNull { it.id == id }
    }
}

/** How an entry opened the sheet (roadmap D60). zh-CN: 入口打开弹层的方式 (路线图 D60). */
internal enum class TriggerRoute {
    /** `VoiceInteractionService.showSession`, as the assist gesture. zh-CN: 与助理手势相同, 经助理会话. */
    SESSION,

    /** The sheet activity itself. zh-CN: 直接打开弹层 Activity. */
    SHEET,
}

/**
 * The rules of the trigger entries (roadmap P7.1, decided with the maintainer on 2026-10-20 as D58 - D65). Pure Kotlin.
 * zh-CN: 触发式入口的规则 (路线图 P7.1, 2026-10-20 与维护者确定为 D58 - D65). 纯 Kotlin.
 */
internal object TriggerPolicy {
    /**
     * D60: every entry opens the sheet of the assist gesture; through the assistant session while 3-Macaw Speech is the
     * default digital assistant and its service is running, otherwise the same sheet directly.
     * zh-CN: D60: 各入口都打开助理手势的弹层; 3-Macaw Speech 是默认数字助理且其服务在运行时经助理会话, 否则直接打开同一个弹层.
     */
    fun route(assistantActive: Boolean, serviceRunning: Boolean): TriggerRoute =
        if (assistantActive && serviceRunning) TriggerRoute.SESSION else TriggerRoute.SHEET
}

/**
 * The single way the trigger entries open the assistant sheet (roadmap D60). Nothing records before the sheet listens
 * (D58): the entries themselves only start this.
 * zh-CN: 触发式入口打开助理弹层的唯一途径 (路线图 D60). 弹层开始收音之前不录音 (D58): 各入口本身只负责启动它.
 */
internal object AssistantEntry {
    private const val TAG = "MacawTrigger"

    /** The entry that opened the sheet, in the session arguments and the sheet intent. zh-CN: 打开弹层的入口, 用于会话参数与弹层 Intent. */
    const val EXTRA_SOURCE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.extra.TRIGGER_SOURCE"

    fun open(context: Context, source: TriggerSource): TriggerRoute {
        val service = MacawVoiceInteractionService.running
        if (TriggerPolicy.route(MacawVoiceInteractionService.isActive(context), service != null) == TriggerRoute.SESSION && service != null) {
            val shown = runCatching { service.showSession(Bundle().apply { putString(EXTRA_SOURCE, source.id) }, 0) }
            if (shown.isSuccess) {
                Log.i(TAG, "Trigger ${source.id}: through the assistant session")
                return TriggerRoute.SESSION
            }
            Log.w(TAG, "Trigger ${source.id}: the assistant session did not show (${shown.exceptionOrNull()}), opening the sheet")
        }
        context.startActivity(AssistantActivity.intent(context, source).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        Log.i(TAG, "Trigger ${source.id}: the sheet directly")
        return TriggerRoute.SHEET
    }
}
