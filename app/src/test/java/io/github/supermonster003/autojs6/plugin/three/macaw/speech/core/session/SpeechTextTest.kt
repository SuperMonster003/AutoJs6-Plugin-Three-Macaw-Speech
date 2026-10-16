package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Text rules (`punctuation`, joining utterances, SenseVoice tags) and session admission (roadmap D16).
 * zh-CN: 文本规则 (`punctuation`, 语句连接, SenseVoice 标签) 与会话准入 (路线图 D16).
 */
class SpeechTextTest {

    @Test
    fun `punctuation is kept or dropped`() {
        val zh = "\u4f60\u597d\uff0c\u4e16\u754c\u3002"
        assertEquals(zh, SpeechText.clean("  $zh ", punctuation = true))
        assertEquals("\u4f60\u597d\u4e16\u754c", SpeechText.clean(zh, punctuation = false))
        assertEquals("Hello world It's 3.5 o'clock", SpeechText.clean("Hello,world! It's 3.5 o'clock.", punctuation = false))
        assertEquals("a b", SpeechText.clean("a   b", punctuation = true))
        assertEquals("", SpeechText.clean("  ", punctuation = false))
    }

    @Test
    fun `utterances join with a space except between CJK sides`() {
        assertEquals("\u4f60\u597d\u4e16\u754c", SpeechText.join(listOf("\u4f60\u597d", "\u4e16\u754c")))
        assertEquals("hello world", SpeechText.join(listOf("hello", " ", "world")))
        assertEquals("\u4eca\u5929 MONDAY \u660e\u5929", SpeechText.join(listOf("\u4eca\u5929", "MONDAY", "\u660e\u5929")))
        assertEquals("\u3053\u3093\u306b\u3061\u306f\u3002\u305d\u3046", SpeechText.join(listOf("\u3053\u3093\u306b\u3061\u306f\u3002", "\u305d\u3046")))
        assertTrue(SpeechText.isCjk('\u4e00'))
        assertFalse(SpeechText.isCjk('a'))
    }

    @Test
    fun `SenseVoice tags lose their brackets and case`() {
        assertEquals("zh", SpeechText.tag("<|zh|>"))
        assertEquals("neutral", SpeechText.tag("<|NEUTRAL|>"))
        assertEquals("speech", SpeechText.tag("<|Speech|>"))
        assertNull(SpeechText.tag(""))
        assertNull(SpeechText.tag(null))
        assertNull(SpeechText.tag("<|bad tag|>"))
    }

    @Test
    fun `one microphone session for the whole plugin and four external sessions per caller`() {
        val table = SessionTable()
        val microphone = table.reserve(uid = 10, microphone = true)
        assertTrue(table.microphoneInUse)
        busy { table.reserve(uid = 11, microphone = true) }
        busy { table.reserve(uid = 10, microphone = true) }
        val external = List(4) { table.reserve(uid = 10, microphone = false) }
        busy { table.reserve(uid = 10, microphone = false) }
        table.reserve(uid = 11, microphone = false)
        assertEquals(4, table.externalSessions(10))
        table.release(external[0])
        table.release(external[0])
        assertEquals(3, table.externalSessions(10))
        table.reserve(uid = 10, microphone = false)
        table.release(microphone)
        table.release(microphone)
        assertFalse(table.microphoneInUse)
        table.reserve(uid = 11, microphone = true)
    }

    private fun busy(block: () -> Unit) {
        val error = assertThrows(SpeechException::class.java) { block() }
        assertEquals(SpeechErrorCodes.SESSION_BUSY, error.code)
    }
}
