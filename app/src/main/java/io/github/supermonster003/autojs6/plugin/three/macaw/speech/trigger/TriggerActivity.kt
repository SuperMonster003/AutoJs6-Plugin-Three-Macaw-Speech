package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager

/**
 * The launch target of the widget, the launcher shortcut and the quick settings tile while the device is unlocked
 * (roadmap D61, D63): no window of its own, it hands over to [AssistantEntry] (the assistant session or the sheet, D60)
 * and finishes. It does not show over the lock screen on purpose: for a pending intent of such an activity the system
 * UI (AOSP 14+, ColorOS) starts it without collapsing the notification shade, which then covered the sheet; a plain
 * activity goes through the path that collapses the shade. A tile tapped on the lock screen starts
 * [LockedTriggerActivity] instead. Internal; the launcher starts the static shortcut as the plugin itself.
 * zh-CN: 设备已解锁时桌面小部件, 启动器快捷方式与快速设置磁贴的启动目标 (路线图 D61, D63): 没有自己的窗口, 交给 [AssistantEntry] (助理会话或弹层, D60)
 * 后即结束. 它有意不显示在锁屏之上: 对于这类 Activity 的 PendingIntent, 系统界面 (AOSP 14+, ColorOS) 启动时不收起通知栏, 通知栏因此盖住弹层; 普通
 * Activity 走会收起通知栏的路径. 在锁屏上点磁贴时改为启动 [LockedTriggerActivity]. 不导出; 启动器以插件自身的身份启动静态快捷方式.
 */
open class TriggerActivity : Activity() {

    /** Whether the Manifest lets this target show over the lock screen. zh-CN: Manifest 是否允许该目标显示在锁屏之上. */
    protected open val overKeyguard: Boolean get() = false

    internal open fun source(intent: Intent?): TriggerSource =
        TriggerSource.of(intent?.getStringExtra(AssistantEntry.EXTRA_SOURCE)) ?: TriggerSource.SHORTCUT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (overKeyguard && Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        val source = source(intent)
        runCatching { AssistantEntry.open(this, source) }.onFailure { Log.w(TAG, "Trigger ${source.id} failed: $it") }
        finish()
    }

    companion object {
        private const val TAG = "MacawTrigger"

        internal fun intent(context: Context, source: TriggerSource, overKeyguard: Boolean = false): Intent =
            Intent(context, if (overKeyguard) LockedTriggerActivity::class.java else TriggerActivity::class.java)
                .putExtra(AssistantEntry.EXTRA_SOURCE, source.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}

/**
 * The launch target of a tile tapped on the lock screen (roadmap D61): it shows over the lock screen, so the sheet
 * dictates there and unlocks only before sending (D55). Some systems (ColorOS, HyperOS) ask for the unlock before they
 * pass the tap on; then the tile starts [TriggerActivity].
 * zh-CN: 在锁屏上点磁贴时的启动目标 (路线图 D61): 可显示在锁屏之上, 因此弹层在锁屏上听写, 发送前才解锁 (D55). 部分系统 (ColorOS, HyperOS) 先要求解锁
 * 再转交点按; 此时磁贴启动 [TriggerActivity].
 */
class LockedTriggerActivity : TriggerActivity() {
    override val overKeyguard: Boolean get() = true
}

/**
 * The headset entry (roadmap D65, decided with the maintainer on 2026-10-20 after the device runs): it answers
 * `VOICE_COMMAND`, which the Bluetooth service starts when a headset asks for voice recognition (a long press on its
 * voice key or touch area), and opens the sheet like the other entries (D60). With another app that answers the action
 * (the Google app, a vendor assistant) the system lets the user choose first. Exported without a permission, as the
 * voice input is: it only opens the plugin's own sheet, whatever the intent carries. It shows over the lock screen
 * (D55). The settings switch is its component state ([HeadsetTriggerComponent]).
 * zh-CN: 耳机入口 (路线图 D65, 2026-10-20 真机实测后与维护者确定): 应答蓝牙服务在耳机请求语音识别 (长按语音键或触控区) 时启动的 `VOICE_COMMAND`,
 * 与其他入口一样打开弹层 (D60). 有其他应用也应答该 action (Google 应用, 厂商助理) 时系统先让用户选择. 与系统语音输入一样无权限导出:
 * 无论 Intent 带什么, 都只打开插件自己的弹层. 可显示在锁屏之上 (D55). 设置开关即其组件状态 ([HeadsetTriggerComponent]).
 */
class HeadsetTriggerActivity : TriggerActivity() {
    override val overKeyguard: Boolean get() = true

    override fun source(intent: Intent?): TriggerSource = TriggerSource.HEADSET
}

/**
 * The settings switch "headset voice key" (roadmap D65): the component state of [HeadsetTriggerActivity] is the setting
 * itself, enabled by the Manifest default.
 * zh-CN: 设置开关 "耳机语音键" (路线图 D65): [HeadsetTriggerActivity] 的组件状态即设置本身, 按 Manifest 默认启用.
 */
internal object HeadsetTriggerComponent {
    fun component(context: Context) = ComponentName(context, HeadsetTriggerActivity::class.java)

    fun isEnabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(component(context)) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    fun setEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(component(context), state, PackageManager.DONT_KILL_APP)
    }
}
