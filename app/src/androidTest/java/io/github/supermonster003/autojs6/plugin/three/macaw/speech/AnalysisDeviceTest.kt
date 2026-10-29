package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAnalysis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechModels
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileRunOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileTranscription
import org.autojs.plugin.speech.api.ISpeechProgressCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * The offline analyses on the device (roadmap P8.4.5, D146 - D153) with the recordings of `locks/analysis-test-audio.lock`
 * and the models of the development catalog (`py .python/device_catalog_v3.py`): diarization of two LibriSpeech readers
 * through the plugin's Binder (contract version 2, both readers found, at least 90 % of the speech given to the right
 * one, the enrolled reader named by identify), language identification of Chinese, Russian and English, tagging of the
 * made sounds (each label clearest in its own window), a cancel answered at once while the next analysis still runs,
 * and the file tab's "diarize and transcribe", language and tag actions with their cached records. With `-e macawLong
 * true` the three analyses also run on 10 minutes of audio (the timing of D152). Every case is skipped when its models
 * are not installed. Results are `MacawDeviceRun` lines with numbers only.
 * zh-CN: 设备上的离线分析 (路线图 P8.4.5, D146 - D153), 使用 `locks/analysis-test-audio.lock` 的录音与开发目录的模型 (`py .python/device_catalog_v3.py`):
 * 经插件 Binder (契约版本 2) 分离两位 LibriSpeech 朗读者 (找到两人, 至少 90% 的语音归给正确的人, 认人能说出已注册的朗读者), 识别中文, 俄语与英语,
 * 给合成声音打标签 (每个标签在自己的窗口最明显), 取消立即应答而下一个分析照常运行, 以及文件标签的 "分离 + 转写", 语种与标签操作及其缓存记录.
 * 加 `-e macawLong true` 时三种分析还在 10 分钟音频上运行 (D152 的计时). 模型未安装时跳过相应用例. 结果为只含数值的 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
class AnalysisDeviceTest {

    private val context = SpeechDevice.context
    private val plugin = SpeechDevice.plugin
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val scratch = File(context.cacheDir, "analysis-device-test")

    @After
    fun cleanUp() {
        plugin.removeSpeaker(request { putString(SpeechOptionKeys.NAME, READER_A) })
        scratch.deleteRecursively()
    }

    private fun request(build: Bundle.() -> Unit = {}): Bundle = Bundle().apply {
        putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CALL_VERSION_2)
        build()
    }

    /** One analysis through the Binder. zh-CN: 经 Binder 的一次分析. */
    private class Operation : ISpeechProgressCallback.Stub() {
        val result = AtomicReference<Bundle?>()
        val error = AtomicReference<Bundle?>()
        val reports = AtomicInteger()
        val finished = CountDownLatch(1)
        var id = 0

        override fun onProgress(opId: Int, done: Long, total: Long) {
            if (done > 0) reports.incrementAndGet()
        }

        override fun onCompleted(opId: Int, result: Bundle?) {
            this.result.set(result)
            finished.countDown()
        }

        override fun onFailed(opId: Int, error: Bundle?) {
            this.error.set(error)
            finished.countDown()
        }

        fun await(seconds: Long): Bundle {
            assertTrue("no answer within $seconds s", finished.await(seconds, TimeUnit.SECONDS))
            error.get()?.let { throw AssertionError("${it.getString(SpeechContract.KEY_ERROR_CODE)}: ${it.getString(SpeechContract.KEY_ERROR_MESSAGE)}") }
            return result.get()!!.also { assertEquals(SpeechContract.CALL_VERSION_2, it.getInt(SpeechContract.KEY_CONTRACT_VERSION)) }
        }
    }

    private fun start(file: File, options: Bundle, call: (ParcelFileDescriptor, Bundle, ISpeechProgressCallback) -> Int): Operation {
        val operation = Operation()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { operation.id = call(it, options, operation) }
        return operation
    }

    private fun diarize(file: File, options: Bundle) = start(file, options) { audio, request, callback -> plugin.diarizeFile(audio, request, callback) }

    private fun language(file: File, options: Bundle = request()) = start(file, options) { audio, request, callback -> plugin.detectLanguage(audio, request, callback) }

    private fun tag(file: File, options: Bundle = request()) = start(file, options) { audio, request, callback -> plugin.tagAudio(audio, request, callback) }

    /** The true turns of `two-speakers.wav`: start, end and reader. zh-CN: `two-speakers.wav` 的真实轮次. */
    private fun truth(): List<Triple<Long, Long, String>> {
        val turns = JSONObject(SpeechDevice.asset("$FOLDER/two-speakers.json").readText()).getJSONArray("turns")
        return (0 until turns.length()).map { turns.getJSONObject(it).let { t -> Triple(t.getLong("startMillis"), t.getLong("endMillis"), t.getString("speaker")) } }
    }

    /**
     * Each found speaker mapped to the reader it overlaps most, and the share of the true speech given to the right
     * reader. zh-CN: 每个找到的说话人映射到与其重叠最多的朗读者, 以及正确归属的真实语音比例.
     */
    private fun score(starts: LongArray, ends: LongArray, speakers: IntArray): Pair<Map<Int, String>, Double> {
        val truth = truth()
        fun overlap(a: Long, b: Long, c: Long, d: Long) = maxOf(0L, minOf(b, d) - maxOf(a, c))
        val mapping = speakers.distinct().associateWith { speaker ->
            truth.groupBy { it.third }.maxByOrNull { (_, turns) ->
                turns.sumOf { t -> starts.indices.filter { speakers[it] == speaker }.sumOf { overlap(starts[it], ends[it], t.first, t.second) } }
            }!!.key
        }
        val right = truth.sumOf { t -> starts.indices.filter { mapping[speakers[it]] == t.third }.sumOf { overlap(starts[it], ends[it], t.first, t.second) } }
        return mapping to right.toDouble() / truth.sumOf { it.second - it.first }
    }

    @Test
    fun diarizationFindsBothReadersAndNamesTheEnrolledOne() {
        assumeTrue("the segmentation and speaker models are installed", SpeechDevice.installed(SEGMENT) && SpeechDevice.installed(SPEAKER))
        val enroll = (1..3).map { ParcelFileDescriptor.open(SpeechDevice.asset("speaker-audio/libri-a-enroll-$it.wav"), ParcelFileDescriptor.MODE_READ_ONLY) }
        try {
            val enrolled = plugin.enrollSpeaker(enroll.toTypedArray(), request { putString(SpeechOptionKeys.NAME, READER_A) })
            assertNull(enrolled.getString(SpeechContract.KEY_ERROR_MESSAGE), enrolled.getString(SpeechContract.KEY_ERROR_CODE))
        } finally {
            enroll.forEach { it.close() }
        }
        val memory = PeakMemory().apply { start() }
        val started = SystemClock.elapsedRealtime()
        val operation = diarize(SpeechDevice.asset("$FOLDER/two-speakers.wav"), request { putBoolean(SpeechOptionKeys.IDENTIFY, true) })
        val result = operation.await(300)
        val elapsed = SystemClock.elapsedRealtime() - started
        val peak = memory.stop()
        val starts = result.getLongArray(SpeechResultKeys.SEGMENT_STARTS)!!
        val ends = result.getLongArray(SpeechResultKeys.SEGMENT_ENDS)!!
        val speakers = result.getIntArray(SpeechResultKeys.SEGMENT_SPEAKERS)!!
        val (mapping, accuracy) = score(starts, ends, speakers)
        @Suppress("DEPRECATION")
        val names = result.getParcelableArrayList<Bundle>(SpeechResultKeys.NAMES).orEmpty()
        SpeechDevice.log(
            "analysis diarize speakers=${result.getInt(SpeechResultKeys.SPEAKER_COUNT)} turns=${starts.size} accuracy=${"%.3f".format(accuracy)} " +
                "elapsedMillis=$elapsed durationMillis=${result.getLong(SpeechResultKeys.DURATION_MILLIS)} progress=${operation.reports.get()} " +
                "peakMiB=${peak.mib} (${peak.method}) names=${names.map { "${it.getInt(SpeechResultKeys.SPEAKER)}:${it.getString(SpeechResultKeys.NAME) != null}:${it.getFloat(SpeechResultKeys.SCORE)}" }}",
        )
        assertEquals(2, result.getInt(SpeechResultKeys.SPEAKER_COUNT))
        assertEquals(SEGMENT, result.getString(SpeechResultKeys.MODEL_ID))
        assertEquals("each reader has a speaker", setOf("a", "b"), mapping.values.toSet())
        assertTrue("accuracy $accuracy", accuracy >= MIN_ACCURACY)
        assertTrue("progress was reported", operation.reports.get() > 0)
        val named = names.associate { it.getInt(SpeechResultKeys.SPEAKER) to it.getString(SpeechResultKeys.NAME) }
        mapping.forEach { (speaker, reader) -> assertEquals("speaker $speaker ($reader)", if (reader == "a") READER_A else null, named[speaker]) }
    }

    @Test
    fun languageIdentificationTellsChineseRussianAndEnglish() {
        assumeTrue("the language identification model is installed", SpeechDevice.installed(LANGID))
        for ((name, expected) in listOf("$FOLDER/language-zh.wav" to "zh", "$FOLDER/language-ru.wav" to "ru", "test-audio/en.wav" to "en")) {
            val started = SystemClock.elapsedRealtime()
            val result = language(SpeechDevice.asset(name), request { putString(SpeechOptionKeys.MODEL, LANGID) }).await(120)
            @Suppress("DEPRECATION")
            val windows = result.getParcelableArrayList<Bundle>(SpeechResultKeys.SEGMENTS).orEmpty()
            SpeechDevice.log("analysis language ${name.substringAfterLast('/')} found=${result.getString(SpeechResultKeys.LANGUAGE)} windows=${windows.size} elapsedMillis=${SystemClock.elapsedRealtime() - started}")
            assertEquals(name, expected, result.getString(SpeechResultKeys.LANGUAGE))
            assertTrue("at least one window", windows.isNotEmpty())
        }
    }

    @Test
    fun taggingNamesTheMadeSoundsInTheirWindows() {
        assumeTrue("the tagging model is installed", SpeechDevice.installed(TAGGER))
        val started = SystemClock.elapsedRealtime()
        val result = tag(SpeechDevice.asset("$FOLDER/sounds.wav"), request { putInt(SpeechOptionKeys.TOP_K, 10); putBoolean(SpeechOptionKeys.TIMELINE, true) }).await(120)
        @Suppress("DEPRECATION")
        val labels = result.getParcelableArrayList<Bundle>(SpeechResultKeys.LABELS).orEmpty().associateBy { it.getString(SpeechResultKeys.LABEL) }
        SpeechDevice.log(
            "analysis tag labels=${labels.size} windows=${result.getLongArray(SpeechResultKeys.TIMELINE_STARTS)?.size} elapsedMillis=${SystemClock.elapsedRealtime() - started} " +
                EXPECTED_TAGS.keys.joinToString(" ") { "$it=${labels[it]?.getFloat(SpeechResultKeys.SCORE)}@${labels[it]?.getLong(SpeechResultKeys.START_MILLIS)}" },
        )
        assertEquals(3, result.getLongArray(SpeechResultKeys.TIMELINE_STARTS)?.size)
        EXPECTED_TAGS.forEach { (label, window) ->
            val found = labels[label] ?: throw AssertionError("$label not among ${labels.keys}")
            assertEquals("$label is clearest in its window", window, found.getLong(SpeechResultKeys.START_MILLIS))
        }
    }

    @Test
    fun aCancelAnswersAtOnceAndTheNextAnalysisRuns() {
        assumeTrue("the segmentation, speaker and tagging models are installed", SpeechDevice.installed(SEGMENT) && SpeechDevice.installed(SPEAKER) && SpeechDevice.installed(TAGGER))
        val long = repeated(LONG_CANCEL_MILLIS)
        val running = diarize(long, request())
        Thread.sleep(1500)
        val cancelledAt = SystemClock.elapsedRealtime()
        plugin.cancelOperation(running.id)
        assertTrue("the cancel answers", running.finished.await(5, TimeUnit.SECONDS))
        val answerMillis = SystemClock.elapsedRealtime() - cancelledAt
        assertEquals(SpeechErrorCodes.SESSION_CANCELLED, running.error.get()?.getString(SpeechContract.KEY_ERROR_CODE))
        // The diarization runs to its end in the background (D150); the tagging queues behind it. zh-CN: 分离在后台运行到结束; 标签排在其后.
        val queuedAt = SystemClock.elapsedRealtime()
        tag(SpeechDevice.asset("$FOLDER/sounds.wav")).await(600)
        SpeechDevice.log("analysis cancel answerMillis=$answerMillis nextMillis=${SystemClock.elapsedRealtime() - queuedAt}")
        assertTrue("answered within a second, $answerMillis ms", answerMillis < 1000)
    }

    @Test
    fun theFileTabDiarizesAndTranscribesEachTurn() {
        assumeTrue("the segmentation and speaker models are installed", SpeechDevice.installed(SEGMENT) && SpeechDevice.installed(SPEAKER))
        val recognizer = SpeechModels.get(context).list(kind = SpeechContract.KIND_ASR, installed = true).firstOrNull { "en" in it.languages }
        assumeTrue("an English recognition model is installed", recognizer != null)
        val transcription = FileTranscription.get(context)
        idle(transcription)
        val file = SpeechDevice.asset("$FOLDER/two-speakers.wav")
        val options = FileRunOptions(recognizer!!.id, null, punctuation = false, vad = true, action = FileAction.DIARIZE_TRANSCRIBE)
        val record = run(transcription, FileSource.of(file), options, 600_000)
        val speakers = record.analysis as FileAnalysis.Speakers
        SpeechDevice.log("analysis file diarize+transcribe state=${record.state} speakers=${speakers.speakers} turns=${speakers.turns.size} segments=${record.segments.size} model=${record.modelId}")
        assertEquals(FileTranscriptRecord.State.COMPLETED, record.state)
        assertEquals(FileAction.DIARIZE_TRANSCRIBE, record.action)
        assertEquals(2, speakers.speakers)
        assertTrue("segments", record.segments.isNotEmpty())
        assertEquals(record.segments.size, record.segmentSpeakers.size)
        // Each segment lies in a turn of its speaker. zh-CN: 每个分段都落在其说话人的轮次内.
        record.segments.forEachIndexed { index, segment ->
            val middle = (segment.startMillis + segment.endMillis) / 2
            assertTrue("segment $index", speakers.turns.any { it.speaker == record.segmentSpeakers[index] && middle in it.startMillis - 500..it.endMillis + 500 })
        }
        assertEquals("the cache holds the record", record, FileTranscriptRecord.parse(File(context.cacheDir, "tryout/${FileTranscription.RESULT_NAME}").readBytes()))

        if (SpeechDevice.installed(LANGID)) {
            val language = run(transcription, FileSource.of(SpeechDevice.asset("$FOLDER/language-ru.wav")), FileRunOptions(null, null, false, true, FileAction.LANGUAGE, languageModel = LANGID), 120_000)
            assertEquals("ru", (language.analysis as FileAnalysis.Languages).language)
        }
        if (SpeechDevice.installed(TAGGER)) {
            val tags = run(transcription, FileSource.of(SpeechDevice.asset("$FOLDER/sounds.wav")), FileRunOptions(null, null, false, true, FileAction.TAG), 120_000)
            assertTrue("Music", (tags.analysis as FileAnalysis.Tags).labels.any { it.label.name == "Music" })
        }
        main { transcription.clear() }
    }

    @Test
    fun tenMinutesOfAudio() {
        assumeTrue("pass -e macawLong true", SpeechDevice.argument("macawLong") == "true")
        val file = repeated(TEN_MINUTES)
        if (SpeechDevice.installed(SEGMENT) && SpeechDevice.installed(SPEAKER)) {
            val memory = PeakMemory().apply { start() }
            val started = SystemClock.elapsedRealtime()
            val result = diarize(file, request()).await(1800)
            val peak = memory.stop()
            SpeechDevice.log("analysis long diarize minutes=10 elapsedMillis=${SystemClock.elapsedRealtime() - started} speakers=${result.getInt(SpeechResultKeys.SPEAKER_COUNT)} turns=${result.getLongArray(SpeechResultKeys.SEGMENT_STARTS)?.size} peakMiB=${peak.mib} (${peak.method})")
        }
        if (SpeechDevice.installed(LANGID)) {
            val started = SystemClock.elapsedRealtime()
            val result = language(file, request { putString(SpeechOptionKeys.MODEL, LANGID) }).await(1800)
            SpeechDevice.log("analysis long language minutes=10 elapsedMillis=${SystemClock.elapsedRealtime() - started} found=${result.getString(SpeechResultKeys.LANGUAGE)}")
        }
        if (SpeechDevice.installed(TAGGER)) {
            val started = SystemClock.elapsedRealtime()
            val result = tag(file).await(1800)
            @Suppress("DEPRECATION")
            SpeechDevice.log("analysis long tag minutes=10 elapsedMillis=${SystemClock.elapsedRealtime() - started} labels=${result.getParcelableArrayList<Bundle>(SpeechResultKeys.LABELS)?.size}")
        }
    }

    /** `two-speakers.wav` repeated to [millis] in a cache file. zh-CN: 把 `two-speakers.wav` 重复到 [millis] 写入缓存文件. */
    private fun repeated(millis: Long): File {
        val one = SpeechDevice.samples("$FOLDER/two-speakers.wav")
        val total = (millis * 16).toInt()
        val samples = FloatArray(total) { one[it % one.size] }
        return File(scratch, "repeated-$millis.wav").also { scratch.mkdirs(); SpeechDevice.writeWav(it, samples) }
    }

    private fun idle(transcription: FileTranscription) {
        main { transcription.cancel() }
        await("no run in progress", 600_000) { main { transcription.run } == null }
    }

    private fun run(transcription: FileTranscription, source: FileSource, options: FileRunOptions, timeoutMillis: Long): FileTranscriptRecord {
        assertTrue("a run starts", main { transcription.start(source, options) })
        await("the run ends", timeoutMillis) { main { transcription.run } == null }
        return main { transcription.record }!!
    }

    private fun <T> main(block: () -> T): T {
        var value: Result<T>? = null
        instrumentation.runOnMainSync { value = runCatching(block) }
        return value!!.getOrThrow()
    }

    private fun await(what: String, timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            assertTrue("timed out: $what", System.currentTimeMillis() < deadline)
            Thread.sleep(100)
        }
    }

    private companion object {
        const val FOLDER = "analysis-audio"
        const val SEGMENT = "segment.pyannote.3-0"
        const val SPEAKER = "speaker.eres2net.3dspeaker.en-voxceleb"
        const val LANGID = "langid.whisper.tiny.int8"
        const val TAGGER = "tagger.ced.mini.int8"
        const val READER_A = "device-test-analysis-a"
        const val MIN_ACCURACY = 0.9
        const val LONG_CANCEL_MILLIS = 120_000L
        const val TEN_MINUTES = 600_000L

        /** Each made sound and the window where it is clearest (D149). zh-CN: 每种合成声音及其最明显的窗口. */
        val EXPECTED_TAGS = mapOf("Music" to 0L, "White noise" to 10_000L, "Beep, bleep" to 20_000L)
    }
}
