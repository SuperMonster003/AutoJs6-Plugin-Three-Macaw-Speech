package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.k2fsa.sherpa.onnx.VersionInfo

/**
 * Entry point to the sherpa-onnx runtime (roadmap 5.4, D13).
 *
 * The skeleton only loads `libsherpa-onnx-jni.so` (with `libonnxruntime.so`), reports the runtime versions and runs the
 * silero VAD bundled in the APK; the threads, model loading and idle release of roadmap D30 arrive with roadmap P3.1.
 *
 * zh-CN: sherpa-onnx 运行时的入口 (路线图 5.4, D13). 骨架只加载 `libsherpa-onnx-jni.so` (连同 `libonnxruntime.so`), 报告运行时版本,
 * 并运行随 APK 的 silero VAD; 路线图 D30 的线程, 模型加载与空闲释放随路线图 P3.1 加入.
 */
object SherpaRuntime {

    /** The silero VAD v5 model in the APK assets (roadmap D5). zh-CN: APK 资产中的 silero VAD v5 模型 (路线图 D5). */
    const val VAD_MODEL_ASSET = "vad/silero_vad_v5.onnx"

    /** Internal audio format: 16 kHz mono float (roadmap D15). zh-CN: 内部音频格式: 16 kHz 单声道 float (路线图 D15). */
    const val SAMPLE_RATE = 16000

    /** Samples per silero VAD window at 16 kHz. zh-CN: 16 kHz 下 silero VAD 每个窗口的采样数. */
    const val VAD_WINDOW_SIZE = 512

    /** Versions reported by the loaded native library. zh-CN: 已加载原生库报告的版本. */
    data class Versions(val sherpaOnnx: String, val gitSha1: String, val gitDate: String, val onnxruntime: String)

    /** A detected speech segment in samples. zh-CN: 以采样数表示的语音段. */
    data class VoiceSegment(val startSample: Int, val sampleCount: Int) {
        val startMillis: Long get() = startSample * 1000L / SAMPLE_RATE
        val endMillis: Long get() = (startSample + sampleCount) * 1000L / SAMPLE_RATE
    }

    /**
     * Loads the native library on first use (the sherpa-onnx classes call `System.loadLibrary("sherpa-onnx-jni")`) and
     * reads its versions.
     * zh-CN: 首次使用时加载原生库 (sherpa-onnx 的类会调用 `System.loadLibrary("sherpa-onnx-jni")`) 并读取其版本.
     */
    fun versions(): Versions = VersionInfo.Companion.let {
        Versions(sherpaOnnx = it.version, gitSha1 = it.gitSha1, gitDate = it.gitDate, onnxruntime = it.onnxruntimeVersion)
    }

    /**
     * silero VAD parameters; the defaults are the upstream ones (threshold 0.5, 0.25 s minimum silence and speech, 5 s
     * maximum speech). Invalid values are rejected before any native code runs.
     * zh-CN: silero VAD 参数; 默认值与上游一致 (阈值 0.5, 最短静音与语音 0.25 秒, 最长语音 5 秒). 非法取值在调用任何原生代码之前即被拒绝.
     */
    data class VadSettings(
        val threshold: Float = 0.5f,
        val minSilenceSeconds: Float = 0.25f,
        val minSpeechSeconds: Float = 0.25f,
        val maxSpeechSeconds: Float = 5f,
    ) {
        init {
            require(threshold > 0f && threshold < 1f) { "threshold must be within (0, 1): $threshold" }
            require(minSilenceSeconds in 0f..MAX_DURATION_SECONDS) { "minSilenceSeconds out of range: $minSilenceSeconds" }
            require(minSpeechSeconds in 0f..MAX_DURATION_SECONDS) { "minSpeechSeconds out of range: $minSpeechSeconds" }
            require(maxSpeechSeconds > minSpeechSeconds && maxSpeechSeconds <= MAX_DURATION_SECONDS) { "maxSpeechSeconds out of range: $maxSpeechSeconds" }
        }

        private companion object {
            /** Upper bound of every duration, the 30 minute file limit of roadmap D15. zh-CN: 各时长的上限, 即路线图 D15 的 30 分钟文件上限. */
            const val MAX_DURATION_SECONDS = 1800f
        }
    }

    /**
     * Creates a silero VAD on the bundled model. The caller owns the detector and must [Vad.release] it.
     * zh-CN: 以随包模型创建 silero VAD. 调用方持有检测器并须 [Vad.release].
     */
    fun createVad(assets: AssetManager, settings: VadSettings = VadSettings()): Vad {
        val silero = SileroVadModelConfig(
            model = VAD_MODEL_ASSET,
            threshold = settings.threshold,
            minSilenceDuration = settings.minSilenceSeconds,
            minSpeechDuration = settings.minSpeechSeconds,
            windowSize = VAD_WINDOW_SIZE,
            maxSpeechDuration = settings.maxSpeechSeconds,
        )
        return Vad(assets, VadModelConfig(sileroVadModelConfig = silero, sampleRate = SAMPLE_RATE, numThreads = 1))
    }

    /**
     * Feeds [samples] (16 kHz mono, -1..1) window by window, flushes the tail and returns every speech segment. The trailing
     * partial window is dropped, as in the upstream examples.
     * zh-CN: 逐窗口送入 [samples] (16 kHz 单声道, -1..1), 冲刷尾部并返回全部语音段. 末尾不足一个窗口的部分被丢弃, 与上游示例一致.
     */
    fun detectSegments(vad: Vad, samples: FloatArray): List<VoiceSegment> {
        val segments = ArrayList<VoiceSegment>()
        fun drain() {
            while (!vad.empty()) {
                val segment = vad.front()
                segments += VoiceSegment(segment.start, segment.samples.size)
                vad.pop()
            }
        }
        var offset = 0
        while (offset + VAD_WINDOW_SIZE <= samples.size) {
            vad.acceptWaveform(samples.copyOfRange(offset, offset + VAD_WINDOW_SIZE))
            drain()
            offset += VAD_WINDOW_SIZE
        }
        vad.flush()
        drain()
        return segments
    }

}
