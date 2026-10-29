package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.math.sqrt

/**
 * Roadmap P8.3.3 / P8.3.6 (D125, D134): the voiceprint store keeps the mean of normalized embeddings per name, appends
 * unless `replace`, survives a damaged file through its backup, keeps a newer format untouched, and the scores and
 * names follow the contract rules.
 * zh-CN: 路线图 P8.3.3 / P8.3.6 (D125, D134): 声纹库按名字保存归一化嵌入的平均值, 除非 `replace` 否则追加, 文件损坏时经备份恢复, 较新格式保持不动,
 * 分数与名字遵守契约规则.
 */
class SpeakerStoreTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private var now = 1_000L

    private fun store(directory: File = temporary.root, log: MutableList<String> = ArrayList()) = SpeakerStore(directory, { now++ }, { log += it })

    private fun vector(vararg values: Float) = values

    @Test
    fun `enrolling appends by default, weighs every recording the same and replace starts over`() {
        val speakers = store()
        val first = speakers.enroll("Alice", "speaker.a", 1, listOf(vector(3f, 0f), vector(0f, 2f)), replace = false)
        assertEquals(2, first.samples)
        assertArrayEquals(floatArrayOf(0.5f, 0.5f), first.mean, 1e-6f)

        val more = speakers.enroll("Alice", "speaker.a", 1, listOf(vector(1f, 0f)), replace = false)
        assertEquals(3, more.samples)
        assertArrayEquals(floatArrayOf(2f / 3, 1f / 3), more.mean, 1e-6f)

        val again = speakers.enroll("Alice", "speaker.a", 1, listOf(vector(0f, 5f)), replace = true)
        assertEquals(1, again.samples)
        assertArrayEquals(floatArrayOf(0f, 1f), again.mean, 1e-6f)

        // A fresh store reads what was written.
        val reread = store().get("Alice")!!
        assertEquals(1, reread.samples)
        assertEquals("speaker.a", reread.modelId)
        assertArrayEquals(floatArrayOf(0f, 1f), reread.mean, 1e-6f)
    }

    @Test
    fun `names are listed sorted, removed once and limited to the contract ceiling`() {
        val speakers = store()
        speakers.enroll("Bob", "speaker.a", 1, listOf(vector(1f, 0f)), replace = false)
        speakers.enroll("alice", "speaker.a", 1, listOf(vector(0f, 1f)), replace = false)
        assertEquals(listOf("Bob", "alice"), speakers.list().map { it.name })
        assertTrue(speakers.remove("Bob"))
        assertFalse(speakers.remove("Bob"))
        assertNull(store().get("Bob"))

        for (i in 2..SpeechLimits.MAX_SPEAKERS) speakers.enroll("s$i", "speaker.a", 1, listOf(vector(1f, i.toFloat())), replace = false)
        assertEquals(SpeechLimits.MAX_SPEAKERS, speakers.list().size)
        val refused = assertThrows(SpeechException::class.java) { speakers.enroll("one more", "speaker.a", 1, listOf(vector(1f, 1f)), replace = false) }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, refused.code)
        // An enrolled name may still take more recordings.
        assertEquals(2, speakers.enroll("alice", "speaker.a", 1, listOf(vector(0f, 1f)), replace = false).samples)
    }

    @Test
    fun `adding to a name of another model needs replace`() {
        val speakers = store()
        speakers.enroll("Alice", "speaker.a", 1, listOf(vector(1f, 0f)), replace = false)
        val other = assertThrows(SpeechException::class.java) { speakers.enroll("Alice", "speaker.b", 1, listOf(vector(1f, 0f)), replace = false) }
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, other.code)
        val dims = assertThrows(SpeechException::class.java) { speakers.enroll("Alice", "speaker.a", 2, listOf(vector(1f, 0f, 0f)), replace = false) }
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, dims.code)
        assertEquals("speaker.b", speakers.enroll("Alice", "speaker.b", 1, listOf(vector(1f, 0f, 0f)), replace = true).modelId)
        assertEquals(listOf("Alice"), speakers.of("speaker.b").map { it.name })
        assertEquals(emptyList<String>(), speakers.of("speaker.a").map { it.name })
    }

    @Test
    fun `a damaged file falls back to the backup, then to an empty store`() {
        val log = ArrayList<String>()
        val speakers = store(log = log)
        speakers.enroll("Alice", "speaker.a", 1, listOf(vector(1f, 0f)), replace = false)
        speakers.enroll("Bob", "speaker.a", 1, listOf(vector(0f, 1f)), replace = false)
        val file = File(temporary.root, SpeakerStore.FILE)
        val backup = File(temporary.root, SpeakerStore.FILE + ".bak")
        assertTrue(backup.isFile)

        file.writeText("{\"format\": 1, \"speakers\": [")
        assertEquals(listOf("Alice"), store(log = log).list().map { it.name })

        backup.writeText("not json")
        val empty = store(log = log)
        assertEquals(emptyList<String>(), empty.list().map { it.name })
        assertTrue(File(temporary.root, SpeakerStore.FILE + ".corrupt").isFile)
        assertFalse(file.isFile)
        // Enrolling again works on the empty store.
        assertEquals(1, empty.enroll("Carol", "speaker.a", 1, listOf(vector(1f, 1f)), replace = false).samples)
        assertEquals(listOf("Carol"), store().list().map { it.name })
        assertTrue(log.any { it.contains("moved aside") })
    }

    @Test
    fun `a file of a newer format is kept untouched and refuses writes`() {
        val file = File(temporary.root, SpeakerStore.FILE)
        val newer = "{\"format\": ${SpeakerStore.FORMAT_VERSION + 1}, \"speakers\": []}\n"
        file.writeText(newer)
        val speakers = store()
        assertEquals(emptyList<Voiceprint>(), speakers.list())
        val refused = assertThrows(SpeechException::class.java) { speakers.enroll("Alice", "speaker.a", 1, listOf(vector(1f, 0f)), replace = false) }
        assertEquals(SpeechErrorCodes.IO_FAILED, refused.code)
        assertEquals(newer, file.readText())
    }

    @Test
    fun `entries with invalid names or values make the file unreadable`() {
        val file = File(temporary.root, SpeakerStore.FILE)
        listOf(
            """{"format": 1, "speakers": [{"name": " ", "model": "m", "modelVersion": 1, "samples": 1, "updatedAt": 1, "mean": [1]}]}""",
            """{"format": 1, "speakers": [{"name": "a", "model": "m", "modelVersion": 1, "samples": 0, "updatedAt": 1, "mean": [1]}]}""",
            """{"format": 1, "speakers": [{"name": "a", "model": "m", "modelVersion": 1, "samples": 1, "updatedAt": 1, "mean": []}]}""",
            """{"format": 1, "speakers": [{"name": "a", "model": "m", "modelVersion": 1, "samples": 1, "updatedAt": 1, "mean": [1]},""" +
                """{"name": "a", "model": "m", "modelVersion": 1, "samples": 1, "updatedAt": 1, "mean": [1]}]}""",
            """{"format": 0, "speakers": []}""",
        ).forEach { text ->
            File(temporary.root, SpeakerStore.FILE + ".corrupt").delete()
            file.writeText(text)
            assertEquals(text, emptyList<Voiceprint>(), store().list())
        }
    }

    @Test
    fun `scores are cosine similarities, ranked best first`() {
        assertEquals(1.0, SpeakerMath.cosine(floatArrayOf(1f, 1f), floatArrayOf(2f, 2f)), 1e-9)
        assertEquals(0.0, SpeakerMath.cosine(floatArrayOf(1f, 0f), floatArrayOf(0f, 3f)), 1e-9)
        assertEquals(-1.0, SpeakerMath.cosine(floatArrayOf(1f, 0f), floatArrayOf(-1f, 0f)), 1e-9)
        val prints = listOf(
            Voiceprint("a", "m", 1, 1, floatArrayOf(1f, 0f), 0),
            Voiceprint("b", "m", 1, 1, floatArrayOf(0f, 1f), 0),
            Voiceprint("c", "m", 1, 1, floatArrayOf(1f, 1f), 0),
            Voiceprint("wrong dims", "m", 1, 1, floatArrayOf(1f, 1f, 1f), 0),
        )
        val ranked = SpeakerMath.rank(floatArrayOf(1f, 0.2f), prints, topK = 2)
        assertEquals(listOf("a", "c"), ranked.map { it.name })
        assertEquals(1 / sqrt(1.04), ranked[0].score, 1e-6)
        val match = SpeakerMatch(ranked.first(), 0.99, ranked, "m")
        assertFalse(match.matched)
        assertTrue(match.copy(threshold = 0.9).matched)
        assertFalse(SpeakerMatch(null, 0.0, emptyList(), "m").matched)
    }

    @Test
    fun `names, thresholds, topK and recording counts follow the contract`() {
        assertEquals("Alice", SpeakerRules.name("  Alice "))
        assertEquals(64, SpeakerRules.name("x".repeat(64)).length)
        mapOf(
            null to SpeechErrorCodes.INVALID_OPTIONS,
            "  " to SpeechErrorCodes.INVALID_OPTIONS,
            "a\nb" to SpeechErrorCodes.INVALID_OPTIONS,
            "x".repeat(65) to SpeechErrorCodes.LIMIT_EXCEEDED,
        ).forEach { (name, code) -> assertEquals(code, assertThrows(SpeechException::class.java) { SpeakerRules.name(name) }.code) }

        assertEquals(SpeakerRules.FALLBACK_THRESHOLD, SpeakerRules.threshold(null, "speaker.unknown"), 0.0)
        assertEquals(0.3, SpeakerRules.threshold(0.3, "speaker.unknown"), 0.0)
        listOf(-0.1, 1.1, Double.NaN).forEach { value ->
            assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { SpeakerRules.threshold(value, "m") }.code)
        }
        assertEquals(SpeechLimits.DEFAULT_IDENTIFY_CANDIDATES, SpeakerRules.topK(null))
        assertEquals(10, SpeakerRules.topK(10))
        assertThrows(SpeechException::class.java) { SpeakerRules.topK(0) }
        assertThrows(SpeechException::class.java) { SpeakerRules.topK(11) }
        SpeakerRules.recordings(1)
        SpeakerRules.recordings(SpeechLimits.MAX_ENROLL_FILES)
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { SpeakerRules.recordings(0) }.code)
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, assertThrows(SpeechException::class.java) { SpeakerRules.recordings(11) }.code)
    }
}
