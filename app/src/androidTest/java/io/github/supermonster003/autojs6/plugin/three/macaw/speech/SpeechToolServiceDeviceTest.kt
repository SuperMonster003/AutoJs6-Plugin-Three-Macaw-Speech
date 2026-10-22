package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioCaptures
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MicrophoneSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool.ListenRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool.SpeechToolCatalog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool.SpeechToolService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.settings.SettingsActivity
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.tool.api.IPluginToolCallback
import org.autojs.plugin.tool.api.IPluginToolProvider
import org.autojs.plugin.tool.api.PluginToolAcceptKeys
import org.autojs.plugin.tool.api.PluginToolCancelKeys
import org.autojs.plugin.tool.api.PluginToolCapabilityKeys
import org.autojs.plugin.tool.api.PluginToolCatalogKeys
import org.autojs.plugin.tool.api.PluginToolContract
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorKeys
import org.autojs.plugin.tool.api.PluginToolInvokeKeys
import org.autojs.plugin.tool.api.PluginToolResultKeys
import org.autojs.plugin.tool.api.PluginToolValues
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * The four tools of the plugin tool provider through the service itself (roadmap P7.6.4, D101 E3): discovery, the
 * capability and the catalog, the refusals and the `NOT_READY` answers that need no model (CI's emulators run those),
 * then with installed models a transcription of `en.wav` and of its m4a, a synthesis to WAV and to m4a that reads back
 * and transcribes again, a reading by 3-Macaw Speech's engine with `wait: false` and `BUSY`, a dictation of `en.wav`
 * played into the [VirtualRoom] (`-e macawRoom virtual`) or through the speaker, and the cancellation of each long call.
 * The test runs in the plugin process, so the Binder calls stay in process. Results are `MacawDeviceRun` lines with
 * numbers only, never a transcript or the spoken text.
 * zh-CN: 经服务本身测试插件工具提供方的四个工具 (路线图 P7.6.4, D101 E3): 发现, 能力与目录, 不需要模型的拒绝与 `NOT_READY` (CI 的模拟器运行这些),
 * 以及装有模型时: 识别 `en.wav` 与其 m4a, 合成 WAV 与 m4a 并读回再识别, 以本插件引擎朗读 (含 `wait: false` 与 `BUSY`), 把 `en.wav` 放进
 * [VirtualRoom] (`-e macawRoom virtual`) 或经扬声器播放来听写, 以及每个长调用的取消. 测试运行于插件进程, Binder 调用不跨进程. 结果为只含数值的
 * `MacawDeviceRun` 日志行, 从不记识别文字或朗读文字.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SpeechToolServiceDeviceTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var directory: File

    @Before
    fun setUp() {
        directory = File(context.cacheDir, "speech-tool-test-${UUID.randomUUID()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun test0DiscoveryCapabilityAndCatalog() = withProvider { provider ->
        assertEquals(PluginToolContract.CONTRACT_VERSION, provider.info.capabilities?.getInt(PluginToolCapabilityKeys.CONTRACT_VERSION))
        val catalog = JSONObject(provider.getCatalog(Bundle()).getString(PluginToolCatalogKeys.CATALOG_JSON)!!)
        assertEquals(SpeechToolCatalog.SHORT_NAME, catalog.getString("shortName"))
        val tools = catalog.getJSONArray("tools").let { array -> (0 until array.length()).associate { array.getJSONObject(it).let { tool -> tool.getString("name") to tool } } }
        assertEquals(setOf(TOOL_LISTEN, TOOL_SPEAK, TOOL_TRANSCRIBE, TOOL_SYNTHESIZE), tools.keys)
        assertEquals("write", tools.getValue(TOOL_SYNTHESIZE).getString("risk"))
        listOf(TOOL_LISTEN, TOOL_SPEAK, TOOL_TRANSCRIBE).forEach { assertEquals(it, "readonly", tools.getValue(it).getString("risk")) }
        val output = tools.getValue(TOOL_SYNTHESIZE).getJSONArray("outputs").getJSONObject(0)
        assertEquals(SpeechToolCatalog.OUTPUT_AUDIO, output.getString("name"))
        SpeechDevice.log("tools catalog tools=${tools.size} bytes=${catalog.toString().length}")
    }

    @Test
    fun test1RefusalsAndNotReadyWithoutModels() = withProvider { provider ->
        val en = SpeechDevice.asset("test-audio/en.wav")
        val text = File(directory, "notes.txt").apply { writeText("not audio") }
        val asr = asrInstalled()
        val tts = voices().isNotEmpty()

        assertEquals(PluginToolErrorCodes.UNKNOWN_TOOL, call(provider, "whisper", JSONObject()).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, TOOL_SPEAK, JSONObject()).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, TOOL_SPEAK, JSONObject().put("text", "hello").put("speed", 9)).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, TOOL_SYNTHESIZE, JSONObject().put("text", "hello").put("format", "mp3")).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, TOOL_SYNTHESIZE, JSONObject().put("text", "a".repeat(SpeechToolCatalog.MAX_SYNTHESIZE_CHARS + 1))).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, TOOL_SYNTHESIZE, JSONObject().put("text", "hello").put("voice", "tts.none/0")).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, TOOL_TRANSCRIBE, JSONObject()).code)
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, TOOL_LISTEN, JSONObject().put("hotwords", "x")).code)
        assertEquals(PluginToolErrorCodes.NOT_READY, call(provider, TOOL_TRANSCRIBE, JSONObject().put("model", "asr.none"), file = en).code)
        assertEquals(if (asr) PluginToolErrorCodes.INPUT_UNREADABLE else PluginToolErrorCodes.NOT_READY, call(provider, TOOL_TRANSCRIBE, JSONObject(), file = text).code)
        if (!asr || !microphoneGranted()) assertEquals(PluginToolErrorCodes.NOT_READY, call(provider, TOOL_LISTEN, JSONObject(), timeoutMs = SpeechToolCatalog.LISTEN_TIMEOUT_MS).code)
        if (!tts) assertEquals(PluginToolErrorCodes.NOT_READY, call(provider, TOOL_SYNTHESIZE, JSONObject().put("text", "hello")).code)
        SpeechDevice.log("tools refusals asr=$asr tts=$tts microphone=${microphoneGranted()}")
    }

    @Test
    fun test2Transcribe() = withProvider { provider ->
        assumeTrue("install a recognition model first", asrInstalled())
        val wav = SpeechDevice.asset("test-audio/en.wav")
        val expected = AudioInput.millis(SpeechDevice.samples("test-audio/en.wav").size.toLong())
        val started = SystemClock.elapsedRealtime()
        val result = call(provider, TOOL_TRANSCRIBE, JSONObject().put("language", "en"), file = wav).result()
        val elapsed = SystemClock.elapsedRealtime() - started
        val cer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, result.getString("text"))
        val segments = result.getJSONArray("segments")
        SpeechDevice.log("tools transcribe file=en.wav model=${result.getString("modelId")} cer=${"%.3f".format(cer)} segments=${segments.length()} durationMillis=${result.getLong("durationMillis")} expected=$expected elapsed=$elapsed")
        assertTrue("CER $cer", cer <= MAX_CER)
        assertTrue(segments.length() > 0)
        for (index in 0 until segments.length()) {
            val segment = segments.getJSONObject(index)
            assertTrue(segment.getLong("startMillis") < segment.getLong("endMillis"))
        }
        assertTrue("duration ${result.getLong("durationMillis")}, the WAV $expected", abs(result.getLong("durationMillis") - expected) <= 500)

        val m4a = call(provider, TOOL_TRANSCRIBE, JSONObject().put("language", "en"), file = SpeechDevice.asset("encoded-audio/en.m4a")).result()
        val m4aCer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, m4a.getString("text"))
        SpeechDevice.log("tools transcribe file=en.m4a model=${m4a.getString("modelId")} cer=${"%.3f".format(m4aCer)} wavCer=${"%.3f".format(cer)}")
        assertTrue("CER $m4aCer, the WAV $cer", m4aCer <= cer + 0.05)

        if (SpeechDevice.installed(SpeechDevice.SENSE_VOICE)) {
            val chosen = call(provider, TOOL_TRANSCRIBE, JSONObject().put("model", SpeechDevice.SENSE_VOICE), file = wav).result()
            assertEquals(SpeechDevice.SENSE_VOICE, chosen.getString("modelId"))
            SpeechDevice.log("tools transcribe file=en.wav model=${chosen.getString("modelId")} cer=${"%.3f".format(SpeechDevice.cer(SpeechDevice.EN_REFERENCE, chosen.getString("text")))}")
        }
    }

    @Test
    fun test3Synthesize() = withProvider { provider ->
        val voice = englishVoice()
        assumeTrue("install a TTS model that speaks English first", voice != null)
        val name = "${voice!!.modelId}/${voice.speakerId}"
        val arguments = JSONObject().put("text", SYNTHESIS_TEXT).put("language", "en").put("voice", name)

        val wav = call(provider, TOOL_SYNTHESIZE, arguments, outputs = listOf(SpeechToolCatalog.OUTPUT_AUDIO))
        val wavResult = wav.result()
        val bytes = wav.outputs.single()
        assertEquals(SpeechToolCatalog.MIME_WAV, wav.declaredOutputs().getJSONObject(0).getString("mimeType"))
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
        val sampleRate = ByteBuffer.wrap(bytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(wavResult.getInt("sampleRate"), sampleRate)
        assertEquals(wavResult.getLong("bytes"), bytes.size.toLong())
        val pcmMillis = (bytes.size - 44) / 2 * 1000L / sampleRate
        assertTrue("duration ${wavResult.getLong("durationMillis")}, the PCM $pcmMillis", abs(wavResult.getLong("durationMillis") - pcmMillis) <= 50)
        SpeechDevice.log("tools synthesize format=wav model=${voice.modelId} speaker=${voice.speakerId} rate=$sampleRate durationMillis=${wavResult.getLong("durationMillis")} bytes=${bytes.size}")

        val m4a = call(provider, TOOL_SYNTHESIZE, JSONObject(arguments.toString()).put("format", SpeechToolCatalog.FORMAT_M4A), outputs = listOf(SpeechToolCatalog.OUTPUT_AUDIO))
        val m4aResult = m4a.result()
        assertEquals(SpeechToolCatalog.MIME_M4A, m4a.declaredOutputs().getJSONObject(0).getString("mimeType"))
        assertEquals(SpeechToolCatalog.MIME_M4A, m4aResult.getString("mimeType"))
        val m4aFile = File(directory, "synthesized.m4a").apply { writeBytes(m4a.outputs.single()) }
        assertEquals(m4aResult.getLong("bytes"), m4aFile.length())
        val (mime, readMillis) = track(m4aFile)
        assertEquals(MediaFormat.MIMETYPE_AUDIO_AAC, mime)
        val difference = readMillis - m4aResult.getLong("durationMillis")
        SpeechDevice.log("tools synthesize format=m4a durationMillis=${m4aResult.getLong("durationMillis")} readBack=$readMillis difference=$difference bytes=${m4aFile.length()}")
        assertTrue("the m4a reads back $readMillis ms for ${m4aResult.getLong("durationMillis")} ms", abs(difference) <= 150)

        if (asrInstalled()) {
            val wavFile = File(directory, "synthesized.wav").apply { writeBytes(bytes) }
            val again = call(provider, TOOL_TRANSCRIBE, JSONObject().put("language", "en"), file = wavFile).result()
            val cer = SpeechDevice.cer(SYNTHESIS_TEXT, again.getString("text"))
            SpeechDevice.log("tools synthesize roundTrip model=${again.getString("modelId")} cer=${"%.3f".format(cer)}")
            assertTrue("round trip CER $cer", cer <= MAX_ROUND_TRIP_CER)
        }

        // The cancellation: a long text is still synthesizing after 1.5 s on every device.
        val pending = start(provider, TOOL_SYNTHESIZE, JSONObject(arguments.toString()).put("text", LONG_TEXT).put("format", SpeechToolCatalog.FORMAT_M4A), outputs = listOf(SpeechToolCatalog.OUTPUT_AUDIO))
        Thread.sleep(1500)
        val cancelledAt = SystemClock.elapsedRealtime()
        cancel(provider, pending.invocationId)
        val cancelled = pending.await(60)
        SpeechDevice.log("tools synthesize cancel code=${cancelled.code} afterMillis=${SystemClock.elapsedRealtime() - cancelledAt}")
        assertEquals(PluginToolErrorCodes.CANCELLED, cancelled.code)
    }

    @Test
    fun test4Speak() = withProvider { provider ->
        val voice = englishVoice()
        assumeTrue("install a TTS model that speaks English first", voice != null)
        val name = "${voice!!.modelId}/${voice.speakerId}"
        val arguments = JSONObject().put("text", SPEAK_TEXT).put("language", "en").put("voice", name)

        val spoken = untilFree { call(provider, TOOL_SPEAK, arguments) }.result()
        SpeechDevice.log("tools speak wait=true engine=${spoken.getString("engine")} completed=${spoken.getBoolean("completed")} durationMillis=${spoken.optLong("durationMillis")}")
        assertEquals(SpeechToolCatalog.ENGINE_PLUGIN, spoken.getString("engine"))
        assertEquals(context.packageName, spoken.getString("enginePackage"))
        assertTrue(spoken.getBoolean("completed"))
        assertTrue(spoken.getLong("durationMillis") > 0)

        val started = untilFree { call(provider, TOOL_SPEAK, JSONObject(arguments.toString()).put("wait", false)) }.result()
        assertFalse(started.getBoolean("completed"))
        assertFalse(started.has("durationMillis"))
        val busy = call(provider, TOOL_SPEAK, arguments)
        SpeechDevice.log("tools speak wait=false completed=${started.getBoolean("completed")} next=${busy.code}")
        assertEquals(PluginToolErrorCodes.BUSY, busy.code)

        // The reading of wait: false ends first; then a long reading is cancelled after 2.5 s.
        assertTrue(untilFree { call(provider, TOOL_SPEAK, JSONObject(arguments.toString()).put("text", "Ready.")) }.result().getBoolean("completed"))
        val reading = start(provider, TOOL_SPEAK, JSONObject(arguments.toString()).put("text", LONG_TEXT.take(1500)))
        Thread.sleep(2500)
        val cancelledAt = SystemClock.elapsedRealtime()
        cancel(provider, reading.invocationId)
        val cancelled = reading.await(30)
        SpeechDevice.log("tools speak cancel code=${cancelled.code} afterMillis=${SystemClock.elapsedRealtime() - cancelledAt}")
        assertEquals(PluginToolErrorCodes.CANCELLED, cancelled.code)
        assertTrue(untilFree { call(provider, TOOL_SPEAK, JSONObject(arguments.toString()).put("text", "Done.")) }.result().getBoolean("completed"))
    }

    @Test
    fun test5Listen() {
        assumeTrue("allow the microphone first", microphoneGranted())
        assumeTrue("install a recognition model first", asrInstalled())
        val previous = AudioCaptures.override
        AssistantDevice.setUpRoom()
        val room = AudioCaptures.override
        var opened = CountDownLatch(1)
        AudioCaptures.override = { listener ->
            opened.countDown()
            room?.invoke(listener) ?: MicrophoneSource(listener)
        }
        // D89 (1): a visible screen of the plugin gives the microphone on Android 11+; earlier versions record from the background.
        val screen = if (Build.VERSION.SDK_INT >= ListenRules.WHILE_IN_USE_SDK) ActivityScenario.launch<SettingsActivity>(Intent(context, SettingsActivity::class.java)) else null
        try {
            withProvider { provider ->
                val pending = start(provider, TOOL_LISTEN, JSONObject().put("language", "en"), timeoutMs = SpeechToolCatalog.LISTEN_TIMEOUT_MS)
                assertTrue("the dictation did not open the microphone", opened.await(60, TimeUnit.SECONDS))
                Thread.sleep(300)
                AssistantDevice.play("test-audio/en.wav")
                val result = pending.await(90).result()
                val cer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, result.getString("text"))
                SpeechDevice.log("tools listen sdk=${Build.VERSION.SDK_INT} screen=${screen != null} virtual=${AssistantDevice.virtual} model=${result.getString("modelId")} endReason=${result.getString("endReason")} cer=${"%.3f".format(cer)} durationMillis=${result.getLong("durationMillis")}")
                assertEquals(SpeechToolCatalog.END_ENDPOINT, result.getString("endReason"))
                assertTrue("CER $cer", cer <= MAX_CER)

                opened = CountDownLatch(1)
                val cancelling = start(provider, TOOL_LISTEN, JSONObject().put("language", "en"), timeoutMs = SpeechToolCatalog.LISTEN_TIMEOUT_MS)
                assertTrue("the dictation did not open the microphone", opened.await(60, TimeUnit.SECONDS))
                val cancelledAt = SystemClock.elapsedRealtime()
                cancel(provider, cancelling.invocationId)
                val cancelled = cancelling.await(30)
                SpeechDevice.log("tools listen cancel code=${cancelled.code} afterMillis=${SystemClock.elapsedRealtime() - cancelledAt}")
                assertEquals(PluginToolErrorCodes.CANCELLED, cancelled.code)

                // Only the virtual room is silent for sure; a phone hears the room.
                if (AssistantDevice.virtual) {
                    val silent = untilFree { call(provider, TOOL_LISTEN, JSONObject().put("language", "en").put("noSpeechSeconds", 2), timeoutMs = SpeechToolCatalog.LISTEN_TIMEOUT_MS) }.result()
                    SpeechDevice.log("tools listen silence endReason=${silent.getString("endReason")} chars=${silent.getString("text").length} durationMillis=${silent.getLong("durationMillis")}")
                    assertEquals(SpeechToolCatalog.END_NO_SPEECH, silent.getString("endReason"))
                    assertEquals("", silent.getString("text"))
                }
            }
        } finally {
            screen?.close()
            AssistantDevice.tearDownRoom()
            AudioCaptures.override = previous
        }
    }

    /** A finished call: the result or the error code, from the refusal of `invoke` or the callback. zh-CN: 已结束的调用. */
    private class Answer(val code: String?, val message: String?, val bundle: Bundle?, val outputs: List<ByteArray>) {
        fun result(): JSONObject {
            code?.let { throw AssertionError("the call failed: $it $message") }
            return JSONObject(bundle!!.getString(PluginToolResultKeys.RESULT_JSON)!!)
        }

        fun declaredOutputs(): JSONArray = JSONArray(bundle?.getString(PluginToolResultKeys.OUTPUTS_JSON) ?: "[]")
    }

    /**
     * A call in flight. The input descriptors stay open until it answers: in process the provider reads the very same
     * objects. zh-CN: 进行中的调用. 输入描述符保持打开直到调用结束: 同进程时提供方读取的就是这些对象.
     */
    private class Pending(val invocationId: String, private val accepted: Bundle, private val done: CountDownLatch, private val answer: AtomicReference<Answer>, private val inputs: List<ParcelFileDescriptor>) {
        fun await(seconds: Long): Answer = try {
            if (!accepted.getBoolean(PluginToolAcceptKeys.ACCEPTED)) {
                Answer(accepted.getString(PluginToolAcceptKeys.ERROR_CODE), accepted.getString(PluginToolAcceptKeys.ERROR_MESSAGE), null, emptyList())
            } else {
                assertTrue("$invocationId did not answer within $seconds s", done.await(seconds, TimeUnit.SECONDS))
                answer.get()
            }
        } finally {
            inputs.forEach { runCatching { it.close() } }
        }
    }

    private fun call(provider: IPluginToolProvider, tool: String, arguments: JSONObject, file: File? = null, outputs: List<String> = emptyList(), timeoutMs: Long = SpeechToolCatalog.LONG_TIMEOUT_MS): Answer =
        start(provider, tool, arguments, file, outputs, timeoutMs).await(timeoutMs / 1000 + 30)

    private fun start(provider: IPluginToolProvider, tool: String, arguments: JSONObject, file: File? = null, outputs: List<String> = emptyList(), timeoutMs: Long = SpeechToolCatalog.LONG_TIMEOUT_MS): Pending {
        val invocationId = UUID.randomUUID().toString()
        val done = CountDownLatch(1)
        val answer = AtomicReference<Answer>()
        val callback = object : IPluginToolCallback.Stub() {
            override fun onProgress(progress: Bundle?) = Unit

            // The base class closes the descriptors after this returns, so they are read here.
            override fun onResult(bundle: Bundle?, descriptors: Array<out ParcelFileDescriptor>?) {
                val bytes = descriptors.orEmpty().map { descriptor -> FileInputStream(descriptor.fileDescriptor).readBytes() }
                answer.set(Answer(null, null, bundle, bytes))
                done.countDown()
            }

            override fun onError(error: Bundle?) {
                answer.set(Answer(error?.getString(PluginToolErrorKeys.CODE), error?.getString(PluginToolErrorKeys.MESSAGE), null, emptyList()))
                done.countDown()
            }
        }
        val descriptors = listOfNotNull(file?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) })
        val inputs = JSONArray().apply {
            if (file != null) put(JSONObject().put("name", SpeechToolCatalog.PARAM_FILE).put("source", PluginToolValues.INPUT_SOURCE_FILE).put("displayName", file.name))
        }
        val request = Bundle().apply {
            putInt(PluginToolInvokeKeys.CONTRACT_VERSION, PluginToolContract.CONTRACT_VERSION)
            putString(PluginToolInvokeKeys.INVOCATION_ID, invocationId)
            putString(PluginToolInvokeKeys.TOOL, tool)
            putString(PluginToolInvokeKeys.ARGUMENTS_JSON, arguments.toString())
            putString(PluginToolInvokeKeys.INPUTS_JSON, inputs.toString())
            putString(PluginToolInvokeKeys.OUTPUTS_JSON, JSONArray(outputs).toString())
            putLong(PluginToolInvokeKeys.TIMEOUT_MS, timeoutMs)
            putString(PluginToolInvokeKeys.CONSUMER, PluginToolValues.CONSUMER_SCRIPT)
        }
        val accepted = provider.invoke(request, descriptors.toTypedArray(), callback)
        return Pending(invocationId, accepted, done, answer, descriptors)
    }

    private fun cancel(provider: IPluginToolProvider, invocationId: String) {
        provider.cancel(Bundle().apply {
            putString(PluginToolCancelKeys.INVOCATION_ID, invocationId)
            putString(PluginToolCancelKeys.REASON, PluginToolValues.CANCEL_REASON_CALLER)
        })
    }

    /** Repeats [block] while the tool is still busy with an earlier reading or dictation (at most 30 s). zh-CN: 工具仍忙时重试. */
    private fun untilFree(block: () -> Answer): Answer {
        val until = SystemClock.elapsedRealtime() + 30_000
        while (true) {
            val answer = block()
            if (answer.code != PluginToolErrorCodes.BUSY || SystemClock.elapsedRealtime() > until) return answer
            Thread.sleep(500)
        }
    }

    private fun asrInstalled(): Boolean = SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).isNotEmpty()

    private fun microphoneGranted(): Boolean = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun voices(): List<TtsVoice> = SpeechRuntime.get(context).voiceTable().voices

    /** A voice that speaks English, preferring the smaller models. zh-CN: 说英语的语音, 小模型优先. */
    private fun englishVoice(): TtsVoice? {
        val english = voices().filter { it.languageTag.startsWith("en") }
        return ENGLISH_MODELS.firstNotNullOfOrNull { id -> english.firstOrNull { it.modelId == id } } ?: english.firstOrNull()
    }

    /** The MIME type and the duration in milliseconds of the first audio track. zh-CN: 第一条音轨的 MIME 类型与时长 (毫秒). */
    private fun track(file: File): Pair<String, Long> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val format = (0 until extractor.trackCount).map { extractor.getTrackFormat(it) }.first { it.getString(MediaFormat.KEY_MIME)!!.startsWith("audio/") }
            return format.getString(MediaFormat.KEY_MIME)!! to format.getLong(MediaFormat.KEY_DURATION) / 1000
        } finally {
            extractor.release()
        }
    }

    private fun withProvider(block: (IPluginToolProvider) -> Unit) {
        val intent = Intent(PluginToolContract.SERVICE_ACTION).setPackage(context.packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(intent, 0)
        assertEquals("the tool provider discovery must resolve exactly one service", 1, matches.size)
        val service = matches.single().serviceInfo
        assertEquals(SpeechToolService::class.java.name, service.name)
        assertEquals("org.autojs.permission.PLUGIN", service.permission)
        // D88: the runtime's process, so the models load once.
        assertEquals(context.packageName, service.processName)
        val latch = CountDownLatch(1)
        val binder = AtomicReference<IBinder?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                binder.set(service)
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        assertTrue(context.bindService(Intent(intent).setComponent(ComponentName(service.packageName, service.name)), connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out binding the tool provider", latch.await(15, TimeUnit.SECONDS))
            assertNotNull(binder.get())
            block(IPluginToolProvider.Stub.asInterface(binder.get()))
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val TOOL_LISTEN = SpeechToolCatalog.TOOL_LISTEN
        const val TOOL_SPEAK = SpeechToolCatalog.TOOL_SPEAK
        const val TOOL_TRANSCRIBE = SpeechToolCatalog.TOOL_TRANSCRIBE
        const val TOOL_SYNTHESIZE = SpeechToolCatalog.TOOL_SYNTHESIZE
        const val MAX_CER = 0.15

        /** Synthesized speech recognized again; the voices are clear, the recognizers trained on read speech. zh-CN: 合成再识别的字错率上限. */
        const val MAX_ROUND_TRIP_CER = 0.25
        const val SYNTHESIS_TEXT = "The quick brown fox jumps over the lazy dog near the river bank"
        const val SPEAK_TEXT = "This is a short test of the speech tools."
        val LONG_TEXT = "After early nightfall the yellow lamps would light up here and there in the quiet streets of the old town. ".repeat(60)
        val ENGLISH_MODELS = listOf("tts.vits.piper.en-us-lessac-medium", "tts.vits.melo.zh-en", "tts.kokoro.multi-lang.v1_1")
    }
}
