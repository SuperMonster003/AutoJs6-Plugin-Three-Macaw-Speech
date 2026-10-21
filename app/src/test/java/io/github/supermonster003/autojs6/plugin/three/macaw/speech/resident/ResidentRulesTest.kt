package io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident

import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules of the resident wake word (roadmap P7.4, D67 / D69 - D80): listening time and deadline, the power rule, the
 * resume after a visible screen, a reboot or an update, availability, the wake words and their sensitivity, the route of
 * a hit, the background block, the other resident listener apps and the read-aloud language.
 * zh-CN: 常驻唤醒的规则 (路线图 P7.4, D67 / D69 - D80): 监听时长与截止时刻, 电源规则, 可见界面, 重启或更新后的恢复, 可用性, 唤醒词与灵敏度, 命中的
 * 去向, 后台阻断, 其他常驻监听应用与朗读语言.
 */
class ResidentRulesTest {

    @Test
    fun `the listening time counts from turning on and the duration defaults to always`() {
        assertEquals(listOf("10", "30", "60", "always"), ListeningDuration.entries.map { it.id })
        assertEquals(ListeningDuration.ALWAYS, ListeningDuration.of(null))
        assertEquals(ListeningDuration.ALWAYS, ListeningDuration.of("15"))
        assertEquals(ListeningDuration.MINUTES_30, ListeningDuration.of("30"))
        assertEquals(NOW + 10 * MINUTE, ListeningDuration.MINUTES_10.deadline(NOW))
        assertEquals(NOW + 60 * MINUTE, ListeningDuration.MINUTES_60.deadline(NOW))
        assertNull(ListeningDuration.ALWAYS.deadline(NOW))

        val defaults = ResidentSettings()
        assertEquals("D77: always on and not only while charging by default", ListeningDuration.ALWAYS, defaults.duration)
        assertFalse(defaults.chargingOnly)
        assertTrue("D69: read aloud by default", defaults.readAloud)
        assertNull(defaults.turnedOn(NOW).deadline)
        val thirty = defaults.copy(duration = ListeningDuration.MINUTES_30).turnedOn(NOW)
        assertEquals(NOW + 30 * MINUTE, thirty.deadline)
        assertFalse(thirty.expired(NOW + 30 * MINUTE - 1))
        assertTrue(thirty.expired(NOW + 30 * MINUTE))
    }

    @Test
    fun `another duration counts from now while on and is only remembered while off`() {
        val on = ResidentSettings(duration = ListeningDuration.MINUTES_60).turnedOn(NOW)
        val later = NOW + 20 * MINUTE
        assertEquals(later + 10 * MINUTE, on.withDuration(ListeningDuration.MINUTES_10, enabled = true, now = later).deadline)
        assertNull(on.withDuration(ListeningDuration.ALWAYS, enabled = true, now = later).deadline)
        val off = on.withDuration(ListeningDuration.MINUTES_10, enabled = false, now = later)
        assertEquals(ListeningDuration.MINUTES_10, off.duration)
        assertEquals("the next start counts it", on.deadline, off.deadline)
        assertEquals(later + 10 * MINUTE, off.turnedOn(later).deadline)
    }

    @Test
    fun `the deadline comes before the power rule and the time runs while paused`() {
        val charging = ResidentSettings(duration = ListeningDuration.MINUTES_10, chargingOnly = true).turnedOn(NOW)
        assertEquals(ResidentGate.LISTEN, ResidentGate.of(charging, NOW, plugged = true))
        assertEquals(ResidentGate.PAUSED, ResidentGate.of(charging, NOW + MINUTE, plugged = false))
        assertEquals("unplugged time counts", ResidentGate.EXPIRED, ResidentGate.of(charging, NOW + 10 * MINUTE, plugged = false))
        assertEquals(ResidentGate.EXPIRED, ResidentGate.of(charging, NOW + 10 * MINUTE, plugged = true))
        val anyPower = ResidentSettings().turnedOn(NOW)
        assertEquals(ResidentGate.LISTEN, ResidentGate.of(anyPower, NOW + 1000 * MINUTE, plugged = false))
    }

    @Test
    fun `a visible screen, a reboot or an update resumes an enabled switch unless its time ran out`() {
        val timed = ResidentSettings(duration = ListeningDuration.MINUTES_10).turnedOn(NOW)
        assertEquals(ResidentResume.NOTHING, ResidentResume.of(enabled = false, settings = timed, now = NOW + 20 * MINUTE))
        assertEquals(ResidentResume.RESUME, ResidentResume.of(enabled = true, settings = timed, now = NOW + 9 * MINUTE))
        assertEquals("D74 / D77: off, without a reminder", ResidentResume.TURN_OFF, ResidentResume.of(enabled = true, settings = timed, now = NOW + 10 * MINUTE))
        assertEquals(ResidentResume.RESUME, ResidentResume.of(enabled = true, settings = ResidentSettings().turnedOn(NOW), now = NOW + 100_000 * MINUTE))
    }

    @Test
    fun `the switch names the first missing piece`() {
        assertEquals(ResidentAvailability.MICROPHONE, ResidentAvailability.of(microphoneGranted = false, wakeModels = 0, recognitionModels = 0))
        assertEquals(ResidentAvailability.NO_WAKE_MODEL, ResidentAvailability.of(microphoneGranted = true, wakeModels = 0, recognitionModels = 3))
        assertEquals("the sheet dictates with a recognition model", ResidentAvailability.NO_RECOGNITION_MODEL, ResidentAvailability.of(true, 1, 0))
        assertEquals(ResidentAvailability.READY, ResidentAvailability.of(true, 1, 1))
    }

    @Test
    fun `wake words follow roadmap D70`() {
        assertEquals(listOf("\u5c0f\u9e66\u5c0f\u9e66", "Hey Macaw"), ResidentKeywords.DEFAULT)
        assertTrue(ResidentKeywords.valid(ResidentKeywords.DEFAULT))
        val newline = Char(10).toString()
        assertEquals(listOf("Hey Macaw", "OK Macaw"), ResidentKeywords.parse(listOf("  Hey Macaw ", "", "OK Macaw", "   ").joinToString(newline)))
        assertNull("at least one", ResidentKeywords.parse(" " + newline))
        assertNull("at most five", ResidentKeywords.parse((1..6).joinToString(newline) { "word $it" }))
        assertEquals(5, ResidentKeywords.parse((1..5).joinToString(newline) { "word $it" })?.size)
        assertNull("no repeats, case ignored", ResidentKeywords.parse("Hey Macaw" + newline + "hey macaw"))
        assertEquals(1, ResidentKeywords.parse("a".repeat(32))?.size)
        assertNull("at most 32 characters", ResidentKeywords.parse("a".repeat(33)))
        assertEquals("32 Han characters", 1, ResidentKeywords.parse("\u9e66".repeat(32))?.size)
        assertFalse("no characters beyond the BMP", ResidentKeywords.valid(listOf(String(Character.toChars(0x1F99C)))))
        assertFalse("no control characters", ResidentKeywords.valid(listOf("Hey" + Char(9) + "Macaw")))
        assertFalse("trimmed", ResidentKeywords.valid(listOf(" Hey Macaw")))
        assertTrue(runCatching { ResidentSettings(keywords = emptyList()) }.isFailure)
    }

    @Test
    fun `the sensitivity maps to the keyword threshold`() {
        assertEquals(listOf(0.15f, null, 0.35f), WakeSensitivity.entries.map { it.threshold })
        assertEquals(WakeSensitivity.MEDIUM, WakeSensitivity.of(null))
        assertEquals(WakeSensitivity.MEDIUM, WakeSensitivity.of("max"))
        assertEquals(WakeSensitivity.HIGH, WakeSensitivity.of("high"))
        assertEquals(WakeSensitivity.LOW, WakeSensitivity.of("low"))
    }

    @Test
    fun `a hit opens the sheet where the plugin may, else posts tap to speak, and is ignored while the sheet shows`() {
        assertEquals(HitRoute.IGNORE, HitRoute.of(sheetVisible = true, assistantActive = true, overlayGranted = true, sdk = 35))
        assertEquals("D60: the assistant's session", HitRoute.OPEN, HitRoute.of(false, assistantActive = true, overlayGranted = false, sdk = 35))
        assertEquals(HitRoute.OPEN, HitRoute.of(false, assistantActive = false, overlayGranted = true, sdk = 35))
        assertEquals("no background start restriction before Android 10", HitRoute.OPEN, HitRoute.of(false, false, false, sdk = 28))
        assertEquals(HitRoute.NOTIFY, HitRoute.of(false, false, false, sdk = 29))
        assertTrue("D71: asked when turning on", HitRoute.askOverlay(assistantActive = false, overlayGranted = false, sdk = 33))
        assertFalse(HitRoute.askOverlay(assistantActive = true, overlayGranted = false, sdk = 33))
        assertFalse(HitRoute.askOverlay(assistantActive = false, overlayGranted = true, sdk = 33))
        assertFalse(HitRoute.askOverlay(assistantActive = false, overlayGranted = false, sdk = 28))
    }

    @Test
    fun `only a background open that gets no microphone at once waits for a tap`() {
        val opened = 1_000_000L
        assertTrue(ResidentBlock.blocked(SpeechErrorCodes.PERMISSION_DENIED, opened, opened + 14_999))
        assertFalse("later: another app (a call) may hold it", ResidentBlock.blocked(SpeechErrorCodes.PERMISSION_DENIED, opened, opened + 15_000))
        assertFalse("opened while visible", ResidentBlock.blocked(SpeechErrorCodes.PERMISSION_DENIED, null, opened))
        assertFalse(ResidentBlock.blocked(SpeechErrorCodes.SESSION_BUSY, opened, opened + 1))
        assertFalse(ResidentBlock.blocked(null, opened, opened + 1))
    }

    @Test
    fun `a stop names its reason`() {
        assertEquals(ResidentStop.MICROPHONE, ResidentStop.of(SpeechErrorCodes.PERMISSION_DENIED))
        assertEquals(ResidentStop.MODEL, ResidentStop.of(SpeechErrorCodes.MODEL_MISSING))
        assertEquals(ResidentStop.KEYWORDS, ResidentStop.of(SpeechErrorCodes.INVALID_OPTIONS))
        assertEquals(ResidentStop.ENDED, ResidentStop.of(null))
        assertEquals(ResidentStop.ENDED, ResidentStop.of(SpeechErrorCodes.SESSION_BUSY))
    }

    @Test
    fun `the other resident listener apps exclude this package and name a valid settings activity`() {
        val own = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
        val stove = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
        val stone = "io.github.supermonster003.autojs6.plugin.three.stone.ai"
        val found = listOf(
            ResidentListenerApp(own, "3-Macaw Speech", "$own.ui.settings.ResidentSettingsAlias"),
            ResidentListenerApp(stove, "3-Stove Agent", ".ui.settings.SettingsActivity"),
            ResidentListenerApp(stove, "3-Stove Agent", null),
            ResidentListenerApp(stone, "3-stone AI", "not a class"),
            ResidentListenerApp("example.app", "Another", " example.app.Settings "),
        )
        val others = ResidentListenerApps.others(found, own)
        assertEquals("sorted by label, case ignored, one entry per package", listOf("3-stone AI", "3-Stove Agent", "Another"), others.map { it.label })
        assertEquals(listOf(null, "$stove.ui.settings.SettingsActivity", "example.app.Settings"), others.map { it.settingsClass })
        assertTrue(ResidentListenerApps.others(found.take(1), own).isEmpty())
    }

    @Test
    fun `the read-aloud language comes from the model, else the script of the text`() {
        assertEquals("en-US", ReadAloudPolicy.language("en-US", "\u4f60\u597d"))
        assertEquals("zh", ReadAloudPolicy.language(null, "\u4f60\u597d world"))
        assertEquals("zh", ReadAloudPolicy.language(" ", "\u4f60\u597d"))
        assertEquals("en", ReadAloudPolicy.language(null, "hello"))
        val models = listOf(listOf("zh", "en"), listOf("fr-FR"))
        assertTrue(ReadAloudPolicy.ownEngine(models, "zh-CN"))
        assertTrue(ReadAloudPolicy.ownEngine(models, "FR"))
        assertFalse("the system default engine reads it", ReadAloudPolicy.ownEngine(models, "ja"))
        assertFalse(ReadAloudPolicy.ownEngine(emptyList(), "en"))
    }

    private companion object {
        const val NOW = 1_790_000_000_000L
        const val MINUTE = 60_000L
    }
}
