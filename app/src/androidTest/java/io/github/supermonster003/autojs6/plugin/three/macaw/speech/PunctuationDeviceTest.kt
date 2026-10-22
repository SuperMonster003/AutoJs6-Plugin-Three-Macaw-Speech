package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.SpeechDevice.STREAMING
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The punctuation models of catalog v2 on a device (roadmap D107, P7.7.5): `en.wav`, `zh.wav` and `mixed-zh-en.wav`
 * recognized by the streaming model through `ISpeechPlugin.recognizeFile` with and without `punctuation`. The English
 * text goes to the case model (ASCII marks, capitals restored), text with Chinese to the Chinese and English model
 * (full width marks); the words stay those of the recognizer. Needs the streaming model and both punctuation models,
 * installed from the catalog by [SpeechModelSetupDeviceTest] (`-e macawModels true -e macawModelIds ...`), otherwise
 * skipped. Results are `MacawDeviceRun` lines.
 * zh-CN: 设备上的目录 v2 标点模型 (路线图 D107, P7.7.5): 用流式模型经 `ISpeechPlugin.recognizeFile` 识别 `en.wav`, `zh.wav` 与
 * `mixed-zh-en.wav`, 分别开启与关闭 `punctuation`. 英文文本交给大小写模型 (ASCII 标点, 恢复大写), 含中文的文本交给中英文模型 (全角标点); 词语
 * 保持识别器的输出. 需要流式模型与两个标点模型, 由 [SpeechModelSetupDeviceTest] (`-e macawModels true -e macawModelIds ...`) 从目录安装,
 * 否则跳过. 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
class PunctuationDeviceTest {

    @Before
    fun models() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true -e macawModelIds ...)", (listOf(STREAMING) + MODELS).all(SpeechDevice::installed))
    }

    @Test
    fun streamingFinalsGetPunctuationAndEnglishCapitals() {
        val texts = FILES.associateWith { name ->
            val file = SpeechDevice.asset("test-audio/$name")
            val punctuated = text(file, punctuation = true)
            val plain = text(file, punctuation = false)
            val marks = punctuated.count { it in MARKS }
            val sameWords = words(punctuated) == words(plain)
            SpeechDevice.log(
                "punct file=$name marks=$marks plainMarks=${plain.count { it in MARKS }} fullWidth=${punctuated.count { it in FULL_WIDTH }} " +
                    "upper=${punctuated.count { it.isUpperCase() }} lower=${punctuated.count { it.isLowerCase() }} sameWords=$sameWords text=$punctuated",
            )
            assertTrue("$name: the finals get punctuation", marks > 0)
            assertEquals("$name: punctuation false keeps none", 0, plain.count { it in MARKS })
            assertTrue("$name: the punctuation model keeps the recognized words", sameWords)
            punctuated
        }
        val en = texts.getValue("en.wav")
        assertTrue("en.wav: ASCII marks", en.none { it in FULL_WIDTH })
        assertTrue("en.wav: the case model restores capitals", en.first().isUpperCase() && en.count { it.isLowerCase() } > en.count { it.isUpperCase() })
        assertTrue("zh.wav: full width marks", texts.getValue("zh.wav").any { it in FULL_WIDTH })
    }

    private fun words(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    private fun text(file: File, punctuation: Boolean): String {
        val answer = SpeechDevice.recognize(file, SpeechDevice.request {
            putString(SpeechOptionKeys.MODEL, STREAMING)
            putBoolean(SpeechOptionKeys.PUNCTUATION, punctuation)
        })
        assertNull("${file.name}: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.getString(SpeechContract.KEY_ERROR_CODE))
        return answer.getString(SpeechResultKeys.TEXT).orEmpty()
    }

    companion object {
        const val CASE = "punct.cnn-bilstm.en.2024-08-06.int8"
        const val CHINESE_ENGLISH = "punct.ct-transformer.zh-en.2024-04-12.int8"
        val MODELS = listOf(CASE, CHINESE_ENGLISH)

        private val FILES = listOf("en.wav", "zh.wav", "mixed-zh-en.wav")
        private const val FULL_WIDTH = "\uff0c\u3002\uff1f\uff01\uff1b\uff1a\u3001"
        private const val MARKS = ",.?!;:$FULL_WIDTH"
    }
}
