package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.SpeechDevice.SENSE_VOICE
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.SpeechDevice.STREAMING
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioFiles
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Recognition of the encoded recordings through `ISpeechPlugin.recognizeFile` (roadmap P7.5.6 / P7.5.7, D81 - D86): with
 * the streaming and the SenseVoice model, every English encoding stays within 0.05 of the WAV's character error rate
 * against the LibriSpeech transcript (AMR-NB, a 12.2 kbit/s telephone codec at 8 kHz, only below 0.40: ffmpeg's own
 * decoding of the same file scores the same 0.299 with the streaming model), every Chinese encoding within 0.10 of the
 * Chinese characters of the WAV's text, and the plugin's own Ogg Opus parser gives the same text as the system
 * extractor. Needs the models of [SpeechModelSetupDeviceTest]; skipped otherwise. Results are `MacawDeviceRun` lines.
 * zh-CN: 经 `ISpeechPlugin.recognizeFile` 识别编码录音 (路线图 P7.5.6 / P7.5.7, D81 - D86): 用流式与 SenseVoice 两个模型, 每份英文编码相对
 * LibriSpeech 转写的字错率与 WAV 相差不超过 0.05 (AMR-NB 是 8 kHz, 12.2 kbit/s 的电话语音编码, 只要求低于 0.40: ffmpeg 自己解码同一文件, 流式
 * 模型同样是 0.299), 每份中文编码相对 WAV 结果中汉字的字错率不超过 0.10, 插件自带的 Ogg Opus 解析与系统提取器给出相同的文字. 需要
 * [SpeechModelSetupDeviceTest] 安装的模型, 否则跳过. 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
class EncodedRecognitionDeviceTest {

    @Before
    fun models() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(STREAMING) && SpeechDevice.installed(SENSE_VOICE))
    }

    @After
    fun systemParser() {
        AudioFiles.forceOwnOggOpus = false
    }

    @Test
    fun encodedFilesRecognizeLikeTheirWav() {
        listOf(STREAMING, SENSE_VOICE).forEach { id ->
            val enWav = text(id, SpeechDevice.asset("test-audio/en.wav"))
            val zhWav = text(id, SpeechDevice.asset("test-audio/zh.wav"))
            val baseline = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, enWav)
            SpeechDevice.log("encoded model=$id file=en.wav cer=${"%.3f".format(baseline)} zh.wav chars=${zhWav.length} text=$enWav | $zhWav")
            AudioDecodeDeviceTest.AUDIO.forEach { name ->
                val text = text(id, SpeechDevice.asset("encoded-audio/$name"))
                if (name.startsWith("en")) {
                    val cer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, text)
                    SpeechDevice.log("encoded model=$id file=$name cer=${"%.3f".format(cer)} wavCer=${"%.3f".format(baseline)} text=$text")
                    // AMR-NB is a narrow band telephone codec (12.2 kbit/s at 8 kHz): only a coarse bound.
                    assertTrue("$id $name: CER $cer, the WAV $baseline", if (name.endsWith(".amr")) cer <= 0.40 else cer <= baseline + 0.05)
                } else {
                    // zh.wav ends with a mumbled English word that SenseVoice spells differently for every slight change
                    // ("al always" from the WAV, "OSOS" from the MP3), so the Chinese text is compared character by character.
                    val cer = SpeechDevice.cer(han(zhWav), han(text))
                    SpeechDevice.log("encoded model=$id file=$name cerAgainstWav=${"%.3f".format(cer)} text=$text")
                    assertTrue("$id $name: CER $cer against the Chinese of the WAV's text", cer <= 0.10)
                }
            }
            listOf("en.opus", "zh.opus").forEach { name ->
                val file = SpeechDevice.asset("encoded-audio/$name")
                val default = text(id, file)
                AudioFiles.forceOwnOggOpus = true
                val own = text(id, file)
                AudioFiles.forceOwnOggOpus = false
                SpeechDevice.log("encoded model=$id file=$name ownParserSameText=${own == default}")
                assertEquals("$id $name: own parser and default path", default, own)
            }
        }
    }

    private fun han(text: String): String = text.filter { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }

    private fun text(model: String, file: File): String {
        val answer = SpeechDevice.recognize(file, SpeechDevice.request { putString(SpeechOptionKeys.MODEL, model) })
        assertNull("${file.name}: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.getString(SpeechContract.KEY_ERROR_CODE))
        return answer.getString(SpeechResultKeys.TEXT).orEmpty()
    }
}
