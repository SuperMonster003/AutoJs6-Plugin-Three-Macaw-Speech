package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.assistant

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.Collator
import java.util.Locale

/**
 * The rules of the digital assistant (roadmap P5.7): the target list of `VOICE_COMMAND` (D50, D53: installed entries only,
 * never the plugin itself, label order), the text a target receives (D49), the lock screen gate (D55) and the language
 * passed on (D50).
 * zh-CN: 数字助理的规则 (路线图 P5.7): `VOICE_COMMAND` 的目标列表 (D50, D53: 只列已安装的入口, 不含插件自身, 按 label 排序), 目标收到的文字 (D49),
 * 锁屏门控 (D55) 与转交的语言 (D50).
 */
class AssistantPolicyTest {

    private val self = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
    private val collator: Comparator<String> = Collator.getInstance(Locale.ENGLISH).let { collator -> Comparator { a, b -> collator.compare(a, b) } }

    private fun resolved(pkg: String, cls: String = "$pkg.VoiceCommandActivity", label: String? = pkg, exported: Boolean = true, enabled: Boolean = true) =
        ResolvedTarget(pkg, cls, label, exported, enabled)

    @Test
    fun `no target is installed and the assistant still dictates and copies`() {
        assertEquals(emptyList<VoiceCommandTarget>(), AssistantPolicy.targets(emptyList(), self, collator))
        // An app without the entry is never resolved; the plugin's own package never counts.
        assertEquals(emptyList<VoiceCommandTarget>(), AssistantPolicy.targets(listOf(resolved(self, label = "3-Macaw Speech")), self, collator))
    }

    @Test
    fun `targets are the exported and enabled entries sorted by label`() {
        val targets = AssistantPolicy.targets(
            listOf(
                resolved("org.example.stone", label = "3-Stone AI"),
                resolved("org.example.stove", label = "3-Stove Agent"),
                resolved("org.example.alpha", label = "alpha notes"),
                resolved("org.example.hidden", label = "Hidden", exported = false),
                resolved("org.example.off", label = "Off", enabled = false),
                resolved("org.example.stone", label = "3-Stone AI"),
                resolved("org.example.blank", label = "  "),
                resolved("org.example.twin", "org.example.twin.B", label = "Twin"),
                resolved("org.example.twin", "org.example.twin.A", label = "Twin"),
            ),
            self,
            collator,
        )
        assertEquals(
            listOf("3-Stone AI", "3-Stove Agent", "alpha notes", "org.example.blank", "Twin", "Twin"),
            targets.map { it.label },
        )
        assertEquals(listOf("org.example.twin.A", "org.example.twin.B"), targets.takeLast(2).map { it.className })
    }

    @Test
    fun `the text a target receives is trimmed and bounded`() {
        assertNull(AssistantPolicy.text(""))
        assertNull(AssistantPolicy.text(" \n\t "))
        assertEquals("open the settings", AssistantPolicy.text("  open the settings \n"))
        val long = "a".repeat(AssistantPolicy.MAX_TEXT_CHARS + 10)
        assertEquals(AssistantPolicy.MAX_TEXT_CHARS, AssistantPolicy.text(long)?.length)
        val split = "a".repeat(AssistantPolicy.MAX_TEXT_CHARS - 1) + String(Character.toChars(0x1F600))
        assertEquals("a surrogate pair is never cut", AssistantPolicy.MAX_TEXT_CHARS - 1, AssistantPolicy.text(split)?.length)
    }

    @Test
    fun `the lock screen asks to unlock before anything leaves the sheet`() {
        assertEquals(AssistantGate.NOW, AssistantPolicy.gate(keyguardLocked = false, sdk = 24))
        assertEquals(AssistantGate.NOW, AssistantPolicy.gate(keyguardLocked = false, sdk = 36))
        assertEquals(AssistantGate.WINDOW_UNLOCK, AssistantPolicy.gate(keyguardLocked = true, sdk = 24))
        assertEquals(AssistantGate.WINDOW_UNLOCK, AssistantPolicy.gate(keyguardLocked = true, sdk = 25))
        assertEquals(AssistantGate.REQUEST_UNLOCK, AssistantPolicy.gate(keyguardLocked = true, sdk = 26))
        assertEquals(AssistantGate.REQUEST_UNLOCK, AssistantPolicy.gate(keyguardLocked = true, sdk = 36))
    }

    @Test
    fun `the language passed on is the detected one or the model's only one`() {
        assertEquals("en", VoiceInputPolicy.spokenLanguage("<|en|>".removePrefix("<|").removeSuffix("|>"), listOf("zh", "en", "ja", "ko", "yue")))
        assertEquals("yue", VoiceInputPolicy.spokenLanguage("yue", listOf("zh", "en", "ja", "ko", "yue")))
        assertNull("no speech is not a language", VoiceInputPolicy.spokenLanguage("nospeech", listOf("zh", "en")))
        assertNull(VoiceInputPolicy.spokenLanguage("unk", listOf("zh", "en")))
        assertEquals("zh", VoiceInputPolicy.spokenLanguage(null, listOf("zh")))
        assertEquals("en-US", VoiceInputPolicy.spokenLanguage(null, listOf("en-US")))
        assertNull("a bilingual model without detection", VoiceInputPolicy.spokenLanguage(null, listOf("zh", "en")))
        assertNull(VoiceInputPolicy.spokenLanguage(null, emptyList()))
        assertEquals("org.autojs.plugin.action.VOICE_COMMAND", AssistantPolicy.ACTION_VOICE_COMMAND)
        assertEquals("org.autojs.plugin.extra.VOICE_LANGUAGE", AssistantPolicy.EXTRA_VOICE_LANGUAGE)
    }
}
