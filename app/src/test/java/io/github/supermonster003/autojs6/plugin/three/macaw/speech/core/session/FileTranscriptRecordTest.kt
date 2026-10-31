package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageSegment
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerName
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerTurn
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagLabel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagScore
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

/**
 * The cached result of the file tab (roadmap D113, D115, P7.8.1): JSON round trips for every state, damaged, partial
 * and future records reading as none, and the failure kinds with their English reasons.
 * zh-CN: 文件标签缓存的结果 (路线图 D113, D115, P7.8.1): 各状态的 JSON 往返, 损坏, 残缺与更新版本的记录视为没有, 以及失败分类与英文原因.
 */
class FileTranscriptRecordTest {

    private val completed = FileTranscriptRecord(
        state = FileTranscriptRecord.State.COMPLETED,
        displayName = "meeting \"notes\".mp3",
        sizeBytes = 1_234_567,
        modelId = "asr.sense-voice.zh-en-ja-ko-yue.int8/0",
        language = "zh",
        punctuation = true,
        declaredMillis = 61_000,
        recognizedMillis = 60_992,
        segments = listOf(Utterance("\u4f60\u597d.", 0, 0, 1200, "zh"), Utterance("good morning", 1, 1500, 2600, "en")),
        warnings = listOf(SpeechWarning(SpeechErrorCodes.INVALID_OPTIONS, "hotwords ignored")),
    )

    @Test
    fun `every state survives the round trip`() {
        assertEquals(completed, FileTranscriptRecord.parse(completed.toJson()))
        val cancelled = completed.copy(state = FileTranscriptRecord.State.CANCELLED, displayName = null, sizeBytes = null, declaredMillis = null, recognizedMillis = 1300, segments = completed.segments.take(1), warnings = emptyList())
        assertEquals(cancelled, FileTranscriptRecord.parse(cancelled.toJson()))
        val failed = completed.copy(state = FileTranscriptRecord.State.FAILED, language = null, punctuation = false, recognizedMillis = 0, segments = emptyList(), failure = FileFailure(SpeechErrorCodes.UNSUPPORTED_FORMAT, "The MP4 file has video but no audio track"))
        assertEquals(failed, FileTranscriptRecord.parse(failed.toJson()))
        assertEquals("\u4f60\u597d. good morning", completed.text)
    }

    @Test
    fun `the analyses of roadmap D153 survive the round trip and version 1 reads as a transcription`() {
        val speakers = FileAnalysis.Speakers(
            listOf(SpeakerTurn(0, 1400, 0), SpeakerTurn(1500, 2600, 1)), 2, "speaker.eres2net.3dspeaker.en-voxceleb",
            listOf(SpeakerName(0, "Alice", 0.71), SpeakerName(1, null, null)),
        )
        val diarized = completed.copy(action = FileAction.DIARIZE_TRANSCRIBE, analysis = speakers, segmentSpeakers = listOf(0, 1))
        assertEquals(diarized, FileTranscriptRecord.parse(diarized.toJson()))
        assertEquals("Alice", speakers.name(0))
        val onlySpeakers = completed.copy(action = FileAction.DIARIZE, segments = emptyList(), language = null, analysis = speakers.copy(names = null))
        assertEquals(onlySpeakers, FileTranscriptRecord.parse(onlySpeakers.toJson()))
        val languages = completed.copy(
            action = FileAction.LANGUAGE, segments = emptyList(), modelId = "langid.whisper.tiny.int8",
            analysis = FileAnalysis.Languages("en", listOf("en" to 0.75f, "fr" to 0.25f), listOf(LanguageSegment(0, 4_000, "en"), LanguageSegment(4_300, 5_600, "fr"))),
        )
        assertEquals(languages, FileTranscriptRecord.parse(languages.toJson()))
        // A 1.3.0 record: the same version and key with one segment per window. zh-CN: 1.3.0 的记录: 版本与键相同, 每窗一段.
        val windowed = String(languages.toJson(), Charsets.UTF_8)
        assertTrue(windowed, "\"windows\":[{\"startMillis\":0,\"endMillis\":4000,\"language\":\"en\"}" in windowed)
        val tags = completed.copy(
            action = FileAction.TAG, segments = emptyList(), modelId = "tagger.ced.mini.int8",
            analysis = FileAnalysis.Tags(listOf(TagScore(TagLabel(137, "/m/04rlf", "Music"), 0.875f, 10_000, 20_000))),
        )
        assertEquals(tags, FileTranscriptRecord.parse(tags.toJson()))
        // A 1.2.0 file: version 1 without the new fields. zh-CN: 1.2.0 的文件: 版本 1, 没有新字段.
        val old = String(completed.toJson(), Charsets.UTF_8)
            .replace("\"version\":2", "\"version\":1")
            .replace(",\"action\":\"TRANSCRIBE\",\"analysis\":null,\"segmentSpeakers\":[]", "")
        assertEquals(completed, FileTranscriptRecord.parse(old.toByteArray()))
        assertEquals(listOf(true, false, true, false, false), FileAction.entries.map { it.transcribes })
        assertEquals(listOf(false, true, true, false, false), FileAction.entries.map { it.diarizes })
    }

    @Test
    fun `the source of a record survives the round trip and a 1_3_0 record has none`() {
        val uri = "content://com.android.providers.media.documents/document/audio%3A42"
        val record = completed.copy(sourceUri = uri)
        assertEquals(uri, FileTranscriptRecord.parse(record.toJson())?.sourceUri)
        val older = String(completed.toJson()).replace(",\"sourceUri\":null", "")
        assertTrue("the key is gone: $older", "sourceUri" !in older)
        val read = FileTranscriptRecord.parse(older.toByteArray())
        assertEquals(completed, read)
        assertNull(read?.sourceUri)
    }

    @Test
    fun `damaged, partial and other versions read as no record`() {
        val json = String(completed.toJson(), Charsets.UTF_8)
        assertNull(FileTranscriptRecord.parse(ByteArray(0)))
        assertNull(FileTranscriptRecord.parse(json.substring(0, json.length / 2).toByteArray()))
        assertNull(FileTranscriptRecord.parse("[]".toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"version\":2", "\"version\":3").toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"version\":2,", "").toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"action\":\"TRANSCRIBE\"", "\"action\":\"SING\"").toByteArray()))
        assertNull("speakers for some segments only", FileTranscriptRecord.parse(completed.copy(segmentSpeakers = listOf(0)).toJson()))
        assertNull(FileTranscriptRecord.parse(json.replace("COMPLETED", "PAUSED").toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"modelId\"", "\"model\"").toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"startMillis\":1500", "\"startMillis\":-1").toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"endMillis\":1200", "\"endMillis\":1.5").toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"punctuation\":true", "\"punctuation\":\"yes\"").toByteArray()))
        assertNull("a failure without its reason", FileTranscriptRecord.parse(completed.copy(state = FileTranscriptRecord.State.FAILED).toJson()))
    }

    @Test
    fun `failures map to the kinds the tab explains`() {
        assertEquals(FileProblem.UNSUPPORTED_FORMAT, FileProblem.of(SpeechErrorCodes.UNSUPPORTED_FORMAT))
        assertEquals(FileProblem.LIMIT_EXCEEDED, FileProblem.of(SpeechErrorCodes.LIMIT_EXCEEDED))
        assertEquals(FileProblem.IO_FAILED, FileProblem.of(SpeechErrorCodes.IO_FAILED))
        assertEquals(FileProblem.MODEL_MISSING, FileProblem.of(SpeechErrorCodes.MODEL_MISSING))
        assertEquals(FileProblem.MODEL_MISSING, FileProblem.of(SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED))
        assertEquals(FileProblem.ENGINE_FAILED, FileProblem.of(SpeechErrorCodes.ENGINE_FAILED))
        assertEquals(FileProblem.ENGINE_FAILED, FileProblem.of(SpeechErrorCodes.INTERNAL))
        assertEquals(FileProblem.ENGINE_FAILED, FileProblem.of(SpeechErrorCodes.SESSION_BUSY))
    }

    @Test
    fun `opening failures become IO_FAILED without the provider's message`() {
        val limit = FileFailure.of(SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio lasts 1900000 ms, more than 1800000"))
        assertEquals(FileFailure(SpeechErrorCodes.LIMIT_EXCEEDED, "The audio lasts 1900000 ms, more than 1800000"), limit)
        val denied = FileFailure.of(SecurityException("Permission Denial: reading content://media/external/audio/media/42 secret.mp3"))
        assertEquals(SpeechErrorCodes.IO_FAILED, denied.code)
        assertEquals("The file could not be opened: access was denied", denied.message)
        val missing = FileFailure.of(FileNotFoundException("/storage/emulated/0/Music/secret.mp3"))
        assertEquals(FileFailure(SpeechErrorCodes.IO_FAILED, "The file could not be opened: it was not found"), missing)
        assertEquals(FileFailure(SpeechErrorCodes.IO_FAILED, "The file could not be read (IOException)"), FileFailure.of(IOException("secret.mp3")))
        assertEquals(FileFailure(SpeechErrorCodes.INTERNAL, "Recognition failed (IllegalStateException)"), FileFailure.of(IllegalStateException("x")))
        assertEquals(FileProblem.IO_FAILED, denied.problem)
    }
}
