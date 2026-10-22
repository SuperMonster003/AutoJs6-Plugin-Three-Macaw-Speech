package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CommercialUse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The home list of roadmap P5.1: kind groups in order, the default model first, the row states of downloads (queued,
 * running, paused) and the custom import form of the importer's rules.
 * zh-CN: 路线图 P5.1 的首页列表: 按顺序的种类分组, 默认模型在前, 下载的行状态 (排队, 运行, 暂停), 以及采用导入器规则的自定义导入表单.
 */
class ModelRowsTest {

    private fun entry(id: String, kind: String, installed: Boolean = false, isDefault: Boolean = false, recommended: Boolean = false,
                      commercialUse: CommercialUse = CommercialUse.ALLOWED, source: String = ModelStore.SOURCE_CATALOG) = ModelEntry(
        id = id, kind = kind, modelType = "vits", name = id, languages = listOf("zh"), sizeBytes = 1000, installedBytes = if (installed) 2000 else null,
        installed = installed, isDefault = isDefault, recommended = recommended, version = 1, licenseSpdx = "Apache-2.0", commercialUse = commercialUse,
        noticeKey = null, noticeUrl = null, speakers = 1, sampleRate = 22050, source = source,
    )

    @Test
    fun `groups follow recognition, punctuation, wake words, synthesis and VAD with the default first`() {
        val entries = listOf(
            entry("vad.silero.v5", "vad", installed = true, isDefault = true),
            entry("tts.a", "tts"),
            entry("tts.b", "tts", installed = true),
            entry("tts.c", "tts", installed = true, isDefault = true),
            entry("asr.a", "asr", recommended = true),
            entry("kws.a", "kws", commercialUse = CommercialUse.UNKNOWN),
            entry("punct.a", "punct", installed = true),
            entry("my-model", "asr", installed = true, source = ModelStore.SOURCE_IMPORTED),
        )
        val groups = ModelRows.build(entries, emptyList(), setOf("vad.silero.v5")) { 0 }
        assertEquals(listOf("asr", "punct", "kws", "tts", "vad"), groups.map { it.kind })
        assertEquals(listOf("tts.c", "tts.b", "tts.a"), groups.single { it.kind == "tts" }.rows.map { it.id })
        assertEquals("installed before recommended", listOf("my-model", "asr.a"), groups.first().rows.map { it.id })
        assertTrue(groups.first().rows.first().imported)
        assertEquals(LicenseTag.UNKNOWN, groups.single { it.kind == "kws" }.rows.single().license)
        val vad = groups.last().rows.single()
        assertTrue(vad.builtin)
        assertEquals(RowState.Installed, vad.state)
        assertEquals("the bundled VAD is not counted", 4, ModelRows.installedCount(groups))
        assertEquals(2, groups.single { it.kind == "tts" }.installed)
    }

    @Test
    fun `download operations and partial files decide the row state`() {
        val entries = listOf(entry("tts.queued", "tts"), entry("tts.running", "tts"), entry("tts.paused", "tts"), entry("tts.new", "tts"), entry("tts.update", "tts", installed = true))
        val operations = listOf(
            TrackedOperation(1, "tts.running", TrackedOperation.Kind.DOWNLOAD, started = true, done = 40, total = 100),
            TrackedOperation(2, "tts.queued", TrackedOperation.Kind.DOWNLOAD),
            TrackedOperation(3, "tts.update", TrackedOperation.Kind.DOWNLOAD, started = true),
            TrackedOperation(4, null, TrackedOperation.Kind.IMPORT, started = true),
        )
        val rows = ModelRows.build(entries, operations, emptySet()) { id -> if (id == "tts.paused") 512 else 0 }.single().rows.associateBy { it.id }
        assertEquals(RowState.Queued(2), rows.getValue("tts.queued").state)
        assertEquals(RowState.Running(1, 40, 100), rows.getValue("tts.running").state)
        assertEquals(RowState.Paused(512), rows.getValue("tts.paused").state)
        assertEquals(RowState.Available, rows.getValue("tts.new").state)
        assertEquals("a reinstall shows its progress", RowState.Running(3, 0, 0), rows.getValue("tts.update").state)
    }

    @Test
    fun `the custom import form applies the importer's identity rules`() {
        assertEquals("my-model", ImportFormPolicy.suggestId("My Model.tar.bz2"))
        assertEquals("imported-model", ImportFormPolicy.suggestId(null))
        assertEquals("sherpa-onnx-vits-zh", ImportFormPolicy.suggestId("sherpa-onnx-vits-zh.tar.bz2"))
        listOf("asr.zipformer.tar", "tts.vits", "\u4e2d\u6587").forEach { name ->
            val id = ImportFormPolicy.suggestId(name)
            assertEquals("$name -> $id", emptySet<ImportFormPolicy.Problem>(), ImportFormPolicy.problems(id, "x", "zh"))
        }
        assertEquals(listOf("online-transducer", "sense-voice"), ImportFormPolicy.modelTypes("asr"))
        assertEquals(listOf("kws-transducer"), ImportFormPolicy.modelTypes("kws"))
        assertEquals(listOf("vits", "matcha", "kokoro"), ImportFormPolicy.modelTypes("tts"))
        assertEquals(emptySet<ImportFormPolicy.Problem>(), ImportFormPolicy.problems("my-model", "My model", "zh, en"))
        assertEquals(setOf(ImportFormPolicy.Problem.ID), ImportFormPolicy.problems("asr.custom", "x", "zh"))
        assertEquals(setOf(ImportFormPolicy.Problem.ID), ImportFormPolicy.problems("X", "x", "zh"))
        assertEquals(setOf(ImportFormPolicy.Problem.NAME), ImportFormPolicy.problems("my-model", " ", "zh"))
        assertEquals(setOf(ImportFormPolicy.Problem.LANGUAGES), ImportFormPolicy.problems("my-model", "x", ""))
        assertEquals(setOf(ImportFormPolicy.Problem.LANGUAGES), ImportFormPolicy.problems("my-model", "x", "Chinese"))
        val request = ImportFormPolicy.request("my-model", "tts", "vits", " Voice ", "zh en zh")
        assertEquals(listOf("zh", "en"), request.languages)
        assertEquals("Voice", request.name)
        assertEquals("my-model", request.modelId)
    }
}
