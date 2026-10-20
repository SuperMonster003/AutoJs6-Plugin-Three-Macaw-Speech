package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.KeyTriggerPolicy.KEYCODE_VOLUME_DOWN
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.KeyTriggerPolicy.KEYCODE_VOLUME_UP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The key thresholds and the release rules of the key trigger (roadmap D64): a long press of about 0.8 s triggers, a
 * shorter press is consumed and makes its one volume step, a double press triggers within the window and a single press
 * makes its step afterwards, a held first press repeats steps; every other key, and the gesture key while the screen is
 * off or a call rings or runs, goes to the system.
 * zh-CN: 按键触发的阈值与放行规则 (路线图 D64): 长按约 0.8 秒触发, 更短的按键被拦下并补做一级音量调节, 等待时间内的双击触发而单击在之后补做一级调节,
 * 按住首次按键则重复调节; 其他按键, 以及熄屏, 来电或通话时的手势按键, 都交给系统.
 */
class KeyTriggerTest {

    private val power = 26

    @Test
    fun `a short press is consumed and makes its volume step`() {
        val trigger = KeyTrigger()
        assertEquals(KeyDecision(consume = true, timerAt = 800), trigger.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = true))
        assertEquals(KeyDecision(consume = true, adjust = 1, timerAt = null), trigger.up(KEYCODE_VOLUME_DOWN, 200))
        assertTrue(trigger.idle)
        // The released press leaves no timer behind.
        assertEquals(KeyDecision(consume = false, timerAt = null), trigger.timer(800))
    }

    @Test
    fun `a press held for about 0_8 s triggers once and its release is consumed`() {
        val trigger = KeyTrigger()
        trigger.down(KEYCODE_VOLUME_DOWN, 0, 1_000, eligible = true)
        // Repeats while held keep the timer.
        assertEquals(KeyDecision(consume = true, timerAt = 1_800), trigger.down(KEYCODE_VOLUME_DOWN, 1, 1_450, eligible = true))
        assertEquals("an early timer does nothing", KeyDecision(consume = false, timerAt = 1_800), trigger.timer(1_799))
        assertEquals(KeyDecision(consume = true, trigger = true, timerAt = null), trigger.timer(1_800))
        assertEquals(KeyDecision(consume = true, timerAt = null), trigger.down(KEYCODE_VOLUME_DOWN, 2, 1_900, eligible = true))
        assertEquals("no volume step after a trigger", KeyDecision(consume = true, timerAt = null), trigger.up(KEYCODE_VOLUME_DOWN, 2_400))
        assertTrue(trigger.idle)
    }

    @Test
    fun `the threshold is 800 ms even when the release comes before the timer`() {
        val trigger = KeyTrigger()
        trigger.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = true)
        assertEquals(KeyDecision(consume = true, adjust = 1, timerAt = null), trigger.up(KEYCODE_VOLUME_DOWN, 799))
        trigger.down(KEYCODE_VOLUME_DOWN, 0, 10_000, eligible = true)
        assertEquals(KeyDecision(consume = true, trigger = true, timerAt = null), trigger.up(KEYCODE_VOLUME_DOWN, 10_800))
    }

    @Test
    fun `every other key goes to the system untouched`() {
        val trigger = KeyTrigger()
        assertFalse(trigger.down(KEYCODE_VOLUME_UP, 0, 0, eligible = true).consume)
        assertFalse(trigger.up(KEYCODE_VOLUME_UP, 100).consume)
        assertFalse(trigger.down(power, 0, 0, eligible = true).consume)
        // A press in progress is not disturbed by another key, and other keys keep its timer.
        trigger.down(KEYCODE_VOLUME_DOWN, 0, 1_000, eligible = true)
        assertEquals(KeyDecision(consume = false, timerAt = 1_800), trigger.down(KEYCODE_VOLUME_UP, 0, 1_100, eligible = true))
        assertEquals(KeyDecision(consume = false, timerAt = 1_800), trigger.up(KEYCODE_VOLUME_UP, 1_200))
        assertTrue(trigger.timer(1_800).trigger)
        // A release whose press the trigger never took goes to the system too.
        assertFalse(KeyTrigger().up(KEYCODE_VOLUME_DOWN, 50).consume)
    }

    @Test
    fun `screen off or a call leaves the key to the system`() {
        val trigger = KeyTrigger()
        assertEquals(KeyDecision(consume = false, timerAt = null), trigger.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = false))
        assertEquals(KeyDecision(consume = false, timerAt = null), trigger.up(KEYCODE_VOLUME_DOWN, 1_500))
        assertTrue(KeyTriggerPolicy.eligible(interactive = true, audioMode = KeyTriggerPolicy.AUDIO_MODE_NORMAL))
        assertFalse("screen off", KeyTriggerPolicy.eligible(interactive = false, audioMode = KeyTriggerPolicy.AUDIO_MODE_NORMAL))
        // AudioManager.MODE_RINGTONE, MODE_IN_CALL, MODE_IN_COMMUNICATION, MODE_CALL_SCREENING.
        listOf(1, 2, 3, 4).forEach { mode -> assertFalse("audio mode $mode", KeyTriggerPolicy.eligible(interactive = true, audioMode = mode)) }
    }

    @Test
    fun `a long press of volume up is the other long gesture`() {
        val trigger = KeyTrigger(KeyGesture.LONG_VOLUME_UP)
        assertFalse("volume down goes to the system", trigger.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = true).consume)
        assertFalse(trigger.up(KEYCODE_VOLUME_DOWN, 900).consume)
        assertEquals(KeyDecision(consume = true, timerAt = 2_800), trigger.down(KEYCODE_VOLUME_UP, 0, 2_000, eligible = true))
        assertTrue(trigger.timer(2_800).trigger)
        assertTrue(trigger.up(KEYCODE_VOLUME_UP, 3_000).consume)
    }

    @Test
    fun `a double press of volume down triggers without a volume step`() {
        val trigger = KeyTrigger(KeyGesture.DOUBLE_VOLUME_DOWN)
        assertEquals(KeyDecision(consume = true, timerAt = 500), trigger.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = true))
        assertEquals(KeyDecision(consume = true, timerAt = 450), trigger.up(KEYCODE_VOLUME_DOWN, 100))
        assertEquals(KeyDecision(consume = true, trigger = true, timerAt = null), trigger.down(KEYCODE_VOLUME_DOWN, 0, 450, eligible = true))
        assertEquals(KeyDecision(consume = true, timerAt = null), trigger.up(KEYCODE_VOLUME_DOWN, 520))
        assertTrue(trigger.idle)
    }

    @Test
    fun `a single press in double mode makes its volume step after the window`() {
        val trigger = KeyTrigger(KeyGesture.DOUBLE_VOLUME_DOWN)
        trigger.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = true)
        trigger.up(KEYCODE_VOLUME_DOWN, 100)
        assertEquals(KeyDecision(consume = false, timerAt = 450), trigger.timer(449))
        assertEquals(KeyDecision(consume = true, adjust = 1, timerAt = null), trigger.timer(450))
        assertTrue(trigger.idle)
        // A second press after the window (its timer ran late): the first press makes its step, the second starts anew.
        trigger.down(KEYCODE_VOLUME_DOWN, 0, 1_000, eligible = true)
        trigger.up(KEYCODE_VOLUME_DOWN, 1_080)
        assertEquals(KeyDecision(consume = true, adjust = 1, timerAt = 1_931), trigger.down(KEYCODE_VOLUME_DOWN, 0, 1_431, eligible = true))
        // A call between the presses: the first press makes its step and the second goes to the system.
        val calling = KeyTrigger(KeyGesture.DOUBLE_VOLUME_DOWN)
        calling.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = true)
        calling.up(KEYCODE_VOLUME_DOWN, 100)
        assertEquals(KeyDecision(consume = false, adjust = 1, timerAt = null), calling.down(KEYCODE_VOLUME_DOWN, 0, 200, eligible = false))
        assertTrue(calling.idle)
    }

    @Test
    fun `a held first press in double mode changes the volume like a held key`() {
        val trigger = KeyTrigger(KeyGesture.DOUBLE_VOLUME_DOWN)
        trigger.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = true)
        assertEquals(KeyDecision(consume = true, adjust = 1, timerAt = 600), trigger.timer(500))
        assertEquals(KeyDecision(consume = true, adjust = 1, timerAt = 700), trigger.timer(600))
        assertEquals(KeyDecision(consume = true, timerAt = null), trigger.up(KEYCODE_VOLUME_DOWN, 650))
        assertEquals("no step after the release", KeyDecision(consume = false, timerAt = null), trigger.timer(700))
        assertFalse("never a trigger", trigger.down(KEYCODE_VOLUME_DOWN, 0, 2_000, eligible = true).trigger)
    }

    @Test
    fun `changing the gesture drops a press in progress`() {
        val trigger = KeyTrigger()
        trigger.down(KEYCODE_VOLUME_DOWN, 0, 0, eligible = true)
        trigger.gesture = KeyGesture.LONG_VOLUME_UP
        assertTrue(trigger.idle)
        assertEquals(KeyDecision(consume = false, timerAt = null), trigger.timer(800))
        // Setting the same gesture again keeps the press.
        trigger.down(KEYCODE_VOLUME_UP, 0, 1_000, eligible = true)
        trigger.gesture = KeyGesture.LONG_VOLUME_UP
        assertFalse(trigger.idle)
    }

    @Test
    fun `gesture ids are stable and the default is a long press of volume down`() {
        assertEquals(listOf("longVolumeDown", "longVolumeUp", "doubleVolumeDown"), KeyGesture.entries.map { it.id })
        assertEquals(KeyGesture.LONG_VOLUME_DOWN, KeyGesture.DEFAULT)
        assertEquals(KeyGesture.DEFAULT, KeyGesture.of(null))
        assertEquals(KeyGesture.DEFAULT, KeyGesture.of("unknown"))
        assertEquals(KeyGesture.DOUBLE_VOLUME_DOWN, KeyGesture.of("doubleVolumeDown"))
        assertEquals(800L, KeyTriggerPolicy.LONG_PRESS_MILLIS)
        assertNull(KeyTrigger().timer(0).timerAt)
    }
}
