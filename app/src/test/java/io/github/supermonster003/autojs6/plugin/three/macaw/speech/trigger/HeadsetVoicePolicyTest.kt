package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The headset voice key (roadmap D68): only its sheet touches the headset, Android 12+ asks for "Nearby devices" first
 * except on the lock screen, and only a drop of the audio the headset caused ends the listening.
 * zh-CN: 耳机语音键 (路线图 D68): 只有它打开的弹层才操作耳机, Android 12+ 先申请 "附近的设备" (锁屏时除外), 只有耳机一方造成的音频断开才结束收音.
 */
class HeadsetVoicePolicyTest {

    @Test
    fun `only the sheet of the headset voice key touches the headset`() {
        listOf(null, TriggerSource.TILE, TriggerSource.WIDGET, TriggerSource.SHORTCUT, TriggerSource.FLOATING, TriggerSource.KEY).forEach { source ->
            assertEquals("source ${source?.id ?: "assist"}", HeadsetAccess.SKIP, HeadsetVoicePolicy.access(source, sdk = 35, granted = true, locked = false))
        }
    }

    @Test
    fun `android 12 and later ask for nearby devices unless the keyguard is up`() {
        val headset = TriggerSource.HEADSET
        assertEquals(HeadsetAccess.START, HeadsetVoicePolicy.access(headset, sdk = 31, granted = true, locked = false))
        assertEquals(HeadsetAccess.START, HeadsetVoicePolicy.access(headset, sdk = 37, granted = true, locked = true))
        assertEquals(HeadsetAccess.REQUEST, HeadsetVoicePolicy.access(headset, sdk = 31, granted = false, locked = false))
        assertEquals(HeadsetAccess.REQUEST, HeadsetVoicePolicy.access(headset, sdk = 37, granted = false, locked = false))
        // The system dialog does not show over the keyguard: the phone's microphone, the headset left as it is.
        assertEquals(HeadsetAccess.SKIP, HeadsetVoicePolicy.access(headset, sdk = 33, granted = false, locked = true))
    }

    @Test
    fun `below android 12 the install time permission suffices`() {
        listOf(24, 28, 30).forEach { sdk ->
            assertEquals("API $sdk", HeadsetAccess.START, HeadsetVoicePolicy.access(TriggerSource.HEADSET, sdk, granted = false, locked = true))
        }
        assertEquals(31, HeadsetVoicePolicy.CONNECT_PERMISSION_API)
    }

    @Test
    fun `only a drop the headset caused ends the listening`() {
        assertTrue("the voice key pressed again", HeadsetVoicePolicy.endedByHeadset(audioWasConnected = true, stopped = false))
        assertFalse("the link never came up", HeadsetVoicePolicy.endedByHeadset(audioWasConnected = false, stopped = false))
        assertFalse("the sheet stopped it itself", HeadsetVoicePolicy.endedByHeadset(audioWasConnected = true, stopped = true))
    }
}
