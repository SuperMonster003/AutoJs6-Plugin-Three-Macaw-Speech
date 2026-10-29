package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechPluginBinder
import org.autojs.plugin.speech.api.ISpeechPlugin
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Shared pieces of the recognition device tests (roadmap P3.5): the three catalog models they need, the test
 * recordings of `androidTest/assets/test-audio/` (locked in `locks/test-audio.lock`), WAV writing, a recording session
 * callback, character error rates and the `MacawDeviceRun` log lines that `.python/device_speech_run.py` collects.
 * zh-CN: 识别设备测试的共用部分 (路线图 P3.5): 所需的三个目录模型, `androidTest/assets/test-audio/` 的测试录音 (锁定于
 * `locks/test-audio.lock`), WAV 写出, 记录事件的会话回调, 字错率, 以及 `.python/device_speech_run.py` 收集的 `MacawDeviceRun` 日志行.
 */
internal object SpeechDevice {

    const val TAG = "MacawDeviceRun"
    const val STREAMING = "asr.zipformer-streaming.zh-en.2023-02-20.int8"
    const val SENSE_VOICE = "asr.sense-voice.zh-en-ja-ko-yue.2024-07-17.int8"
    const val KWS = "kws.zipformer.zh-en.2025-12-20"
    val MODELS = listOf(KWS, STREAMING, SENSE_VOICE)

    /** LibriSpeech transcript of `en.wav` (`test_wavs/trans.txt` of the upstream model). zh-CN: `en.wav` 的 LibriSpeech 转写. */
    const val EN_REFERENCE = "AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS"

    val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** The capability Binder of this process (the test runs in the plugin process). zh-CN: 本进程的能力 Binder (测试运行于插件进程). */
    val plugin: ISpeechPlugin by lazy { SpeechPluginBinder(context) }

    fun installed(id: String): Boolean = SpeechModels.get(context).store.installed(id) != null

    fun argument(name: String): String? = InstrumentationRegistry.getArguments().getString(name)

    fun log(message: String) {
        Log.i(TAG, message)
    }

    /** Copies an asset of the test APK into the target's cache. zh-CN: 把测试 APK 的资产复制到目标应用的缓存. */
    fun asset(name: String): File {
        val target = File(context.cacheDir, "device-test/${name.substringAfterLast('/')}")
        target.parentFile?.mkdirs()
        InstrumentationRegistry.getInstrumentation().context.assets.open(name).use { input -> target.outputStream().use { input.copyTo(it) } }
        return target
    }

    /** 16 kHz mono samples of a WAV asset. zh-CN: WAV 资产的 16 kHz 单声道采样. */
    fun samples(name: String): FloatArray {
        val file = asset(name)
        val chunks = ArrayList<FloatArray>()
        WavReader(file.inputStream(), file.length()).use { reader -> AudioInput.decode(reader) { chunks += it } }
        val out = FloatArray(chunks.sumOf { it.size })
        var offset = 0
        chunks.forEach {
            System.arraycopy(it, 0, out, offset, it.size)
            offset += it.size
        }
        return out
    }

    /** Writes 16 kHz mono PCM16 to a WAV file. zh-CN: 把 16 kHz 单声道 PCM16 写成 WAV 文件. */
    fun writeWav(file: File, samples: FloatArray) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            val bytes = samples.size * 2
            header.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVE".toByteArray()).put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(16000).putInt(32000).putShort(2).putShort(16).put("data".toByteArray()).putInt(bytes)
            out.write(header.array())
            val body = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { sample ->
                if (!body.hasRemaining()) {
                    out.write(body.array(), 0, body.position())
                    body.clear()
                }
                body.putShort((sample.coerceIn(-1f, 1f) * 32767).toInt().toShort())
            }
            out.write(body.array(), 0, body.position())
        }
    }

    fun pcm16(samples: FloatArray, from: Int, to: Int): ByteArray {
        val buffer = ByteBuffer.allocate((to - from) * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (index in from until to) buffer.putShort((samples[index].coerceIn(-1f, 1f) * 32767).toInt().toShort())
        return buffer.array()
    }

    fun request(build: Bundle.() -> Unit = {}): Bundle = Bundle().apply {
        putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CALL_VERSION_1)
        build()
    }

    fun recognize(file: File, options: Bundle): Bundle = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { plugin.recognizeFile(it, options) }

    /**
     * Character error rate of [hypothesis] against [reference], ignoring case, white space and punctuation.
     * zh-CN: [hypothesis] 相对 [reference] 的字错率, 忽略大小写, 空白与标点.
     */
    fun cer(reference: String, hypothesis: String): Double {
        fun normalize(text: String) = text.uppercase().filter { it.isLetterOrDigit() }
        val r = normalize(reference)
        val h = normalize(hypothesis)
        if (r.isEmpty()) return if (h.isEmpty()) 0.0 else 1.0
        var previous = IntArray(h.length + 1) { it }
        for (i in 1..r.length) {
            val current = IntArray(h.length + 1)
            current[0] = i
            for (j in 1..h.length) {
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + if (r[i - 1] == h[j - 1]) 0 else 1)
            }
            previous = current
        }
        return previous[h.length].toDouble() / r.length
    }

    /** Native heap and total PSS of this process in MiB. zh-CN: 本进程的原生堆与总 PSS (MiB). */
    fun memory(): Pair<Long, Long> {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        return (Debug.getNativeHeapAllocatedSize() shr 20) to (info.totalPss.toLong() shr 10)
    }

    /** Records the events of a dictation channel with their arrival time. zh-CN: 记录听写通道的事件及到达时间. */
    class Events : ISpeechSessionCallback.Stub() {
        val log: MutableList<Pair<Long, String>> = Collections.synchronizedList(ArrayList())
        val bundles: MutableList<Pair<String, Bundle?>> = Collections.synchronizedList(ArrayList())
        val ready = CountDownLatch(1)
        val ended = CountDownLatch(1)

        private fun add(type: String, bundle: Bundle?) {
            log += android.os.SystemClock.elapsedRealtimeNanos() to type
            bundles += type to bundle
        }

        override fun onReady(info: Bundle?) {
            add("ready", info)
            ready.countDown()
        }

        override fun onPartial(result: Bundle?) = add("partial", result)
        override fun onFinal(result: Bundle?) = add("final", result)
        override fun onEndpoint() = add("endpoint", null)
        override fun onVoiceActivity(activity: Bundle?) = add("vad", activity)
        override fun onWarning(warning: Bundle?) = add("warning", warning)
        override fun onError(error: Bundle?) = add("error", error)
        override fun onEnd(end: Bundle?) {
            add("end", end)
            ended.countDown()
        }

        fun types(): List<String> = synchronized(log) { log.map { it.second } }

        fun finals(): List<String> = synchronized(bundles) { bundles.filter { it.first == "final" }.mapNotNull { it.second?.getString(SpeechResultKeys.TEXT) } }

        fun end(): Bundle? = synchronized(bundles) { bundles.lastOrNull { it.first == "end" }?.second }

        fun awaitEnd(seconds: Long) = ended.await(seconds, TimeUnit.SECONDS)
    }
}
