package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.speaker.SpeakerStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWakeService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeakerRecognition
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Speaker recognition on the device (roadmap P8.3.6 E3, D143) with the recordings of `locks/speaker-test-audio.lock` and
 * the installed default speaker model: enroll / verify / identify / list / remove through the plugin's Binder with
 * contract version 2 (two LibriSpeech readers, names prefixed `device-test-` and removed afterwards, so speakers the
 * device already has stay), and the check of "only enrolled voices" (D139) on a private voiceprint store: the Kokoro
 * wake word of the enrolled voice passes, that of the other voice is rejected. Scores go to the `MacawDeviceRun` log.
 * zh-CN: 设备上的说话人识别 (路线图 P8.3.6 E3, D143), 使用 `locks/speaker-test-audio.lock` 的录音与已安装的默认说话人模型: 以契约版本 2 经插件
 * Binder 注册 / 验证 / 辨认 / 列出 / 删除 (两位 LibriSpeech 朗读者, 名字以 `device-test-` 开头并在结束后删除, 设备已有的说话人保持不变); 在私有
 * 声纹库上检查 "只听主人" (D139): 已注册声音的 Kokoro 唤醒词放行, 另一声音的被拒. 分数写入 `MacawDeviceRun` 日志.
 */
@RunWith(AndroidJUnit4::class)
class SpeakerDeviceTest {

    private val context = SpeechDevice.context
    private val plugin = SpeechDevice.plugin
    private val gateDirectory = File(context.cacheDir, "speaker-device-test")

    @Before
    fun prerequisites() {
        assumeTrue("the speaker model is installed", SpeechDevice.installed(SPEAKER_MODEL))
        cleanUp()
    }

    @After
    fun cleanUp() {
        listOf(A, B).forEach { name -> plugin.removeSpeaker(request { putString(SpeechOptionKeys.NAME, name) }) }
        gateDirectory.deleteRecursively()
    }

    private fun request(build: Bundle.() -> Unit = {}): Bundle = Bundle().apply {
        putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CALL_VERSION_2)
        build()
    }

    private fun <T> descriptors(names: List<String>, block: (Array<ParcelFileDescriptor>) -> T): T {
        val opened = names.map { ParcelFileDescriptor.open(SpeechDevice.asset("$FOLDER/$it.wav"), ParcelFileDescriptor.MODE_READ_ONLY) }
        try {
            return block(opened.toTypedArray())
        } finally {
            opened.forEach { it.close() }
        }
    }

    private fun enroll(name: String, voice: String): Bundle = descriptors((1..3).map { "libri-$voice-enroll-$it" }) { files ->
        plugin.enrollSpeaker(files, request { putString(SpeechOptionKeys.NAME, name) })
    }

    private fun verify(name: String, trial: String): Bundle = descriptors(listOf(trial)) { files ->
        plugin.verifySpeaker(files[0], request { putString(SpeechOptionKeys.NAME, name) })
    }

    private fun identify(trial: String): Bundle = descriptors(listOf(trial)) { files -> plugin.identifySpeaker(files[0], request()) }

    private fun ok(answer: Bundle, what: String): Bundle {
        assertEquals("$what answers contract version 2", SpeechContract.CALL_VERSION_2, answer.getInt(SpeechContract.KEY_CONTRACT_VERSION))
        assertNull("$what: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.getString(SpeechContract.KEY_ERROR_CODE))
        return answer
    }

    @Test
    fun enrollVerifyIdentifyListAndRemoveThroughTheBinder() {
        val started = SystemClock.elapsedRealtime()
        val enrolled = ok(enroll(A, "a"), "enroll a")
        assertEquals(A, enrolled.getString(SpeechResultKeys.NAME))
        assertEquals(3, enrolled.getInt(SpeechResultKeys.SAMPLES))
        assertTrue("about 9 s of speech", enrolled.getLong(SpeechResultKeys.DURATION_MILLIS) in 6_000..9_500)
        assertEquals(SPEAKER_MODEL, enrolled.getString(SpeechResultKeys.MODEL_ID))
        ok(enroll(B, "b"), "enroll b")
        val enrollMillis = SystemClock.elapsedRealtime() - started

        for (voice in listOf("a", "b")) {
            for (trial in 1..2) {
                val clip = "libri-$voice-trial-$trial"
                val same = ok(verify(if (voice == "a") A else B, clip), "verify $clip")
                val other = ok(verify(if (voice == "a") B else A, clip), "verify $clip against the other reader")
                SpeechDevice.log("speaker verify $clip same=${same.getFloat(SpeechResultKeys.SCORE)} other=${other.getFloat(SpeechResultKeys.SCORE)}")
                assertTrue("$clip matches its reader", same.getBoolean(SpeechResultKeys.MATCHED))
                assertFalse("$clip does not match the other reader", other.getBoolean(SpeechResultKeys.MATCHED))
                assertEquals(0.5f, same.getFloat(SpeechResultKeys.THRESHOLD), 1e-6f)
                val found = ok(identify(clip), "identify $clip")
                assertEquals("$clip is identified", if (voice == "a") A else B, found.getString(SpeechResultKeys.NAME))
                assertTrue(found.getBoolean(SpeechResultKeys.MATCHED))
            }
        }
        // A stricter threshold of the request rejects even the reader. zh-CN: 请求给出的更严阈值连本人也拒绝.
        val strict = descriptors(listOf("libri-a-trial-1")) { files ->
            plugin.verifySpeaker(files[0], request { putString(SpeechOptionKeys.NAME, A); putDouble(SpeechOptionKeys.THRESHOLD, 0.99) })
        }
        assertFalse(ok(strict, "strict verify").getBoolean(SpeechResultKeys.MATCHED))

        @Suppress("DEPRECATION")
        val listed = ok(plugin.listSpeakers(request()), "list").getParcelableArrayList<Bundle>(SpeechResultKeys.SPEAKERS)!!
        assertEquals(listOf(A, B), listed.map { it.getString(SpeechResultKeys.NAME) }.filter { it == A || it == B })
        assertTrue(ok(plugin.removeSpeaker(request { putString(SpeechOptionKeys.NAME, A) }), "remove").getBoolean(SpeechResultKeys.REMOVED))
        assertEquals(SpeechErrorCodes.SPEAKER_NOT_FOUND, verify(A, "libri-a-trial-1").getString(SpeechContract.KEY_ERROR_CODE))
        // Contract version 1 cannot reach the speaker calls (D136). zh-CN: 契约版本 1 不能调用说话人方法.
        val v1 = plugin.listSpeakers(SpeechDevice.request())
        assertEquals(SpeechErrorCodes.PLUGIN_INCOMPATIBLE, v1.getString(SpeechContract.KEY_ERROR_CODE))
        SpeechDevice.log("speaker binder round trips passed; two enrollments in $enrollMillis ms")
    }

    @Test
    fun onlyEnrolledVoicesLetTheEnrolledVoiceThrough() {
        val speakers = SpeakerRecognition(SpeechRuntime.get(context), SpeakerStore(gateDirectory))
        assertNull("no voiceprint: the check does not run and the hit passes", speakers.gate(SpeechDevice.samples("$FOLDER/kokoro-b-wake.wav")))
        val recordings = (1..3).map { SpeakerRecognition.Recording.Samples(SpeechDevice.samples("$FOLDER/kokoro-a-enroll-$it.wav"), "Sentence $it") }
        speakers.enroll("voice a", recordings, replace = false, model = null)
        for ((voice, expected) in listOf("a" to true, "b" to false)) {
            val window = window(SpeechDevice.samples("$FOLDER/kokoro-$voice-wake.wav"))
            val started = SystemClock.elapsedRealtime()
            val match = speakers.gate(window)!!
            SpeechDevice.log("speaker gate voice $voice score=${match.best?.score} matched=${match.matched} in ${SystemClock.elapsedRealtime() - started} ms")
            assertEquals("voice $voice", expected, match.matched)
        }
    }

    /**
     * The window the resident wake word scores: the last [ResidentWakeService.GATE_WINDOW_MILLIS] before the hit, here the
     * wake word at its end after room silence. zh-CN: 常驻唤醒打分的窗口: 命中前的最后一段, 此处为静音后结尾处的唤醒词.
     */
    private fun window(wake: FloatArray): FloatArray {
        val size = (ResidentWakeService.GATE_WINDOW_MILLIS * 16).toInt()
        return if (wake.size >= size) wake.copyOfRange(wake.size - size, wake.size) else FloatArray(size - wake.size) + wake
    }

    private companion object {
        const val SPEAKER_MODEL = "speaker.eres2net.3dspeaker.en-voxceleb"
        const val FOLDER = "speaker-audio"
        const val A = "device-test-reader-a"
        const val B = "device-test-reader-b"
    }
}
