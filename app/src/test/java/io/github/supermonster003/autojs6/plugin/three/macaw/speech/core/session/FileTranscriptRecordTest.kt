package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `damaged, partial and other versions read as no record`() {
        val json = String(completed.toJson(), Charsets.UTF_8)
        assertNull(FileTranscriptRecord.parse(ByteArray(0)))
        assertNull(FileTranscriptRecord.parse(json.substring(0, json.length / 2).toByteArray()))
        assertNull(FileTranscriptRecord.parse("[]".toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"version\":1", "\"version\":2").toByteArray()))
        assertNull(FileTranscriptRecord.parse(json.replace("\"version\":1,", "").toByteArray()))
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
