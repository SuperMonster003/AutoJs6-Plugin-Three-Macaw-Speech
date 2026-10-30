package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.LanguageSegment
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerName
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.SpeakerTurn
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagLabel
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis.TagScore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.JsonNumber

/**
 * What the file tab does with a picked file (roadmap D153): transcribe it (the 1.2.0 tab), find who spoke when, both
 * (each speaker turn transcribed on its own), identify the spoken language or tag the sounds.
 * zh-CN: 文件标签对所选文件执行的操作 (路线图 D153): 转写 (1.2.0 的标签), 分离说话人, 分离 + 转写 (逐个说话人轮次转写), 识别语种, 音频标签.
 */
enum class FileAction {
    TRANSCRIBE,
    DIARIZE,
    DIARIZE_TRANSCRIBE,
    LANGUAGE,
    TAG;

    /** Whether the action writes text segments (and so offers TXT / SRT). zh-CN: 该操作是否产生文字分段 (因此提供 TXT / SRT). */
    val transcribes: Boolean get() = this == TRANSCRIBE || this == DIARIZE_TRANSCRIBE

    /** Whether the action needs a segmentation and a speaker model. zh-CN: 该操作是否需要分割模型与说话人模型. */
    val diarizes: Boolean get() = this == DIARIZE || this == DIARIZE_TRANSCRIBE
}

/**
 * The analysis part of a file tab result (roadmap D153), kept in [FileTranscriptRecord] next to the text segments.
 * zh-CN: 文件标签结果中的分析部分 (路线图 D153), 与文字分段一起存放在 [FileTranscriptRecord] 中.
 */
sealed interface FileAnalysis {

    /** Speaker turns and, with identify, the enrolled names. zh-CN: 说话人轮次, 认人时附已注册的名字. */
    data class Speakers(val turns: List<SpeakerTurn>, val speakers: Int, val speakerModelId: String, val names: List<SpeakerName>? = null) : FileAnalysis {
        /** The enrolled name of [speaker], or null. zh-CN: [speaker] 的已注册名字, 没有时为 null. */
        fun name(speaker: Int): String? = names?.firstOrNull { it.speaker == speaker }?.name
    }

    /**
     * The voted language, its candidates (share of the speaking time) and each sentence's language; a 1.3.0 record has
     * one segment per window and shares of the windows.
     * zh-CN: 投票得到的语种, 候选 (说话时长占比) 与每句的语种; 1.3.0 的记录每窗一段, 占比为窗数占比.
     */
    data class Languages(val language: String, val candidates: List<Pair<String, Float>>, val segments: List<LanguageSegment>) : FileAnalysis

    /** The best labels with their scores and peak windows. zh-CN: 得分最高的标签, 附分数与峰值窗口. */
    data class Tags(val labels: List<TagScore>) : FileAnalysis

    companion object {
        internal fun toJson(analysis: FileAnalysis): Map<String, Any?> = when (analysis) {
            is Speakers -> linkedMapOf(
                "kind" to "speakers",
                "speakers" to analysis.speakers,
                "speakerModelId" to analysis.speakerModelId,
                "turns" to analysis.turns.map { linkedMapOf("startMillis" to it.startMillis, "endMillis" to it.endMillis, "speaker" to it.speaker) },
                "names" to analysis.names?.map { linkedMapOf("speaker" to it.speaker, "name" to it.name, "score" to it.score) },
            )
            is Languages -> linkedMapOf(
                "kind" to "languages",
                "language" to analysis.language,
                "candidates" to analysis.candidates.map { (language, share) -> linkedMapOf("language" to language, "share" to share.toDouble()) },
                // The key of 1.3.0, whose records stay readable; the items have not changed. zh-CN: 1.3.0 的键, 其记录仍可读; 条目形状未变.
                "windows" to analysis.segments.map { linkedMapOf("startMillis" to it.startMillis, "endMillis" to it.endMillis, "language" to it.language) },
            )
            is Tags -> linkedMapOf(
                "kind" to "tags",
                "labels" to analysis.labels.map {
                    linkedMapOf(
                        "index" to it.label.index, "mid" to it.label.mid, "name" to it.label.name, "score" to it.score.toDouble(),
                        "startMillis" to it.startMillis, "endMillis" to it.endMillis,
                    )
                },
            )
        }

        /** Throws on a wrong shape, which makes the whole record unreadable. zh-CN: 形态错误时抛出, 使整条记录不可读. */
        internal fun read(json: Map<*, *>): FileAnalysis = when (json["kind"]) {
            "speakers" -> Speakers(
                turns = (json["turns"] as List<*>).map { item ->
                    val turn = item as Map<*, *>
                    SpeakerTurn(long(turn["startMillis"]), long(turn["endMillis"]), long(turn["speaker"]).toInt())
                },
                speakers = long(json["speakers"]).toInt(),
                speakerModelId = json["speakerModelId"] as String,
                names = (json["names"] as List<*>?)?.map { item ->
                    val name = item as Map<*, *>
                    SpeakerName(long(name["speaker"]).toInt(), name["name"] as String?, (name["score"] as JsonNumber?)?.toDouble())
                },
            )
            "languages" -> Languages(
                language = json["language"] as String,
                candidates = (json["candidates"] as List<*>).map { item ->
                    val candidate = item as Map<*, *>
                    (candidate["language"] as String) to (candidate["share"] as JsonNumber).toDouble().toFloat()
                },
                segments = (json["windows"] as List<*>).map { item ->
                    val segment = item as Map<*, *>
                    LanguageSegment(long(segment["startMillis"]), long(segment["endMillis"]), segment["language"] as String)
                },
            )
            "tags" -> Tags(
                (json["labels"] as List<*>).map { item ->
                    val label = item as Map<*, *>
                    TagScore(
                        TagLabel(long(label["index"]).toInt(), label["mid"] as String, label["name"] as String),
                        (label["score"] as JsonNumber).toDouble().toFloat(),
                        long(label["startMillis"]),
                        long(label["endMillis"]),
                    )
                },
            )
            else -> throw IllegalArgumentException("Unknown analysis ${json["kind"]}")
        }

        private fun long(value: Any?): Long = requireNotNull((value as JsonNumber).longOrNull()) { "Not an integer: $value" }.also { require(it >= 0) { "Negative: $it" } }
    }
}
