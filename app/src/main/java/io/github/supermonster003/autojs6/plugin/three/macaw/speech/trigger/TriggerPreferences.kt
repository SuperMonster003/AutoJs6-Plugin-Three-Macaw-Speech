package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.content.Context
import android.content.SharedPreferences

/**
 * The settings of the trigger entries (roadmap P7.1): the floating microphone (D62, off by default) and where it rests.
 * zh-CN: 触发式入口的设置 (路线图 P7.1): 悬浮麦克风 (D62, 默认关) 及其停靠位置.
 */
internal class TriggerPreferences(context: Context) {

    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** D62: the user turned the floating microphone on (it also needs the overlay permission). zh-CN: 用户开启了悬浮麦克风 (另需悬浮窗权限). */
    var floating: Boolean
        get() = preferences.getBoolean(KEY_FLOATING, false)
        set(value) = edit { putBoolean(KEY_FLOATING, value) }

    /** The side edge the floating microphone rests on. zh-CN: 悬浮麦克风停靠的一侧. */
    var floatingRight: Boolean
        get() = preferences.getBoolean(KEY_FLOATING_RIGHT, true)
        set(value) = edit { putBoolean(KEY_FLOATING_RIGHT, value) }

    /** Its height on that edge, 0 (top) to 1 (bottom). zh-CN: 在该侧的高度, 0 (顶部) 到 1 (底部). */
    var floatingY: Float
        get() = preferences.getFloat(KEY_FLOATING_Y, FloatingMicPolicy.DEFAULT_Y).coerceIn(0f, 1f)
        set(value) = edit { putFloat(KEY_FLOATING_Y, value.coerceIn(0f, 1f)) }

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(block).apply()
    }

    private companion object {
        const val FILE = "trigger-preferences"
        const val KEY_FLOATING = "floating"
        const val KEY_FLOATING_RIGHT = "floatingRight"
        const val KEY_FLOATING_Y = "floatingY"
    }
}
