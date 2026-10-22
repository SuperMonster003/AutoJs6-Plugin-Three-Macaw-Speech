package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReader
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

/**
 * The new recognition and synthesis models of catalog v2 on a device (roadmap D102 / D103, P7.7.5): the Russian and
 * French Piper voices each synthesize a sentence that the streaming recognizer of their language reads back (character
 * error rate), the Spanish and German voices synthesize a sentence that is checked for length and level, and the Korean
 * recognizer (no Korean recording in the test assets) loads and answers `en.wav` without an error. Every model that is
 * not installed is skipped; install them with `SpeechSynthesisDeviceTest#test0InstallsTheSynthesisModels`
 * (`-e macawTtsModels <ids>`, which accepts the licenses of the non-commercial ones). Results are `MacawDeviceRun` lines.
 * zh-CN: 设备上的目录 v2 新增识别与合成模型 (路线图 D102 / D103, P7.7.5): 俄语与法语 Piper 声音各合成一句, 由同语言的流式识别读回 (字错率);
 * 西班牙语与德语声音合成一句, 检查时长与音量; 韩语识别 (测试资产里没有韩语录音) 加载后识别 `en.wav` 不出错. 未安装的模型跳过; 安装用
 * `SpeechSynthesisDeviceTest#test0InstallsTheSynthesisModels` (`-e macawTtsModels <ids>`, 会接受非商用模型的许可). 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
class CatalogV2ModelsDeviceTest {

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

    @Test
    fun russianAndFrenchVoicesAreReadBackByTheirRecognizers() {
        val pairs = ROUND_TRIPS.filter { (voice, recognizer, _) -> SpeechDevice.installed(voice) && SpeechDevice.installed(recognizer) }
        assumeTrue("install the Russian or French voice and recognizer first", pairs.isNotEmpty())
        pairs.forEach { (voice, recognizer, text) ->
            val wav = synthesize(voice, text)
            val answer = SpeechDevice.recognize(wav, SpeechDevice.request { putString(SpeechOptionKeys.MODEL, recognizer) })
            assertNull("$recognizer: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.getString(SpeechContract.KEY_ERROR_CODE))
            val heard = answer.getString(SpeechResultKeys.TEXT).orEmpty()
            val cer = SpeechDevice.cer(text, heard)
            SpeechDevice.log("v2 roundtrip voice=$voice recognizer=$recognizer cer=${"%.3f".format(cer)} text=$heard")
            assertTrue("$voice -> $recognizer: character error rate $cer", cer < MAX_CER)
        }
    }

    @Test
    fun spanishAndGermanVoicesSynthesize() {
        val voices = SYNTHESIS_ONLY.filterKeys(SpeechDevice::installed)
        assumeTrue("install the Spanish or German voice first", voices.isNotEmpty())
        voices.forEach { (voice, text) ->
            val samples = samples(synthesize(voice, text))
            val seconds = samples.size / 16000.0
            val rms = sqrt(samples.sumOf { it.toDouble() * it } / samples.size.coerceAtLeast(1))
            SpeechDevice.log("v2 synthesis voice=$voice seconds=${"%.2f".format(seconds)} rms=${"%.3f".format(rms)}")
            assertTrue("$voice: ${"%.2f".format(seconds)} s of audio", seconds in 1.0..15.0)
            assertTrue("$voice: level $rms", rms > 0.01)
        }
    }

    @Test
    fun koreanRecognizerLoads() {
        assumeTrue("install $KOREAN first", SpeechDevice.installed(KOREAN))
        val answer = SpeechDevice.recognize(SpeechDevice.asset("test-audio/en.wav"), SpeechDevice.request { putString(SpeechOptionKeys.MODEL, KOREAN) })
        assertNull("$KOREAN: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.getString(SpeechContract.KEY_ERROR_CODE))
        SpeechDevice.log("v2 load recognizer=$KOREAN chars=${answer.getString(SpeechResultKeys.TEXT).orEmpty().length}")
    }

    private fun synthesize(voice: String, text: String): File {
        val file = File(SpeechDevice.context.cacheDir, "device-test/v2-$voice.wav").apply { parentFile?.mkdirs(); delete(); createNewFile() }
        val progress = Progress()
        val options = SpeechDevice.request {
            putString(SpeechOptionKeys.TEXT, text)
            putString(SpeechOptionKeys.MODEL, voice)
            putString(SpeechOptionKeys.FORMAT, SpeechContract.FORMAT_WAV)
        }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE).use { output ->
            SpeechDevice.plugin.synthesizeToFile(options, output, progress)
            assertTrue("$voice did not finish within 300 s", progress.finished.await(300, TimeUnit.SECONDS))
        }
        assertNull("$voice failed: ${progress.error.get()?.getString(SpeechContract.KEY_ERROR_CODE)}", progress.error.get())
        return file
    }

    /** 16 kHz mono samples of a WAV file. zh-CN: WAV 文件的 16 kHz 单声道采样. */
    private fun samples(file: File): FloatArray {
        val chunks = ArrayList<FloatArray>()
        WavReader(file.inputStream(), file.length()).use { reader -> AudioInput.decode(reader) { chunks += it } }
        return chunks.fold(FloatArray(0)) { all, chunk -> all + chunk }
    }

    companion object {
        const val KOREAN = "asr.zipformer-streaming.ko.2024-06-16.int8"
        private const val MAX_CER = 0.35

        private val ROUND_TRIPS = listOf(
            Triple("tts.vits.piper.ru-ru-denis-medium.int8", "asr.zipformer-streaming.ru.2025-08-16.int8", "\u0421\u0435\u0433\u043e\u0434\u043d\u044f \u0445\u043e\u0440\u043e\u0448\u0430\u044f \u043f\u043e\u0433\u043e\u0434\u0430, \u0438 \u043c\u044b \u0438\u0434\u0451\u043c \u0433\u0443\u043b\u044f\u0442\u044c \u0432 \u043f\u0430\u0440\u043a."),
            Triple("tts.vits.piper.fr-fr-siwis-medium.int8", "asr.zipformer-streaming.fr.2023-04-14.int8", "Bonjour, aujourd'hui il fait beau et nous allons au parc."),
        )
        private val SYNTHESIS_ONLY = mapOf(
            "tts.vits.piper.es-es-davefx-medium.int8" to "Hoy hace buen tiempo y vamos al parque.",
            "tts.vits.piper.de-de-thorsten-medium.int8" to "Heute ist sch\u00f6nes Wetter und wir gehen in den Park.",
        )
    }
}
