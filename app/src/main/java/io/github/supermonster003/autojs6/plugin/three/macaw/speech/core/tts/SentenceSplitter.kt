package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

/**
 * One piece of the text to synthesize: [text] is `source.substring(start, end)` without the surrounding white space.
 * [speakable] is false for pieces without any letter or digit (punctuation only), which are skipped but still count as
 * progress.
 * zh-CN: 待合成文本中的一段: [text] 为去掉首尾空白的 `source.substring(start, end)`. 没有任何字母或数字的段 (只有标点) 的 [speakable] 为
 * false, 跳过合成但仍计入进度.
 */
data class TextChunk(val start: Int, val end: Int, val text: String) {
    val speakable: Boolean get() = text.any { it.isLetterOrDigit() }
}

/**
 * Splits text into sentences for synthesis (roadmap D19, D20): the plugin synthesizes, reports progress and checks for
 * cancellation sentence by sentence. Sentences end at Chinese and English terminators (the ideographic full stop, the
 * full-width exclamation and question marks and semicolon, the ellipsis, and `.!?;`), at line breaks and after a run
 * of them, closing quotes and brackets included. An ASCII period ends a sentence only before
 * white space, the end of the text or a CJK character, and never after a number's digit followed by another digit
 * (`3.14`), a single letter (`J. K.`), a token with inner periods (`U.S.`, `e.g.`) or a known abbreviation (`Mr.`,
 * `Dr.`, `etc.`). A sentence longer than [maxChars] is cut at the last comma, colon or dash before the limit (not one
 * between digits such as `1,000` or `12:30`), then at the last white space, then before the word or number that would
 * cross the limit, and only as a last resort in the middle of one. The chunks cover the text in order. Pure Kotlin.
 * zh-CN: 为合成把文本切分成句 (路线图 D19, D20): 插件逐句合成, 报告进度并检查取消. 句子在中英文句末标点 (中文句号, 全角叹号, 问号与分号, 省略号,
 * 以及 `.!?;`), 换行处以及连续的此类标点之后结束, 后随的右引号与右括号归入本句. ASCII 句点只有在空白, 文本末尾或 CJK 字符之前才结束句子, 且不在数字之间 (`3.14`),
 * 单个字母之后 (`J. K.`), 带内部句点的词之后 (`U.S.`, `e.g.`) 或已知缩写之后 (`Mr.`, `Dr.`, `etc.`) 结束. 超过 [maxChars] 的句子先在上限前
 * 最后一个逗号, 冒号或破折号处切开 (不含数字之间的 `1,000` 或 `12:30`), 其次在最后一个空白处, 再次在会越过上限的词或数字之前, 只有万不得已
 * 才在词中切开. 各段按顺序覆盖全文. 纯 Kotlin.
 */
object SentenceSplitter {

    /** Characters of one chunk: short enough for every catalog model and for a responsive cancel. zh-CN: 每段字符数上限. */
    const val DEFAULT_MAX_CHARS = 120

    private const val TERMINATORS = "\u3002\uff01\uff1f\uff1b\u2026!?;\n"
    private const val CLOSERS = "\"'\u201d\u2019)]}\uff09\u300d\u300f\u3011\u300b\u3009\uff3d\uff5d"
    private const val SOFT_BREAKS = "\uff0c\u3001\uff1a,:\u2014\u2013"

    /** English abbreviations whose period never ends a sentence (lower case, without the period). zh-CN: 句点不结束句子的英文缩写. */
    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "mt", "vs", "etc", "no", "nos", "fig", "figs", "vol", "vols",
        "inc", "ltd", "co", "corp", "dept", "est", "approx", "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep",
        "sept", "oct", "nov", "dec", "mon", "tue", "wed", "thu", "fri", "sat", "sun", "gen", "gov", "sgt", "capt",
    )

    fun split(text: String, maxChars: Int = DEFAULT_MAX_CHARS): List<TextChunk> {
        require(maxChars >= 8) { "maxChars must be at least 8: $maxChars" }
        val chunks = ArrayList<TextChunk>()
        var start = 0
        var index = 0
        while (index < text.length) {
            if (isBoundary(text, index)) {
                var end = index + 1
                while (end < text.length && (isTerminatorChar(text[end]) || text[end] in CLOSERS)) end++
                emit(text, start, end, maxChars, chunks)
                start = end
                index = end
            } else {
                index++
            }
        }
        if (start < text.length) emit(text, start, text.length, maxChars, chunks)
        return chunks
    }

    private fun isTerminatorChar(c: Char): Boolean = c in TERMINATORS || c == '.'

    private fun isBoundary(text: String, index: Int): Boolean {
        val c = text[index]
        if (c in TERMINATORS) return true
        if (c != '.') return false
        val next = text.getOrNull(index + 1)
        // A run of periods (an ASCII ellipsis) ends at its last period.
        if (next == '.') return false
        if (next != null && !next.isWhitespace() && !isCjk(next) && next !in CLOSERS) return false
        if (text.getOrNull(index - 1) == '.') return true
        var tokenStart = index
        while (tokenStart > 0 && (text[tokenStart - 1].isLetterOrDigit() || text[tokenStart - 1] == '.')) tokenStart--
        val token = text.substring(tokenStart, index)
        if (token.isEmpty() || token.all { it == '.' }) return true
        if (token.length == 1 && (token[0] in 'a'..'z' || token[0] in 'A'..'Z')) return false
        if ('.' in token.trimStart('.')) return false
        return token.lowercase() !in ABBREVIATIONS
    }

    private fun isCjk(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
            block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.HANGUL_SYLLABLES
    }

    /** Adds `text[start, end)` as one or more chunks of at most [maxChars] trimmed characters. zh-CN: 把 `text[start, end)` 加为一段或多段. */
    private fun emit(text: String, start: Int, end: Int, maxChars: Int, out: MutableList<TextChunk>) {
        var from = start
        while (from < end) {
            val leading = skipWhitespace(text, from, end)
            if (leading >= end) {
                // Only white space is left: it joins the previous chunk's range, or forms an empty one.
                if (out.isNotEmpty() && out.last().end == from) out[out.size - 1] = out.last().copy(end = end) else out += TextChunk(from, end, "")
                return
            }
            if (end - leading <= maxChars) {
                out += chunk(text, from, end)
                return
            }
            val cut = cutPoint(text, leading, leading + maxChars)
            out += chunk(text, from, cut)
            from = cut
        }
    }

    private fun chunk(text: String, start: Int, end: Int) = TextChunk(start, end, text.substring(start, end).trim())

    private fun skipWhitespace(text: String, from: Int, end: Int): Int {
        var index = from
        while (index < end && text[index].isWhitespace()) index++
        return index
    }

    /** Where to cut a sentence that starts at [from] and must end by [limit]. zh-CN: 从 [from] 开始, 须在 [limit] 前结束的句子的切点. */
    private fun cutPoint(text: String, from: Int, limit: Int): Int {
        val floor = from + (limit - from) / 3
        for (index in limit - 1 downTo floor) {
            val c = text[index]
            val betweenDigits = text.getOrNull(index - 1)?.isDigit() == true && text.getOrNull(index + 1)?.isDigit() == true
            if (c in SOFT_BREAKS && !(betweenDigits && (c == ',' || c == ':'))) return absorbClosers(text, index + 1, limit)
        }
        for (index in limit - 1 downTo floor) {
            if (text[index].isWhitespace()) return index + 1
        }
        // Cut before the word or number that crosses the limit, unless it started near the beginning.
        var wordStart = limit
        while (wordStart > from && isWordChar(text[wordStart - 1]) && isWordChar(text[wordStart])) wordStart--
        if (wordStart > floor) return wordStart
        return limit
    }

    private fun isWordChar(c: Char): Boolean = (c.isLetterOrDigit() && !isCjk(c)) || c == '.' || c == '\'' || c == '-'

    private fun absorbClosers(text: String, from: Int, limit: Int): Int {
        var index = from
        while (index < limit && text[index] in CLOSERS) index++
        return index
    }
}
