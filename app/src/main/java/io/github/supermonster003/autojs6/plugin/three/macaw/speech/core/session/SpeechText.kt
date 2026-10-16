package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

/**
 * Text rules shared by sessions and file recognition (roadmap appendix B): utterances join with a space unless both
 * sides are CJK, `punctuation: false` drops the punctuation the model emits (apostrophes and decimal points inside
 * words stay), and SenseVoice tags such as `<|zh|>`, `<|NEUTRAL|>` or `<|Speech|>` become `zh`, `neutral`, `speech`.
 * Pure Kotlin.
 * zh-CN: 会话与文件识别共用的文本规则 (路线图附录 B): 语句之间以空格连接, 两侧都是中日文字时不加空格; `punctuation: false` 去掉模型输出的
 * 标点 (词内的撇号与小数点保留); SenseVoice 标签如 `<|zh|>`, `<|NEUTRAL|>`, `<|Speech|>` 转为 `zh`, `neutral`, `speech`. 纯 Kotlin.
 */
object SpeechText {

    /** Trims, collapses white space and optionally drops punctuation. zh-CN: 去除首尾空白, 合并空白, 可选去掉标点. */
    fun clean(text: String, punctuation: Boolean): String {
        val source = text.trim()
        if (source.isEmpty()) return ""
        if (punctuation) return collapse(source)
        val out = StringBuilder(source.length)
        for (index in source.indices) {
            val char = source[index]
            if (!isPunctuation(char)) {
                out.append(char)
                continue
            }
            val before = source.getOrNull(index - 1)
            val after = source.getOrNull(index + 1)
            val inner = before != null && after != null && before.isLetterOrDigit() && after.isLetterOrDigit()
            when {
                inner && (char == '\'' || char == '\u2019' || (char == '.' && before.isDigit() && after.isDigit())) -> out.append(char)
                inner && !isCjk(before) && !isCjk(after) -> out.append(' ')
            }
        }
        return collapse(out.toString())
    }

    /** Joins utterance texts, without a space between two CJK sides. zh-CN: 连接各语句文本, 两侧都是中日文字时不加空格. */
    fun join(parts: List<String>): String {
        val out = StringBuilder()
        for (part in parts) {
            val text = part.trim()
            if (text.isEmpty()) continue
            if (out.isNotEmpty() && !(isCjk(out.last()) && isCjk(text.first()))) out.append(' ')
            out.append(text)
        }
        return out.toString()
    }

    /** `<|Speech|>` to `speech`; null for an empty or missing tag. zh-CN: `<|Speech|>` 转为 `speech`; 空或缺失时为 null. */
    fun tag(raw: String?): String? {
        val value = raw?.trim()?.removePrefix("<|")?.removeSuffix("|>")?.trim()?.lowercase()
        return value?.takeIf { it.isNotEmpty() && it.all { char -> char.isLetterOrDigit() || char == '_' || char == '-' } }
    }

    /**
     * CJK ideographs, kana and the CJK and full width punctuation: no space is needed next to them.
     * zh-CN: 中日表意文字, 假名, 以及中日文与全角标点: 相邻时无需空格.
     */
    fun isCjk(char: Char): Boolean {
        val code = char.code
        return code in 0x3000..0x30FF || code in 0x3400..0x4DBF || code in 0x4E00..0x9FFF || code in 0xF900..0xFAFF || code in 0xFF00..0xFFEF
    }

    private fun isPunctuation(char: Char): Boolean = when (Character.getType(char).toByte()) {
        Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION,
        Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION -> true
        else -> false
    }

    private fun collapse(text: String): String = text.trim().replace(WHITESPACE, " ")

    private val WHITESPACE = Regex("\\s+")
}
