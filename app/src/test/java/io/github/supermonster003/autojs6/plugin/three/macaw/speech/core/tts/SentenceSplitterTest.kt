package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sentence splitting for synthesis (roadmap P4.1): Chinese and English terminators, closing quotes, line breaks, numbers
 * and abbreviations kept whole, the chunk limit with its fallbacks, and chunks that cover the text in order.
 * zh-CN: 合成用的分句 (路线图 P4.1): 中英文句末标点, 右引号, 换行, 数字与缩写不拆开, 段长上限及其回退, 以及按顺序覆盖全文的分段.
 */
class SentenceSplitterTest {

    private fun texts(text: String, maxChars: Int = SentenceSplitter.DEFAULT_MAX_CHARS) = SentenceSplitter.split(text, maxChars).map { it.text }

    @Test
    fun `chinese and english terminators end sentences`() {
        // Full-width terminators are written as escapes: repository text uses ASCII punctuation only.
        val zh = "\u4eca\u5929\u5929\u6c14\u5f88\u597d\u3002\u4f60\u53bb\u54ea\u91cc\uff1f\u592a\u597d\u4e86\uff01\u7ed3\u675f"
        assertEquals(listOf("\u4eca\u5929\u5929\u6c14\u5f88\u597d\u3002", "\u4f60\u53bb\u54ea\u91cc\uff1f", "\u592a\u597d\u4e86\uff01", "\u7ed3\u675f"), texts(zh))
        assertEquals(listOf("It works.", "Does it?", "Yes!", "Fine;", "next"), texts("It works. Does it? Yes! Fine; next"))
        assertEquals(listOf("Wait...", "then go."), texts("Wait... then go."))
        assertEquals(listOf("What?!", "No."), texts("What?! No."))
    }

    @Test
    fun `closing quotes and brackets stay with their sentence`() {
        val quoted = "\u4ed6\u8bf4: \u201c\u597d\u7684\u3002\u201d\u7136\u540e\u8d70\u4e86\u3002"
        assertEquals(listOf("\u4ed6\u8bf4: \u201c\u597d\u7684\u3002\u201d", "\u7136\u540e\u8d70\u4e86\u3002"), texts(quoted))
        assertEquals(listOf("She said \"go.\"", "(Really.)", "Ok"), texts("She said \"go.\" (Really.) Ok"))
    }

    @Test
    fun `line breaks end sentences and blank lines disappear`() {
        assertEquals(listOf("first line", "second line"), texts("first line\n\n  second line\n"))
    }

    @Test
    fun `numbers, initials and abbreviations keep their periods`() {
        assertEquals(listOf("Pi is 3.14 and e is 2.718."), texts("Pi is 3.14 and e is 2.718."))
        assertEquals(listOf("Mr. Smith met Dr. Jones at 5 p.m. today."), texts("Mr. Smith met Dr. Jones at 5 p.m. today."))
        assertEquals(listOf("J. K. Rowling wrote it, e.g. this one.", "Next."), texts("J. K. Rowling wrote it, e.g. this one. Next."))
        assertEquals(listOf("The U.S. Army etc. arrived.", "Done."), texts("The U.S. Army etc. arrived. Done."))
        assertEquals(listOf("Version 2.", "Then 3."), texts("Version 2. Then 3."))
        assertEquals(listOf("\u7248\u672c 1.13.8 \u5df2\u53d1\u5e03."), texts("\u7248\u672c 1.13.8 \u5df2\u53d1\u5e03."))
        assertEquals(listOf("End.", "\u4e2d\u6587"), texts("End.\u4e2d\u6587"))
    }

    @Test
    fun `long sentences are cut at commas, then spaces, then before a word`() {
        val sentence = "alpha beta gamma, delta epsilon zeta eta theta iota kappa"
        assertEquals(listOf("alpha beta gamma,", "delta epsilon zeta eta theta iota kappa"), texts(sentence, 40))
        val spaces = "one two three four five six seven eight nine ten eleven"
        val cut = texts(spaces, 20)
        assertTrue(cut.all { it.length <= 20 })
        assertEquals(spaces, cut.joinToString(" "))
        assertEquals(listOf("1,000,000 and 12:30 are kept together"), texts("1,000,000 and 12:30 are kept together", 40))
        val numbers = "values 1,000,000 2,000,000 3,000,000"
        assertTrue(texts(numbers, 12).none { it.endsWith(",") || it.startsWith(",") })
        val cjk = "\u5b57".repeat(250)
        val pieces = texts(cjk, 100)
        assertEquals(listOf(100, 100, 50), pieces.map { it.length })
        val word = "a".repeat(30)
        assertEquals(listOf(word.substring(0, 20), word.substring(20)), texts(word, 20))
    }

    @Test
    fun `chunks cover the text in order and punctuation alone is not speakable`() {
        val text = "  Hello.  \u2026\u2026  \u3002 World!\n\n"
        val chunks = SentenceSplitter.split(text)
        assertEquals(0, chunks.first().start)
        assertEquals(text.length, chunks.last().end)
        chunks.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
        chunks.forEach { assertEquals(text.substring(it.start, it.end).trim(), it.text) }
        assertEquals(listOf("Hello.", "World!"), chunks.filter { it.speakable }.map { it.text })
        assertFalse(TextChunk(0, 3, "...").speakable)
        assertEquals(emptyList<TextChunk>(), SentenceSplitter.split(""))
    }

    @Test
    fun `the limit holds for a long mixed text`() {
        val text = buildString {
            repeat(200) { index -> append(if (index % 3 == 0) "Sentence number $index has several words in it, and a clause. " else "\u8fd9\u662f\u7b2c $index \u53e5, \u5b83\u6709\u4e00\u4e9b\u6c49\u5b57\u3002") }
        }
        val chunks = SentenceSplitter.split(text)
        assertTrue(chunks.all { it.text.length <= SentenceSplitter.DEFAULT_MAX_CHARS })
        assertEquals(text.length, chunks.last().end)
        assertEquals(text.filterNot { it.isWhitespace() }, chunks.joinToString("") { it.text }.filterNot { it.isWhitespace() })
    }
}
