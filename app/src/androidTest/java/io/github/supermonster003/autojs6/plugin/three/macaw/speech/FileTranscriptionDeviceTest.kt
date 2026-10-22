package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.app.Notification
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.SpeechDevice.STREAMING
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileProblem
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.TranscriptExport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.FileRecognitionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileDescription
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileRun
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileRunOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileTranscription
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.TryoutActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.TryoutController
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * The file tab of the tryout screen on a device (roadmap D110 - D116, P7.8.4): the system picker is the maintainer's
 * step, so the tests hand files to [FileTranscription] the way the picker result does. A missing model fails with
 * `MODEL_MISSING` and the cached JSON equals the shown record; the foreground service runs as `mediaProcessing` on
 * Android 15+, `dataSync` on 10 - 14 and without a type before, survives leaving the screen and leaves a result
 * notification that names no file; MP3 and Ogg Opus give the text of `ISpeechPlugin.recognizeFile` with the same
 * options while the progress only grows up to the declared length; a pipe is copied to the cache, gives the same text
 * and the copy is deleted; a cancel keeps the segments so far; the switch turns the automatic punctuation on and off;
 * the exports reach a document unchanged; the tab shows segments, actions, the localized hint with the English
 * reason, and no result notification while it is visible. The model resolves before the file is decoded, so the
 * recognition cases need the streaming model of [SpeechModelSetupDeviceTest] (skipped otherwise) and the punctuation
 * case also the models of [PunctuationDeviceTest]. Notifications are checked only where the maintainer allowed them.
 * Results are `MacawDeviceRun` lines; screenshots go to the external files folder `screens/`.
 * zh-CN: 设备上的试听试说页文件标签 (路线图 D110 - D116, P7.8.4): 系统文件选择器由维护者操作, 测试按选择结果的方式把文件交给 [FileTranscription].
 * 缺少模型时以 `MODEL_MISSING` 失败, 缓存的 JSON 与显示的记录相同; 前台服务在 Android 15+ 为 `mediaProcessing`, 10 - 14 为 `dataSync`, 更早无类型,
 * 离开页面后继续, 结束时留下不含文件名的结果通知; MP3 与 Ogg Opus 的文字与相同选项下 `ISpeechPlugin.recognizeFile` 的一致, 进度只增不减并到达
 * 声明的时长; 管道先复制到缓存, 文字相同, 副本随后删除; 取消保留已有分段; 开关控制自动标点; 导出的内容原样写入文档; 标签显示分段, 操作按钮,
 * 本地化提示与英文原因, 页面可见时不发结果通知. 模型先于解码解析, 因此识别用例需要 [SpeechModelSetupDeviceTest] 安装的流式模型 (否则跳过),
 * 标点用例还需要 [PunctuationDeviceTest] 的模型. 只在维护者允许通知时检查通知. 结果为 `MacawDeviceRun` 日志行, 截图保存到外部文件目录 `screens/`.
 */
@RunWith(AndroidJUnit4::class)
class FileTranscriptionDeviceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val transcription = FileTranscription.get(context)
    private val cached = File(context.cacheDir, "tryout/${FileTranscription.RESULT_NAME}")
    private val input = File(context.cacheDir, "tryout/${FileTranscription.INPUT_NAME}")
    private val notifications: Boolean get() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    @Before
    fun idle() {
        // A run left by a failed case. zh-CN: 失败用例留下的识别.
        main { transcription.cancel() }
        await("no run in progress", 120_000) { main { transcription.run } == null }
        main { transcription.clear() }
        await("the cache is empty", 5_000) { !cached.exists() }
        FileRecognitionService.clearResult(context)
    }

    @Test
    fun aMissingModelFailsAndTheCacheHoldsTheRecord() {
        val file = SpeechDevice.asset("encoded-audio/en.mp3")
        val record = recognize(FileSource.of(file), options(MISSING))
        assertEquals(FileTranscriptRecord.State.FAILED, record.state)
        assertEquals(SpeechErrorCodes.MODEL_MISSING, record.failure?.code)
        assertEquals(FileProblem.MODEL_MISSING, record.failure?.problem)
        assertEquals("en.mp3", record.displayName)
        assertEquals(file.length(), record.sizeBytes)
        assertTrue(record.segments.isEmpty())
        assertEquals("the cache holds the shown record", record, cachedRecord())
        assertFalse("no copy is left", input.exists())
        SpeechDevice.log("file missingModel state=${record.state} code=${record.failure?.code} cachedEqual=true")
    }

    @Test
    fun theServiceTypeFollowsTheAndroidVersionAndNoNotificationNamesTheFile() {
        val file = SpeechDevice.asset("encoded-audio/en.mp3")
        val gate = CountDownLatch(1)
        // Holds the run in PREPARING until the screen is gone. zh-CN: 让识别停在 PREPARING, 直到页面关闭.
        val source = object : FileSource {
            override fun describe() = FileDescription(SECRET_NAME, file.length())

            override fun open(): ParcelFileDescriptor {
                gate.await(60, TimeUnit.SECONDS)
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }
        val expected = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else -> 0
        }
        val shown = notifications
        var type: Int? = null
        var ongoing: Notification? = null
        try {
            ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use {
                // The run starts from the visible screen, as after the picker. zh-CN: 与选择文件后一样, 从可见的页面开始识别.
                assertTrue(main { transcription.start(source, options(MISSING)) })
                assertFalse("one file at a time", main { transcription.start(source, options(MISSING)) })
                await("the foreground service runs", 10_000) { main { FileRecognitionService.runningType } != null }
                type = main { FileRecognitionService.runningType }
                assertEquals(expected, type)
                if (shown) {
                    await("the ongoing notification", 5_000) { active(FileRecognitionService.RUNNING_ID) != null }
                    ongoing = active(FileRecognitionService.RUNNING_ID)
                }
            }
        } finally {
            gate.countDown()
        }
        // The screen is gone; the run goes on. zh-CN: 页面已关闭, 识别继续.
        val record = finished()
        assertEquals(FileTranscriptRecord.State.FAILED, record.state)
        assertEquals(SECRET_NAME, record.displayName)
        await("the service stops", 10_000) { main { FileRecognitionService.runningType } == null }
        var result: Notification? = null
        if (shown) {
            await("the result notification", 5_000) { active(FileRecognitionService.RESULT_ID) != null }
            result = active(FileRecognitionService.RESULT_ID)
            assertNull("the ongoing notification is gone", active(FileRecognitionService.RUNNING_ID))
            listOf(ongoing!!, result!!).forEach { notification ->
                assertFalse("no notification names the file", texts(notification).contains(SECRET))
                assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
            }
            assertTrue("the result can be dismissed", result.flags and Notification.FLAG_ONGOING_EVENT == 0)
        }
        FileRecognitionService.clearResult(context)
        SpeechDevice.log(
            "file service sdk=${Build.VERSION.SDK_INT} type=$type expected=$expected notifications=$shown " +
                "ongoingActions=${ongoing?.actions?.size} resultShown=${result != null}",
        )
    }

    @Test
    fun encodedFilesAndPipesGiveTheTextOfTheScriptApi() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(STREAMING))
        listOf("en.mp3", "en.opus").forEach { name ->
            val file = SpeechDevice.asset("encoded-audio/$name")
            val samples = ArrayList<FileRun>()
            val record = recognize(FileSource.of(file), options(STREAMING), samples)
            assertEquals("$name: ${record.failure}", FileTranscriptRecord.State.COMPLETED, record.state)
            assertTrue("$name: segments", record.segments.isNotEmpty())
            assertEquals("$name: the text of recognizeFile", script(file), record.text)
            val progress = samples.map { it.recognizedMillis }
            assertEquals("$name: the progress never goes back", progress.sorted(), progress)
            assertTrue("$name: the run was seen recognizing", samples.any { it.phase == FileRun.Phase.RUNNING })
            val declared = record.declaredMillis
            assertNotNull("$name: the declared length", declared)
            assertTrue("$name: recognized ${record.recognizedMillis} of $declared", abs(record.recognizedMillis - declared!!) <= maxOf(500L, declared / 20))
            assertTrue("$name: the last progress ${progress.lastOrNull()} is within the record", (progress.lastOrNull() ?: 0) <= record.recognizedMillis)
            SpeechDevice.log(
                "file encoded name=$name segments=${record.segments.size} declared=$declared recognized=${record.recognizedMillis} " +
                    "samples=${samples.size} cer=${"%.3f".format(SpeechDevice.cer(SpeechDevice.EN_REFERENCE, record.text))} sameAsScript=true text=${record.text}",
            )
        }
        // Some cloud providers return a pipe: it is copied to the cache first. zh-CN: 部分云盘返回管道: 先复制到缓存.
        val file = SpeechDevice.asset("encoded-audio/en.mp3")
        val piped = recognize(pipe(file), options(STREAMING))
        assertEquals("${piped.failure}", FileTranscriptRecord.State.COMPLETED, piped.state)
        assertEquals("a pipe gives the text of the file", script(file), piped.text)
        assertNull(piped.displayName)
        assertNull(piped.sizeBytes)
        assertFalse("the copy is deleted", input.exists())

        // The exports reach a document unchanged (D116). zh-CN: 导出内容原样写入文档 (D116).
        val controller = main { TryoutController(context) }
        listOf("txt" to TranscriptExport.text(piped.segments), "srt" to TranscriptExport.srt(piped.segments)).forEach { (extension, bytes) ->
            val target = File(context.cacheDir, "device-test/file-tab-export.$extension")
            target.parentFile?.mkdirs()
            target.writeText("an older and longer content that the export must replace completely")
            val written = CountDownLatch(1)
            var ok = false
            main { controller.writeDocument(Uri.fromFile(target), bytes) { ok = it; written.countDown() } }
            assertTrue(written.await(10, TimeUnit.SECONDS))
            assertTrue("$extension written", ok)
            assertArrayEquals("$extension: the document holds the export", bytes, target.readBytes())
            target.delete()
        }
        val srt = String(TranscriptExport.srt(piped.segments))
        SpeechDevice.log("file pipe segments=${piped.segments.size} sameAsScript=true copyDeleted=true export=txt,srt srtCues=${srt.lines().count { it.contains(" --> ") }}")
    }

    @Test
    fun aCancelKeepsTheSegmentsSoFar() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(STREAMING))
        // About 100 s: the clip twelve times with 1.5 s of silence after each. zh-CN: 约 100 秒: 片段重复 12 次, 每次后接 1.5 秒静音.
        val clip = SpeechDevice.samples("test-audio/en.wav")
        val round = clip.size + 24_000
        val long = FloatArray(round * 12).also { samples -> repeat(12) { clip.copyInto(samples, it * round) } }
        val file = File(context.cacheDir, "device-test/file-tab-long.wav")
        SpeechDevice.writeWav(file, long)
        try {
            assertTrue(main { transcription.start(FileSource.of(file), options(STREAMING)) })
            await("the first segment", 120_000) { main { transcription.run?.segments?.isNotEmpty() ?: true } }
            main { transcription.cancel() }
            assertEquals(FileRun.Phase.CANCELLING, main { transcription.run?.phase })
            val record = finished()
            assertEquals(FileTranscriptRecord.State.CANCELLED, record.state)
            assertNull(record.failure)
            assertTrue("the segments so far", record.segments.isNotEmpty())
            val declared = record.declaredMillis ?: 0
            assertTrue("stopped early: ${record.recognizedMillis} of $declared", record.recognizedMillis < declared - 10_000)
            assertEquals("the cache holds the cancelled record", record, cachedRecord())
            SpeechDevice.log("file cancel segments=${record.segments.size} recognized=${record.recognizedMillis} declared=$declared")
        } finally {
            file.delete()
        }
    }

    @Test
    fun theSwitchTurnsTheAutomaticPunctuationOnAndOff() {
        assumeTrue(
            "install the models first (SpeechModelSetupDeviceTest, -e macawModels true -e macawModelIds ...)",
            (listOf(STREAMING) + PunctuationDeviceTest.MODELS).all(SpeechDevice::installed),
        )
        val file = SpeechDevice.asset("encoded-audio/en.mp3")
        val on = recognize(FileSource.of(file), options(STREAMING, punctuation = true))
        val off = recognize(FileSource.of(file), options(STREAMING, punctuation = false))
        assertEquals(FileTranscriptRecord.State.COMPLETED, on.state)
        assertEquals(FileTranscriptRecord.State.COMPLETED, off.state)
        assertTrue(on.punctuation)
        assertFalse(off.punctuation)
        val marks = on.text.count { it in MARKS }
        assertTrue("punctuation on: marks", marks > 0)
        assertEquals("punctuation off: no marks", 0, off.text.count { it in MARKS })
        SpeechDevice.log("file punctuation marksOn=$marks marksOff=0 text=${on.text}")
    }

    @Test
    fun theTabShowsSegmentsActionsAndTheReasonOfAFailure() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(STREAMING))
        val prefix = SpeechDevice.argument("macawPrefix") ?: "file"
        ActivityScenario.launch<TryoutActivity>(TryoutActivity.fileIntent(context)).use {
            compose.waitUntil(15_000) { exists("macaw.tryout.file.pick") }
            assertTrue(exists("macaw.tryout.file.model"))
            assertTrue(exists("macaw.tryout.file.punctuation"))
            capture(prefix, "empty")
            val file = SpeechDevice.asset("encoded-audio/en.mp3")
            assertTrue(main { transcription.start(FileSource.of(file), options(STREAMING)) })
            val record = finished()
            assertEquals(FileTranscriptRecord.State.COMPLETED, record.state)
            compose.waitUntil(10_000) { exists("macaw.tryout.file.summary") }
            compose.waitForIdle()
            val shown = record.segments.count { it.text.isNotBlank() }
            assertTrue(exists("macaw.tryout.file.segment.0"))
            assertTrue(exists("macaw.tryout.file.segment.${shown - 1}"))
            assertFalse(exists("macaw.tryout.file.segment.$shown"))
            listOf("name", "saveText", "saveSrt", "copy", "clear").forEach { assertTrue(it, exists("macaw.tryout.file.$it")) }
            assertFalse("the run is over", exists("macaw.tryout.file.cancel"))
            compose.onNodeWithTag("macaw.tryout.file.saveSrt", useUnmergedTree = true).performScrollTo()
            capture(prefix, "result")
            assertNull("no result notification while the tab is visible", active(FileRecognitionService.RESULT_ID))

            compose.onNodeWithTag("macaw.tryout.file.clear", useUnmergedTree = true).performScrollTo().performClick()
            compose.waitUntil(5_000) { !exists("macaw.tryout.file.summary") }
            assertFalse(exists("macaw.tryout.file.segment.0"))
            assertTrue(exists("macaw.tryout.file.pick"))
            await("the cache is cleared", 5_000) { !cached.exists() }

            // A video without an audio track. zh-CN: 没有音轨的视频.
            val video = SpeechDevice.asset("encoded-audio/video-only.mp4")
            assertTrue(main { transcription.start(FileSource.of(video), options(STREAMING)) })
            val failed = finished()
            assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, failed.failure?.code)
            compose.waitUntil(10_000) { exists("macaw.tryout.file.error") }
            assertTrue(exists("macaw.tryout.file.errorHint"))
            val detail = text("macaw.tryout.file.errorDetail").orEmpty()
            assertTrue(detail, detail.startsWith("${SpeechErrorCodes.UNSUPPORTED_FORMAT}: "))
            assertFalse("no save for a failure", enabled("macaw.tryout.file.saveSrt"))
            compose.onNodeWithTag("macaw.tryout.file.errorDetail", useUnmergedTree = true).performScrollTo()
            capture(prefix, "error")
            SpeechDevice.log("file tab segments=$shown summary=true clear=true errorDetail=$detail")
        }
    }

    // Helpers ----------------------------------------------------------------------------------------------------

    private fun options(model: String, punctuation: Boolean = false) = FileRunOptions(model, null, punctuation, vad = true)

    /** Runs one file to its end, sampling the run every 50 ms. zh-CN: 识别一个文件直到结束, 每 50 毫秒采样一次. */
    private fun recognize(source: FileSource, options: FileRunOptions, samples: MutableList<FileRun>? = null): FileTranscriptRecord {
        assertTrue("a run starts", main { transcription.start(source, options) })
        return finished(samples)
    }

    private fun finished(samples: MutableList<FileRun>? = null): FileTranscriptRecord {
        var record: FileTranscriptRecord? = null
        await("the run ends", 300_000) {
            val (run, last) = main { transcription.run to transcription.record }
            run?.let { samples?.add(it) }
            record = last.takeIf { run == null }
            record != null
        }
        return record!!
    }

    /** The same options through the script API. zh-CN: 相同选项经脚本接口识别. */
    private fun script(file: File): String {
        val answer = SpeechDevice.recognize(file, SpeechDevice.request {
            putString(SpeechOptionKeys.MODEL, STREAMING)
            putBoolean(SpeechOptionKeys.PUNCTUATION, false)
            putBoolean(SpeechOptionKeys.VAD, true)
        })
        assertNull("${file.name}: ${answer.getString(SpeechContract.KEY_ERROR_MESSAGE)}", answer.getString(SpeechContract.KEY_ERROR_CODE))
        return answer.getString(SpeechResultKeys.TEXT).orEmpty()
    }

    /** A source that streams the file through a pipe and tells nothing about itself. zh-CN: 经管道传送文件且不提供自身信息的来源. */
    private fun pipe(file: File) = object : FileSource {
        override fun describe() = FileDescription(null, null)

        override fun open(): ParcelFileDescriptor {
            val (read, write) = ParcelFileDescriptor.createPipe()
            Thread {
                // A reader that stops early breaks the pipe; that must not end the process. zh-CN: 读取方提前停止会断开管道, 不能因此结束进程.
                runCatching { ParcelFileDescriptor.AutoCloseOutputStream(write).use { output -> file.inputStream().use { it.copyTo(output) } } }
            }.apply { isDaemon = true }.start()
            return read
        }
    }

    private fun cachedRecord(): FileTranscriptRecord? = FileTranscriptRecord.parse(cached.readBytes())

    private fun <T> main(block: () -> T): T {
        var value: Result<T>? = null
        instrumentation.runOnMainSync { value = runCatching(block) }
        return value!!.getOrThrow()
    }

    private fun await(what: String, timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            assertTrue("timed out: $what", System.currentTimeMillis() < deadline)
            Thread.sleep(50)
        }
    }

    private fun active(id: Int): Notification? = context.getSystemService(NotificationManager::class.java).activeNotifications.firstOrNull { it.id == id }?.notification

    private fun texts(notification: Notification): String = listOf(
        Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT, Notification.EXTRA_SUB_TEXT, Notification.EXTRA_INFO_TEXT,
    ).mapNotNull { notification.extras.getCharSequence(it)?.toString() }.plus(notification.tickerText?.toString().orEmpty()).joinToString("\n")

    private fun SemanticsNodeInteractionsProvider.nodes(tag: String) = onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()

    private fun exists(tag: String): Boolean = compose.nodes(tag).isNotEmpty()

    private fun enabled(tag: String): Boolean = compose.nodes(tag).firstOrNull()?.let { !it.config.contains(SemanticsProperties.Disabled) } ?: false

    private fun text(tag: String): String? = compose.nodes(tag).firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }

    private fun capture(prefix: String, name: String) {
        compose.waitForIdle()
        Thread.sleep(500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val folder = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }
        val file = File(folder, "$prefix-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        SpeechDevice.log("screen ${file.name} ${bitmap.width}x${bitmap.height}")
    }

    private companion object {
        const val MISSING = "asr.not-installed.file-tab-test"
        const val SECRET = "macaw-secret-4711"
        const val SECRET_NAME = "$SECRET.mp3"
        const val MARKS = ",.?!;:"
    }
}
