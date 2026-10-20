package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import java.lang.ref.WeakReference

/**
 * The key trigger (roadmap D64): an accessibility service that only filters key events (`flagRequestFilterKeyEvents`,
 * `canRetrieveWindowContent="false"`, no event types), so it never sees the screen content (D54). Off until the user turns
 * it on in the system accessibility settings. The chosen gesture (a long press of volume down by default) opens the
 * assistant sheet (D60); a short press of that key is consumed and makes the volume step it would have made through
 * `AudioManager.adjustSuggestedStreamVolume`; every other key, and every key while the screen is off or a call rings
 * or runs, goes to the system untouched. Nothing records until the sheet listens (D58).
 * zh-CN: 按键触发 (路线图 D64): 只过滤按键事件的无障碍服务 (`flagRequestFilterKeyEvents`, `canRetrieveWindowContent="false"`, 不订阅任何事件类型),
 * 因此从不接触屏幕内容 (D54). 用户在系统无障碍设置中开启前不工作. 所选手势 (默认长按音量下键) 打开助理弹层 (D60); 该键的短按被拦下, 由
 * `AudioManager.adjustSuggestedStreamVolume` 补做原本的一级音量调节; 其他按键, 以及熄屏, 来电或通话时的所有按键, 原样交给系统. 弹层收音之前不录音 (D58).
 */
class KeyTriggerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val trigger = KeyTrigger()
    private lateinit var preferences: TriggerPreferences
    private val tick = Runnable { apply(trigger.timer(SystemClock.uptimeMillis())) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferences = TriggerPreferences(this)
        trigger.gesture = preferences.keyGesture
        current = WeakReference(this)
        Log.i(TAG, "Key trigger connected (${trigger.gesture.id})")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!::preferences.isInitialized) return false
        val decision = when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // The gesture is read before each new press, so a changed setting applies to the next press.
                if (trigger.idle) trigger.gesture = preferences.keyGesture
                trigger.down(event.keyCode, event.repeatCount, event.eventTime, eligible())
            }
            KeyEvent.ACTION_UP -> trigger.up(event.keyCode, event.eventTime)
            else -> return false
        }
        apply(decision)
        return decision.consume
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(tick)
        trigger.reset()
        if (current?.get() === this) current = null
        Log.i(TAG, "Key trigger disconnected")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        if (current?.get() === this) current = null
        super.onDestroy()
    }

    private fun eligible(): Boolean {
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive != false
        val mode = getSystemService(AudioManager::class.java)?.mode ?: KeyTriggerPolicy.AUDIO_MODE_NORMAL
        return KeyTriggerPolicy.eligible(interactive, mode)
    }

    private fun apply(decision: KeyDecision) {
        handler.removeCallbacks(tick)
        decision.timerAt?.let { handler.postAtTime(tick, it) }
        if (decision.adjust > 0) {
            // A consumed press that did not trigger: the volume step it would have made (for device runs and reports).
            Log.i(TAG, "Key trigger ${trigger.gesture.id}: volume step")
            repeat(decision.adjust) { adjustVolume() }
        }
        if (decision.trigger) {
            Log.i(TAG, "Key trigger ${trigger.gesture.id}")
            runCatching { AssistantEntry.open(this, TriggerSource.KEY) }.onFailure { Log.w(TAG, "The key trigger could not open the sheet: $it") }
        }
    }

    /** The volume step of a consumed press, as the system makes it for a volume key. zh-CN: 被拦下的按键原本的一级音量调节. */
    private fun adjustVolume() {
        val direction = if (trigger.gesture.keyCode == KeyTriggerPolicy.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        runCatching {
            getSystemService(AudioManager::class.java)?.adjustSuggestedStreamVolume(direction, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
        }.onFailure { Log.w(TAG, "The volume could not be adjusted: $it") }
    }

    companion object {
        private const val TAG = "MacawTrigger"

        private var current: WeakReference<KeyTriggerService>? = null

        /** The service while the system keeps it connected (main thread). zh-CN: 系统保持连接期间的服务 (主线程). */
        internal val connected: Boolean get() = current?.get() != null

        fun component(context: Context) = ComponentName(context, KeyTriggerService::class.java)

        /** The user turned the key trigger on in the accessibility settings. zh-CN: 用户已在无障碍设置中开启按键触发. */
        fun isEnabled(context: Context): Boolean {
            val id = component(context).flattenToString()
            val manager = context.getSystemService(AccessibilityManager::class.java)
            val listed = runCatching {
                manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).orEmpty().any { it.id == id }
            }.getOrDefault(false)
            if (listed) return true
            val enabled = runCatching { Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) }.getOrNull().orEmpty()
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == component(context) }
        }

        /**
         * The system accessibility settings, with the service highlighted where the settings app supports it; the user
         * turns it on there (an app cannot).
         * zh-CN: 系统无障碍设置, 设置应用支持时高亮本服务; 由用户在那里开启 (应用无法自行开启).
         */
        fun openSettings(context: Context): Boolean {
            val id = component(context).flattenToString()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .putExtra(":settings:fragment_args_key", id)
                .putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", id) })
            return runCatching { context.startActivity(intent) }.isSuccess
        }
    }
}
