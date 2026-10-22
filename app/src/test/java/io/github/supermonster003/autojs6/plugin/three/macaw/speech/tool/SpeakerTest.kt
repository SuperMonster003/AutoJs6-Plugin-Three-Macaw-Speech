package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsModelVoices
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Roadmap P7.6.3 (D96, D101 E2): [Speaker] on fake text-to-speech engines: the engine order and the fallback, the
 * language or voice and the rate handed to the engine, waiting for the end or only for the start, `BUSY` while a reading
 * goes on, cancellation and the time limit stopping the reading, engine errors and an engine that stops answering.
 * zh-CN: 路线图 P7.6.3 (D96, D101 E2): 以假 TTS 引擎驱动 [Speaker]: 引擎顺序与回退, 交给引擎的语言或语音与语速, 等待结束或只等开始, 朗读进行中时
 * `BUSY`, 取消与时限停止朗读, 引擎错误与不再应答的引擎.
 */
class SpeakerTest {

    private var now = 1000L
    private val speaker = Speaker { now }
    private val table = TtsVoiceTable(
        listOf(
            TtsModelVoices("tts.vits.melo.zh-en", listOf("zh", "en"), 1, 44100, isDefault = true),
            TtsModelVoices("tts.vits.piper.en-us-lessac-medium", listOf("en"), 1, 22050, isDefault = false),
        ),
    )

    @Test
    fun `the plugin's engine reads when one of its voices speaks the language`() {
        val own = FakeOutput(OWN, languages = mapOf("zh" to TtsVoiceTable.LANG_AVAILABLE)) { listener ->
            listener.started()
            now += 2500
            listener.done()
        }
        val outputs = FakeOutputs(own, system = FakeOutput(SYSTEM))
        val report = speak(outputs, SpeakRequest("打开设置"))
        assertEquals(SpeakReport(OWN, "zh", "tts.vits.melo.zh-en/0", completed = true, durationMillis = 2500, warnings = emptyList()), report)
        assertEquals(listOf<String?>(OWN), outputs.opened)
        assertEquals(listOf("language zh", "rate 1.0", "speak 4", "shutdown"), own.calls)
    }

    @Test
    fun `a named voice is selected by name with the requested rate`() {
        val voice = "tts.vits.melo.zh-en/0/en-US"
        val own = FakeOutput(OWN, voices = setOf(voice)) { it.done() }
        val report = speak(FakeOutputs(own), SpeakRequest("hello", language = "en", voice = VoiceChoice("tts.vits.melo.zh-en", "0"), speed = 1.5f))
        assertEquals(voice, report.voice)
        assertEquals("en", report.language)
        assertEquals("a reading that never reported its start", 0L, report.durationMillis)
        assertEquals(listOf("voice $voice", "rate 1.5", "speak 5", "shutdown"), own.calls)
    }

    @Test
    fun `the system engine reads when the plugin's engine cannot`() {
        val system = FakeOutput(SYSTEM, languages = mapOf("en-US" to TtsVoiceTable.LANG_COUNTRY_AVAILABLE)) { it.started(); it.done() }
        val refused = speak(FakeOutputs(own = null, system = system), SpeakRequest("hello", voice = VoiceChoice("tts.vits.piper.en-us-lessac-medium", "0")))
        assertEquals(SYSTEM, refused.enginePackage)
        assertNull("the voice belongs to the plugin's engine", refused.voice)
        assertEquals(
            listOf(ToolWarning(SpeechErrorCodes.ENGINE_FAILED, "3-Macaw Speech's engine could not read (it did not start (-1)); the system default engine read instead and the voice was ignored")),
            refused.warnings,
        )
        assertEquals("the language of the named voice", listOf("language en-US", "rate 1.0", "speak 5", "shutdown"), system.calls)

        val missing = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_MISSING_DATA))
        val quiet = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE), accepts = false)
        listOf(missing to "its voice data for the language is not installed", quiet to "it refused the text").forEach { (own, problem) ->
            val other = FakeOutput(SYSTEM, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) { it.done() }
            val report = speak(FakeOutputs(own, other), SpeakRequest("hello"))
            assertEquals(SYSTEM, report.enginePackage)
            assertEquals(listOf(SpeakRules.fallbackWarning(problem, named = false)), report.warnings)
            assertEquals("the refused engine is shut down", "shutdown", own.calls.last())
        }
    }

    @Test
    fun `NOT_READY names every engine asked and its problem`() {
        val system = FakeOutput(SYSTEM, languages = mapOf("fr" to TtsVoiceTable.LANG_MISSING_DATA))
        val french = assertThrows(PluginToolException::class.java) { speak(FakeOutputs(FakeOutput(OWN), system), SpeakRequest("bonjour", language = "fr")) }
        assertEquals(PluginToolErrorCodes.NOT_READY, french.code)
        assertEquals(
            SpeakRules.unavailable("fr", listOf(SpeakRules.OWN_ENGINE to "no installed voice speaks the language", "the system default engine $SYSTEM" to "its voice data for the language is not installed")),
            french.message,
        )
        assertEquals(listOf("language fr", "shutdown"), system.calls)

        val asOwn = FakeOutput(OWN)
        val sameEngine = assertThrows(PluginToolException::class.java) { speak(FakeOutputs(own = null, system = asOwn), SpeakRequest("hello")) }
        assertEquals(
            "the system default engine is the plugin's own",
            SpeakRules.unavailable("en", listOf(SpeakRules.OWN_ENGINE to "it did not start (-1)", "the system default engine $OWN" to "it is 3-Macaw Speech's engine")),
            sameEngine.message,
        )
        assertEquals(listOf("shutdown"), asOwn.calls)

        val nothing = assertThrows(PluginToolException::class.java) { speak(FakeOutputs(own = null, system = null), SpeakRequest("bonjour", language = "fr")) }
        assertTrue(nothing.message!!.contains("the system default engine: it did not start (-1)"))
    }

    @Test
    fun `without wait the call returns at the start and the reading holds the speaker until it ends`() {
        val first = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) { it.started() }
        val report = speak(FakeOutputs(first), SpeakRequest("hello", wait = false))
        assertFalse(report.completed)
        assertNull(report.durationMillis)
        assertFalse("still reading", "shutdown" in first.calls)

        val busy = assertThrows(PluginToolException::class.java) { speak(FakeOutputs(FakeOutput(OWN)), SpeakRequest("again")) }
        assertEquals(PluginToolErrorCodes.BUSY, busy.code)
        now += SpeakRules.GRACE_MILLIS
        assertEquals("the engine still speaks", PluginToolErrorCodes.BUSY, assertThrows(PluginToolException::class.java) { speak(FakeOutputs(FakeOutput(OWN)), SpeakRequest("again")) }.code)

        first.listener!!.done()
        assertEquals("the end releases the reading", "shutdown", first.calls.last())
        val second = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) { it.started(); it.done() }
        assertTrue(speak(FakeOutputs(second), SpeakRequest("again")).completed)

        val stale = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) { it.started() }
        speak(FakeOutputs(stale), SpeakRequest("hello", wait = false))
        stale.speaking = false
        now += SpeakRules.GRACE_MILLIS
        val third = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) { it.done() }
        assertTrue("a reading the engine no longer speaks does not hold the speaker", speak(FakeOutputs(third), SpeakRequest("again")).completed)
        assertEquals("shutdown", stale.calls.last())
    }

    @Test
    fun `a reading stopped by the engine itself is not completed`() {
        val own = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) {
            it.started()
            now += 1200
            it.stopped()
        }
        val report = speak(FakeOutputs(own), SpeakRequest("hello"))
        assertFalse(report.completed)
        assertEquals(1200L, report.durationMillis)
    }

    @Test
    fun `cancelling the call stops the reading`() {
        val cancel = FakeCancel()
        val own = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) {
            it.started()
            cancel.cancel()
        }
        val error = assertThrows(PluginToolException::class.java) { speak(FakeOutputs(own), SpeakRequest("hello"), cancel = cancel) }
        assertEquals(PluginToolErrorCodes.CANCELLED, error.code)
        assertEquals(listOf("language en", "rate 1.0", "speak 5", "stop", "shutdown"), own.calls)
        assertEquals("one hook per call", 1, cancel.hooks.size)

        val early = FakeCancel().apply { cancel() }
        assertEquals(PluginToolErrorCodes.CANCELLED, assertThrows(PluginToolException::class.java) { speak(FakeOutputs(FakeOutput(OWN)), SpeakRequest("hello"), cancel = early) }.code)

        val ignored = FakeCancel()
        val deaf = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE), stopReports = false) {
            it.started()
            ignored.cancel()
        }
        val clock = ticking()
        val stuck = assertThrows(PluginToolException::class.java) { Speaker(clock).speak(FakeOutputs(deaf), SpeakRequest("hello"), plan(SpeakRequest("hello")), Long.MAX_VALUE, ignored) }
        assertEquals("an engine that does not report the stop", PluginToolErrorCodes.CANCELLED, stuck.code)
        assertEquals("shutdown", deaf.calls.last())
    }

    @Test
    fun `the time limit stops the reading`() {
        val own = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) {
            it.started()
            now += 6000
        }
        val error = assertThrows(PluginToolException::class.java) { speak(FakeOutputs(own), SpeakRequest("hello"), deadline = now + 5000) }
        assertEquals(PluginToolErrorCodes.FAILED, error.code)
        assertEquals(SpeakRules.expired().message, error.message)
        assertEquals(listOf("language en", "rate 1.0", "speak 5", "stop", "shutdown"), own.calls)

        val none = FakeOutputs(FakeOutput(OWN))
        assertEquals(SpeakRules.expired().message, assertThrows(PluginToolException::class.java) { speak(none, SpeakRequest("hello"), deadline = now) }.message)
        assertTrue("no engine opened after the limit", none.opened.isEmpty())
    }

    @Test
    fun `engine errors fail the call`() {
        val missing = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) { it.failed(SpeakRules.ERROR_NOT_INSTALLED_YET) }
        val notReady = assertThrows(PluginToolException::class.java) { speak(FakeOutputs(missing), SpeakRequest("hello")) }
        assertEquals(SpeakRules.engineError(SpeakRules.ERROR_NOT_INSTALLED_YET, OWN).message, notReady.message)
        assertEquals(PluginToolErrorCodes.NOT_READY, notReady.code)
        assertEquals("shutdown", missing.calls.last())

        val broken = FakeOutput(SYSTEM, languages = mapOf("fr" to TtsVoiceTable.LANG_AVAILABLE)) { it.started(); it.failed(SpeakRules.ERROR_SYNTHESIS) }
        val failed = assertThrows(PluginToolException::class.java) { speak(FakeOutputs(null, broken), SpeakRequest("bonjour", language = "fr")) }
        assertEquals(PluginToolErrorCodes.FAILED, failed.code)
        assertEquals(SpeakRules.engineError(SpeakRules.ERROR_SYNTHESIS, SYSTEM).message, failed.message)
    }

    @Test
    fun `an engine that stops answering fails the call`() {
        val dead = FakeOutput(OWN, languages = mapOf("en" to TtsVoiceTable.LANG_AVAILABLE)) { it.started() }
        dead.speaking = false
        val error = assertThrows(PluginToolException::class.java) { Speaker(ticking()).speak(FakeOutputs(dead), SpeakRequest("hello"), plan(SpeakRequest("hello")), Long.MAX_VALUE, FakeCancel()) }
        assertEquals(SpeakRules.silent(OWN).message, error.message)
        assertEquals("shutdown", dead.calls.last())
    }

    private fun plan(request: SpeakRequest) = SpeakRules.plan(request, table)

    private fun speak(outputs: FakeOutputs, request: SpeakRequest, deadline: Long = now + 60_000, cancel: FakeCancel = FakeCancel()): SpeakReport =
        speaker.speak(outputs, request, plan(request), deadline, cancel)

    /** A clock that moves 500 ms on every reading, for the loops that wait. zh-CN: 每读一次前进 500 ms 的时钟. */
    private fun ticking(): () -> Long {
        var time = 0L
        return { time += 500; time }
    }

    private class FakeOutput(
        override val enginePackage: String,
        private val languages: Map<String, Int> = emptyMap(),
        private val voices: Set<String> = emptySet(),
        private val accepts: Boolean = true,
        private val stopReports: Boolean = true,
        private val script: (SpeakListener) -> Unit = {},
    ) : SpeechOutput {
        val calls = CopyOnWriteArrayList<String>()
        var listener: SpeakListener? = null

        @Volatile
        var speaking = true

        override fun language(tag: String): Int = (languages[tag] ?: TtsVoiceTable.LANG_NOT_SUPPORTED).also { calls += "language $tag" }

        override fun voice(name: String): Boolean = (name in voices).also { calls += "voice $name" }

        override fun rate(speed: Float) {
            calls += "rate $speed"
        }

        override fun speak(text: String, listener: SpeakListener): Boolean {
            calls += "speak ${text.length}"
            if (!accepts) return false
            this.listener = listener
            script(listener)
            return true
        }

        override fun isSpeaking(): Boolean = speaking

        override fun stop() {
            calls += "stop"
            if (stopReports) listener?.stopped()
        }

        override fun shutdown() {
            calls += "shutdown"
        }
    }

    private class FakeOutputs(private val own: FakeOutput?, private val system: FakeOutput? = null) : SpeechOutputs {
        override val ownPackage = OWN
        val opened = ArrayList<String?>()

        override fun open(enginePackage: String?, timeoutMillis: Long): SpeechOutputs.Opened {
            opened += enginePackage
            val output = if (enginePackage == null) system else own
            return output?.let { SpeechOutputs.Opened.Ready(it) } ?: SpeechOutputs.Opened.Refused("it did not start (-1)")
        }
    }

    private class FakeCancel : ToolCancellation {
        val hooks = CopyOnWriteArrayList<Runnable>()

        @Volatile
        override var cancelled = false

        override fun onCancel(hook: Runnable) {
            hooks += hook
        }

        fun cancel() {
            cancelled = true
            hooks.forEach { it.run() }
        }
    }

    private companion object {
        const val OWN = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
        const val SYSTEM = "com.example.tts"
    }

}
