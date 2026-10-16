package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.kws

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.RepositoryFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Keyword
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Keyword spelling of roadmap D18 / appendix D Q4 against the real zh-en KWS token set (`tokens.txt` of
 * `kws.zipformer.zh-en.2025-12-20`, Apache-2.0, kept as a test resource and pinned to the catalog digest) and the
 * generated pinyin table: the expectations are the upstream `test_wavs/keywords.txt` of that model, which sherpa-onnx
 * produced from `keywords_raw.txt` with pypinyin and the model's lexicon.
 * zh-CN: 路线图 D18 / 附录 D Q4 的关键词拼写, 使用真实的 zh-en KWS token 集合 (`kws.zipformer.zh-en.2025-12-20` 的 `tokens.txt`, Apache-2.0,
 * 作为测试资源保存并按目录摘要固定) 与生成的拼音表: 预期值即该模型上游的 `test_wavs/keywords.txt`, 由 sherpa-onnx 以 pypinyin 与模型词典从
 * `keywords_raw.txt` 生成.
 */
class KeywordTokenizerTest {

    private val tokenBytes = javaClass.getResourceAsStream("/kws/tokens.txt")!!.use { it.readBytes() }
    private val tokens = KeywordTokenizer.readTokens(tokenBytes.inputStream())

    // The en.phone lines of the words used here, copied from the model's lexicon.
    private val lexicon = """
        CHILD CH AY1 L D
        HEY HH EY1
        LIGHT L AY1 T
        LOVELY L AH1 V L IY0
        MACAW M AH0 K AO1
        UP AH1 P
        WORLD W ER1 L D
    """.trimIndent()

    private fun tokenizer(keywords: List<String>): KeywordTokenizer {
        val (words, characters) = KeywordTokenizer.needs(keywords)
        val readings = Files.newInputStream(RepositoryFiles.path("app/src/main/assets/kws/pinyin.txt")).use { KeywordTokenizer.readReadings(it, characters) }
        val phones = KeywordTokenizer.readPhones(BufferedReader(StringReader(lexicon)), words)
        return KeywordTokenizer(tokens, readings::get, phones::get)
    }

    private fun encode(keyword: String) = tokenizer(listOf(keyword)).encode(keyword).joinToString(" ")

    @Test
    fun `the test token set is the catalog file`() {
        val digest = MessageDigest.getInstance("SHA-256").digest(tokenBytes).joinToString("") { "%02x".format(it) }
        assertEquals("2d3f32311f9b692b964da3c90e830258d3e78e013cb0c992dbfb15cd5a1a71b0", digest)
        assertEquals(263, tokens.size)
    }

    @Test
    fun `chinese keywords match the upstream keywords file`() {
        mapOf(
            "文森特卡索" to "w én s ēn t è k ǎ s uǒ",
            "周望军" to "zh ōu w àng j ūn",
            "朱丽楠" to "zh ū l ì n án",
            "蒋友伯" to "j iǎng y ǒu b ó",
            "女儿" to "n ǚ ér",
            "法国" to "f ǎ g uó",
            "见面会" to "j iàn m iàn h uì",
            "落实" to "l uò sh í",
            "小六小六" to "x iǎo l iù x iǎo l iù",
        ).forEach { (keyword, expected) -> assertEquals(keyword, expected, encode(keyword)) }
    }

    @Test
    fun `english words come from the lexicon and mix with chinese`() {
        assertEquals("L AY1 T AH1 P", encode("LIGHT UP"))
        assertEquals("L AH1 V L IY0 CH AY1 L D", encode("lovely child"))
        assertEquals("HH EY1 M AH0 K AO1", encode("Hey, macaw!"))
        assertEquals("n ǐ h ǎo W ER1 L D", encode("你好 world"))
    }

    @Test
    fun `space separated model tokens are taken as they are`() {
        assertEquals("x iǎo l è", encode("x iǎo l è"))
        assertEquals("HH EY1", encode("HH EY1"))
    }

    @Test
    fun `keywords the model cannot spell are refused`() {
        listOf("hello", "小六 2", "@@", "  ", "x iǎo xyz").forEach { keyword ->
            val error = assertThrows(keyword, SpeechException::class.java) { tokenizer(listOf(keyword)).encode(keyword) }
            assertEquals(keyword, SpeechErrorCodes.INVALID_OPTIONS, error.code)
        }
        val noReading = KeywordTokenizer(tokens, { null }, { null })
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { noReading.encode("小") }.code)
        val noToken = KeywordTokenizer(setOf("x"), { "xiǎo" }, { null })
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { noToken.encode("小") }.code)
    }

    @Test
    fun `syllables split into the initial and the tonal final`() {
        assertEquals(listOf("zh", "uāng"), KeywordTokenizer.split("zhuāng"))
        assertEquals(listOf("y", "uè"), KeywordTokenizer.split("yuè"))
        assertEquals(listOf("ér"), KeywordTokenizer.split("ér"))
        assertEquals(listOf("ǎo"), KeywordTokenizer.split("ǎo"))
        assertEquals(listOf("ń"), KeywordTokenizer.split("ń"))
        assertEquals(listOf("h", "m"), KeywordTokenizer.split("hm"))
    }

    @Test
    fun `the stream buffer carries boosts, thresholds and index labels`() {
        val keywords = listOf(Keyword("小六小六"), Keyword("hey macaw", boost = 2f, threshold = 0.3f))
        val encoded = listOf(listOf("x", "iǎo", "l", "iù"), listOf("HH", "EY1", "M", "AH0", "K", "AO1"))
        assertEquals("x iǎo l iù @k0/HH EY1 M AH0 K AO1 :2.0 #0.3 @k1", KeywordTokenizer.buffer(keywords, encoded))
        assertEquals(1, KeywordTokenizer.indexOf("k1", 2))
        assertNull(KeywordTokenizer.indexOf("k2", 2))
        assertNull(KeywordTokenizer.indexOf("LIGHT UP", 2))
    }

    @Test
    fun `lexicon and pinyin lookups read only what a keyword needs`() {
        val (words, characters) = KeywordTokenizer.needs(listOf("Hey macaw", "小六", "x iǎo"))
        assertEquals(setOf("HEY", "MACAW", "X", "I", "O"), words)
        assertEquals(setOf('小', '六'), characters)
        val phones = KeywordTokenizer.readPhones(BufferedReader(StringReader("HEY HH EY1\nHEY HH EY2\nMACAW M AH0 K AO1\n")), setOf("HEY", "NOPE"))
        assertEquals(mapOf("HEY" to listOf("HH", "EY1")), phones)
        val readings = KeywordTokenizer.readReadings("# header\n小\txiǎo\n六\tliù\n".byteInputStream(), setOf('六'))
        assertEquals(mapOf('六' to "liù"), readings)
    }
}
