package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.AssistantSessions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.MacawVoiceInteractionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant.AssistantActivity

/** The entries of roadmap P7.1 (D59) and the wake word of P7.4 that open the assistant sheet. zh-CN: 路线图 P7.1 (D59) 与 P7.4 唤醒词中打开助理弹层的入口. */
internal enum class TriggerSource(val id: String) {
    TILE("tile"),
    WIDGET("widget"),
    SHORTCUT("shortcut"),
    FLOATING("floating"),
    KEY("key"),
    HEADSET("headset"),

    /** The resident wake word (roadmap P7.4, D79). zh-CN: 常驻唤醒 (路线图 P7.4, D79). */
    WAKE("wake"),
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

    /** API level of `StatusBarManager.requestAddTileService`. zh-CN: `StatusBarManager.requestAddTileService` 的 API 级别. */
    const val TILE_REQUEST_API = 33

    /** The request could not be made (no status bar service, an exception). zh-CN: 无法发出请求 (没有状态栏服务, 抛出异常). */
    const val TILE_REQUEST_UNAVAILABLE = -1

    // Result codes of StatusBarManager.requestAddTileService (API 33); every error code is 1000 or higher.
    private const val TILE_NOT_ADDED = 0
    private const val TILE_ALREADY_ADDED = 1
    private const val TILE_ADDED = 2

    /**
     * D61: "Add to Quick Settings" asks the system on Android 13+ and shows the manual steps on older versions; a request
     * that fails for any reason also falls back to the manual steps. A declined request shows nothing.
     * zh-CN: D61: "添加到快速设置" 在 Android 13+ 请系统询问用户, 更早的版本显示手动步骤; 请求因任何原因失败时同样退回手动步骤. 用户拒绝则不显示任何内容.
     */
    fun tileOutcome(sdk: Int, code: Int? = null): TileOutcome = when {
        sdk < TILE_REQUEST_API -> TileOutcome.MANUAL
        code == null -> TileOutcome.REQUEST
        code == TILE_ADDED -> TileOutcome.ADDED
        code == TILE_ALREADY_ADDED -> TileOutcome.ALREADY_ADDED
        code == TILE_NOT_ADDED -> TileOutcome.DECLINED
        else -> TileOutcome.MANUAL
    }
}

/** What "Add to Quick Settings" does next (roadmap D61). zh-CN: "添加到快速设置" 的下一步 (路线图 D61). */
internal enum class TileOutcome { REQUEST, ADDED, ALREADY_ADDED, DECLINED, MANUAL }

/**
 * The single way the trigger entries open the assistant sheet (roadmap D60). Nothing records before the sheet listens
 * (D58): the entries themselves only start this.
 * zh-CN: 触发式入口打开助理弹层的唯一途径 (路线图 D60). 弹层开始收音之前不录音 (D58): 各入口本身只负责启动它.
 */
internal object AssistantEntry {
    private const val TAG = "MacawTrigger"

    /** The entry that opened the sheet, in the session arguments and the sheet intent. zh-CN: 打开弹层的入口, 用于会话参数与弹层 Intent. */
    const val EXTRA_SOURCE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.extra.TRIGGER_SOURCE"

    /**
     * How long a session request may take before the sheet opens directly. ColorOS and HyperOS (Android 15) silently drop
     * a request made while the keyguard goes away, as after a tile tapped on the lock screen; a request normally shows
     * within 50 ms.
     * zh-CN: 会话请求在直接打开弹层之前可等待的时长. ColorOS 与 HyperOS (Android 15) 会静默丢弃锁屏正在退出时发出的请求 (例如在锁屏上点磁贴之后);
     * 请求通常在 50 毫秒内显示.
     */
    private const val SESSION_WAIT_MILLIS = 1_200L

    private val main = Handler(Looper.getMainLooper())

    /** Main thread. zh-CN: 主线程. */
    fun open(context: Context, source: TriggerSource): TriggerRoute {
        val service = MacawVoiceInteractionService.running
        if (TriggerPolicy.route(MacawVoiceInteractionService.isActive(context), service != null) == TriggerRoute.SESSION && service != null) {
            val requested = AssistantSessions.shownCount
            val shown = runCatching { service.showSession(Bundle().apply { putString(EXTRA_SOURCE, source.id) }, 0) }
            if (shown.isSuccess) {
                Log.i(TAG, "Trigger ${source.id}: through the assistant session")
                // The bound assistant service lets the plugin start the sheet from the background if the session never shows.
                val application = context.applicationContext
                main.postDelayed({
                    if (AssistantSessions.shownCount == requested && !AssistantSessions.directSheetOpen) {
                        Log.w(TAG, "Trigger ${source.id}: the assistant session did not show, opening the sheet")
                        runCatching { openSheet(application, source) }.onFailure { Log.w(TAG, "The sheet could not open: $it") }
                    }
                }, SESSION_WAIT_MILLIS)
                return TriggerRoute.SESSION
            }
            Log.w(TAG, "Trigger ${source.id}: the assistant session did not show (${shown.exceptionOrNull()}), opening the sheet")
        }
        openSheet(context, source)
        return TriggerRoute.SHEET
    }

    private fun openSheet(context: Context, source: TriggerSource) {
        context.startActivity(AssistantActivity.intent(context, source).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        Log.i(TAG, "Trigger ${source.id}: the sheet directly")
    }
}
