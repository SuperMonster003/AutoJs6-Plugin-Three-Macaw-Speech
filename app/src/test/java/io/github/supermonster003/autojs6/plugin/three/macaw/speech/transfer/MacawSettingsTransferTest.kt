package io.github.supermonster003.autojs6.plugin.three.macaw.speech.transfer

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferencePolicy
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.data.SpeechPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateStore
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import org.autojs.plugin.common.api.AutoJs6PluginSettingsTransferContract as Contract

/**
 * The settings document of the settings transfer (roadmap P10.1, host settings roadmap P18.1).
 * zh-CN: 设置迁移的设置文档 (路线图 P10.1, 宿主设置路线图 P18.1).
 */
class MacawSettingsTransferTest {

    private val keywords = listOf("Hey Macaw", "Hello Parrot").joinToString(ResidentPreferences.LINE)

    private fun files(vararg initial: Pair<String, Map<String, Any>>): Map<String, FakeSharedPreferences> {
        val given = initial.toMap()
        return MacawSettingsTransfer.STORE_NAMES.associateWith { FakeSharedPreferences(given[it].orEmpty()) }
    }

    private fun transfer(files: Map<String, FakeSharedPreferences>) = MacawSettingsTransfer { name -> files.getValue(name) }

    private fun entry(type: String, value: Any?): JSONObject = JSONObject().put(Contract.VALUE_KEY_TYPE, type).put(Contract.VALUE_KEY_VALUE, value)

    private fun stored(): Map<String, FakeSharedPreferences> = files(
        SpeechPreferences.FILE to mapOf(
            SpeechPreferences.KEY_ENDPOINT to 2000L,
            SpeechPreferences.KEY_VAD to false,
            SpeechPreferences.KEY_SPEED to 1.3f,
            SpeechPreferences.KEY_IDLE_MINUTES to 15,
            SpeechPreferences.KEY_METERED to true,
            "speaker.kokoro-en" to 3,
            "voice.vits-zh" to "vits-zh-aishell3",
            // Records of this device. zh-CN: 本机记录.
            SpeechPreferences.KEY_NOTIFICATIONS_ASKED to true,
            SpeechPreferences.KEY_FILE_ACTION to "transcribe",
            SpeechPreferences.KEY_FILE_LYRICS to true,
            "unrelated" to "kept here",
        ),
        TriggerPreferences.FILE to mapOf(
            TriggerPreferences.KEY_FLOATING to true,
            TriggerPreferences.KEY_FLOATING_RIGHT to false,
            TriggerPreferences.KEY_FLOATING_Y to 0.25f,
            TriggerPreferences.KEY_GESTURE to "doubleVolumeDown",
        ),
        ResidentPreferences.FILE to mapOf(
            ResidentPreferences.KEY_KEYWORDS to keywords,
            ResidentPreferences.KEY_SENSITIVITY to "high",
            ResidentPreferences.KEY_DURATION to "always",
            ResidentPreferences.KEY_CHARGING to true,
            ResidentPreferences.KEY_READ_ALOUD to false,
            ResidentPreferences.KEY_POWER_SAVING to true,
            ResidentPreferences.KEY_ONLY_ENROLLED to true,
            ResidentPreferences.KEY_DEADLINE to 1_800_000_000_000L,
        ),
        AppearancePreferences.FILE to mapOf(
            AppearancePreferences.KEY_LANGUAGE to "ja",
            AppearancePreferences.KEY_DARK_MODE to AppearancePreferences.DARK,
            AppearancePreferences.KEY_COLOR to 0x6750A4,
        ),
        AppUpdateStore.FILE to mapOf(
            AppUpdateStore.KEY_AUTOMATIC to false,
            "ignored-tags" to setOf("v1.4.0"),
            "last-automatic-attempt" to 1_700_000_000_000L,
        ),
    )

    @Test
    fun `the document has one store per file and leaves records of this device out`() {
        val exported = transfer(stored()).exportSettings()

        assertEquals(MacawSettingsTransfer.STORE_NAMES.toSet(), exported.settings.keys().asSequence().toSet())
        assertEquals(22, exported.entries)
        assertFalse("nothing in the document is a secret", exported.sensitive)

        val speech = exported.settings.getJSONObject(SpeechPreferences.FILE)
        assertEquals(
            setOf("endpointMillis", "vad", "speed", "idleMinutes", "allowMeteredDownloads", "speaker.kokoro-en", "voice.vits-zh"),
            speech.keys().asSequence().toSet(),
        )
        assertEquals(Contract.TYPE_LONG, speech.getJSONObject("endpointMillis").getString(Contract.VALUE_KEY_TYPE))
        assertEquals(Contract.TYPE_INT, speech.getJSONObject("speaker.kokoro-en").getString(Contract.VALUE_KEY_TYPE))
        assertFalse(exported.settings.getJSONObject(ResidentPreferences.FILE).has(ResidentPreferences.KEY_DEADLINE))
        assertEquals(setOf(AppUpdateStore.KEY_AUTOMATIC), exported.settings.getJSONObject(AppUpdateStore.FILE).keys().asSequence().toSet())
    }

    @Test
    fun `an exported document imports into a fresh device unchanged`() {
        val source = stored()
        val exported = transfer(source).exportSettings()
        val target = files()

        val imported = transfer(target).importSettings(JSONObject(exported.settings.toString()))

        assertEquals(exported.entries, imported.written)
        assertEquals(0, imported.skipped)
        assertFalse(imported.restartNeeded)
        target.forEach { (name, file) ->
            assertEquals("one commit for $name", 1, file.commits)
            val expected = exported.settings.getJSONObject(name).keys().asSequence().associateWith { source.getValue(name).values[it] }
            assertEquals(name, expected, file.values)
        }
        assertEquals(exported.entries, transfer(target).exportSettings().entries)
    }

    @Test
    fun `an empty device exports an empty document`() {
        val exported = transfer(files()).exportSettings()

        assertEquals(0, exported.settings.length())
        assertEquals(0, exported.entries)
        assertFalse(exported.sensitive)
    }

    @Test
    fun `unknown stores and keys, other tags and values the settings screen cannot save are skipped`() {
        val target = files(SpeechPreferences.FILE to mapOf("unrelated" to "kept here", SpeechPreferences.KEY_VAD to true))
        val document = JSONObject()
            .put("future-store", JSONObject().put("a", entry(Contract.TYPE_BOOL, true)).put("b", entry(Contract.TYPE_INT, 1)))
            .put(AppUpdateStore.FILE, 5)
            .put(
                SpeechPreferences.FILE, JSONObject()
                    .put(SpeechPreferences.KEY_ENDPOINT, entry(Contract.TYPE_LONG, 600))
                    .put(SpeechPreferences.KEY_IDLE_MINUTES, entry(Contract.TYPE_INT, 7))
                    .put(SpeechPreferences.KEY_SPEED, entry(Contract.TYPE_FLOAT, 3.0))
                    .put(SpeechPreferences.KEY_METERED, entry(Contract.TYPE_INT, 1))
                    .put(SpeechPreferences.KEY_NOTIFICATIONS_ASKED, entry(Contract.TYPE_BOOL, true))
                    .put("speaker.", entry(Contract.TYPE_INT, 1))
                    .put("speaker.kokoro-en", entry(Contract.TYPE_INT, -1))
                    .put("voice.vits-zh", entry(Contract.TYPE_STRING, "vits-zh-aishell3"))
                    .put("voice.piper-en", "not an entry"),
            )
            .put(
                TriggerPreferences.FILE, JSONObject()
                    .put(TriggerPreferences.KEY_GESTURE, entry(Contract.TYPE_STRING, "shake"))
                    .put(TriggerPreferences.KEY_FLOATING_Y, entry(Contract.TYPE_FLOAT, 1.5))
                    .put(TriggerPreferences.KEY_FLOATING, entry(Contract.TYPE_BOOL, true)),
            )
            .put(
                ResidentPreferences.FILE, JSONObject()
                    .put(ResidentPreferences.KEY_KEYWORDS, entry(Contract.TYPE_STRING, listOf("Macaw", "macaw").joinToString(ResidentPreferences.LINE)))
                    .put(ResidentPreferences.KEY_SENSITIVITY, entry(Contract.TYPE_STRING, "extreme"))
                    .put(ResidentPreferences.KEY_DURATION, entry(Contract.TYPE_STRING, "30"))
                    .put(ResidentPreferences.KEY_DEADLINE, entry(Contract.TYPE_LONG, 1L)),
            )
            .put(
                AppearancePreferences.FILE, JSONObject()
                    .put(AppearancePreferences.KEY_LANGUAGE, entry(Contract.TYPE_STRING, "de"))
                    .put(AppearancePreferences.KEY_COLOR, entry(Contract.TYPE_INT, -16777216)),
            )

        val imported = transfer(target).importSettings(document)

        // future-store 2, a store that is not an object 1, speech 7 (idle 7, speed 3.0, metered as int, the
        // notification record, an empty speaker family name, a negative speaker, an entry that is not an object),
        // trigger 2, resident 3, appearance 1.
        // zh-CN: 未知 store 2, 非对象 store 1, speech 7, trigger 2, resident 3, appearance 1.
        assertEquals(16, imported.skipped)
        assertEquals(5, imported.written)
        assertEquals(
            mapOf("unrelated" to "kept here", SpeechPreferences.KEY_VAD to true, SpeechPreferences.KEY_ENDPOINT to 600L, "voice.vits-zh" to "vits-zh-aishell3"),
            target.getValue(SpeechPreferences.FILE).values,
        )
        assertEquals(mapOf<String, Any>(TriggerPreferences.KEY_FLOATING to true), target.getValue(TriggerPreferences.FILE).values)
        assertEquals(mapOf<String, Any>(ResidentPreferences.KEY_DURATION to "30"), target.getValue(ResidentPreferences.FILE).values)
        assertEquals(mapOf<String, Any>(AppearancePreferences.KEY_COLOR to -16777216), target.getValue(AppearancePreferences.FILE).values)
        assertEquals(0, target.getValue(AppUpdateStore.FILE).commits)
    }

    @Test
    fun `numbers without a fraction are accepted, numbers with one are not`() {
        val target = files()
        val document = JSONObject().put(
            SpeechPreferences.FILE, JSONObject()
                .put(SpeechPreferences.KEY_ENDPOINT, entry(Contract.TYPE_LONG, 2000.0))
                .put(SpeechPreferences.KEY_IDLE_MINUTES, entry(Contract.TYPE_INT, 15.5))
                .put(SpeechPreferences.KEY_SPEED, entry(Contract.TYPE_FLOAT, 2)),
        )

        val imported = transfer(target).importSettings(document)

        assertEquals(2, imported.written)
        assertEquals(1, imported.skipped)
        assertEquals(mapOf<String, Any>(SpeechPreferences.KEY_ENDPOINT to 2000L, SpeechPreferences.KEY_SPEED to 2f), target.getValue(SpeechPreferences.FILE).values)
    }

    @Test
    fun `the idle release time never travels as 0`() {
        // Roadmap P11.2.2 (D205): "never" is stored and transferred as 0 and imported again on the other device.
        // zh-CN: 路线图 P11.2.2 (D205): "不释放" 以 0 保存与迁移, 在另一台设备上照样导入.
        val source = files(SpeechPreferences.FILE to mapOf(SpeechPreferences.KEY_IDLE_MINUTES to SpeechPreferencePolicy.NEVER_IDLE_MINUTES))
        val exported = transfer(source).exportSettings()
        val idle = exported.settings.getJSONObject(SpeechPreferences.FILE).getJSONObject(SpeechPreferences.KEY_IDLE_MINUTES)
        assertEquals(Contract.TYPE_INT, idle.getString(Contract.VALUE_KEY_TYPE))
        assertEquals(0, idle.getInt(Contract.VALUE_KEY_VALUE))

        val target = files()
        val imported = transfer(target).importSettings(exported.settings)

        assertEquals(1, imported.written)
        assertEquals(0, imported.skipped)
        assertEquals(mapOf<String, Any>(SpeechPreferences.KEY_IDLE_MINUTES to 0), target.getValue(SpeechPreferences.FILE).values)
        val negative = JSONObject().put(SpeechPreferences.FILE, JSONObject().put(SpeechPreferences.KEY_IDLE_MINUTES, entry(Contract.TYPE_INT, -1)))
        assertEquals(1, transfer(files()).importSettings(negative).skipped)
    }

    @Test
    fun `a document without valid entries writes nothing`() {
        val target = files()

        val imported = transfer(target).importSettings(JSONObject().put("future-store", JSONObject()))

        assertEquals(0, imported.written)
        assertEquals(0, imported.skipped)
        assertTrue(target.values.all { it.commits == 0 && it.values.isEmpty() })
    }

    @Test
    fun `a failed commit fails the import`() {
        val target = files()
        target.getValue(TriggerPreferences.FILE).commitSucceeds = false
        val document = JSONObject().put(TriggerPreferences.FILE, JSONObject().put(TriggerPreferences.KEY_FLOATING, entry(Contract.TYPE_BOOL, true)))

        assertThrows(IOException::class.java) { transfer(target).importSettings(document) }
    }
}
