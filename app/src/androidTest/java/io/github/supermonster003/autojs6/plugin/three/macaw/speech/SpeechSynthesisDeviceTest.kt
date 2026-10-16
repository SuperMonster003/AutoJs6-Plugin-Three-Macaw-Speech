package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsSpeakerHints
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.WavWriter
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.DataInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Synthesis on devices (roadmap P4.1 - P4.3): the five TTS models of appendix A (installed from the `speech-models-v1`
 * mirror by [test0InstallsTheSynthesisModels], opt-in with `-e macawTtsModels true` because it downloads about 740 MB, or
 * with a comma separated list of model ids)
 * each synthesize a sentence through `synthesizeToFile` (sample rate, duration, real-time factor, memory; the bilingual
 * models are checked for dropped words by recognizing their output with SenseVoice when it is installed), the speaker
 * rules and refusals, an 8000 character text to the end and cancelled, and the system TTS engine through Android's
 * `TextToSpeech` (engine, voices, languages, voice selection, the speech rate, `stop`). Results go to logcat
 * `MacawDeviceRun` for `.python/device_speech_run.py`; models that are not installed are skipped.
 * zh-CN: 设备上的合成 (路线图 P4.1 - P4.3): 附录 A 的五个 TTS 模型 (由 [test0InstallsTheSynthesisModels] 从 `speech-models-v1` 镜像安装, 因下载约
 * 740 MB 需以 `-e macawTtsModels true` 开启) 各经 `synthesizeToFile` 合成一句 (采样率, 时长, 实时率, 内存; 双语模型在已安装 SenseVoice 时以其识别
 * 输出检查是否丢字), 说话人规则与拒绝, 8000 字符文本合成到底与取消, 以及经 Android `TextToSpeech` 使用的系统 TTS 引擎 (引擎, 语音, 语言, 语音选择,
 * 语速, `stop`). 结果写入 logcat `MacawDeviceRun` 供 `.python/device_speech_run.py` 收集; 未安装的模型跳过.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SpeechSynthesisDeviceTest {

    private val context get() = SpeechDevice.context

    /** Collects one operation of `ISpeechProgressCallback`. zh-CN: 收集 `ISpeechProgressCallback` 的一个操作. */
    private class Progress : ISpeechProgressCallback.Stub() {
        val updates: MutableList<Pair<Long, Long>> = Collections.synchronizedList(ArrayList())
        val result = AtomicReference<Bundle?>()
        val error = AtomicReference<Bundle?>()
        val finished = CountDownLatch(1)
        val firstProgress = CountDownLatch(1)

        override fun onProgress(opId: Int, done: Long, total: Long) {
            updates += done to total
            if (done > 0) firstProgress.countDown()
        }

        override fun onCompleted(opId: Int, result: Bundle?) {
            this.result.set(result)
            finished.countDown()
        }

        override fun onFailed(opId: Int, error: Bundle?) {
            this.error.set(error)
            finished.countDown()
        }
    }

    private class Run(val progress: Progress, val millis: Long, val file: File)

    private fun synthesize(text: String, model: String, speaker: String? = null, speed: Double? = null, timeoutSeconds: Long = 900, name: String = "out"): Run {
        val file = File(context.cacheDir, "device-test/tts-$name.wav").apply { parentFile?.mkdirs(); delete(); createNewFile() }
        val progress = Progress()
        val started = SystemClock.elapsedRealtime()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE).use { output ->
            SpeechDevice.plugin.synthesizeToFile(request(text, model, speaker, speed), output, progress)
            assertTrue("$model did not finish within $timeoutSeconds s", progress.finished.await(timeoutSeconds, TimeUnit.SECONDS))
        }
        return Run(progress, SystemClock.elapsedRealtime() - started, file)
    }

    private fun request(text: String, model: String, speaker: String?, speed: Double?) = SpeechDevice.request {
        putString(SpeechOptionKeys.TEXT, text)
        putString(SpeechOptionKeys.MODEL, model)
        speaker?.let { putString(SpeechOptionKeys.SPEAKER, it) }
        speed?.let { putDouble(SpeechOptionKeys.SPEED, it) }
        putString(SpeechOptionKeys.FORMAT, SpeechContract.FORMAT_WAV)
    }

    private fun completed(run: Run, model: String): Bundle {
        assertNull("$model failed: ${run.progress.error.get()?.getString(SpeechContract.KEY_ERROR_CODE)} ${run.progress.error.get()?.getString(SpeechContract.KEY_ERROR_MESSAGE)}", run.progress.error.get())
        return requireNotNull(run.progress.result.get())
    }

    private fun installedTts(): List<String> = MODELS.filter(SpeechDevice::installed)

    @Test
    fun test0InstallsTheSynthesisModels() {
        val wanted = SpeechDevice.argument("macawTtsModels")
        assumeTrue("opt-in: -e macawTtsModels true (all five) or a comma separated list of model ids", !wanted.isNullOrEmpty())
        val ids = if (wanted == "true") MODELS else wanted!!.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val manager = SpeechModels.get(context)
        ids.forEach { id ->
            if (SpeechDevice.installed(id)) {
                SpeechDevice.log("setup $id already installed")
                return@forEach
            }
            val done = CountDownLatch(1)
            val completed = AtomicReference<OperationResult>()
            val failure = AtomicReference<ModelException>()
            val started = SystemClock.elapsedRealtime()
            // The run accepts the licenses of the non-commercial and undeclared models for testing (roadmap D11).
            manager.install(id, acceptLicense = true, listener = object : OperationListener {
                override fun onProgress(opId: Int, done: Long, total: Long) = Unit

                override fun onCompleted(opId: Int, result: OperationResult) {
                    completed.set(result)
                    done.countDown()
                }

                override fun onFailed(opId: Int, error: ModelException) {
                    failure.set(error)
                    done.countDown()
                }
            })
            assertTrue("$id did not finish within 30 minutes", done.await(30, TimeUnit.MINUTES))
            assertNull("$id failed: ${failure.get()?.code} ${failure.get()?.message}", failure.get())
            val model = completed.get() as OperationResult.Model
            val seconds = (SystemClock.elapsedRealtime() - started) / 1000.0
            SpeechDevice.log("setup $id installed origin=${model.origin} downloaded=${model.downloadedBytes} installed=${model.entry.installedBytes} seconds=${"%.1f".format(seconds)} mirrorFailure=${model.mirrorFailure}")
        }
    }

    @Test
    fun test1EachModelSynthesizesASentence() {
        val installed = installedTts()
        assumeTrue("no TTS model installed", installed.isNotEmpty())
        val runtime = SpeechRuntime.get(context)
        installed.forEach { id ->
            runtime.engines.evict(id)
            val entry = requireNotNull(SpeechModels.get(context).entry(id))
            val text = sentenceFor(entry.languages)
            val cold = synthesize(text, id, name = "$id-cold")
            completed(cold, id)
            val warm = synthesize(text, id, name = id)
            val result = completed(warm, id)
            val (native, pss) = SpeechDevice.memory()
            val format = WavReader(warm.file.inputStream(), warm.file.length()).use { it.format }
            assertEquals(entry.sampleRate, format.sampleRate)
            assertEquals(format.sampleRate, result.getInt(SpeechResultKeys.SAMPLE_RATE))
            assertEquals(1, format.channels)
            assertEquals(16, format.bitsPerSample)
            assertEquals(format.durationMillis, result.getLong(SpeechResultKeys.DURATION_MILLIS))
            assertEquals(warm.file.length(), result.getLong(SpeechResultKeys.SIZE_BYTES))
            assertEquals(id, result.getString(SpeechResultKeys.MODEL_ID))
            assertEquals("the model's default speaker", TtsSpeakerHints.defaultSpeaker(id, entry.speakers ?: 1), result.getInt(SpeechResultKeys.SPEAKER_ID))
            val perChar = format.durationMillis.toDouble() / text.length
            assertTrue("$id: ${format.durationMillis} ms for ${text.length} characters", perChar in 25.0..700.0)
            val updates = warm.progress.updates.toList()
            assertEquals(text.length.toLong(), updates.last().first)
            assertTrue(updates.zipWithNext().all { (a, b) -> b.first >= a.first })
            val rtf = warm.millis.toDouble() / format.durationMillis
            val words = recognizedCheck(id, entry.languages, text, warm.file)
            SpeechDevice.log(
                "tts $id rate=${format.sampleRate} chars=${text.length} audioMs=${format.durationMillis} coldMs=${cold.millis} warmMs=${warm.millis} " +
                    "rtf=${"%.3f".format(rtf)} loadMs=${(cold.millis - warm.millis).coerceAtLeast(0)} nativeMiB=$native pssMiB=$pss $words",
            )
            runtime.engines.evict(id)
        }
    }

    /**
     * Bilingual models must not drop the English or the Chinese words: SenseVoice recognizes the output, and its
     * character error rate against the input stays at most 0.35. Answers a summary for the log.
     * zh-CN: 双语模型不得丢失英文或中文词: 由 SenseVoice 识别输出, 其相对输入的字错率不超过 0.35. 返回日志摘要.
     */
    private fun recognizedCheck(id: String, languages: List<String>, text: String, file: File): String {
        if (!(languages.contains("zh") && languages.contains("en"))) return "asr=-"
        if (!SpeechDevice.installed(SpeechDevice.SENSE_VOICE)) return "asr=skipped"
        val answer = SpeechDevice.recognize(file, SpeechDevice.request { putString(SpeechOptionKeys.MODEL, SpeechDevice.SENSE_VOICE) })
        assertNull(answer.getString(SpeechContract.KEY_ERROR_CODE))
        val heard = answer.getString(SpeechResultKeys.TEXT).orEmpty()
        val cer = SpeechDevice.cer(text, heard)
        val english = ENGLISH_WORDS.count { heard.lowercase(Locale.ROOT).contains(it) }
        assertTrue("$id dropped words: CER ${"%.2f".format(cer)}", cer <= 0.35)
        return "asrCer=${"%.3f".format(cer)} englishWords=$english/${ENGLISH_WORDS.size}"
    }

    @Test
    fun test2SpeakersWarningsAndRefusals() {
        val installed = installedTts()
        if (MELO in installed) {
            val result = completed(synthesize("\u4f60\u597d.", MELO, speaker = "3", name = "melo-speaker"), MELO)
            @Suppress("DEPRECATION")
            val warnings = result.getParcelableArrayList<Bundle>(SpeechResultKeys.WARNINGS).orEmpty()
            assertEquals(listOf(SpeechErrorCodes.INVALID_OPTIONS), warnings.map { it.getString(SpeechContract.KEY_ERROR_CODE) })
            assertEquals(0, result.getInt(SpeechResultKeys.SPEAKER_ID))
            SpeechDevice.log("tts speaker on a single speaker model: warning ${warnings.first().getString(SpeechContract.KEY_ERROR_MESSAGE)}")
        }
        if (AISHELL in installed) {
            val named = completed(synthesize("\u4f60\u597d.", AISHELL, speaker = "SSB0009", name = "aishell-named"), AISHELL)
            assertEquals(1, named.getInt(SpeechResultKeys.SPEAKER_ID))
            val numbered = completed(synthesize("\u4f60\u597d.", AISHELL, speaker = "173", name = "aishell-173"), AISHELL)
            assertEquals(173, numbered.getInt(SpeechResultKeys.SPEAKER_ID))
            refused(SpeechErrorCodes.INVALID_OPTIONS) { synthesize("\u4f60\u597d.", AISHELL, speaker = "174") }
            refused(SpeechErrorCodes.INVALID_OPTIONS) { synthesize("\u4f60\u597d.", AISHELL, speed = 2.5) }
            refused(SpeechErrorCodes.LIMIT_EXCEEDED) { synthesize("x".repeat(8001), AISHELL) }
        }
        refused(SpeechErrorCodes.MODEL_MISSING) { synthesize("hello", "tts.no.such.model") }
        if (SpeechDevice.installed(SpeechDevice.SENSE_VOICE)) refused(SpeechErrorCodes.INVALID_OPTIONS) { synthesize("hello", SpeechDevice.SENSE_VOICE) }
        SpeechDevice.log("tts refusals ok (installed: ${installed.size})")
    }

    private fun refused(code: String, block: () -> Unit) {
        try {
            block()
            fail("expected $code")
        } catch (e: RuntimeException) {
            assertTrue("expected $code, got ${e.message}", e.message.orEmpty().startsWith("$code:"))
        }
    }

    @Test
    fun test3LongTextToTheEndAndCancelled() {
        assumeTrue("needs $AISHELL", SpeechDevice.installed(AISHELL))
        val text = longText()
        assertEquals(8000, text.length)
        if (SpeechDevice.argument("macawLongFull") != "false") {
            val run = synthesize(text, AISHELL, name = "long", timeoutSeconds = 1800)
            val result = completed(run, AISHELL)
            // Longer than the 30 minute input limit of WavReader: read the canonical header directly.
            val header = ByteBuffer.wrap(ByteArray(WavWriter.HEADER_BYTES).also { bytes -> DataInputStream(run.file.inputStream()).use { it.readFully(bytes) } }).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(8000, header.getInt(24))
            assertEquals(run.file.length() - WavWriter.HEADER_BYTES, header.getInt(40).toLong())
            val audioMillis = header.getInt(40).toLong() / 2 * 1000 / 8000
            assertEquals(audioMillis, result.getLong(SpeechResultKeys.DURATION_MILLIS))
            val updates = run.progress.updates.toList()
            assertEquals(8000L, updates.last().first)
            assertTrue("progress per sentence", updates.size > 100)
            val (native, pss) = SpeechDevice.memory()
            SpeechDevice.log(
                "tts long $AISHELL chars=8000 audioMs=$audioMillis synthMs=${run.millis} rtf=${"%.3f".format(run.millis.toDouble() / audioMillis)} " +
                    "sizeBytes=${result.getLong(SpeechResultKeys.SIZE_BYTES)} progressUpdates=${updates.size} nativeMiB=$native pssMiB=$pss",
            )
        }
        val file = File(context.cacheDir, "device-test/tts-cancel.wav").apply { parentFile?.mkdirs(); delete(); createNewFile() }
        val progress = Progress()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE).use { output ->
            val id = SpeechDevice.plugin.synthesizeToFile(request(text, AISHELL, null, null), output, progress)
            assertTrue(progress.firstProgress.await(120, TimeUnit.SECONDS))
            val cancelledAt = SystemClock.elapsedRealtime()
            SpeechDevice.plugin.cancelOperation(id)
            assertTrue("the cancel was not answered within 15 s", progress.finished.await(15, TimeUnit.SECONDS))
            val latency = SystemClock.elapsedRealtime() - cancelledAt
            assertNull(progress.result.get())
            assertEquals(SpeechErrorCodes.SESSION_CANCELLED, progress.error.get()?.getString(SpeechContract.KEY_ERROR_CODE))
            SpeechDevice.log("tts cancel after ${progress.updates.last().first} of 8000 chars answered in $latency ms, partial file ${file.length()} bytes")
        }
    }

    @Test
    fun test4SystemEngine() {
        val installed = installedTts()
        assumeTrue("no TTS model installed", installed.isNotEmpty())
        val ready = CountDownLatch(1)
        val status = AtomicReference<Int>()
        val tts = TextToSpeech(context, { status.set(it); ready.countDown() }, context.packageName)
        try {
            assertTrue(ready.await(20, TimeUnit.SECONDS))
            assertEquals(TextToSpeech.SUCCESS, status.get())
            assertTrue(tts.engines.any { it.name == context.packageName })
            val voices = tts.voices.orEmpty()
            val table = SpeechRuntime.get(context).voiceTable()
            assertEquals(table.voices.map { it.name }.toSet(), voices.map { it.name }.toSet())
            assertTrue(voices.none { it.isNetworkConnectionRequired })
            val zh = tts.isLanguageAvailable(Locale.SIMPLIFIED_CHINESE)
            val en = tts.isLanguageAvailable(Locale.US)
            val ja = tts.isLanguageAvailable(Locale.JAPAN)
            SpeechDevice.log("tts engine voices=${voices.size} zh=$zh en=$en ja=$ja defaultVoice=${tts.defaultVoice?.name}")
            if (installed.any { it in ZH_MODELS }) assertEquals(TextToSpeech.LANG_COUNTRY_AVAILABLE, zh)
            assertEquals("no Japanese model in the catalog", TextToSpeech.LANG_NOT_SUPPORTED, ja)

            val listener = Listener()
            tts.setOnUtteranceProgressListener(listener)
            val voiceName = if (AISHELL in installed) "$AISHELL/10" else table.voices.first().name
            assertEquals(TextToSpeech.SUCCESS, tts.setVoice(voices.single { it.name == voiceName }))
            val sentence = sentenceFor(listOf(table.voice(voiceName)!!.languageTag.substringBefore('-')))
            val normal = listener.toFile(tts, sentence, "normal", rate = 1f)
            val fast = listener.toFile(tts, sentence, "fast", rate = 2f)
            val ratio = fast.second.toDouble() / normal.second
            assertTrue("rate 2.0 must shorten the audio: ratio $ratio", ratio < 0.75)
            SpeechDevice.log("tts engine $voiceName rate=${normal.first} normalMs=${normal.second} fastMs=${fast.second} ratio=${"%.2f".format(ratio)}")

            if (MELO in installed) {
                assertEquals(TextToSpeech.SUCCESS, tts.setVoice(voices.single { it.name == "$MELO/0/en-US" }))
                val english = listener.toFile(tts, "Hello, this is an example of offline neural speech synthesis.", "english", rate = 1f)
                assertEquals(44100, english.first)
                SpeechDevice.log("tts engine $MELO/0/en-US englishMs=${english.second}")
            }
            assertTrue(tts.setLanguage(Locale.SIMPLIFIED_CHINESE) >= TextToSpeech.LANG_AVAILABLE || installed.none { it in ZH_MODELS })

            // stop() ends a running synthesis within one sentence.
            tts.setSpeechRate(1f)
            val stopFile = File(context.cacheDir, "device-test/tts-engine-stop.wav").apply { delete() }
            listener.reset()
            tts.synthesizeToFile(longText().take(3000), Bundle(), stopFile, "stop")
            assertTrue(listener.started.await(60, TimeUnit.SECONDS))
            SystemClock.sleep(500)
            val stoppedAt = SystemClock.elapsedRealtime()
            tts.stop()
            assertTrue("stop was not answered", listener.ended.await(15, TimeUnit.SECONDS))
            SpeechDevice.log("tts engine stop answered in ${SystemClock.elapsedRealtime() - stoppedAt} ms (${listener.outcome.get()})")
        } finally {
            tts.shutdown()
        }
    }

    private class Listener : UtteranceProgressListener() {
        var started = CountDownLatch(1)
        var ended = CountDownLatch(1)
        val outcome = AtomicReference<String>()

        fun reset() {
            started = CountDownLatch(1)
            ended = CountDownLatch(1)
            outcome.set(null)
        }

        /** Synthesizes [text] into a file at [rate]; answers (sample rate, duration in ms). zh-CN: 以 [rate] 合成到文件. */
        fun toFile(tts: TextToSpeech, text: String, name: String, rate: Float): Pair<Int, Long> {
            reset()
            tts.setSpeechRate(rate)
            val file = File(SpeechDevice.context.cacheDir, "device-test/tts-engine-$name.wav").apply { parentFile?.mkdirs(); delete() }
            assertEquals(TextToSpeech.SUCCESS, tts.synthesizeToFile(text, Bundle(), file, name))
            assertTrue("$name did not end", ended.await(120, TimeUnit.SECONDS))
            assertEquals("done", outcome.get())
            val format = WavReader(file.inputStream(), file.length()).use { it.format }
            assertNotNull(format)
            return format.sampleRate to format.durationMillis
        }

        override fun onStart(utteranceId: String?) = started.countDown()

        override fun onDone(utteranceId: String?) {
            outcome.set("done")
            ended.countDown()
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            outcome.set("error")
            ended.countDown()
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            outcome.set("error $errorCode")
            ended.countDown()
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            outcome.set("stopped interrupted=$interrupted")
            ended.countDown()
        }
    }

    private companion object {
        const val MELO = "tts.vits.melo.zh-en"
        const val KOKORO = "tts.kokoro.multi-lang.v1_1"
        const val AISHELL = "tts.vits.icefall.zh-aishell3"
        const val PIPER = "tts.vits.piper.en-us-lessac-medium"
        const val MATCHA = "tts.matcha.icefall.zh-baker"
        val MODELS = listOf(AISHELL, MELO, PIPER, MATCHA, KOKORO)
        val ZH_MODELS = setOf(AISHELL, MELO, MATCHA, KOKORO)

        // "This project's deadline is tomorrow, please submit the report on time." with English words in it.
        const val MIXED = "\u8fd9\u4e2a project \u7684 deadline \u662f\u660e\u5929, \u8bf7\u6309\u65f6\u63d0\u4ea4 report."
        val ENGLISH_WORDS = listOf("project", "deadline", "report")

        // "The weather is nice today; let us take a walk in the park."
        const val CHINESE = "\u4eca\u5929\u5929\u6c14\u5f88\u597d, \u6211\u4eec\u4e00\u8d77\u53bb\u516c\u56ed\u6563\u6b65\u5427."
        const val ENGLISH = "The weather is nice today, so let us take a walk in the park."

        fun sentenceFor(languages: List<String>): String = when {
            "zh" in languages && "en" in languages -> MIXED
            "zh" in languages -> CHINESE
            else -> ENGLISH
        }

        /** 8000 characters of numbered Chinese sentences. zh-CN: 8000 个字符的编号中文句子. */
        fun longText(): String {
            val sentences = listOf(
                "\u4eca\u5929\u5929\u6c14\u5f88\u597d, \u6211\u4eec\u53bb\u516c\u56ed\u6563\u6b65.",
                "\u5c0f\u732b\u5728\u7a97\u53f0\u4e0a\u6652\u592a\u9633.",
                "\u8bf7\u5728\u4e0b\u5348\u4e09\u70b9\u4e4b\u524d\u5b8c\u6210\u8fd9\u9879\u5de5\u4f5c.",
                "\u706b\u8f66\u5c06\u5728\u5341\u5206\u949f\u540e\u5230\u8fbe\u7ec8\u70b9\u7ad9.",
            )
            val out = StringBuilder()
            var index = 0
            while (out.length < 8000) {
                out.append("\u7b2c").append(index + 1).append("\u53e5: ").append(sentences[index % sentences.size]).append(' ')
                index++
            }
            return out.substring(0, 8000)
        }
    }
}
