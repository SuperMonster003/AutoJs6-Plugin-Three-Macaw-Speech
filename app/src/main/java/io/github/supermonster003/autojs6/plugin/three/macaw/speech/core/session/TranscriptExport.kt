package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import java.util.Locale

/**
 * The text forms of a file transcript on the file tab of the tryout screen (roadmap D112, D113): the "mm:ss.s" times
 * of the segment list, the "mm:ss" progress, plain text with one segment per line, SRT subtitles with one cue per
 * segment, and the default name of a saved file. Segments whose text is blank are left out of both files. Pure JVM.
 * zh-CN: 试听试说页文件标签上文件转写的文本形式 (路线图 D112, D113): 分段列表的 "mm:ss.s" 时间, "mm:ss" 进度, 每段一行的纯文本, 一段一条的
 * SRT 字幕, 以及保存文件的默认名. 文字为空的分段两种文件都不写. 纯 JVM.
 */
object TranscriptExport {

    const val TEXT_MIME = "text/plain"
    const val SRT_MIME = "application/x-subrip"
    const val TEXT_EXTENSION = "txt"
    const val SRT_EXTENSION = "srt"

    private const val FALLBACK_NAME = "transcript"
    private const val CRLF = "\r\n"

    /** "mm:ss.s", rounded to the nearest tenth of a second. zh-CN: "mm:ss.s", 四舍五入到十分之一秒. */
    fun segmentClock(millis: Long): String {
        val tenths = (maxOf(millis, 0L) + 50) / 100
        return String.format(Locale.ROOT, "%02d:%02d.%d", tenths / 600, tenths / 10 % 60, tenths % 10)
    }

    /** "mm:ss", whole seconds rounded down (progress). zh-CN: "mm:ss", 向下取整到秒 (进度). */
    fun clock(millis: Long): String {
        val seconds = maxOf(millis, 0L) / 1000
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
    }

    /** SRT time "HH:MM:SS,mmm". zh-CN: SRT 时间 "HH:MM:SS,mmm". */
    fun srtTime(millis: Long): String {
        val value = maxOf(millis, 0L)
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", value / 3_600_000, value / 60_000 % 60, value / 1000 % 60, value % 1000)
    }

    /** UTF-8 text, one segment per line, without times. zh-CN: UTF-8 文本, 每段一行, 不带时间. */
    fun text(segments: List<Utterance>): ByteArray = buildString {
        for (segment in segments) {
            val line = line(segment.text) ?: continue
            append(line).append('\n')
        }
    }.toByteArray(Charsets.UTF_8)

    /**
     * UTF-8 SRT with CRLF line ends: one cue per segment, numbered from 1, a cue never ending before it starts.
     * zh-CN: 以 CRLF 换行的 UTF-8 SRT: 一段一条, 序号从 1 起, 结束时间不早于开始时间.
     */
    fun srt(segments: List<Utterance>): ByteArray = buildString {
        var index = 0
        for (segment in segments) {
            val line = line(segment.text) ?: continue
            index += 1
            append(index).append(CRLF)
            append(srtTime(segment.startMillis)).append(" --> ").append(srtTime(maxOf(segment.endMillis, segment.startMillis))).append(CRLF)
            append(line).append(CRLF).append(CRLF)
        }
    }.toByteArray(Charsets.UTF_8)

    /**
     * The default name of a saved file: the picked file's name with [extension] instead of its own.
     * zh-CN: 保存文件的默认名: 把所选文件名的扩展名换成 [extension].
     */
    fun fileName(displayName: String?, extension: String): String {
        val name = displayName?.trim()?.takeIf { it.isNotEmpty() } ?: FALLBACK_NAME
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        return "$stem.$extension"
    }

    // A cue with a blank line inside would end early, so line breaks inside a segment become spaces.
    private fun line(text: String): String? = text.trim().replace(LINE_BREAKS, " ").takeIf { it.isNotEmpty() }

    private val LINE_BREAKS = Regex("\\s*[\\r\\n]+\\s*")
}
