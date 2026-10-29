package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.k2fsa.sherpa.onnx.AudioTagging
import com.k2fsa.sherpa.onnx.AudioTaggingConfig
import com.k2fsa.sherpa.onnx.AudioTaggingModelConfig
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineCanaryModelConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineOmnilingualAsrCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.k2fsa.sherpa.onnx.OfflineZipformerAudioTaggingModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentification
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentificationConfig
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentificationWhisperConfig
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaEngines
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsEngineCore
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Candidate models before they enter the catalog (roadmap P8.2.1, D124): loads a model directory that
 * `.python/device_model_probe.py` pushed to `/data/local/tmp`, straight through the sherpa-onnx 1.13.8 API with the
 * plugin's thread counts, and reports the load time, the peak resident memory of the process while loading and answering
 * the first request (`VmHWM` after resetting it, or the highest sampled `VmRSS`), the PSS afterwards and the first-answer
 * latency: the first partial after the first audible sample for a streaming recognizer fed at real-time pace, the
 * decoding time of the whole recording for an offline recognizer (one sentence, what a VAD segment gives it), the time
 * to the first samples for a synthesizer, the embedding times for a speaker model (roadmap P8.3), the processing time of a diarization, a language identification
 * or the windows of an audio tagger (roadmap P8.4). Skipped unless `macawProbeDir` is given. Results are `MacawDeviceRun` lines.
 * zh-CN: 候选模型入目录前的实测 (路线图 P8.2.1, D124): 加载 `.python/device_model_probe.py` 推送到 `/data/local/tmp` 的模型目录, 直接经
 * sherpa-onnx 1.13.8 API 并沿用插件的线程数, 报告加载耗时, 加载与回答第一个请求期间进程的峰值常驻内存 (重置后的 `VmHWM`, 不能重置时取采样到的
 * 最大 `VmRSS`), 之后的 PSS, 以及首个结果的延迟: 流式识别按实时速度送入, 记首个可闻采样到首个 partial 的时间; 离线识别记整段录音 (一句, 相当于一个
 * VAD 片段) 的解码时间; 合成记到首批采样的时间; 说话人模型记嵌入耗时 (路线图 P8.3), 说话人分离, 语种识别与音频标签记处理耗时 (路线图 P8.4). 未给出 `macawProbeDir` 时跳过. 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
class ModelProbeDeviceTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun probe() {
        val source = SpeechDevice.argument("macawProbeDir")
        assumeTrue("give macawProbeDir (see .python/device_model_probe.py)", !source.isNullOrBlank())
        val type = SpeechDevice.argument("macawProbeType").orEmpty()
        val roles = SpeechDevice.argument("macawProbeRoles").orEmpty().split(';').filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
        val label = SpeechDevice.argument("macawProbeLabel") ?: source!!.substringAfterLast('/')
        val directory = copy(source!!, File(SpeechDevice.context.cacheDir, "probe/${source.substringAfterLast('/')}"))
        fun path(role: String) = roles[role]?.takeIf { it.isNotEmpty() }?.split(',')?.joinToString(",") { File(directory, it).path }.orEmpty()

        val memory = PeakMemory()
        val (_, pssBefore) = SpeechDevice.memory()
        memory.start()
        val loadStarted = SystemClock.elapsedRealtime()
        val line = when (type) {
            "online-transducer" -> {
                val recognizer = OnlineRecognizer(
                    null,
                    OnlineRecognizerConfig(
                        featConfig = FeatureConfig(sampleRate = AudioInput.SAMPLE_RATE, featureDim = 80),
                        modelConfig = OnlineModelConfig(
                            transducer = OnlineTransducerModelConfig(encoder = path("encoder"), decoder = path("decoder"), joiner = path("joiner")),
                            tokens = path("tokens"),
                            numThreads = SherpaEngines.ASR_THREADS,
                            modelType = roles["modelType"].orEmpty(),
                        ),
                        enableEndpoint = false,
                        decodingMethod = "greedy_search",
                    ),
                )
                SherpaEngines.requireNative(recognizer, label)
                val loadMs = SystemClock.elapsedRealtime() - loadStarted
                try {
                    streaming(recognizer, samples(wav()), loadMs)
                } finally {
                    recognizer.release()
                }
            }
            "nemo-transducer", "canary", "omnilingual", "sense-voice" -> {
                val modelConfig = when (type) {
                    "nemo-transducer" -> OfflineModelConfig(
                        transducer = OfflineTransducerModelConfig(encoder = path("encoder"), decoder = path("decoder"), joiner = path("joiner")),
                        tokens = path("tokens"),
                        numThreads = SherpaEngines.ASR_THREADS,
                        modelType = roles["modelType"] ?: "nemo_transducer",
                    )
                    "canary" -> OfflineModelConfig(
                        canary = OfflineCanaryModelConfig(encoder = path("encoder"), decoder = path("decoder"), srcLang = roles["language"].orEmpty(), tgtLang = roles["language"].orEmpty(), usePnc = true),
                        tokens = path("tokens"),
                        numThreads = SherpaEngines.ASR_THREADS,
                    )
                    "sense-voice" -> OfflineModelConfig(
                        senseVoice = OfflineSenseVoiceModelConfig(model = path("model"), language = roles["language"].orEmpty(), useInverseTextNormalization = true),
                        tokens = path("tokens"),
                        numThreads = SherpaEngines.ASR_THREADS,
                    )
                    else -> OfflineModelConfig(
                        omnilingual = OfflineOmnilingualAsrCtcModelConfig(model = path("model")),
                        tokens = path("tokens"),
                        numThreads = SherpaEngines.ASR_THREADS,
                    )
                }
                val recognizer = OfflineRecognizer(null, OfflineRecognizerConfig(featConfig = FeatureConfig(sampleRate = AudioInput.SAMPLE_RATE, featureDim = 80), modelConfig = modelConfig))
                SherpaEngines.requireNative(recognizer, label)
                val loadMs = SystemClock.elapsedRealtime() - loadStarted
                try {
                    offline(recognizer, samples(wav()), loadMs)
                } finally {
                    recognizer.release()
                }
            }
            "kokoro", "vits" -> {
                val modelConfig = if (type == "kokoro") {
                    OfflineTtsModelConfig(
                        kokoro = OfflineTtsKokoroModelConfig(
                            model = path("model"), voices = path("voices"), tokens = path("tokens"), dataDir = path("dataDir"),
                            lexicon = path("lexicon"), lang = roles["lang"].orEmpty(),
                        ),
                        numThreads = TtsEngineCore.threads(),
                    )
                } else {
                    OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(model = path("model"), tokens = path("tokens"), dataDir = path("dataDir"), lexicon = path("lexicon")),
                        numThreads = TtsEngineCore.threads(),
                    )
                }
                val tts = OfflineTts(null, OfflineTtsConfig(model = modelConfig, ruleFsts = path("ruleFsts"), maxNumSentences = 1))
                SherpaEngines.requireNative(tts, label)
                val loadMs = SystemClock.elapsedRealtime() - loadStarted
                try {
                    synthesis(tts, loadMs)
                } finally {
                    tts.release()
                }
            }
            "speaker" -> {
                val extractor = SpeakerEmbeddingExtractor(null, SpeakerEmbeddingExtractorConfig(model = path("model"), numThreads = SherpaEngines.ASR_THREADS))
                SherpaEngines.requireNative(extractor, label)
                val loadMs = SystemClock.elapsedRealtime() - loadStarted
                try {
                    speaker(extractor, samples(wav()), loadMs)
                } finally {
                    extractor.release()
                }
            }
            "diarization" -> {
                val config = OfflineSpeakerDiarizationConfig(
                    segmentation = OfflineSpeakerSegmentationModelConfig(
                        pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(
                            model = path("segmentation"),
                            windowShiftRatio = SpeechDevice.argument("macawProbeShift")?.toFloatOrNull() ?: 0.1f,
                        ),
                        numThreads = SherpaEngines.ASR_THREADS,
                    ),
                    embedding = SpeakerEmbeddingExtractorConfig(model = path("embedding"), numThreads = SherpaEngines.ASR_THREADS),
                    clustering = FastClusteringConfig(numClusters = SpeechDevice.argument("macawProbeClusters")?.toIntOrNull() ?: -1, threshold = SpeechDevice.argument("macawProbeThreshold")?.toFloatOrNull() ?: 0.5f),
                )
                val diarization = OfflineSpeakerDiarization(null, config)
                SherpaEngines.requireNative(diarization, label)
                val loadMs = SystemClock.elapsedRealtime() - loadStarted
                try {
                    diarize(diarization, samples(wav()), loadMs)
                } finally {
                    diarization.release()
                }
            }
            "langid" -> {
                val config = SpokenLanguageIdentificationConfig(
                    whisper = SpokenLanguageIdentificationWhisperConfig(encoder = path("encoder"), decoder = path("decoder")),
                    numThreads = SherpaEngines.ASR_THREADS,
                )
                val identification = SpokenLanguageIdentification(null, config)
                SherpaEngines.requireNative(identification, label)
                val loadMs = SystemClock.elapsedRealtime() - loadStarted
                try {
                    language(identification, samples(wav()), loadMs)
                } finally {
                    identification.release()
                }
            }
            "tagger-ced", "tagger-zipformer" -> {
                val model = if (type == "tagger-ced") {
                    AudioTaggingModelConfig(ced = path("model"), numThreads = SherpaEngines.ASR_THREADS)
                } else {
                    AudioTaggingModelConfig(zipformer = OfflineZipformerAudioTaggingModelConfig(model = path("model")), numThreads = SherpaEngines.ASR_THREADS)
                }
                val tagging = AudioTagging(null, AudioTaggingConfig(model = model, labels = path("labels"), topK = 5))
                SherpaEngines.requireNative(tagging, label)
                val loadMs = SystemClock.elapsedRealtime() - loadStarted
                try {
                    tags(tagging, samples(wav()), loadMs)
                } finally {
                    tagging.release()
                }
            }
            else -> throw AssertionError("unknown macawProbeType '$type'")
        }
        val peak = memory.stop()
        val (_, pssAfter) = SpeechDevice.memory()
        SpeechDevice.log("probe $label type=$type $line peakRss=${peak.mib} MiB (${peak.method}) pssBefore=$pssBefore MiB pssAfter=$pssAfter MiB")
        // Only the copy is deleted; a directory read in place (an installed model) stays.
        if (directory.path != File(source).path) directory.deleteRecursively()
    }

    /** Real-time feeding in 100 ms chunks, then 0.8 s of silence. zh-CN: 按实时速度每次送 100 ms, 最后补 0.8 s 静音. */
    private fun streaming(recognizer: OnlineRecognizer, samples: FloatArray, loadMs: Long): String {
        val stream = recognizer.createStream("")
        val chunk = AudioInput.SAMPLE_RATE / 10
        val audible = samples.indexOfFirst { abs(it) > AUDIBLE }.coerceAtLeast(0)
        val started = SystemClock.elapsedRealtime()
        var firstPartial = -1L
        var offset = 0
        try {
            while (offset < samples.size) {
                val end = minOf(samples.size, offset + chunk)
                stream.acceptWaveform(samples.copyOfRange(offset, end), AudioInput.SAMPLE_RATE)
                while (recognizer.isReady(stream)) recognizer.decode(stream)
                if (firstPartial < 0 && recognizer.getResult(stream).text.isNotBlank()) {
                    firstPartial = SystemClock.elapsedRealtime() - started - audible * 1000L / AudioInput.SAMPLE_RATE
                }
                offset = end
                val due = started + offset * 1000L / AudioInput.SAMPLE_RATE
                SystemClock.sleep((due - SystemClock.elapsedRealtime()).coerceAtLeast(0))
            }
            stream.acceptWaveform(FloatArray(AudioInput.SAMPLE_RATE * 8 / 10), AudioInput.SAMPLE_RATE)
            stream.inputFinished()
            while (recognizer.isReady(stream)) recognizer.decode(stream)
            return "loadMs=$loadMs firstPartialMs=$firstPartial ${judge(recognizer.getResult(stream).text, samples.size)}"
        } finally {
            stream.release()
        }
    }

    private fun offline(recognizer: OfflineRecognizer, samples: FloatArray, loadMs: Long): String {
        val stream = recognizer.createStream()
        try {
            val started = SystemClock.elapsedRealtime()
            stream.acceptWaveform(samples, AudioInput.SAMPLE_RATE)
            recognizer.decode(stream)
            val text = recognizer.getResult(stream).text
            val decodeMs = SystemClock.elapsedRealtime() - started
            return "loadMs=$loadMs firstResultMs=$decodeMs rtf=${"%.3f".format(decodeMs / 1000.0 / (samples.size.toDouble() / AudioInput.SAMPLE_RATE))} ${judge(text, samples.size)}"
        } finally {
            stream.release()
        }
    }

    private fun synthesis(tts: OfflineTts, loadMs: Long): String {
        val text = text("macawProbeText")
        assertTrue("give macawProbeText for a synthesizer", text.isNotBlank())
        val sid = SpeechDevice.argument("macawProbeSid")?.toIntOrNull() ?: 0
        val pieces = ArrayList<FloatArray>()
        val started = SystemClock.elapsedRealtime()
        var first = -1L
        val audio = tts.generateWithCallback(text, sid, 1.0f, TtsEngine.SampleCallback { piece ->
            if (first < 0 && piece.isNotEmpty()) first = SystemClock.elapsedRealtime() - started
            pieces += piece
            true
        })
        val totalMs = SystemClock.elapsedRealtime() - started
        val samples = audio.samples
        val seconds = samples.size.toDouble() / tts.sampleRate()
        SpeechDevice.argument("macawProbeOut")?.takeIf { it.isNotBlank() }?.let { name ->
            val out = File(SpeechDevice.context.cacheDir, "probe-out/$name").apply { parentFile?.mkdirs() }
            writeWav(out, samples, tts.sampleRate())
        }
        return "loadMs=$loadMs firstAudioMs=$first totalMs=$totalMs sampleRate=${tts.sampleRate()} speakers=${tts.numSpeakers()} seconds=${"%.2f".format(seconds)} rtf=${"%.3f".format(totalMs / 1000.0 / seconds.coerceAtLeast(0.001))}"
    }

    /**
     * A speaker embedding model (roadmap P8.3, D125): the first embedding of 2 s (part of the peak), then the mean time
     * of five 2 s and five 5 s embeddings, and the cosine scores of two halves of the recording (same speaker) and, with
     * `macawProbeWav2`, of the recording against another speaker's.
     * zh-CN: 说话人嵌入模型 (路线图 P8.3, D125): 首个 2 s 嵌入 (计入峰值), 再取 2 s 与 5 s 各五次的平均耗时, 以及录音前后两半 (同一人) 与 (给出
     * `macawProbeWav2` 时) 与另一人录音的余弦分数.
     */
    private fun speaker(extractor: SpeakerEmbeddingExtractor, samples: FloatArray, loadMs: Long): String {
        val rate = AudioInput.SAMPLE_RATE
        fun embed(piece: FloatArray): FloatArray {
            val stream = extractor.createStream()
            try {
                stream.acceptWaveform(piece, rate)
                stream.inputFinished()
                assertTrue("the extractor is not ready for ${piece.size} samples", extractor.isReady(stream))
                return extractor.compute(stream)
            } finally {
                stream.release()
            }
        }
        fun cut(seconds: Double, from: Int = 0) = samples.copyOfRange(from, minOf(samples.size, from + (seconds * rate).toInt()))
        fun cosine(a: FloatArray, b: FloatArray): Double {
            var dot = 0.0; var na = 0.0; var nb = 0.0
            for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
            return dot / (Math.sqrt(na) * Math.sqrt(nb))
        }
        val started = SystemClock.elapsedRealtime()
        val first = embed(cut(2.0))
        val firstMs = SystemClock.elapsedRealtime() - started
        fun mean(seconds: Double): Long {
            val piece = cut(seconds)
            val begin = SystemClock.elapsedRealtime()
            repeat(5) { embed(piece) }
            return (SystemClock.elapsedRealtime() - begin) / 5
        }
        val ms2 = mean(2.0)
        val ms5 = mean(5.0)
        val half = samples.size / 2
        val same = cosine(embed(samples.copyOfRange(0, half)), embed(samples.copyOfRange(half, samples.size)))
        val other = SpeechDevice.argument("macawProbeWav2")?.takeIf { it.isNotBlank() }?.let { name ->
            val target = File(SpeechDevice.context.cacheDir, "probe-wav/${name.substringAfterLast('/')}")
            shellCopy(name, target)
            cosine(embed(samples), embed(samples(target)))
        }
        val audioS = "%.2f".format(samples.size.toDouble() / rate)
        return "loadMs=$loadMs dim=${first.size} first2sMs=$firstMs mean2sMs=$ms2 mean5sMs=$ms5 audioS=$audioS sameHalves=${"%.3f".format(same)}" +
            (other?.let { " otherSpeaker=${"%.3f".format(it)}" } ?: "")
    }

    /**
     * Speaker diarization (roadmap P8.4, D126): the whole recording at once with the progress callback, then (with
     * `macawProbeCancelAt`, a fraction) a second run whose callback answers non-zero at that share of the chunks, to time
     * how fast a cancel takes effect.
     * zh-CN: 说话人分离 (路线图 P8.4, D126): 带进度回调一次处理整段录音; 给出 `macawProbeCancelAt` (比例) 时再跑一次, 回调在该比例的块处返回非零,
     * 记录取消生效的时间.
     */
    private fun diarize(diarization: OfflineSpeakerDiarization, samples: FloatArray, loadMs: Long): String {
        var callbacks = 0
        var total = 0
        val started = SystemClock.elapsedRealtime()
        val segments = diarization.processWithCallback(samples, Progress { done, all ->
            callbacks += 1
            total = all
            0
        }, 0L)
        val processMs = SystemClock.elapsedRealtime() - started
        val seconds = samples.size.toDouble() / AudioInput.SAMPLE_RATE
        val speakers = segments.map { it.speaker }.toSet().size
        val cancel = SpeechDevice.argument("macawProbeCancelAt")?.toDoubleOrNull()?.let { share ->
            var stoppedAt = -1L
            val begin = SystemClock.elapsedRealtime()
            val partial = diarization.processWithCallback(samples, Progress { done, all ->
                if (done >= all * share) {
                    if (stoppedAt < 0) stoppedAt = SystemClock.elapsedRealtime()
                    1
                } else {
                    0
                }
            }, 0L)
            val end = SystemClock.elapsedRealtime()
            " cancelAt=$share cancelReturnMs=${if (stoppedAt < 0) -1 else end - stoppedAt} cancelTotalMs=${end - begin} cancelSegments=${partial.size}"
        }.orEmpty()
        return "loadMs=$loadMs processMs=$processMs audioS=${"%.2f".format(seconds)} rtf=${"%.3f".format(processMs / 1000.0 / seconds)} " +
            "speakers=$speakers segments=${segments.size} callbacks=$callbacks chunks=$total$cancel"
    }

    /**
     * The progress callback of `processWithCallback`: sherpa-onnx 1.13.8 looks up `invoke(IIJ)Ljava/lang/Integer;` through
     * JNI, which a Kotlin lambda (an invokedynamic lambda after D8) lacks, so a class with the specialized method is needed.
     * zh-CN: `processWithCallback` 的进度回调: sherpa-onnx 1.13.8 经 JNI 查找 `invoke(IIJ)Ljava/lang/Integer;`, Kotlin lambda (经 D8 成为
     * invokedynamic lambda) 没有这个方法, 所以要用带特化方法的类.
     */
    private class Progress(private val block: (Int, Int) -> Int) : Function3<Int, Int, Long, Int> {
        override fun invoke(done: Int, total: Int, arg: Long): Int = block(done, total)
    }

    /** Spoken language identification (roadmap P8.4): the first call, then the mean of three. zh-CN: 语种识别: 首次调用与随后三次的平均耗时. */
    private fun language(identification: SpokenLanguageIdentification, samples: FloatArray, loadMs: Long): String {
        fun once(): String {
            val stream = identification.createStream()
            try {
                stream.acceptWaveform(samples, AudioInput.SAMPLE_RATE)
                return identification.compute(stream)
            } finally {
                stream.release()
            }
        }
        val started = SystemClock.elapsedRealtime()
        val language = once()
        val firstMs = SystemClock.elapsedRealtime() - started
        val begin = SystemClock.elapsedRealtime()
        repeat(3) { once() }
        val meanMs = (SystemClock.elapsedRealtime() - begin) / 3
        return "loadMs=$loadMs firstMs=$firstMs meanMs=$meanMs audioS=${"%.2f".format(samples.size.toDouble() / AudioInput.SAMPLE_RATE)} language=$language"
    }

    /**
     * Audio tagging (roadmap P8.4): 10 s windows (CED takes at most 10 s), the first window, then the mean over the rest
     * of the recording, and the top labels of the first window.
     * zh-CN: 音频标签: 按 10 s 窗口 (CED 至多接受 10 s), 首个窗口耗时, 其余窗口的平均耗时, 以及首个窗口的前几个标签.
     */
    private fun tags(tagging: AudioTagging, samples: FloatArray, loadMs: Long): String {
        val window = AudioInput.SAMPLE_RATE * 10
        val windows = (0 until samples.size step window).map { samples.copyOfRange(it, minOf(samples.size, it + window)) }.filter { it.size >= AudioInput.SAMPLE_RATE / 2 }
        fun once(piece: FloatArray): List<String> {
            val stream = tagging.createStream()
            try {
                stream.acceptWaveform(piece, AudioInput.SAMPLE_RATE)
                return tagging.compute(stream, 3).map { "${it.name}:${"%.2f".format(it.prob)}" }
            } finally {
                stream.release()
            }
        }
        val started = SystemClock.elapsedRealtime()
        val top = once(windows.first())
        val firstMs = SystemClock.elapsedRealtime() - started
        val begin = SystemClock.elapsedRealtime()
        windows.drop(1).forEach { once(it) }
        val meanMs = if (windows.size > 1) (SystemClock.elapsedRealtime() - begin) / (windows.size - 1) else -1
        return "loadMs=$loadMs firstWindowMs=$firstMs meanWindowMs=$meanMs windows=${windows.size} audioS=${"%.2f".format(samples.size.toDouble() / AudioInput.SAMPLE_RATE)} " +
            "top=${top.joinToString("|").replace(' ', '_')}"
    }

    /** Character error rate against `macawProbeRef`, or only the length of the answer. zh-CN: 与 `macawProbeRef` 比较的字错率, 或只报告结果长度. */
    private fun judge(text: String, sampleCount: Int): String {
        val reference = text("macawProbeRef")
        val seconds = "%.2f".format(sampleCount.toDouble() / AudioInput.SAMPLE_RATE)
        val head = text.trim().take(SpeechDevice.argument("macawProbeHead")?.toIntOrNull() ?: 48).replace(' ', '_')
        return if (reference.isNotBlank()) "audioS=$seconds cer=${"%.3f".format(SpeechDevice.cer(reference, text))} head=$head" else "audioS=$seconds chars=${text.trim().length} head=$head"
    }

    /** A text argument, sent base64 encoded as `<name>64`. zh-CN: 以 base64 编码传入 (`<name>64`) 的文本参数. */
    private fun text(name: String): String =
        SpeechDevice.argument(name + "64")?.let { String(android.util.Base64.decode(it, android.util.Base64.DEFAULT), Charsets.UTF_8) }.orEmpty()

    /** `macawProbeWav`: a file in `/data/local/tmp` (copied) or under the plugin's cache (`cache:<name>`). zh-CN: 测试录音. */
    private fun wav(): File {
        val name = SpeechDevice.argument("macawProbeWav").orEmpty()
        assertTrue("give macawProbeWav for a recognizer", name.isNotBlank())
        if (name.startsWith("cache:")) return File(SpeechDevice.context.cacheDir, "probe-out/${name.removePrefix("cache:")}")
        val target = File(SpeechDevice.context.cacheDir, "probe-wav/${name.substringAfterLast('/')}")
        shellCopy(name, target)
        return target
    }

    private fun samples(file: File): FloatArray {
        val chunks = ArrayList<FloatArray>()
        WavReader(file.inputStream(), file.length()).use { reader -> AudioInput.decode(reader) { chunks += it } }
        return chunks.fold(FloatArray(0)) { all, piece -> all + piece }
    }

    /**
     * Copies a pushed directory through the shell user (an app cannot read `/data/local/tmp`); a directory of the plugin's
     * own data, such as an installed model, is read where it is (the shell user cannot read it).
     * zh-CN: 经 shell 用户复制推送的目录; 插件自身数据中的目录 (如已安装的模型) 原地读取 (shell 用户读不到).
     */
    private fun copy(source: String, target: File): File {
        val own = File(source)
        if (own.canonicalPath.startsWith(SpeechDevice.context.dataDir.canonicalPath + File.separator)) {
            assertTrue("$source has no files", own.walkTopDown().any { it.isFile })
            return File(source)
        }
        target.deleteRecursively()
        val files = shell("find $source -type f").lines().map { it.trim() }.filter { it.startsWith(source) }
        assertTrue("$source has no files", files.isNotEmpty())
        files.forEach { path -> shellCopy(path, File(target, path.removePrefix(source).trimStart('/'))) }
        return target
    }

    private fun shellCopy(path: String, target: File) {
        target.parentFile?.mkdirs()
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cat $path")).use { input ->
            FileOutputStream(target).use { input.copyTo(it, 1 shl 20) }
        }
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes().toString(Charsets.UTF_8) }

    private fun writeWav(file: File, samples: FloatArray, sampleRate: Int) {
        FileOutputStream(file).use { out ->
            val bytes = samples.size * 2
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVE".toByteArray()).put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(bytes)
            out.write(header.array())
            val body = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { body.putShort((it.coerceIn(-1f, 1f) * 32767).toInt().toShort()) }
            out.write(body.array())
        }
    }

    companion object {
        private const val AUDIBLE = 0.02f
    }
}
