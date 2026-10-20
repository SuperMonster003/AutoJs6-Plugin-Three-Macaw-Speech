package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

/**
 * The key gestures of roadmap D64: a long press of volume down (the default) or volume up, or a double press of volume
 * down. Key codes are `KeyEvent.KEYCODE_VOLUME_DOWN` / `KEYCODE_VOLUME_UP`.
 * zh-CN: 路线图 D64 的按键手势: 长按音量下键 (默认) 或音量上键, 或双击音量下键. 键码即 `KeyEvent.KEYCODE_VOLUME_DOWN` / `KEYCODE_VOLUME_UP`.
 */
internal enum class KeyGesture(val id: String, val keyCode: Int, val double: Boolean) {
    LONG_VOLUME_DOWN("longVolumeDown", KeyTriggerPolicy.KEYCODE_VOLUME_DOWN, false),
    LONG_VOLUME_UP("longVolumeUp", KeyTriggerPolicy.KEYCODE_VOLUME_UP, false),
    DOUBLE_VOLUME_DOWN("doubleVolumeDown", KeyTriggerPolicy.KEYCODE_VOLUME_DOWN, true),
    ;

    companion object {
        val DEFAULT = LONG_VOLUME_DOWN

        fun of(id: String?): KeyGesture = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * What the key service does with one key event or timer (roadmap D64): [consume] keeps the event from the system,
 * [trigger] opens the assistant sheet, [adjust] is how many volume steps to make in the key's direction (the volume
 * change a consumed short press would have made), [timerAt] is the uptime of the next timer, or null for none.
 * zh-CN: 按键服务对一次按键事件或定时器的处理 (路线图 D64): [consume] 不再交给系统, [trigger] 打开助理弹层, [adjust] 按该键方向调节音量的级数 (被拦下的短按
 * 原本会做的音量调节), [timerAt] 为下一个定时器的 uptime, null 表示没有.
 */
internal data class KeyDecision(val consume: Boolean, val trigger: Boolean = false, val adjust: Int = 0, val timerAt: Long? = null)

/** The constants and the gate of the key service (roadmap D64). Pure Kotlin. zh-CN: 按键服务的常量与准入 (路线图 D64). 纯 Kotlin. */
internal object KeyTriggerPolicy {
    const val KEYCODE_VOLUME_UP = 24
    const val KEYCODE_VOLUME_DOWN = 25

    /** D64: "a long press of about 0.8 s". zh-CN: D64: "长按约 0.8 秒". */
    const val LONG_PRESS_MILLIS = 800L

    /** A second press of volume down within this time after the first release is a double press. zh-CN: 首次松开后此时间内再按为双击. */
    const val DOUBLE_PRESS_MILLIS = 350L

    /** Double press mode: a first press held this long changes the volume like a held key. zh-CN: 双击模式: 首次按下保持此时长后如常按住调音量. */
    const val HOLD_START_MILLIS = 500L

    /** ... and repeats every this long. zh-CN: ... 并按此间隔重复. */
    const val HOLD_REPEAT_MILLIS = 100L

    /** `AudioManager.MODE_NORMAL`: no call, no ringtone, no voice chat. zh-CN: `AudioManager.MODE_NORMAL`: 没有通话, 铃声或语音聊天. */
    const val AUDIO_MODE_NORMAL = 0

    /**
     * D64: keys are only taken while the screen is on and no call rings or runs (the volume keys silence the ringer and
     * set the call volume then); otherwise every key goes to the system untouched.
     * zh-CN: D64: 只在亮屏且没有来电或通话时拦截按键 (此时音量键用于静音来电与调节通话音量); 否则所有按键原样交给系统.
     */
    fun eligible(interactive: Boolean, audioMode: Int): Boolean = interactive && audioMode == AUDIO_MODE_NORMAL
}

/**
 * The key state machine of roadmap D64, driven by the key events of the accessibility service and its timer (main
 * thread). Only the key of the chosen gesture is taken; every other key, and the gesture key while not [eligible], goes
 * to the system. A long press triggers once the key is held for [KeyTriggerPolicy.LONG_PRESS_MILLIS]; releasing it
 * earlier makes the one volume step the press would have made. In double press mode a single press makes its volume
 * step after the double press window, and a held first press repeats volume steps like a held key. Pure Kotlin.
 * zh-CN: 路线图 D64 的按键状态机, 由无障碍服务的按键事件与其定时器驱动 (主线程). 只拦截所选手势的按键; 其他按键, 以及不满足 [eligible] 时的手势按键, 都交给
 * 系统. 长按保持 [KeyTriggerPolicy.LONG_PRESS_MILLIS] 即触发; 提前松开则补做这次按键原本的一级音量调节. 双击模式下, 单击在双击等待时间之后补做一级音量调节,
 * 首次按下并按住则如常按住按键一样重复调节音量. 纯 Kotlin.
 */
internal class KeyTrigger(gesture: KeyGesture = KeyGesture.DEFAULT) {

    private enum class State { IDLE, PRESSED, TRIGGERED, HOLDING, WAIT_SECOND, SECOND }

    private var state = State.IDLE
    private var downAt = 0L
    private var upAt = 0L
    private var deadline: Long? = null

    /** The gesture; changing it drops a press in progress. zh-CN: 手势; 更改时丢弃进行中的按键. */
    var gesture: KeyGesture = gesture
        set(value) {
            if (field != value) reset()
            field = value
        }

    val idle: Boolean get() = state == State.IDLE

    fun reset() {
        state = State.IDLE
        deadline = null
    }

    fun down(keyCode: Int, repeatCount: Int, time: Long, eligible: Boolean): KeyDecision {
        if (keyCode != gesture.keyCode) return pass()
        return when (state) {
            State.IDLE -> if (repeatCount > 0 || !eligible) pass() else press(time)
            State.WAIT_SECOND -> if (!eligible) {
                // A call came in between the presses: the first press makes its volume step, this one goes to the system.
                reset()
                KeyDecision(consume = false, adjust = 1, timerAt = null)
            } else if (time - upAt <= KeyTriggerPolicy.DOUBLE_PRESS_MILLIS) {
                state = State.SECOND
                take(trigger = true, timer = null)
            } else {
                // The window ended before its timer ran: the first press was a single one, this one starts anew.
                press(time).copy(adjust = 1)
            }
            // A repeat or a duplicate of the press in progress.
            else -> take(timer = deadline)
        }
    }

    fun up(keyCode: Int, time: Long): KeyDecision {
        if (keyCode != gesture.keyCode) return pass()
        return when (state) {
            State.IDLE, State.WAIT_SECOND -> pass()
            State.PRESSED -> when {
                gesture.double -> {
                    state = State.WAIT_SECOND
                    upAt = time
                    take(timer = time + KeyTriggerPolicy.DOUBLE_PRESS_MILLIS)
                }
                time - downAt >= KeyTriggerPolicy.LONG_PRESS_MILLIS -> {
                    // Released just as the threshold passed, before the timer ran: still a long press.
                    state = State.IDLE
                    take(trigger = true, timer = null)
                }
                else -> {
                    state = State.IDLE
                    take(adjust = 1, timer = null)
                }
            }
            State.TRIGGERED, State.HOLDING, State.SECOND -> {
                state = State.IDLE
                take(timer = null)
            }
        }
    }

    fun timer(time: Long): KeyDecision {
        val due = deadline
        if (due == null || time < due) return KeyDecision(consume = false, timerAt = due)
        return when (state) {
            State.PRESSED -> if (gesture.double) {
                state = State.HOLDING
                take(adjust = 1, timer = time + KeyTriggerPolicy.HOLD_REPEAT_MILLIS)
            } else {
                state = State.TRIGGERED
                take(trigger = true, timer = null)
            }
            State.HOLDING -> take(adjust = 1, timer = time + KeyTriggerPolicy.HOLD_REPEAT_MILLIS)
            State.WAIT_SECOND -> {
                state = State.IDLE
                take(adjust = 1, timer = null)
            }
            else -> take(timer = null)
        }
    }

    private fun press(time: Long): KeyDecision {
        state = State.PRESSED
        downAt = time
        return take(timer = time + if (gesture.double) KeyTriggerPolicy.HOLD_START_MILLIS else KeyTriggerPolicy.LONG_PRESS_MILLIS)
    }

    private fun pass(): KeyDecision = KeyDecision(consume = false, timerAt = deadline)

    private fun take(trigger: Boolean = false, adjust: Int = 0, timer: Long?): KeyDecision {
        deadline = timer
        return KeyDecision(consume = true, trigger = trigger, adjust = adjust, timerAt = timer)
    }
}
