package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.kws

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Keyword
import org.autojs.plugin.speech.api.SpeechErrorCodes
import java.io.BufferedReader
import java.io.InputStream

/**
 * Spells wake keywords in the tokens of a keyword spotting model (roadmap D18, appendix D Q4: sherpa-onnx 1.13.8 has no
 * text-to-token conversion for `KeywordSpotter`, so the plugin does it). The zh-en model uses `phone+ppinyin` tokens:
 * a Chinese character becomes the initial and the tonal final of its customary reading (`小` -> `x iǎo`, `五` -> `w ǔ`,
 * as pypinyin with `strict=False`), an English word the ARPAbet phones of the model's `en.phone` lexicon (`HEY` -> `HH
 * EY1`). White space and punctuation separate words; anything else (digits, unknown words, characters without a
 * reading, syllables the model has no token for) is refused with `INVALID_OPTIONS`, so `KeywordSpotter` never sees an
 * unknown token. A keyword that already consists of space separated model tokens (`x iǎo l iù`) is taken as it is,
 * which also lets a script choose another reading of a polyphone. Pure JVM.
 * zh-CN: 用关键词检测模型的 token 拼写唤醒关键词 (路线图 D18, 附录 D Q4: sherpa-onnx 1.13.8 的 `KeywordSpotter` 没有文本到 token 的转换,
 * 因此由插件完成). zh-en 模型使用 `phone+ppinyin` token: 汉字取其常用读音的声母与带调韵母 (`小` -> `x iǎo`, `五` -> `w ǔ`, 同 pypinyin
 * `strict=False`), 英文单词取模型 `en.phone` 词典中的 ARPAbet 音素 (`HEY` -> `HH EY1`). 空白与标点分隔单词; 其他内容 (数字, 未知单词, 无读音的
 * 字符, 模型没有对应 token 的音节) 以 `INVALID_OPTIONS` 拒绝, 因此 `KeywordSpotter` 不会遇到未知 token. 本身就是以空格分隔的模型 token 的关键词
 * (`x iǎo l iù`) 原样使用, 脚本也可借此为多音字选择其他读音. 纯 JVM.
 */
class KeywordTokenizer(
    private val tokens: Set<String>,
    private val reading: (Char) -> String?,
    private val phones: (String) -> List<String>?,
) {

    fun encode(keyword: String): List<String> {
        val pieces = keyword.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (pieces.isNotEmpty() && pieces.all { it in tokens }) return pieces
        val out = ArrayList<String>()
        val word = StringBuilder()
        fun endWord() {
            if (word.isEmpty()) return
            val upper = word.toString().uppercase()
            val spelled = phones(upper) ?: invalid(keyword, "the English word '$word' is not in the model's lexicon")
            spelled.forEach { if (it !in tokens) invalid(keyword, "the model has no token '$it'") }
            out += spelled
            word.setLength(0)
        }
        for (char in keyword) {
            when {
                char in 'a'..'z' || char in 'A'..'Z' || (char == '\'' && word.isNotEmpty()) -> word.append(char)
                isIdeograph(char) -> {
                    endWord()
                    val syllable = reading(char) ?: invalid(keyword, "'$char' has no known reading")
                    val parts = split(syllable)
                    parts.forEach { if (it !in tokens) invalid(keyword, "the model cannot spell '$char' ($syllable)") }
                    out += parts
                }
                char.isWhitespace() || isPunctuation(char) -> endWord()
                else -> invalid(keyword, "'$char' cannot be spelled; write numbers and symbols as words")
            }
        }
        endWord()
        if (out.isEmpty()) invalid(keyword, "nothing to spell")
        return out
    }

    companion object {

        /** pypinyin's initials with `strict=False` (y and w count as initials), longest first. zh-CN: pypinyin `strict=False` 的声母 (含 y, w), 长者优先. */
        private val INITIALS = listOf("zh", "ch", "sh", "b", "p", "m", "f", "d", "t", "n", "l", "g", "k", "h", "j", "q", "x", "r", "z", "c", "s", "y", "w")

        private val WHITESPACE = Regex("\\s+")

        /** A tone-marked syllable as its initial and final (`xiǎo` -> `x`, `iǎo`; `ér` -> `ér`). zh-CN: 带调音节拆为声母与韵母. */
        fun split(syllable: String): List<String> {
            val initial = INITIALS.firstOrNull { syllable.startsWith(it) && syllable.length > it.length } ?: return listOf(syllable)
            return listOf(initial, syllable.substring(initial.length))
        }

        fun isIdeograph(char: Char): Boolean {
            val code = char.code
            return code == 0x3007 || code in 0x3400..0x4DBF || code in 0x4E00..0x9FFF || code in 0xF900..0xFAFF
        }

        private fun isPunctuation(char: Char): Boolean = when (Character.getType(char).toByte()) {
            Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION -> true
            else -> false
        }

        private fun invalid(keyword: String, detail: String): Nothing =
            throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "Keyword '$keyword' cannot be spelled with the model's tokens: $detail")

        /**
         * The `KeywordSpotter.createStream` buffer: one keyword per `/`, its tokens, the optional `:boost` and
         * `#threshold`, and the label `@k<index>` that `KeywordSpotterResult.keyword` reports back.
         * zh-CN: `KeywordSpotter.createStream` 的缓冲区: 每个关键词以 `/` 分隔, 依次为其 token, 可选的 `:boost` 与 `#threshold`, 以及
         * `KeywordSpotterResult.keyword` 回报的标签 `@k<index>`.
         */
        fun buffer(keywords: List<Keyword>, encoded: List<List<String>>): String = keywords.indices.joinToString("/") { index ->
            val keyword = keywords[index]
            buildString {
                append(encoded[index].joinToString(" "))
                keyword.boost?.let { append(" :").append(it.toString()) }
                keyword.threshold?.let { append(" #").append(it.toString()) }
                append(" @k").append(index)
            }
        }

        /** The keyword index of a result label (`k3` -> 3). zh-CN: 结果标签对应的关键词序号. */
        fun indexOf(label: String, count: Int): Int? = label.trim().removePrefix("k").toIntOrNull()?.takeIf { it in 0 until count }

        /** The token set of a `tokens.txt` (first column). zh-CN: `tokens.txt` 的 token 集合 (第一列). */
        fun readTokens(input: InputStream): Set<String> = input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapNotNull { it.trim().split(' ', '\t').firstOrNull()?.takeIf { token -> token.isNotEmpty() } }.toSet()
        }

        /**
         * The readings of [characters] in a `kws/pinyin.txt` table (`<character><TAB><reading>`), read in one pass.
         * zh-CN: 一次读取 `kws/pinyin.txt` 表 (`<字符><TAB><读音>`) 中 [characters] 的读音.
         */
        fun readReadings(input: InputStream, characters: Set<Char>): Map<Char, String> {
            if (characters.isEmpty()) return emptyMap()
            val found = HashMap<Char, String>()
            input.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    if (line.length < 3 || line[0] == '#' || line[1] != '\t' || line[0] !in characters) continue
                    found[line[0]] = line.substring(2).trim()
                    if (found.size == characters.size) break
                }
            }
            return found
        }

        /**
         * The phones of [words] (upper case) in a lexicon such as `en.phone` (`WORD PH1 PH2 ...`), the first entry of each.
         * zh-CN: 词典 (如 `en.phone`, `WORD PH1 PH2 ...`) 中 [words] (大写) 的音素, 各取第一条.
         */
        fun readPhones(reader: BufferedReader, words: Set<String>): Map<String, List<String>> {
            if (words.isEmpty()) return emptyMap()
            val found = HashMap<String, List<String>>()
            reader.useLines { lines ->
                for (line in lines) {
                    val parts = line.trim().split(' ', '\t').filter { it.isNotEmpty() }
                    if (parts.size < 2 || parts[0] !in words || parts[0] in found) continue
                    found[parts[0]] = parts.drop(1)
                    if (found.size == words.size) break
                }
            }
            return found
        }

        /** Upper case English words and ideographs a set of keywords needs. zh-CN: 一组关键词需要的大写英文单词与汉字. */
        fun needs(keywords: List<String>): Pair<Set<String>, Set<Char>> {
            val words = HashSet<String>()
            val characters = HashSet<Char>()
            for (keyword in keywords) {
                Regex("[A-Za-z][A-Za-z']*").findAll(keyword).forEach { words += it.value.uppercase() }
                keyword.filter(::isIdeograph).forEach { characters += it }
            }
            return words to characters
        }
    }
}
