package io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import android.view.WindowManager
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant.AssistantActivity
import java.lang.ref.WeakReference

/**
 * The digital assistant (roadmap P5.7, D48): the voice interaction service the user picks as the default digital
 * assistant app. It records nothing itself and has no notification: the assist gesture (long press of the power or
 * Home button) shows a session, which opens the assistant sheet. Roadmap D54: the screen is never read; the system
 * only lists services with `supportsAssist="true"` as assistants (`VoiceInteractionManagerService` skips the others
 * when the role is assigned), so the service declares it and disables the assist data and the screenshot for every
 * session instead.
 * zh-CN: 数字助理 (路线图 P5.7, D48): 用户选为默认数字助理应用的语音交互服务. 它自身不录音, 没有通知: 助理手势 (长按电源键或 Home 键) 显示一个会话,
 * 会话打开助理弹层. 路线图 D54: 从不读取屏幕; 系统只把 `supportsAssist="true"` 的服务列为助理 (分配角色时 `VoiceInteractionManagerService` 跳过
 * 其他服务), 因此服务声明了它, 并对每个会话禁用辅助数据与截图.
 */
class MacawVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        disabledShowContext = NO_SCREEN_CONTENT
        Log.i(TAG, "Digital assistant ready")
    }

    companion object {
        private const val TAG = "MacawAssistant"

        /** D54: neither the `AssistStructure` of the current app nor a screenshot. zh-CN: D54: 既不要当前应用的 `AssistStructure`, 也不要截图. */
        internal const val NO_SCREEN_CONTENT = VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT

        /** True when 3-Macaw Speech is the active digital assistant. zh-CN: 3-Macaw Speech 当前是数字助理时为真. */
        fun isActive(context: Context): Boolean =
            runCatching { isActiveService(context, ComponentName(context, MacawVoiceInteractionService::class.java)) }.getOrDefault(false)
    }
}

/** Creates the sessions of the digital assistant. zh-CN: 创建数字助理的会话. */
class MacawVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = AssistantSession(this)
}

/**
 * One showing of the digital assistant: no window of its own (the sheet is the assistant activity, D48), no assist data
 * or screenshot (D54); hiding the session closes the sheet and closing the sheet hides the session.
 * zh-CN: 数字助理的一次显示: 不使用自己的窗口 (弹层是助理 Activity, D48), 不接收辅助数据与截图 (D54); 会话隐藏即关闭弹层, 弹层关闭即隐藏会话.
 */
internal class AssistantSession(context: Context) : VoiceInteractionSession(context) {

    override fun onCreate() {
        super.onCreate()
        disabledShowContext = MacawVoiceInteractionService.NO_SCREEN_CONTENT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // setUiEnabled is API 26: below it the empty session window must neither show nor take touches.
            window.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        }
    }

    override fun onPrepareShow(args: Bundle?, showFlags: Int) {
        super.onPrepareShow(args, showFlags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setUiEnabled(false)
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        AssistantSessions.shown(this)
        Log.i(TAG, "Assistant session shown (flags $showFlags)")
        try {
            val sheet = AssistantActivity.intent(context)
            // startAssistantActivity is API 26; below it the sheet starts as an ordinary task over the current app (API 24 - 25
            // have no background activity start limits) and the empty session window stays out of the way (onCreate).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startAssistantActivity(sheet) else context.startActivity(sheet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: RuntimeException) {
            Log.w(TAG, "The assistant sheet could not start: $e")
            hide()
        }
    }

    override fun onHide() {
        super.onHide()
        AssistantSessions.hidden(this)
    }

    override fun onDestroy() {
        AssistantSessions.hidden(this)
        super.onDestroy()
    }

    // D54: onHandleAssist and onHandleScreenshot keep their empty defaults, so nothing the system still sends is read.

    private companion object {
        const val TAG = "MacawAssistant"
    }
}

/**
 * Links the shown session and the assistant sheet (main thread): the sheet closes when the system hides the session
 * (the Home button, another app, the lock screen), and closing the sheet hides the session. The sheet also opens without
 * a session (device tests, the settings screen); then nothing is linked.
 * zh-CN: 关联已显示的会话与助理弹层 (主线程): 系统隐藏会话时 (Home 键, 切到其他应用, 锁屏) 弹层关闭, 弹层关闭时隐藏会话. 弹层也可以在没有会话时打开
 * (设备测试, 设置页), 此时不关联.
 */
internal object AssistantSessions {
    private var session: WeakReference<VoiceInteractionSession>? = null
    private var sheet: WeakReference<AssistantActivity>? = null

    fun shown(shown: VoiceInteractionSession) {
        session = WeakReference(shown)
    }

    fun hidden(hidden: VoiceInteractionSession) {
        if (session?.get() !== hidden) return
        session = null
        sheet?.get()?.onSessionHidden()
    }

    fun opened(activity: AssistantActivity) {
        sheet = WeakReference(activity)
    }

    fun closed(activity: AssistantActivity) {
        if (sheet?.get() !== activity) return
        sheet = null
        val current = session?.get() ?: return
        session = null
        runCatching { current.hide() }
    }
}
