package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap D107, P7.7.3 (E2): which punctuation model serves a recognizer and each final, what the models read and
 * how their answers become the final text, the fallback when a model fails, finals only, `punctuation: false`, and
 * how punctuated finals join into a file transcript.
 * zh-CN: 路线图 D107, P7.7.3 (E2): 哪个标点模型服务识别器与每句最终结果, 模型读入什么以及其应答如何成为最终文本, 模型出错时的回退, 只处理最终
 * 结果, `punctuation: false`, 以及加了标点的最终结果如何拼接为文件转写.
 */
class PunctuationTest {

    private val ct = PunctuationModel("punct-ct-transformer-zh-en", PunctuationRules.CT_TRANSFORMER, listOf("zh", "en"), isDefault = true)
    private val case = PunctuationModel("online-punct-en", PunctuationRules.CNN_BILSTM, listOf("en"))
    private val installed = listOf(ct, case)

    @Test
    fun `candidates prefer the more specific model and skip models no final can pick`() {
        assertEquals(listOf(case, ct), PunctuationRules.candidates("zipformer2", listOf("zh", "en"), installed))
        assertEquals(listOf(ct), PunctuationRules.candidates("zipformer2", listOf("zh"), installed))
        assertEquals(listOf(ct), PunctuationRules.candidates("zipformer2", listOf("zh-CN"), installed))
        assertEquals("the English case model wins English", listOf(case), PunctuationRules.candidates("zipformer", listOf("en"), installed))
        assertEquals(listOf(ct), PunctuationRules.candidates("zipformer", listOf("en"), listOf(ct)))
        assertEquals(emptyList<PunctuationModel>(), PunctuationRules.candidates("zipformer", listOf("fr"), installed))
        assertEquals(emptyList<PunctuationModel>(), PunctuationRules.candidates("zipformer", listOf("ru"), installed))
        assertEquals(emptyList<PunctuationModel>(), PunctuationRules.candidates("zipformer", listOf("ko"), installed))
        assertEquals(emptyList<PunctuationModel>(), PunctuationRules.candidates("zipformer", listOf("en"), emptyList()))
        assertEquals("SenseVoice punctuates itself", emptyList<PunctuationModel>(), PunctuationRules.candidates("sense-voice", listOf("zh", "en"), installed))
    }

    @Test
    fun `among models of the same breadth the default comes first, then the id`() {
        val a = PunctuationModel("a", PunctuationRules.CNN_BILSTM, listOf("en"))
        val b = PunctuationModel("b", PunctuationRules.CNN_BILSTM, listOf("en"))
        val z = PunctuationModel("z", PunctuationRules.CNN_BILSTM, listOf("en"), isDefault = true)
        assertEquals(listOf(a), PunctuationRules.candidates("zipformer", listOf("en"), listOf(b, a)))
        assertEquals(listOf(z), PunctuationRules.candidates("zipformer", listOf("en"), listOf(b, a, z)))
    }

    @Test
    fun `each final picks by its own text`() {
        val languages = listOf("zh", "en")
        val candidates = PunctuationRules.candidates("zipformer2", languages, installed)
        assertEquals(ct, PunctuationRules.choose("\u4eca\u5929\u5929\u6c14\u5f88\u597d", languages, candidates))
        assertEquals("Chinese with English words is Chinese", ct, PunctuationRules.choose("\u6211\u4eec\u7528 PYTHON \u5199\u4ee3\u7801", languages, candidates))
        assertEquals(case, PunctuationRules.choose("HELLO WORLD HOW ARE YOU", languages, candidates))
        assertEquals("a decimal point is not punctuation", case, PunctuationRules.choose("IT IS 3.5 METERS", languages, candidates))
        assertEquals(case, PunctuationRules.choose("IT'S FINE", languages, candidates))
        assertNull("already punctuated", PunctuationRules.choose("hello, world", languages, candidates))
        assertNull(PunctuationRules.choose("\u4f60\u597d\uff0c\u4e16\u754c", languages, candidates))
        assertNull(PunctuationRules.choose("the end.", languages, candidates))
        assertNull("Cyrillic", PunctuationRules.choose("\u043f\u0440\u0438\u0432\u0435\u0442 \u043c\u0438\u0440", languages, candidates))
        assertNull("accented Latin", PunctuationRules.choose("\u00e7a va bien", languages, candidates))
        assertNull("Hangul", PunctuationRules.choose("\uc548\ub155\ud558\uc138\uc694", languages, candidates))
        assertNull("no letters", PunctuationRules.choose("123 456", languages, candidates))
        assertNull("the recognizer does not speak English", PunctuationRules.choose("HELLO", listOf("zh"), PunctuationRules.candidates("zipformer2", listOf("zh"), installed)))
        assertNull("no model covers French", PunctuationRules.choose("bonjour", listOf("fr"), PunctuationRules.candidates("zipformer", listOf("fr"), installed)))
    }

    @Test
    fun `the case model reads lower case and English answers use ASCII marks`() {
        assertEquals("hello world", PunctuationRules.input(case, "HELLO WORLD"))
        assertEquals("HELLO WORLD", PunctuationRules.input(ct, "HELLO WORLD"))
        assertEquals("hello world, how are you?", PunctuationRules.output("hello world\uff0chow are you\uff1f", "en"))
        assertEquals("one, two. three", PunctuationRules.output("one\uff0c two\u3002 three", "en"))
        assertEquals("Hello world. How are you?", PunctuationRules.output(" Hello world.  How are you? ", "en"))
        assertEquals("\u4eca\u5929\u5929\u6c14\u5f88\u597d\uff0c\u6211\u4eec\u51fa\u53bb\u73a9\u3002", PunctuationRules.output("\u4eca\u5929\u5929\u6c14\u5f88\u597d\uff0c\u6211\u4eec\u51fa\u53bb\u73a9\u3002", "zh"))
        assertFalse(PunctuationRules.hasPunctuation("version 2.0 of it's"))
        assertTrue(PunctuationRules.hasPunctuation("2. go"))
    }

    @Test
    fun `utterance punctuation routes each final and falls back to the recognized text`() {
        val calls = ArrayList<String>()
        val failures = ArrayList<String>()
        var fail = false
        val caseEngine = Punctuator { text ->
            calls += "case:$text"
            if (fail) throw IllegalStateException("native failure")
            "Hello world. How are you?"
        }
        val ctEngine = Punctuator { text ->
            calls += "ct:$text"
            if (text.startsWith("\u7a7a")) "" else "${text}\u3002"
        }
        val languages = listOf("zh", "en")
        val engines = PunctuationRules.candidates("zipformer2", languages, installed).map { it to (if (it == case) caseEngine else ctEngine) }
        val punctuation = UtterancePunctuation(languages, engines) { model, _ -> failures += model.modelId }

        assertEquals("Hello world. How are you?", punctuation.punctuate("HELLO WORLD HOW ARE YOU"))
        assertEquals("\u4eca\u5929\u5929\u6c14\u5f88\u597d\u3002", punctuation.punctuate("\u4eca\u5929\u5929\u6c14\u5f88\u597d"))
        assertEquals("already punctuated", "ok, fine", punctuation.punctuate("ok, fine"))
        assertEquals("Cyrillic stays", "\u043f\u0440\u0438\u0432\u0435\u0442", punctuation.punctuate("\u043f\u0440\u0438\u0432\u0435\u0442"))
        assertEquals("an empty answer keeps the text", "\u7a7a\u767d", punctuation.punctuate("\u7a7a\u767d"))
        fail = true
        assertEquals("a failing model keeps the text", "HELLO AGAIN", punctuation.punctuate("HELLO AGAIN"))
        assertEquals(listOf("online-punct-en"), failures)
        assertEquals(listOf("case:hello world how are you", "ct:\u4eca\u5929\u5929\u6c14\u5f88\u597d", "ct:\u7a7a\u767d", "case:hello again"), calls)
    }

    @Test
    fun `dictations punctuate finals only and not with punctuation off`() {
        val calls = ArrayList<String>()
        val punctuator = Punctuator { text ->
            calls += text
            "$text."
        }
        val events = RecordingEvents()
        val session = DictationSession("s1", ListenOptions(), events)
        session.start("asr.m") { StreamingDictation(FakeStream(), FakeVad(), it, true, true, 1200, true, punctuator) }
        session.accept(Audio.speech(500))
        session.accept(Audio.silence(1500))
        session.finish()
        assertEquals(listOf("w w w w w"), calls)
        assertEquals("w w w w w.", events.finals.single().text)
        assertEquals("end:stopped:w w w w w.", events.log.last())
        assertTrue("partials stay as recognized", events.log.filter { it.startsWith("partial:") }.none { it.endsWith(".") })

        val plain = RecordingEvents()
        val off = DictationSession("s2", ListenOptions(punctuation = false), plain)
        off.start("asr.m") { StreamingDictation(FakeStream(), FakeVad(), it, true, true, 1200, false, punctuator) }
        off.accept(Audio.speech(500))
        off.finish()
        assertEquals("w w w w w", plain.finals.single().text)
        assertEquals(1, calls.size)

        val offline = RecordingEvents()
        val segments = DictationSession("s3", ListenOptions(), offline)
        segments.start("asr.sense") { OfflineDictation(FakeRecognizer(), FakeVad(), it, reportVad = false, silenceEndMillis = 1200, punctuation = true, punctuator = punctuator) }
        segments.accept(Audio.speech(1000))
        segments.finish()
        assertEquals("s10..", offline.finals.single().text)
        assertTrue(offline.log.filter { it.startsWith("partial:") }.all { it.endsWith("s10.") })
    }

    @Test
    fun `punctuated finals join without spaces between Chinese and with spaces between English`() {
        assertEquals("\u4eca\u5929\u5929\u6c14\u5f88\u597d\u3002\u6211\u4eec\u51fa\u53bb\u73a9\u5427\uff1f", SpeechText.join(listOf("\u4eca\u5929\u5929\u6c14\u5f88\u597d\u3002", "\u6211\u4eec\u51fa\u53bb\u73a9\u5427\uff1f")))
        assertEquals("Hello world. How are you?", SpeechText.join(listOf("Hello world.", "How are you?")))
        assertEquals("\u4f60\u597d\u3002 Hello.", SpeechText.join(listOf("\u4f60\u597d\u3002", "Hello.")))
    }
}
