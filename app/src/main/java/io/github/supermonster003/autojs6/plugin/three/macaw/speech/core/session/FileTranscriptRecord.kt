package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.JsonNumber
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJsonException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.FileNotFoundException
import java.io.IOException

/**
 * What the file tab tells the user about a failure (roadmap D115): one localized hint per kind, with the English
 * [FileFailure.message] below it.
 * zh-CN: 文件标签向用户说明失败时的分类 (路线图 D115): 每类一条本地化提示, 下方附英文的 [FileFailure.message].
 */
enum class FileProblem {
    /** The audio cannot be decoded. zh-CN: 读不出音频. */
    UNSUPPORTED_FORMAT,

    /** More than 200 MB or 30 minutes. zh-CN: 超过 200 MB 或 30 分钟. */
    LIMIT_EXCEEDED,

    /** The file cannot be read (no access, moved away). zh-CN: 文件无法读取 (没有权限或已被移走). */
    IO_FAILED,

    /** The chosen model is not available. zh-CN: 所选模型不可用. */
    MODEL_MISSING,

    /** The recognizer failed, or anything else. zh-CN: 识别引擎出错, 以及其他情况. */
    ENGINE_FAILED;

    companion object {
        fun of(code: String): FileProblem = when (code) {
            SpeechErrorCodes.UNSUPPORTED_FORMAT -> UNSUPPORTED_FORMAT
            SpeechErrorCodes.LIMIT_EXCEEDED -> LIMIT_EXCEEDED
            SpeechErrorCodes.IO_FAILED -> IO_FAILED
            SpeechErrorCodes.MODEL_MISSING, SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED -> MODEL_MISSING
            else -> ENGINE_FAILED
        }
    }
}

/**
 * A failed file recognition: the contract code and its English reason (roadmap D85, D115). Opening the picked document
 * can fail before the decoders run (`SecurityException`, `FileNotFoundException`); those become `IO_FAILED` with a
 * fixed reason, since the provider's message may name the file.
 * zh-CN: 一次失败的文件识别: 契约错误码与英文原因 (路线图 D85, D115). 所选文档可能在解码之前就打不开 (`SecurityException`,
 * `FileNotFoundException`), 这类失败归为 `IO_FAILED` 并使用固定的原因, 因为提供方的信息可能带有文件名.
 */
data class FileFailure(val code: String, val message: String) {

    val problem: FileProblem get() = FileProblem.of(code)

    companion object {
        fun of(error: Throwable): FileFailure = when (error) {
            is SpeechException -> FileFailure(error.code, error.message.orEmpty())
            is SecurityException -> FileFailure(SpeechErrorCodes.IO_FAILED, "The file could not be opened: access was denied")
            is FileNotFoundException -> FileFailure(SpeechErrorCodes.IO_FAILED, "The file could not be opened: it was not found")
            is IOException -> FileFailure(SpeechErrorCodes.IO_FAILED, "The file could not be read (${error.javaClass.simpleName})")
            else -> FileFailure(SpeechErrorCodes.INTERNAL, "Recognition failed (${error.javaClass.simpleName})")
        }
    }
}

/**
 * The latest result of the file tab (roadmap D113): kept in the plugin's cache as versioned JSON so that the tab shows
 * it again after the plugin process restarts. A cancelled run keeps the segments found so far, a failed one its
 * [failure]. [declaredMillis] is the length the file declares, [recognizedMillis] how much audio went to the
 * recognizer. Since roadmap P8.4.4 (D153) a record also names its [action]: an analysis keeps its result in [analysis]
 * (for the speakers also while [segments] is empty), and "diarize and transcribe" gives each segment its speaker in
 * [segmentSpeakers]. Version 1 files (1.2.0) read as transcriptions. A file that is damaged, misses a field or has
 * another version reads as no record at all.
 * zh-CN: 文件标签的最近一次结果 (路线图 D113): 以带版本号的 JSON 存放在插件缓存中, 插件进程重启后标签再次显示它. 取消的识别保留已得到的
 * 分段, 失败的保留 [failure]. [declaredMillis] 为文件声明的时长, [recognizedMillis] 为已送入识别的音频时长. 自路线图 P8.4.4 (D153) 起记录还写明
 * [action]: 分析结果存于 [analysis], "分离 + 转写" 在 [segmentSpeakers] 中给出每个分段的说话人. 版本 1 的文件 (1.2.0) 读作转写. 损坏, 缺少字段
 * 或版本不同的文件一律视为没有记录.
 */
data class FileTranscriptRecord(
    val state: State,
    val displayName: String?,
    val sizeBytes: Long?,
    val modelId: String,
    val language: String?,
    val punctuation: Boolean,
    val declaredMillis: Long?,
    val recognizedMillis: Long,
    val segments: List<Utterance>,
    val warnings: List<SpeechWarning> = emptyList(),
    val failure: FileFailure? = null,
    val action: FileAction = FileAction.TRANSCRIBE,
    val analysis: FileAnalysis? = null,
    val segmentSpeakers: List<Int> = emptyList(),
) {

    enum class State { COMPLETED, CANCELLED, FAILED }

    /** All segments joined, the same as a script's `text`. zh-CN: 各段拼接, 与脚本得到的 `text` 相同. */
    val text: String get() = SpeechText.join(segments.map { it.text })

    fun toJson(): ByteArray = StrictJson.write(
        linkedMapOf(
            "version" to VERSION,
            "state" to state.name,
            "displayName" to displayName,
            "sizeBytes" to sizeBytes,
            "modelId" to modelId,
            "language" to language,
            "punctuation" to punctuation,
            "declaredMillis" to declaredMillis,
            "recognizedMillis" to recognizedMillis,
            "segments" to segments.map { linkedMapOf("text" to it.text, "startMillis" to it.startMillis, "endMillis" to it.endMillis, "language" to it.language) },
            "warnings" to warnings.map { linkedMapOf("code" to it.code, "message" to it.message) },
            "failure" to failure?.let { linkedMapOf("code" to it.code, "message" to it.message) },
            "action" to action.name,
            "analysis" to analysis?.let(FileAnalysis::toJson),
            "segmentSpeakers" to segmentSpeakers,
        ),
    )

    companion object {
        const val VERSION = 2

        /** The 1.2.0 format: a transcription without action, analysis or speakers. zh-CN: 1.2.0 的格式: 没有操作, 分析与说话人的转写. */
        private const val VERSION_1 = 1

        /** The record in [bytes], or null when they hold none this version can read. zh-CN: [bytes] 中的记录; 本版本读不了时为 null. */
        fun parse(bytes: ByteArray): FileTranscriptRecord? = try {
            read(StrictJson.parseObject(bytes))
        } catch (_: StrictJsonException) {
            null
        } catch (_: RuntimeException) {
            // A missing field (null cast), a wrong type, an unknown state or a negative time.
            null
        }

        private fun read(json: Map<String, Any?>): FileTranscriptRecord? {
            val version = (json["version"] as? JsonNumber)?.longOrNull()
            if (version != VERSION.toLong() && version != VERSION_1.toLong()) return null
            val current = version == VERSION.toLong()
            val state = State.valueOf(json["state"] as String)
            val segments = (json["segments"] as List<*>).mapIndexed { index, item ->
                val segment = item as Map<*, *>
                Utterance(segment["text"] as String, index, long(segment["startMillis"]), long(segment["endMillis"]), segment["language"] as String?)
            }
            val warnings = (json["warnings"] as List<*>).map { item ->
                val warning = item as Map<*, *>
                SpeechWarning(warning["code"] as String, warning["message"] as String)
            }
            val failure = (json["failure"] as Map<*, *>?)?.let { FileFailure(it["code"] as String, it["message"] as String) }
            if (state == State.FAILED && failure == null) return null
            val speakers = if (current) (json["segmentSpeakers"] as List<*>).map { long(it).toInt() } else emptyList()
            if (speakers.isNotEmpty() && speakers.size != segments.size) return null
            return FileTranscriptRecord(
                state = state,
                displayName = json["displayName"] as String?,
                sizeBytes = optionalLong(json["sizeBytes"]),
                modelId = json["modelId"] as String,
                language = json["language"] as String?,
                punctuation = json["punctuation"] as Boolean,
                declaredMillis = optionalLong(json["declaredMillis"]),
                recognizedMillis = long(json["recognizedMillis"]),
                segments = segments,
                warnings = warnings,
                failure = failure,
                action = if (current) FileAction.valueOf(json["action"] as String) else FileAction.TRANSCRIBE,
                analysis = if (current) (json["analysis"] as Map<*, *>?)?.let(FileAnalysis::read) else null,
                segmentSpeakers = speakers,
            )
        }

        private fun long(value: Any?): Long = requireNotNull((value as JsonNumber).longOrNull()) { "Not an integer: $value" }.also { require(it >= 0) { "Negative: $it" } }

        private fun optionalLong(value: Any?): Long? = value?.let(::long)
    }
}
