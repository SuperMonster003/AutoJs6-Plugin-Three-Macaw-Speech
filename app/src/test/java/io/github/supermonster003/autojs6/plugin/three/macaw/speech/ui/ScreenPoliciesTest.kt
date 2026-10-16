package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsModelVoices
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferencePolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.about.LegalBlock
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.about.LegalText
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.release.ReleaseHistoryAssets
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.release.ReleaseHistoryParser
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.UpdateVersionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.Locale

/**
 * Pure rules behind the screens of roadmap P5.2 - P5.4: the bundled release history, the update rules of 3-Stone AI, the
 * legal texts of the About page, the settings presets and the per-language default voice of the system engine.
 * zh-CN: 路线图 P5.2 - P5.4 界面背后的纯规则: 内置发行历史, 沿用 3-Stone AI 的更新规则, 关于页的法律文本, 设置页预设, 以及系统引擎按语言的默认语音.
 */
class ScreenPoliciesTest {

    @Test
    fun `the bundled changelog of every language parses into releases`() {
        val assets = mapOf(
            "zh-Hans" to "CHANGELOG-zh-Hans.md", "zh-CN" to "CHANGELOG-zh-Hans.md", "zh-HK" to "CHANGELOG-zh-Hant-HK.md", "zh-MO" to "CHANGELOG-zh-Hant-HK.md",
            "zh-TW" to "CHANGELOG-zh-Hant-TW.md", "zh-Hant" to "CHANGELOG-zh-Hant-TW.md", "en-US" to "CHANGELOG-en.md", "de" to "CHANGELOG-en.md",
            "ar" to "CHANGELOG-ar.md", "ja" to "CHANGELOG-ja.md", "ko" to "CHANGELOG-ko.md", "fr" to "CHANGELOG-fr.md", "es" to "CHANGELOG-es.md", "ru" to "CHANGELOG-ru.md",
        )
        assets.forEach { (tag, file) -> assertEquals(tag, "doc/$file", ReleaseHistoryAssets.assetFor(Locale.forLanguageTag(tag))) }
        assets.values.distinct().forEach { file ->
            val text = String(Files.readAllBytes(RepositoryFiles.path("app/src/main/assets/doc/$file")), Charsets.UTF_8)
            val entries = ReleaseHistoryParser.parse(text)
            assertTrue(file, entries.isNotEmpty())
            assertEquals(file, "v1.0.0", entries.first().version)
            assertTrue(file, entries.first().date?.matches(Regex("\\d{4}/\\d{2}/\\d{2}")) == true)
            assertTrue(file, entries.first().items.size >= 5 && entries.first().items.all { it.tag != null && it.text.isNotBlank() })
        }
    }

    @Test
    fun `the release dialect ignores titles and keeps untagged bullets`() {
        val entries = ReleaseHistoryParser.parse("### Release history\n\n# v2.0.0\n\n###### 2026/11/01\n\n* `Fix` one\n* plain\n\n## v1.0.0\n* `New` first\n")
        assertEquals(listOf("v2.0.0", "v1.0.0"), entries.map { it.version })
        assertEquals(listOf("Fix", null), entries.first().items.map { it.tag })
        assertEquals(null, entries.last().date)
    }

    @Test
    fun `newer versions follow semantic versioning`() {
        assertTrue(UpdateVersionPolicy.isNewer("v1.0.1", "1.0.0"))
        assertTrue(UpdateVersionPolicy.isNewer("1.1", "1.0.9"))
        assertTrue(UpdateVersionPolicy.isNewer("v1.0.0", "1.0.0-beta"))
        assertTrue(UpdateVersionPolicy.isNewer("1.0.0-rc.2", "1.0.0-rc.1"))
        assertFalse(UpdateVersionPolicy.isNewer("v1.0.0", "1.0.0"))
        assertFalse(UpdateVersionPolicy.isNewer("1.0.0-beta", "1.0.0"))
        assertFalse(UpdateVersionPolicy.isNewer("v0.9.9", "1.0.0"))
        assertFalse(UpdateVersionPolicy.isNewer("latest", "1.0.0"))
    }

    @Test
    fun `automatic checks run at most every 12 hours and ignored tags stay ignored`() {
        val values = HashMap<String, Any>()
        var now = 1_000_000L
        val store = AppUpdateStore(object : AppUpdateStore.Storage {
            override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
            override fun putBoolean(key: String, value: Boolean) { values[key] = value }
            override fun getLong(key: String, default: Long) = values[key] as? Long ?: default
            override fun putLong(key: String, value: Long) { values[key] = value }
            @Suppress("UNCHECKED_CAST")
            override fun getStrings(key: String) = values[key] as? Set<String> ?: emptySet()
            override fun putStrings(key: String, value: Set<String>) { values[key] = value }
        }) { now }
        assertTrue(store.automaticChecks)
        assertTrue(store.shouldCheckAutomatically())
        store.recordAutomaticAttempt()
        now += AppUpdateStore.INTERVAL_MILLIS - 1
        assertFalse(store.shouldCheckAutomatically())
        now += 1
        assertTrue(store.shouldCheckAutomatically())
        now = 10L
        assertTrue("a clock set back counts as due", store.shouldCheckAutomatically())
        store.automaticChecks = false
        assertFalse(store.shouldCheckAutomatically())
        store.ignore("v1.1.0")
        store.ignore("v1.2.0")
        store.stopIgnoring(listOf("v1.1.0"))
        assertEquals(setOf("v1.2.0"), store.ignoredTags)
    }

    @Test
    fun `the legal texts read as headings, rows and paragraphs`() {
        val notices = LegalText.markdown(String(Files.readAllBytes(RepositoryFiles.path("THIRD_PARTY_NOTICES.md")), Charsets.UTF_8))
        assertEquals(LegalBlock.Heading(1, "Third-party notices"), notices.first())
        val rows = notices.filterIsInstance<LegalBlock.Row>()
        val espeak = rows.single { it.cells.first().second.startsWith("eSpeak NG") }
        assertEquals("GPL-3.0-or-later", espeak.cells.single { it.first == "License" }.second)
        assertTrue("no table separator becomes a row", rows.none { row -> row.cells.all { it.second.all { c -> c == '-' } } })
        assertTrue(notices.filterIsInstance<LegalBlock.Bullet>().any { it.text.startsWith("License of the APK") && it.text.contains("GPL-3.0-or-later") })
        assertTrue("no code marks remain", notices.none { it.toString().contains('`') })

        val gpl = LegalText.plainText(String(Files.readAllBytes(RepositoryFiles.path("third_party/licenses/GPL-3.0.txt")), Charsets.UTF_8))
        assertTrue((gpl.first() as LegalBlock.Paragraph).text.startsWith("GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007"))
        assertTrue(gpl.any { it is LegalBlock.Paragraph && it.text.startsWith("15. Disclaimer of Warranty.") })
        assertTrue("hard wraps are joined", gpl.all { it !is LegalBlock.Paragraph || '\n' !in it.text })
        val unicode = LegalText.plainText(String(Files.readAllBytes(RepositoryFiles.path("third_party/licenses/COPYING.UCD")), Charsets.UTF_8))
        assertTrue(unicode.any { it is LegalBlock.Paragraph && it.text.startsWith("COPYRIGHT AND PERMISSION NOTICE") })
    }

    @Test
    fun `the settings presets clamp stored values`() {
        assertEquals(listOf(600L, 1200L, 2000L), SpeechPreferencePolicy.ENDPOINT_MILLIS)
        assertEquals(1200L, SpeechPreferencePolicy.endpoint(999))
        assertEquals(600L, SpeechPreferencePolicy.endpoint(600))
        assertEquals(listOf(1, 5, 15, 30), SpeechPreferencePolicy.IDLE_MINUTES)
        assertEquals(5, SpeechPreferencePolicy.idleMinutes(7))
        assertEquals(0.5f, SpeechPreferencePolicy.speed(0.1f))
        assertEquals(2.0f, SpeechPreferencePolicy.speed(9f))
        assertEquals(1.3f, SpeechPreferencePolicy.speed(1.26f))
    }

    @Test
    fun `a voice chosen per language wins while it exists`() {
        val models = listOf(
            TtsModelVoices("tts.melo", listOf("zh", "en"), 1, 44100, isDefault = true),
            TtsModelVoices("tts.aishell3", listOf("zh"), 174, 8000, isDefault = false),
        )
        val automatic = TtsVoiceTable(models)
        assertEquals("tts.melo/0", automatic.defaultVoice("zh", "CN")?.name)
        val chosen = TtsVoiceTable(models, preferredVoices = mapOf("zh" to "tts.aishell3/20", "en" to "tts.gone/1", "ja" to "tts.melo/0"))
        assertEquals("tts.aishell3/20", chosen.defaultVoice("zho", "CHN")?.name)
        assertEquals("a missing voice falls back to the rules", "tts.melo/0/en-US", chosen.defaultVoice("en", "US")?.name)
        assertEquals("a voice of another language is ignored", null, chosen.defaultVoice("ja", "JP"))
    }
}
