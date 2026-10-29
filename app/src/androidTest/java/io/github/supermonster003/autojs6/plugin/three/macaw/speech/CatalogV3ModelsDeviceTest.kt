package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationListener
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.OperationResult
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

/**
 * The language models of catalog v3 on a device (roadmap D131, P8.2.4), installed by [test0InstallsTheCatalogV3Models]
 * from the unsigned development catalog that `.python/device_catalog_v3.py` pushes (Shared-Assets
 * `pending/v3/catalog-v3.json` in `filesDir/dev-catalog/`; the `speech-models-v3` mirror is not published yet, so every
 * model comes from its sherpa-onnx asset). The test assets have no Spanish, Arabic or Korean recording: the new voices
 * synthesize sentences that the new recognizers read back (character error rate), PengChengStarling also reads the
 * English and Chinese recordings, the Korean voice is read back by SenseVoice when it is installed, and Kokoro v1.0
 * speaks one sentence per language while its Japanese speakers are refused. The first use of PengChengStarling,
 * NeMo and Kokoro records the peak resident memory of the process (cached engines released first). Models that are not
 * installed are skipped; [test9RemovesTheCatalogV3Models] deletes them and the ones listed in `macawV3Models`
 * (`-e macawV3Remove true`). Results are `MacawDeviceRun` lines.
 * zh-CN: 设备上的目录 v3 语言模型 (路线图 D131, P8.2.4), 由 [test0InstallsTheCatalogV3Models] 从 `.python/device_catalog_v3.py` 推送的未签名
 * 开发目录安装 (Shared-Assets `pending/v3/catalog-v3.json` 放在 `filesDir/dev-catalog/`; `speech-models-v3` 镜像尚未发布, 因此每个模型都来自其
 * sherpa-onnx 资源). 测试资产里没有西班牙语, 阿拉伯语与韩语录音: 新声音合成句子, 由新识别模型读回 (字错率); PengChengStarling 另外识别英文与
 * 中文录音; 已安装 SenseVoice 时由它读回韩语声音; Kokoro v1.0 每种语言朗读一句, 日语说话人被拒绝. PengChengStarling, NeMo 与 Kokoro 首次使用时
 * 记录进程峰值常驻内存 (先释放缓存的引擎). 未安装的模型跳过; [test9RemovesTheCatalogV3Models] 删除它们以及 `macawV3Models` 列出的模型
 * (`-e macawV3Remove true`). 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CatalogV3ModelsDeviceTest {

    private class Progress : ISpeechProgressCallback.Stub() {
        val result = AtomicReference<Bundle?>()
        val error = AtomicReference<Bundle?>()
        val finished = CountDownLatch(1)

        override fun onProgress(opId: Int, done: Long, total: Long) = Unit

        override fun onCompleted(opId: Int, result: Bundle?) {
            this.result.set(result)
            finished.countDown()
        }

        override fun onFailed(opId: Int, error: Bundle?) {
            this.error.set(error)
            finished.countDown()
        }
    }

    private class Synthesis(val file: File, val result: Bundle, val millis: Long)

    @Test
    fun test0InstallsTheCatalogV3Models() {
        val wanted = SpeechDevice.argument("macawV3Models")
        assumeTrue("opt-in: -e macawV3Models true (all six) or a comma separated list of model ids", !wanted.isNullOrEmpty())
        val ids = if (wanted == "true") MODELS else wanted!!.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val manager = SpeechModels.get(SpeechDevice.context)
        assertEquals("the development catalog is in use", 3, manager.catalogVersion())
        ids.forEach { id ->
            if (SpeechDevice.installed(id)) {
                SpeechDevice.log("v3 setup $id already installed")
                return@forEach
            }
            val done = CountDownLatch(1)
            val completed = AtomicReference<OperationResult>()
            val failure = AtomicReference<ModelException>()
            val started = SystemClock.elapsedRealtime()
            // The run accepts the licenses of the non-commercial and unknown-data models for testing (roadmap D11).
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
            SpeechDevice.log("v3 setup $id installed origin=${model.origin} downloaded=${model.downloadedBytes} installed=${model.entry.installedBytes} seconds=${"%.1f".format(seconds)}")
        }
    }

    @Test
    fun test1SpanishVoicesAreReadBackByNemo() {
        assumeTrue("install $NEMO_ES first", SpeechDevice.installed(NEMO_ES))
        val voices = listOfNotNull(KOKORO.takeIf(SpeechDevice::installed)?.let { it to "28" }, DAVEFX.takeIf(SpeechDevice::installed)?.let { it to null })
        assumeTrue("install $KOKORO or $DAVEFX first", voices.isNotEmpty())
        voices.forEachIndexed { index, (voice, speaker) ->
            val synthesis = synthesize(voice, ES_TEXT, speaker)
            readBack(NEMO_ES, synthesis.file, ES_TEXT, "es voice=$voice speaker=${speaker ?: 0}", measure = index == 0)
        }
    }

    @Test
    fun test2ArabicVoicesAreReadBackByPengCheng() {
        assumeTrue("install $PENGCHENG first", SpeechDevice.installed(PENGCHENG))
        val voices = listOf(DII, MIRO).filter(SpeechDevice::installed)
        assumeTrue("install $DII or $MIRO first", voices.isNotEmpty())
        voices.forEachIndexed { index, voice ->
            val synthesis = synthesize(voice, AR_TEXT, null)
            readBack(PENGCHENG, synthesis.file, AR_TEXT, "ar voice=$voice", MAX_AR_CER, measure = index == 0)
        }
    }

    @Test
    fun test3PengChengReadsTheEnglishAndChineseRecordings() {
        assumeTrue("install $PENGCHENG first", SpeechDevice.installed(PENGCHENG))
        val en = recognize(PENGCHENG, SpeechDevice.asset("test-audio/en.wav"))
        val cer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, en)
        val zh = recognize(PENGCHENG, SpeechDevice.asset("test-audio/zh.wav"))
        SpeechDevice.log("v3 recordings recognizer=$PENGCHENG en.wav cer=${"%.3f".format(cer)} zh.wav chars=${zh.length}")
        assertTrue("en.wav: character error rate $cer", cer < MAX_CER)
        assertTrue("zh.wav: ${zh.length} characters", zh.length >= 10)
    }

    @Test
    fun test4KoreanVoiceSynthesizes() {
        assumeTrue("install $KSS first", SpeechDevice.installed(KSS))
        val synthesis = synthesize(KSS, KO_TEXT, null)
        if (SpeechDevice.installed(SpeechDevice.SENSE_VOICE)) {
            val heard = recognize(SpeechDevice.SENSE_VOICE, synthesis.file) { putString(SpeechOptionKeys.LANGUAGE, "ko") }
            val cer = SpeechDevice.cer(KO_TEXT, heard)
            SpeechDevice.log("v3 roundtrip ko voice=$KSS recognizer=${SpeechDevice.SENSE_VOICE} cer=${"%.3f".format(cer)}")
            assertTrue("$KSS -> SenseVoice: character error rate $cer", cer < MAX_AR_CER)
        }
    }

    @Test
    fun test5KokoroSpeaksEachLanguageAndRefusesJapanese() {
        assumeTrue("install $KOKORO first", SpeechDevice.installed(KOKORO))
        KOKORO_SENTENCES.entries.forEachIndexed { index, (speaker, text) ->
            val synthesis = if (index == 0) peak("synthesis $KOKORO") { synthesize(KOKORO, text, speaker.toString()) } else synthesize(KOKORO, text, speaker.toString())
            assertEquals(24000, synthesis.result.getInt(SpeechResultKeys.SAMPLE_RATE))
            assertEquals(speaker, synthesis.result.getInt(SpeechResultKeys.SPEAKER_ID))
            val reader = KOKORO_READERS[speaker]?.takeIf(SpeechDevice::installed) ?: return@forEachIndexed
            readBack(reader, synthesis.file, text, "kokoro speaker=$speaker", MAX_AR_CER)
        }
        // A Japanese speaker fails the call itself (TtsSpeakerHints.unavailable).
        val refusal = try {
            synthesize(KOKORO, KO_TEXT, "37")
            null
        } catch (e: RuntimeException) {
            e.message.orEmpty()
        }
        SpeechDevice.log("v3 kokoro speaker=37 refused=${refusal?.substringBefore(':')}")
        assertTrue("speaker 37: $refusal", refusal.orEmpty().startsWith("${SpeechErrorCodes.INVALID_OPTIONS}:"))
    }

    @Test
    fun test9RemovesTheCatalogV3Models() {
        assumeTrue("opt-in: -e macawV3Remove true", SpeechDevice.argument("macawV3Remove") == "true")
        val manager = SpeechModels.get(SpeechDevice.context)
        SpeechRuntime.get(SpeechDevice.context).engines.trim()
        // A list in macawV3Models goes too (the analysis models of P8.4). zh-CN: macawV3Models 给出的列表也一并删除 (P8.4 的分析模型).
        val listed = SpeechDevice.argument("macawV3Models")?.takeIf { it != "true" }?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        (MODELS + listed).distinct().filter(SpeechDevice::installed).forEach { id ->
            assertTrue("$id was not removed", manager.remove(id))
            assertTrue("$id is still installed", !SpeechDevice.installed(id))
            SpeechDevice.log("v3 removed $id")
        }
    }

    private fun readBack(recognizer: String, file: File, text: String, label: String, limit: Double = MAX_CER, measure: Boolean = false) {
        val heard = if (measure) peak("recognition $recognizer") { recognize(recognizer, file) } else recognize(recognizer, file)
        val cer = SpeechDevice.cer(text, heard)
        SpeechDevice.log("v3 roundtrip $label recognizer=$recognizer cer=${"%.3f".format(cer)}")
        assertTrue("$label -> $recognizer: character error rate $cer", cer < limit)
    }

    private fun recognize(recognizer: String, file: File, options: Bundle.() -> Unit = {}): String {
        val answer = SpeechDevice.recognize(file, SpeechDevice.request { putString(SpeechOptionKeys.MODEL, recognizer); options() })
        assertNull("$recognizer: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.getString(SpeechContract.KEY_ERROR_CODE))
        return answer.getString(SpeechResultKeys.TEXT).orEmpty()
    }

    /** Runs [block] after releasing the cached engines and logs the peak resident memory. zh-CN: 释放缓存引擎后运行并记录峰值内存. */
    private fun <T> peak(label: String, block: () -> T): T {
        SpeechRuntime.get(SpeechDevice.context).engines.trim()
        val (_, pssBefore) = SpeechDevice.memory()
        val memory = PeakMemory()
        memory.start()
        val value = try {
            block()
        } finally {
            val peak = memory.stop()
            val (_, pssAfter) = SpeechDevice.memory()
            SpeechDevice.log("v3 memory $label peakRss=${peak.mib} MiB (${peak.method}) pssBefore=$pssBefore MiB pssAfter=$pssAfter MiB")
        }
        return value
    }

    private fun synthesize(voice: String, text: String, speaker: String?): Synthesis {
        val file = File(SpeechDevice.context.cacheDir, "device-test/v3-$voice-${speaker ?: 0}.wav").apply { parentFile?.mkdirs(); delete(); createNewFile() }
        val progress = Progress()
        val options = SpeechDevice.request {
            putString(SpeechOptionKeys.TEXT, text)
            putString(SpeechOptionKeys.MODEL, voice)
            putString(SpeechOptionKeys.FORMAT, SpeechContract.FORMAT_WAV)
            speaker?.let { putString(SpeechOptionKeys.SPEAKER, it) }
        }
        val started = SystemClock.elapsedRealtime()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE).use { output ->
            SpeechDevice.plugin.synthesizeToFile(options, output, progress)
            assertTrue("$voice did not finish within 600 s", progress.finished.await(600, TimeUnit.SECONDS))
        }
        val millis = SystemClock.elapsedRealtime() - started
        assertNull("$voice failed: ${progress.error.get()?.getString(SpeechContract.KEY_ERROR_CODE)}", progress.error.get())
        val result = progress.result.get()!!
        val samples = samples(file)
        val seconds = samples.size / AudioInput.SAMPLE_RATE.toDouble()
        val rms = sqrt(samples.sumOf { it.toDouble() * it } / samples.size.coerceAtLeast(1))
        SpeechDevice.log(
            "v3 synthesis voice=$voice speaker=${result.getInt(SpeechResultKeys.SPEAKER_ID)} sampleRate=${result.getInt(SpeechResultKeys.SAMPLE_RATE)} " +
                "seconds=${"%.2f".format(result.getLong(SpeechResultKeys.DURATION_MILLIS) / 1000.0)} ms=$millis rms=${"%.3f".format(rms)}",
        )
        assertTrue("$voice: ${"%.2f".format(seconds)} s of audio", seconds in 0.8..20.0)
        assertTrue("$voice: level $rms", rms > 0.01)
        return Synthesis(file, result, millis)
    }

    /** 16 kHz mono samples of a WAV file. zh-CN: WAV 文件的 16 kHz 单声道采样. */
    private fun samples(file: File): FloatArray {
        val chunks = ArrayList<FloatArray>()
        WavReader(file.inputStream(), file.length()).use { reader -> AudioInput.decode(reader) { chunks += it } }
        return chunks.fold(FloatArray(0)) { all, chunk -> all + chunk }
    }

    companion object {
        const val NEMO_ES = "asr.nemo-transducer.es.2026-05-03.int8"
        const val PENGCHENG = "asr.zipformer-streaming.ar-en-id-ja-ru-th-vi-zh.2025-02-10.int8"
        const val KOKORO = "tts.kokoro.multi-lang.v1_0.int8"
        const val DII = "tts.vits.piper.ar-jo-sa-dii-high.int8"
        const val MIRO = "tts.vits.piper.ar-jo-sa-miro-v2-high.int8"
        const val KSS = "tts.vits.mimic3.ko-ko-kss-low"
        const val DAVEFX = "tts.vits.piper.es-es-davefx-medium.int8"
        val MODELS = listOf(NEMO_ES, PENGCHENG, KOKORO, DII, MIRO, KSS)

        private const val MAX_CER = 0.35

        /** Arabic and Korean read-backs include the recognizer's own errors (0.18 - 0.36 on the G8441 in P8.2.1). zh-CN: 阿语与韩语读回的上限. */
        private const val MAX_AR_CER = 0.5

        private const val ES_TEXT = "Hoy hace buen tiempo y vamos al parque."
        private const val AR_TEXT = "\u0635\u0628\u0627\u062d \u0627\u0644\u062e\u064a\u0631\u060c \u0643\u064a\u0641 \u062d\u0627\u0644\u0643 \u0627\u0644\u064a\u0648\u0645\u061f \u0627\u0644\u0637\u0642\u0633 \u062c\u0645\u064a\u0644 \u062c\u062f\u0627."
        private const val KO_TEXT = "\uc548\ub155\ud558\uc138\uc694, \uc624\ub298 \ub0a0\uc528\uac00 \uc815\ub9d0 \uc88b\ub124\uc694."

        /** One sentence per Kokoro v1.0 language, by speaker (TtsSpeakerHints). zh-CN: Kokoro v1.0 每种语言一句, 按说话人. */
        private val KOKORO_SENTENCES = linkedMapOf(
            3 to "The weather is nice today, so we are going to the park.",
            20 to "The weather is lovely today, so we are off to the park.",
            28 to ES_TEXT,
            30 to "Aujourd'hui il fait beau et nous allons au parc.",
            31 to "\u0906\u091c \u092e\u094c\u0938\u092e \u0905\u091a\u094d\u091b\u093e \u0939\u0948 \u0914\u0930 \u0939\u092e \u092a\u093e\u0930\u094d\u0915 \u091c\u093e \u0930\u0939\u0947 \u0939\u0948\u0902\u0964",
            35 to "Oggi fa bel tempo e andiamo al parco.",
            42 to "Hoje o tempo est\u00e1 bom e vamos ao parque.",
            45 to "\u4eca\u5929\u5929\u6c14\u5f88\u597d, \u6211\u4eec\u53bb\u516c\u56ed\u6563\u6b65.",
        )

        /** Recognizers that read a Kokoro sentence back. zh-CN: 读回 Kokoro 句子的识别模型. */
        private val KOKORO_READERS = mapOf(3 to PENGCHENG, 28 to NEMO_ES, 45 to PENGCHENG)
    }
}
